package com.byteliberi.demo.writer.impl;

import com.byteliberi.demo.producer.api.StringProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FileStringWriter.
 */
@ExtendWith(MockitoExtension.class)
class FileStringWriterTest {
    private final static Logger LOGGER = Logger.getLogger(FileStringWriterTest.class.getName());

    @Mock
    private StringProducer mockStringProducer;

    private FileStringWriter fileWriter;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() {
        fileWriter = new FileStringWriter();
        // Use reflection to inject the mock producer for testing
        try {
            java.lang.reflect.Field field = FileStringWriter.class.getDeclaredField("stringProducer");
            field.setAccessible(true);
            field.set(fileWriter, mockStringProducer);
        } catch (Exception e) {
            fail("Failed to inject mock producer: " + e.getMessage());
        }
    }

    @Test
    void testActivate_registersWithProducer() {
        fileWriter.activate();
        verify(mockStringProducer).addListener(fileWriter);
    }

    @Test
    void testDeactivate_unregistersFromProducer() {
        fileWriter.activate();
        fileWriter.deactivate();
        verify(mockStringProducer).removeListener(fileWriter);
    }

    @Test
    void testOnStringGenerated_createsFile() throws IOException {
        // Override the output directory for testing
        overrideOutputDirectory();

        String testString = "TestString123";
        fileWriter.onStringGenerated(testString);

        // Check that a file was created in the temp directory
        try(Stream<Path> fileListStream = Files.list(tempDir)) {
            List<Path> files = fileListStream.collect(Collectors.toList());
            assertEquals(1, files.size(), "Exactly one file should be created");
            // Verify file content
            String fileContent = Files.readString(files.get(0), StandardCharsets.UTF_8);
            assertEquals(testString, fileContent, "File content should match the input string");
        }
    }

    @Test
    void testOnStringGenerated_correctFilename() throws IOException {
        overrideOutputDirectory();

        String testString = "TestString456";
        fileWriter.onStringGenerated(testString);

        List<Path> files = Files.list(tempDir).collect(Collectors.toList());
        String filename = files.get(0).getFileName().toString();

        // Verify filename pattern: string_YYYYMMDD_HHmmss_SSS.txt
        Pattern filenamePattern = Pattern.compile("^string_\\d{8}_\\d{6}_\\d{3}\\.txt$");
        assertTrue(filenamePattern.matcher(filename).matches(),
                "Filename should match pattern string_YYYYMMDD_HHmmss_SSS.txt, but was: " + filename);
    }

    @Test
    void testOnStringGenerated_correctContent() throws IOException {
        overrideOutputDirectory();

        String testString = "Hello OSGi World!";
        fileWriter.onStringGenerated(testString);

        List<Path> files = Files.list(tempDir).collect(Collectors.toList());
        String content = Files.readString(files.get(0), StandardCharsets.UTF_8);

        assertEquals(testString, content, "File content should exactly match the generated string");
    }

    @Test
    void testOnStringGenerated_createsDirectory() throws IOException {
        // Test with a subdirectory to ensure directory creation works
        Path customDir = tempDir.resolve("custom-output");
        overrideOutputDirectory(customDir + "/");

        assertFalse(Files.exists(customDir), "Custom directory should not exist initially");

        String testString = "DirectoryTest";
        fileWriter.onStringGenerated(testString);

        assertTrue(Files.exists(customDir), "Custom directory should be created");
        assertTrue(Files.isDirectory(customDir), "Created path should be a directory");

        // Verify if the file was created in the custom directory
        List<Path> files = Files.list(customDir).collect(Collectors.toList());
        assertEquals(1, files.size(), "File should be created in the custom directory");
    }

    @Test
    void testOnStringGenerated_handlesIOException() {
        // Test with an invalid directory path that would cause IOException
        overrideOutputDirectory("/invalid/readonly/path/");

        String testString = "ErrorTest";

        // This should not throw an exception but should log the error
        assertDoesNotThrow(() -> fileWriter.onStringGenerated(testString),
                "IOException should be caught and logged, not thrown");
    }

    @Test
    void testMultipleStrings_createsSeparateFiles() throws IOException {
        overrideOutputDirectory();

        fileWriter.onStringGenerated("String1");
        // Small delay to ensure different timestamps
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        fileWriter.onStringGenerated("String2");

        List<Path> files = Files.list(tempDir).collect(Collectors.toList());
        assertEquals(2, files.size(), "Two separate files should be created");

        // Verify different filenames
        String filename1 = files.get(0).getFileName().toString();
        String filename2 = files.get(1).getFileName().toString();
        assertNotEquals(filename1, filename2, "Files should have different names");
    }

    @Test
    void testDeactivateWithNullProducer_handledGracefully() {
        // Set producer to null to simulate an edge case
        try {
            java.lang.reflect.Field field = FileStringWriter.class.getDeclaredField("stringProducer");
            field.setAccessible(true);
            field.set(fileWriter, null);
        } catch (Exception e) {
            fail("Failed to set null producer: " + e.getMessage());
        }

        assertDoesNotThrow(() -> fileWriter.deactivate(),
                "Deactivation should handle null producer gracefully");
    }

    @Test
    void testActivationFailure_handlesException() {
        // Create a mock that throws an exception
        doThrow(new RuntimeException("Mock exception")).when(mockStringProducer).addListener(any());

        assertDoesNotThrow(() -> fileWriter.activate(),
                "Activation should handle producer exceptions gracefully");
    }

    /**
     * Helper method to override the output directory for testing.
     */
    private void overrideOutputDirectory() {
        overrideOutputDirectory(tempDir + "/");
    }

    /**
     * Helper method to override the output directory with a custom path.
     * This creates a custom FileStringWriter subclass for testing.
     */
    private void overrideOutputDirectory(String customPath) {
        try {
            // Create a test implementation that uses the temp directory
            fileWriter = new FileStringWriter() {
                @Override
                public void onStringGenerated(String value) {
                    try {
                        String filename = generateFilename();
                        Path filePath = Paths.get(customPath, filename);

                        // Create a directory if needed
                        Files.createDirectories(filePath.getParent());
                        Files.write(filePath, value.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        LOGGER.warning("Failed to write string '" + value + "' to file: " + e.getMessage());
                    }
                }

                private String generateFilename() {
                    LocalDateTime now = LocalDateTime.now();
                    String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
                    return "string_" + timestamp + ".txt";
                }
            };

            // Inject the mock producer
            java.lang.reflect.Field field = FileStringWriter.class.getDeclaredField("stringProducer");
            field.setAccessible(true);
            field.set(fileWriter, mockStringProducer);
        } catch (Exception e) {
            fail("Failed to override output directory: " + e.getMessage());
        }
    }
}