package org.keycloak.quarkus.runtime;

import org.jboss.logging.Logger;
import org.keycloak.config.SecurityOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;

/**
 * Builds a system-wide truststore from the given config options.
 */
public class TruststoreBuilder {

    private static final String SYSTEM_TRUSTSTORE_KEY = "javax.net.ssl.trustStore";
    private static final String PKCS12 = "PKCS12";

    private static final Logger LOGGER = Logger.getLogger(TruststoreBuilder.class);

    public static void setSystemTruststore() {
        String[] truststores = Configuration.getOptionalKcValue(SecurityOptions.TRUSTSTORE.getKey()).map(s -> s.split(",")).orElse(null);

        if (truststores == null || truststores.length == 0) {
            return;
        }

        boolean trustStoreIncludeDefault = Configuration.getOptionalBooleanKcValue(SecurityOptions.TRUSTSTORE_INCLUDE_DEFAULT.getKey()).orElse(false);

        KeyStore truststore = createPkcs12KeyStore();

        if (trustStoreIncludeDefault) {
            File defaultTrustStore = getDefaultTrustStoreFile();
            if (defaultTrustStore.exists()) {
                String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType());
                String path = defaultTrustStore.getAbsolutePath();
                mergeTrustStore(truststore, path, loadStore(path, trustStoreType, null));
            } else {
                LOGGER.warnf("Default truststore was to be included, but could not be found at: %s", defaultTrustStore);
            }
        }

        for (String file : truststores) {
            if (file.endsWith(".p12") || file.endsWith(".pfx")) {
                mergeTrustStore(truststore, file, loadStore(file, PKCS12, null));
            } else {
                mergePemFile(truststore, file);
            }
        }

        String confDir = System.getProperty("jboss.server.config.dir");
        if (confDir == null) {
            throw new RuntimeException("Failed to save truststore, jboss.server.config.dir is not set");
        }
        File file = new File(confDir, "keycloak-truststore.p12");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            truststore.store(fos, null);
            System.setProperty(SYSTEM_TRUSTSTORE_KEY, file.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save truststore: " + file.getAbsolutePath(), e);
        }
    }

    private static KeyStore createPkcs12KeyStore() {
        try {
            KeyStore truststore = KeyStore.getInstance(PKCS12);
            truststore.load(null, null);
            return truststore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize truststore: cannot create a PKCS12 keystore", e);
        }
    }

    static File getDefaultTrustStoreFile() {
        String originalTruststoreKey = SYSTEM_TRUSTSTORE_KEY + ".orig";
        String trustStorePath = System.getProperty(originalTruststoreKey);
        if (trustStorePath == null) {
            trustStorePath = System.getProperty(SYSTEM_TRUSTSTORE_KEY);
        }
        if (trustStorePath != null) {
            System.setProperty(originalTruststoreKey, trustStorePath);
            return new File(trustStorePath);
        }
        String securityDirectory = System.getProperty("java.home") + File.separator + "lib" + File.separator
                + "security" + File.separator;
        File jssecacertsFile = new File(securityDirectory + "jssecacerts");
        if (jssecacertsFile.exists() && jssecacertsFile.isFile()) {
            return jssecacertsFile;
        }
        return new File(securityDirectory + "cacerts");
    }

    private static KeyStore loadStore(String path, String type, char[] password) {
        try (InputStream is = new FileInputStream(path)) {
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(is, password);
            return ks;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize truststore: " + new File(path).getAbsolutePath() + ", type: " + type, e);
        }
    }

    private static void mergePemFile(KeyStore truststore, String file) {
        try (FileInputStream pemInputStream = new FileInputStream(file)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            while (pemInputStream.available() > 0) {
                try {
                    X509Certificate cert = (X509Certificate) certFactory.generateCertificate(pemInputStream);
                    String alias = cert.getSubjectX500Principal().getName() + "_" + cert.getSerialNumber().toString(16);
                    truststore.setCertificateEntry(alias, cert);
                } catch (CertificateException e) {
                    if (pemInputStream.available() > 0) {
                        // any remaining input means there is an actual problem with the key contents or
                        // file format
                        throw e;
                    }
                    LOGGER.debugf(e, "The trailing entry for %s generated a certificate exception, assuming instead that the file ends with comments",  new File(file).getAbsolutePath());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize truststore, could not merge: " + new File(file).getAbsolutePath(), e);
        }
    }

    private static void mergeTrustStore(KeyStore truststore, String file, KeyStore additionalStore) {
        try {
            for (String alias : Collections.list(additionalStore.aliases())) {
                truststore.setCertificateEntry(alias, additionalStore.getCertificate(alias));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize truststore, could not merge: " + new File(file).getAbsolutePath(), e);
        }
    }

}
