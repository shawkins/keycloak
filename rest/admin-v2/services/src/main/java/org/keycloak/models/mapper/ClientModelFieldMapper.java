package org.keycloak.models.mapper;

import java.util.Map;
import java.util.function.BiConsumer;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.FieldMapper;

public class ClientModelFieldMapper implements FieldMapper {
        
    private final Map<String, MappedField<ClientModel, ?>> properties;
    private final ClientModel model;
        
    public ClientModelFieldMapper(ClientModel model, Map<String, MappedField<ClientModel, ?>> properties) {
        this.model = model;
        this.properties = properties;
    }
    
    @Override
    public <T> T get(String field) {
        return (T) properties.get(field).modelGetter.apply(model);
    }
    
    @Override
    public <T> void set(String field, T value) {
        ((BiConsumer)(properties.get(field).modelSetter)).accept(model, value);
    }
    
}
