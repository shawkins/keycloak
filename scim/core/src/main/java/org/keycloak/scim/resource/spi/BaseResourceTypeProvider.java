package org.keycloak.scim.resource.spi;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.SearchRequest;

/**
 * Created as a common base for v2 and scim logic.
 * 
 * TODO: v2 is not yet planning on using formal providers, that could be removed from here.
 */
public abstract class BaseResourceTypeProvider<M extends Model, R> implements ScimResourceTypeProvider<R> {

    protected final KeycloakSession session;

    public BaseResourceTypeProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public R create(R resource) {
        if (!hasPermission(getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        return onCreate(resource);
    }
    
    public abstract String getId(R resource);

    @Override
    public R update(R resource) {
        M model = getModel(getId(resource));

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        populate(model, resource);

        return onUpdate(model, resource);
    }

    @Override
    public R get(String id) {
        return get(id, null, null);
    }

    public R get(String id, List<String> attributes, List<String> excludedAttributes) {
        M model = getModel(id);

        if (model == null) {
            return null;
        }

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.VIEW)) {
            throw new ForbiddenException();
        }

        return createResourceTypeInstance(model, attributes, excludedAttributes);
    }

    @Override
    public Stream<R> getAll(SearchRequest searchRequest) {
        if (!canQuery()) {
            throw new ForbiddenException();
        }

        return getModels(searchRequest)
                .map(m -> createResourceTypeInstance(m, searchRequest.getAttributes(), searchRequest.getExcludedAttributes()));
    }

    @Override
    public boolean delete(String id) {
        M model = getModel(id);

        if (!hasPermission(model, getRealmResourceType(), AdminPermissionsSchema.MANAGE)) {
            throw new ForbiddenException();
        }

        return onDelete(id);
    }

    protected abstract R onCreate(R resource);

    protected abstract R onUpdate(M model, R resource);

    protected abstract boolean onDelete(String id);

    protected abstract Stream<M> getModels(SearchRequest searchRequest);

    protected abstract M getModel(String id);

    protected abstract String getRealmResourceType();

    protected abstract void populate(M model, R resource);

    protected abstract R createResourceTypeInstance(M model, List<String> attributes, List<String> excludedAttributes);

    private boolean canQuery() {
        return session.getContext().getPermissions().hasPermission(getRealmResourceType(), AdminPermissionsSchema.QUERY)
                || session.getContext().getPermissions().hasPermission(getRealmResourceType(), AdminPermissionsSchema.VIEW);
    }

    protected boolean hasPermission(String realmResourceType, String scope) {
        return session.getContext().getPermissions().hasPermission(realmResourceType, scope);
    }

    protected boolean hasPermission(M model, String realmResourceType, String scope) {
        if (AdminPermissionsSchema.VIEW.equals(scope)) {
            return session.getContext().getPermissions().hasPermission(model, realmResourceType, scope);
        }

        return session.getContext().getPermissions().hasPermission(model, realmResourceType, scope) && isManageable(model);
    }

    protected boolean isManageable(M model) {
        return true;
    }
}
