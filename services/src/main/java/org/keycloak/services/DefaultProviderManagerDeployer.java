package org.keycloak.services;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ThemeManager;
import org.keycloak.provider.InvalidationHandler.ObjectType;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.ProviderManagerDeployer;
import org.keycloak.provider.Spi;
import org.keycloak.theme.ThemeManagerFactory;

import org.jboss.logging.Logger;

public class DefaultProviderManagerDeployer implements ProviderManagerDeployer {

    private static final Logger logger = Logger.getLogger(DefaultProviderManagerDeployer.class);

    private DefaultKeycloakSessionFactory sessionFactory;

    public DefaultProviderManagerDeployer(DefaultKeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void deploy(ProviderManager pm) {
        registerNewSpis(pm);

        Map<Class<? extends Provider>, Map<String, ProviderFactory>> copy = getFactoriesCopy();
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> newFactories = sessionFactory.loadFactories(pm);
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> deployed = new HashMap<>();
        List<ProviderFactory> undeployed = new LinkedList<>();

        for (Map.Entry<Class<? extends Provider>, Map<String, ProviderFactory>> entry : newFactories.entrySet()) {
            Class<? extends Provider> provider = entry.getKey();
            Map<String, ProviderFactory> current = copy.get(provider);
            if (current == null) {
                copy.put(provider, entry.getValue());
            } else {
                for (Map.Entry<String, ProviderFactory> e : entry.getValue().entrySet()) {
                    deployed.compute(provider, (k, v) -> {
                        Map<String, ProviderFactory> map = Objects.requireNonNullElseGet(v, HashMap::new);
                        map.put(e.getKey(), e.getValue());
                        return map;
                    });
                    ProviderFactory old = current.remove(e.getValue().getId());
                    if (old != null) {
                        undeployed.add(old);
                    }
                }
                current.putAll(entry.getValue());
            }

        }
        sessionFactory.setFactoriesMap(copy);
        // need to update the default provider map
        sessionFactory.checkProvider();
        boolean cfChanged = false;
        for (ProviderFactory factory : undeployed) {
            sessionFactory.invalidate(null, ObjectType.PROVIDER_FACTORY, factory.getClass());
            factory.close();
            cfChanged |= (sessionFactory.getComponentFactoryPF() == factory);
        }
        sessionFactory.initProviderFactories(cfChanged, deployed);

        if (pm.getInfo().hasThemes() || pm.getInfo().hasThemeResources()) {
            ((ThemeManagerFactory)sessionFactory.getProviderFactory(ThemeManager.class)).clearCache();
        }
    }

    protected Map<Class<? extends Provider>, Map<String, ProviderFactory>> getFactoriesCopy() {
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> copy = new HashMap<>();
        for (Map.Entry<Class<? extends Provider>, Map<String, ProviderFactory>> entry : sessionFactory.getFactoriesMap().entrySet()) {
            Map<String, ProviderFactory> valCopy = new HashMap<>(entry.getValue());
            copy.put(entry.getKey(), valCopy);
        }
        return copy;

    }

    // Register SPIs of this providerManager, which are possibly not yet registered in this factory
    private void registerNewSpis(ProviderManager pm) {
        Set<String> existingSpiNames = sessionFactory.getSpis().stream()
                .map(spi -> spi.getName())
                .collect(Collectors.toSet());

        for (Spi newSpi : pm.loadSpis()) {
            if (!existingSpiNames.contains(newSpi.getName())) {
                this.sessionFactory.addSpi(newSpi);
            }
        }
    }

    @Override
    public void undeploy(ProviderManager pm) {
        logger.debug("undeploy");
        // we make a copy to avoid concurrent access exceptions
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> copy = getFactoriesCopy();
        MultivaluedHashMap<Class<? extends Provider>, ProviderFactory> factories = pm.getLoadedFactories();
        List<ProviderFactory> undeployed = new LinkedList<>();
        for (Map.Entry<Class<? extends Provider>, List<ProviderFactory>> entry : factories.entrySet()) {
            Map<String, ProviderFactory> registered = copy.get(entry.getKey());
            for (ProviderFactory factory : entry.getValue()) {
                undeployed.add(factory);
                logger.debugv("undeploying {0} of id {1}", factory.getClass().getName(), factory.getId());
                if (registered != null) {
                    registered.remove(factory.getId());
                }
            }
        }
        sessionFactory.setFactoriesMap(copy);
        for (ProviderFactory factory : undeployed) {
            factory.close();
        }
    }

}
