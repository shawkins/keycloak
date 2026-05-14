package org.keycloak.encoding;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class GzipResourceEncodingProviderFactoryTest {

    private static final String KC_TMPDIR = "kc.io.tmpdir";
    private String originalTmpDir;

    @BeforeEach
    void setUp() {
        originalTmpDir = System.getProperty(KC_TMPDIR);
        System.clearProperty(KC_TMPDIR);
    }

    @AfterEach
    void tearDown() {
        if (originalTmpDir != null) {
            System.setProperty(KC_TMPDIR, originalTmpDir);
        } else {
            System.clearProperty(KC_TMPDIR);
        }
    }

    @Test
    void testCreateWithoutTmpDir() {
        GzipResourceEncodingProviderFactory factory = new GzipResourceEncodingProviderFactory();

        ResourceEncodingProvider provider = assertDoesNotThrow(() -> factory.create(null));
        assertThat(provider, notNullValue());
        assertThat(provider.getEncodedStream(() -> null, "test.css"), nullValue());
    }

    @Test
    void testCreateWithTmpDir() throws IOException {
        Path tmpDir = Files.createTempDirectory("kc-test-gzip");
        try {
            System.setProperty(KC_TMPDIR, tmpDir.toAbsolutePath().toString());

            GzipResourceEncodingProviderFactory factory = new GzipResourceEncodingProviderFactory();
            ResourceEncodingProvider provider = factory.create(null);

            assertThat(provider, notNullValue());
            File cacheDir = new File(tmpDir.toFile(), "kc-gzip-cache");
            assertThat(cacheDir.isDirectory(), is(true));
        } finally {
            FileUtils.deleteDirectory(tmpDir.toFile());
        }
    }
}
