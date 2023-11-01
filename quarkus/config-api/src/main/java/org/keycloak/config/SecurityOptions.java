package org.keycloak.config;

import java.util.Arrays;
import java.util.List;

import org.keycloak.common.Profile;
import org.keycloak.common.crypto.FipsMode;

public class SecurityOptions {

    public static final Option<FipsMode> FIPS_MODE = new OptionBuilder<>("fips-mode", FipsMode.class)
            .category(OptionCategory.SECURITY)
            .expectedValues(SecurityOptions::getFipsModeValues)
            .buildTime(true)
            .description("Sets the FIPS mode. If '" + FipsMode.NON_STRICT + "' is set, FIPS is enabled but on non-approved mode. For full FIPS compliance, set '" + FipsMode.STRICT + "' to run on approved mode. "
                    + "This option defaults to '" + FipsMode.DISABLED + "' when '" + Profile.Feature.FIPS.getKey() + "' feature is disabled, which is by default. "
                    + "This option defaults to '" + FipsMode.NON_STRICT + "' when '" + Profile.Feature.FIPS.getKey() + "' feature is enabled.")
            .defaultValue(FipsMode.DISABLED)
            .build();

    public static final Option<String> TRUSTSTORE = new OptionBuilder<>("truststore", String.class)
            .category(OptionCategory.SECURITY)
            .description("List of pkcs12 (.p12 or .pfx) or PEM files that will be used to construct the javax.net.ssl.trustStore used at runtime.")
            .build();

    public static final Option<Boolean> TRUSTSTORE_INCLUDE_DEFAULT = new OptionBuilder<>("truststore-include-default", Boolean.class)
            .category(OptionCategory.SECURITY)
            .defaultValue(Boolean.TRUE)
            .description("Whether to include the Java default truststore certs when using the --truststore option")
            .build();

    private static List<String> getFipsModeValues() {
        return Arrays.asList(FipsMode.NON_STRICT.toString(), FipsMode.STRICT.toString());
    }
}
