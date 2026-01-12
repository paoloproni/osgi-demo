package com.byteliberi.demo.producer.impl;

import com.byteliberi.demo.producer.api.StringProducer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Random string producer implementation that generates random alphanumeric strings
 * at random intervals and notifies registered listeners.
 */
@Component(immediate = true, service = StringProducer.class)
public class RandomStringProducer implements StringProducer {

    private static final Logger LOGGER = Logger.getLogger(RandomStringProducer.class.getName());
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 16;
    private static final int MIN_INTERVAL_MS = 1000; // 1 second
    private static final int MAX_INTERVAL_MS = 5000; // 5 seconds

    private final List<StringListener> listeners = new CopyOnWriteArrayList<>();
    private final SecureRandom random = new SecureRandom();
    private volatile boolean running = false;
    private Thread producerThread;

    /**
     * Activates the producer service and starts the background thread.
     */
    @Activate
    public void activate() {
        running = true;
        producerThread = new Thread(this::generateStrings, "RandomStringProducer");
        producerThread.start();
        LOGGER.info("Random string producer activated");
    }

    /**
     * Deactivates the producer service and stops the background thread.
     */
    @Deactivate
    public void deactivate() {
        running = false;
        if (producerThread != null) {
            producerThread.interrupt();
            try {
                producerThread.join(1000); // Wait up to 1 second for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warning("Interrupted while waiting for producer thread to finish");
            }
        }
        listeners.clear();
        LOGGER.info("Random string producer deactivated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(StringListener listener) {
        if (listener != null) {
            listeners.add(listener);
            LOGGER.info("Added string listener: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(StringListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            LOGGER.info("Removed string listener: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * Main loop that generates random strings at random intervals.
     */
    private void generateStrings() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                String randomString = generateRandomString();
                LOGGER.info("Generated string: " + randomString);
                notifyListeners(randomString);

                int waitTime = MIN_INTERVAL_MS + random.nextInt(MAX_INTERVAL_MS - MIN_INTERVAL_MS + 1);
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                break;
            } catch (Exception e) {
                LOGGER.severe("Error in string generation loop: " + e.getMessage());
            }
        }
        LOGGER.info("String generation loop terminated");
    }

    /**
     * Generates a random alphanumeric string of length between 8-16 characters.
     * @return the generated string
     */
    private String generateRandomString() {
        int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHANUMERIC_CHARS.length());
            sb.append(ALPHANUMERIC_CHARS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Notifies all registered listeners about the generated string.
     * @param value the generated string
     */
    private void notifyListeners(String value) {
        for (StringListener listener : listeners) {
            try {
                listener.onStringGenerated(value);
            } catch (Exception e) {
                LOGGER.warning("Error notifying listener " + listener.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}