package org.keycloak.quarkus.runtime.configuration.mappers;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import org.keycloak.config.ProviderOptions;

public final class ProviderPropertyMappers {

    private ProviderPropertyMappers() {
    }

    public static PropertyMapper<?>[] getMappers() {
        return new PropertyMapper[]{
                fromOption(ProviderOptions.PROVIDER)
                        .to("kc.spi-<spi>-provider")
                        .paramLabel("provider")
                        .build(),
                fromOption(ProviderOptions.PROVIDER_DEFAULT)
                        .to("kc.spi-<spi>-provider-default")
                        .paramLabel("default")
                        .build(),
                fromOption(ProviderOptions.PROVIDER_ENABLED)
                        .to("kc.spi-<spi and id>-enabled")
                        .build(),
        };
    }
}
