package com.byteliberi.demo.producer.api;

/**
 * Service interface for string production.
 * Consumers register as listeners to receive generated strings.
 */
public interface StringProducer {

    /**
     * Register a listener to receive generated strings.
     * @param listener the listener to register
     */
    void addListener(StringListener listener);

    /**
     * Unregister a listener.
     * @param listener the listener to remove
     */
    void removeListener(StringListener listener);

    /**
     * Listener interface for string consumers.
     */
    @FunctionalInterface
    interface StringListener {
        /**
         * Called when a new string is generated.
         * @param value the generated string
         */
        void onStringGenerated(String value);
    }
}