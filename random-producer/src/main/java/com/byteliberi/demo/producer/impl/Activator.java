package com.byteliberi.demo.producer.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.logging.Logger;

/**
 * Bundle activator for the random string producer bundle.
 * This is optional as we use Declarative Services, but provides
 * additional lifecycle hooks if needed.
 */
public class Activator implements BundleActivator {

    private static final Logger LOGGER = Logger.getLogger(Activator.class.getName());

    /**
     * Called when the bundle starts.
     * @param context the bundle context
     */
    @Override
    public void start(BundleContext context) {
        LOGGER.info("Random Producer Bundle started");
    }

    /**
     * Called when the bundle stops.
     * @param context the bundle context
     */
    @Override
    public void stop(BundleContext context) {
        LOGGER.info("Random Producer Bundle stopped");
    }
}