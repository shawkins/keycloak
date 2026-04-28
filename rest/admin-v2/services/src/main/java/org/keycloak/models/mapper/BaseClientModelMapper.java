package org.keycloak.models.mapper;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.FieldMapper;

public abstract class BaseClientModelMapper {
    
    Map<String, MappedField<ClientModel, ?>> fields = new HashMap<>();
    
    protected <F> void addMapping(String name, Function<ClientModel, F> modelGetter, BiConsumer<ClientModel, F> modelSetter) {
        fields.put(name, new MappedField<>(modelGetter, modelSetter));
    }
    
    public BaseClientModelMapper() {
        addMapping(BaseClientRepresentation.UUID_FIELD, ClientModel::getId, null);
        addMapping(BaseClientRepresentation.ENABLED_FIELD, ClientModel::isEnabled, (model, enabled) -> model.setEnabled(Boolean.TRUE.equals(enabled)));
        addMapping(BaseClientRepresentation.CLIENT_ID_FIELD, ClientModel::getClientId, ClientModel::setClientId);
        addMapping(BaseClientRepresentation.DESCRIPTION_FIELD, ClientModel::getDescription, ClientModel::setDescription);
        addMapping(BaseClientRepresentation.DISPLAY_NAME_FIELD, ClientModel::getName, ClientModel::setName);
        addMapping(BaseClientRepresentation.APP_URL_FIELD, ClientModel::getBaseUrl, ClientModel::setBaseUrl);
        // TODO: consider built-in logic for copying collections
        addMapping(BaseClientRepresentation.REDIRECT_URIS_FIELD, model -> new LinkedHashSet<>(model.getRedirectUris()), (model, uris) -> model.setRedirectUris(new LinkedHashSet<>(uris)));
        addMapping(BaseClientRepresentation.ROLES_FIELD, model -> model.getRolesStream().map(RoleModel::getName).collect(Collectors.toSet()), null);
    }
    
    abstract BaseClientRepresentation createClientRepresentation(FieldMapper mapper);

    public BaseClientRepresentation fromModel(ClientModel client) {
        return createClientRepresentation(new ClientModelFieldMapper(client, fields));
    }

    public void toModel(BaseClientRepresentation client, ClientModel model) {
        fields.forEach((k, v) -> v.modelSetter.accept(model, client.getFieldMapper().get(k)));
    }
    
    public void materialize(Set<String> fields) {
        
    }
    
}
