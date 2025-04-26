package org.keycloak.config;

public class ProviderOptions {

    public static final Option<String> PROVIDER = new OptionBuilder<>("provider-spi-<spi>", String.class)
            .category(OptionCategory.PROVIDERS)
            .description("Configure a single provider for the given SPI")
            .buildTime(true)
            .build();

    public static final Option<String> PROVIDER_DEFAULT = new OptionBuilder<>("provider-default-spi-<spi>", String.class)
            .category(OptionCategory.PROVIDERS)
            .description("Specify the default provider id for the given SPI")
            .buildTime(true)
            .build();

    public static final Option<Boolean> PROVIDER_ENABLED = new OptionBuilder<>("provider-enabled-<spi and id>", Boolean.class)
            .category(OptionCategory.PROVIDERS)
            .description("Enable or disable the given SPI and id")
            .buildTime(true)
            .build();

}
