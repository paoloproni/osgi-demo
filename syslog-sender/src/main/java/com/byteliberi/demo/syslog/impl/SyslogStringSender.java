package com.byteliberi.demo.syslog.impl;

import com.byteliberi.demo.producer.api.StringProducer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Syslog string sender implementation that consumes strings from the StringProducer
 * and sends them to the local syslog server via UDP port 514.
 */
@Component(immediate = true)
public class SyslogStringSender implements StringProducer.StringListener {

    private static final Logger LOGGER = Logger.getLogger(SyslogStringSender.class.getName());
    private static final String SYSLOG_HOST = "localhost";
    private static final int SYSLOG_PORT = 514;
    private static final int FACILITY = 16; // local0
    private static final int SEVERITY = 6;  // info
    private static final int PRIORITY = FACILITY * 8 + SEVERITY; // 134
    private static final String HOSTNAME = "osgi-demo";
    private static final DateTimeFormatter SYSLOG_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss");

    @Reference
    private StringProducer stringProducer;

    private DatagramSocket socket;
    private InetAddress syslogAddress;

    /**
     * Activates the syslog sender service and registers as a listener.
     */
    @Activate
    public void activate() {
        try {
            socket = new DatagramSocket();
            syslogAddress = InetAddress.getByName(SYSLOG_HOST);
            stringProducer.addListener(this);
            LOGGER.info("Syslog sender activated and registered with string producer");
        } catch (SocketException e) {
            LOGGER.severe("Failed to create DatagramSocket: " + e.getMessage());
            throw new RuntimeException("Cannot start syslog sender without UDP socket", e);
        } catch (Exception e) {
            LOGGER.severe("Failed to activate syslog sender: " + e.getMessage());
            throw new RuntimeException("Cannot start syslog sender", e);
        }
    }

    /**
     * Deactivates the syslog sender service and unregisters as a listener.
     */
    @Deactivate
    public void deactivate() {
        try {
            if (stringProducer != null) {
                stringProducer.removeListener(this);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            LOGGER.info("Syslog sender deactivated and unregistered from string producer");
        } catch (Exception e) {
            LOGGER.warning("Error during syslog sender deactivation: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStringGenerated(String value) {
        try {
            String syslogMessage = formatSyslogMessage(value);
            byte[] messageBytes = syslogMessage.getBytes(StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(
                messageBytes,
                messageBytes.length,
                syslogAddress,
                SYSLOG_PORT
            );

            socket.send(packet);

            LOGGER.info("Successfully sent string '" + value + "' to syslog");
        } catch (IOException e) {
            LOGGER.warning("Failed to send string '" + value + "' to syslog: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Unexpected error sending string to syslog: " + e.getMessage());
        }
    }

    /**
     * Formats a message in simplified syslog format.
     * Format: &lt;134&gt;MMM dd HH:mm:ss osgi-demo: MESSAGE
     *
     * @param message the message to format
     * @return the formatted syslog message
     */
    private String formatSyslogMessage(String message) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(SYSLOG_TIMESTAMP_FORMAT);

        return String.format("<%d>%s %s: %s",
            PRIORITY,
            timestamp,
            HOSTNAME,
            message
        );
    }
}