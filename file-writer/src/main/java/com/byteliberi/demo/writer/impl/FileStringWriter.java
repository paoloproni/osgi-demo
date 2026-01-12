package com.byteliberi.demo.writer.impl;

import com.byteliberi.demo.producer.api.StringProducer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * File string writer implementation that consumes strings from the StringProducer
 * and writes them to individual files in the /tmp/osgi-demo/ directory.
 */
@Component(immediate = true)
public class FileStringWriter implements StringProducer.StringListener {

    private static final Logger LOGGER = Logger.getLogger(FileStringWriter.class.getName());
    private static final String OUTPUT_DIR = "/tmp/osgi-demo/";
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Reference
    private StringProducer stringProducer;

    /**
     * Activates the file writer service and registers as a listener.
     */
    @Activate
    public void activate() {
        try {
            createOutputDirectory();
            stringProducer.addListener(this);
            LOGGER.info("File writer activated and registered with string producer");
        } catch (Exception e) {
            LOGGER.severe("Failed to activate file writer: " + e.getMessage());
        }
    }

    /**
     * Deactivates the file writer service and unregisters as a listener.
     */
    @Deactivate
    public void deactivate() {
        try {
            if (stringProducer != null) {
                stringProducer.removeListener(this);
            }
            LOGGER.info("File writer deactivated and unregistered from string producer");
        } catch (Exception e) {
            LOGGER.warning("Error during file writer deactivation: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStringGenerated(String value) {
        try {
            String filename = generateFilename();
            Path filePath = Paths.get(OUTPUT_DIR, filename);

            Files.write(filePath, value.getBytes(StandardCharsets.UTF_8));

            LOGGER.info("Successfully wrote string '" + value + "' to file: " + filename);
        } catch (IOException e) {
            LOGGER.warning("Failed to write string '" + value + "' to file: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Unexpected error writing string to file: " + e.getMessage());
        }
    }

    /**
     * Creates the output directory if it doesn't exist.
     * @throws IOException if directory creation fails
     */
    private void createOutputDirectory() throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIR);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
            LOGGER.info("Created output directory: " + OUTPUT_DIR);
        } else {
            LOGGER.info("Output directory already exists: " + OUTPUT_DIR);
        }
    }

    /**
     * Generates a timestamp-based filename in the format: string_YYYYMMDD_HHmmss_SSS.txt
     * @return the generated filename
     */
    private String generateFilename() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FILE_TIMESTAMP_FORMAT);
        return "string_" + timestamp + ".txt";
    }
}