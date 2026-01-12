package com.byteliberi.demo.producer.impl;

import com.byteliberi.demo.producer.api.StringProducer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RandomStringProducer.
 */
@ExtendWith(MockitoExtension.class)
class RandomStringProducerTest {

    @Mock
    private StringProducer.StringListener mockListener1;

    private RandomStringProducer producer;

    @BeforeEach
    void setUp() {
        producer = new RandomStringProducer();
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.deactivate();
        }
    }

    @Test
    void testActivate_startsProduction() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        StringProducer.StringListener listener = value -> latch.countDown();

        producer.addListener(listener);
        producer.activate();

        assertTrue(latch.await(6, TimeUnit.SECONDS), "String should be generated within 6 seconds");
    }

    @Test
    void testDeactivate_stopsProduction() throws InterruptedException {
        AtomicInteger callCount = new AtomicInteger(0);
        StringProducer.StringListener listener = value -> callCount.incrementAndGet();

        producer.addListener(listener);
        producer.activate();

        // Wait for at least one string generation
        Thread.sleep(6000);
        producer.deactivate();

        int countAtStop = callCount.get();
        // Wait a bit more to ensure no more strings are generated
        Thread.sleep(2000);
        int countAfterStop = callCount.get();

        assertEquals(countAtStop, countAfterStop, "No new strings should be generated after deactivation");
    }

    @Test
    void testAddListener_receivesNotifications() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

        producer.addListener(mockListener1);
        producer.activate();

        // Use a separate thread to wait for the mock to be called
        Thread waiter = new Thread(() -> {
            try {
                // Wait up to 6 seconds for a string to be generated
                for (int i = 0; i < 60; i++) {
                    if (!org.mockito.Mockito.mockingDetails(mockListener1).getInvocations().isEmpty()) {
                        latch.countDown();
                        break;
                    }
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();

        assertTrue(latch.await(7, TimeUnit.SECONDS), "Listener should receive notification within 7 seconds");
        verify(mockListener1, atLeastOnce()).onStringGenerated(stringCaptor.capture());
        assertNotNull(stringCaptor.getValue());
        assertFalse(stringCaptor.getValue().isEmpty());
    }

    @Test
    void testRemoveListener_stopsReceivingNotifications() throws InterruptedException {
        producer.addListener(mockListener1);
        producer.activate();

        // Wait for at least one call
        Thread.sleep(6000);
        producer.removeListener(mockListener1);

        reset(mockListener1);
        Thread.sleep(6000); // Wait for potential calls after removal

        verify(mockListener1, never()).onStringGenerated(any());
    }

    @Test
    void testMultipleListeners_allReceiveSameString() throws InterruptedException {
        AtomicReference<String> string1 = new AtomicReference<>();
        AtomicReference<String> string2 = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(2);

        StringProducer.StringListener listener1 = value -> {
            string1.set(value);
            latch.countDown();
        };

        StringProducer.StringListener listener2 = value -> {
            string2.set(value);
            latch.countDown();
        };

        producer.addListener(listener1);
        producer.addListener(listener2);
        producer.activate();

        assertTrue(latch.await(7, TimeUnit.SECONDS), "Both listeners should receive notification within 7 seconds");
        assertEquals(string1.get(), string2.get(), "Both listeners should receive the same string");
    }

    @Test
    void testGeneratedString_hasCorrectFormat() throws InterruptedException {
        AtomicReference<String> generatedString = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        StringProducer.StringListener listener = value -> {
            generatedString.set(value);
            latch.countDown();
        };

        producer.addListener(listener);
        producer.activate();

        assertTrue(latch.await(7, TimeUnit.SECONDS), "String should be generated within 7 seconds");

        String result = generatedString.get();
        assertNotNull(result);
        assertTrue(result.length() >= 8 && result.length() <= 16,
                "String length should be between 8-16 characters, but was: " + result.length());

        Pattern alphanumericPattern = Pattern.compile("^[A-Za-z0-9]+$");
        assertTrue(alphanumericPattern.matcher(result).matches(),
                "String should contain only alphanumeric characters: " + result);
    }

    @Test
    void testThreadSafety_concurrentListenerOperations() throws InterruptedException {
        final int NUM_LISTENERS = 10;
        final CountDownLatch startLatch = new CountDownLatch(NUM_LISTENERS);
        final CountDownLatch endLatch = new CountDownLatch(NUM_LISTENERS);
        final AtomicInteger successfulOperations = new AtomicInteger(0);

        producer.activate();

        // Create multiple threads that add and remove listeners concurrently
        for (int i = 0; i < NUM_LISTENERS; i++) {
            Thread thread = new Thread(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await(); // Wait for all threads to be ready

                    StringProducer.StringListener listener = value -> {
                        // Do nothing, just a test listener
                    };

                    producer.addListener(listener);
                    Thread.sleep(100); // Small delay
                    producer.removeListener(listener);

                    successfulOperations.incrementAndGet();
                } catch (Exception e) {
                    // Failed operation
                } finally {
                    endLatch.countDown();
                }
            });
            thread.start();
        }

        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "All threads should complete within 10 seconds");
        assertEquals(NUM_LISTENERS, successfulOperations.get(), "All operations should succeed");
    }

    @Test
    void testAddNullListener_handledGracefully() {
        assertDoesNotThrow(() -> producer.addListener(null));
    }

    @Test
    void testRemoveNullListener_handledGracefully() {
        assertDoesNotThrow(() -> producer.removeListener(null));
    }
}