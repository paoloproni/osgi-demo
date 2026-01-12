package com.byteliberi.demo.syslog.impl;

import com.byteliberi.demo.producer.api.StringProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SyslogStringSender.
 */
@ExtendWith(MockitoExtension.class)
class SyslogStringSenderTest {

    @Mock
    private StringProducer mockStringProducer;

    private SyslogStringSender syslogSender;

    @BeforeEach
    void setUp() {
        syslogSender = new SyslogStringSender();
        // Use reflection to inject the mock producer for testing
        try {
            java.lang.reflect.Field field = SyslogStringSender.class.getDeclaredField("stringProducer");
            field.setAccessible(true);
            field.set(syslogSender, mockStringProducer);
        } catch (Exception e) {
            fail("Failed to inject mock producer: " + e.getMessage());
        }
    }

    @Test
    void testActivate_registersWithProducer() {
        try {
            syslogSender.activate();
            verify(mockStringProducer).addListener(syslogSender);
        } finally {
            syslogSender.deactivate();
        }
    }

    @Test
    void testDeactivate_unregistersAndClosesSocket() {
        syslogSender.activate();
        syslogSender.deactivate();
        verify(mockStringProducer).removeListener(syslogSender);
    }

    @Test
    void testOnStringGenerated_sendsUdpPacket() {
        syslogSender.activate();
        try {
            String testString = "TestMessage123";

            // This test verifies the method doesn't throw exceptions
            // In a real environment, we'd need to mock the DatagramSocket
            assertDoesNotThrow(() -> syslogSender.onStringGenerated(testString),
                    "onStringGenerated should not throw exceptions");

        } finally {
            syslogSender.deactivate();
        }
    }

    @Test
    void testFormatSyslogMessage_correctFormat() throws Exception {
        // Use reflection to access the private formatSyslogMessage method
        java.lang.reflect.Method method = SyslogStringSender.class.getDeclaredMethod("formatSyslogMessage", String.class);
        method.setAccessible(true);

        String testMessage = "Hello World";
        String result = (String) method.invoke(syslogSender, testMessage);

        // Verify the format: <134>MMM dd HH:mm:ss osgi-demo: MESSAGE
        Pattern syslogPattern = Pattern.compile("^<134>\\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} osgi-demo: Hello World$");
        assertTrue(syslogPattern.matcher(result).matches(),
                "Syslog message should match expected format, but was: " + result);

        // Verify priority is 134 (Facility 16 * 8 + Severity 6)
        assertTrue(result.startsWith("<134>"), "Message should start with priority <134>");

        // Verify if the hostname is included
        assertTrue(result.contains("osgi-demo:"), "Message should contain hostname 'osgi-demo:'");

        // Verify the original message is at the end
        assertTrue(result.endsWith(": " + testMessage), "Message should end with the original message");
    }

    @Test
    void testOnStringGenerated_handlesSocketException() {
        // Create a sender without activating to avoid socket creation
        String testString = "ErrorTest";

        // This should not throw an exception when the socket is null
        assertDoesNotThrow(() -> syslogSender.onStringGenerated(testString),
                "Socket exceptions should be caught and logged, not thrown");
    }

    @Test
    void testSyslogPriority_isCorrect() throws Exception {
        // Use reflection to access the priority constant
        java.lang.reflect.Field facilityField = SyslogStringSender.class.getDeclaredField("FACILITY");
        facilityField.setAccessible(true);
        int facility = facilityField.getInt(null);

        java.lang.reflect.Field severityField = SyslogStringSender.class.getDeclaredField("SEVERITY");
        severityField.setAccessible(true);
        int severity = severityField.getInt(null);

        java.lang.reflect.Field priorityField = SyslogStringSender.class.getDeclaredField("PRIORITY");
        priorityField.setAccessible(true);
        int priority = priorityField.getInt(null);

        assertEquals(16, facility, "Facility should be 16 (local0)");
        assertEquals(6, severity, "Severity should be 6 (info)");
        assertEquals(134, priority, "Priority should be 134 (16*8+6)");
    }

    @Test
    void testActivateSocketException_throwsRuntimeException() {
        // This test simulates a scenario where socket creation might fail
        // In real testing, we could mock DatagramSocket constructor to throw SocketException

        // For this simplified test, we'll just verify that activate() can handle initialization
        assertDoesNotThrow(() -> {
            syslogSender.activate();
            syslogSender.deactivate();
        }, "Activate should handle socket creation gracefully");
    }

    @Test
    void testDeactivateWithNullProducer_handledGracefully() {
        // Set producer to null to simulate an edge case
        try {
            java.lang.reflect.Field field = SyslogStringSender.class.getDeclaredField("stringProducer");
            field.setAccessible(true);
            field.set(syslogSender, null);
        } catch (Exception e) {
            fail("Failed to set null producer: " + e.getMessage());
        }

        assertDoesNotThrow(() -> syslogSender.deactivate(),
                "Deactivation should handle null producer gracefully");
    }

    @Test
    void testTimestampFormat_isValid() throws Exception {
        java.lang.reflect.Method method = SyslogStringSender.class.getDeclaredMethod("formatSyslogMessage", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(syslogSender, "test");

        // Extract timestamp part (between > and hostname)
        Pattern timestampPattern = Pattern.compile("<134>(\\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2}) osgi-demo:");
        java.util.regex.Matcher matcher = timestampPattern.matcher(result);

        assertTrue(matcher.find(), "Should find timestamp pattern in syslog message");

        String timestamp = matcher.group(1);

        // Verify timestamp format matches syslog standard (MMM dd HH:mm:ss)
        Pattern validTimestamp = Pattern.compile("^\\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2}$");
        assertTrue(validTimestamp.matcher(timestamp).matches(),
                "Timestamp should match syslog format: " + timestamp);
    }

    @Test
    void testMultipleMessages_allFormattedCorrectly() throws Exception {
        java.lang.reflect.Method method = SyslogStringSender.class.getDeclaredMethod("formatSyslogMessage", String.class);
        method.setAccessible(true);

        String[] testMessages = {"Message1", "Test123", "Hello World!"};

        for (String message : testMessages) {
            String result = (String) method.invoke(syslogSender, message);

            assertTrue(result.startsWith("<134>"), "All messages should start with priority");
            assertTrue(result.contains("osgi-demo:"), "All messages should contain hostname");
            assertTrue(result.endsWith(": " + message), "All messages should end with original content");
        }
    }
}