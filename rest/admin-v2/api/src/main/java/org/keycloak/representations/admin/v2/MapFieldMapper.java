package org.keycloak.representations.admin.v2;

import java.util.LinkedHashMap;

public class MapFieldMapper implements FieldMapper {
    
    //TODO: initialize collections - need the type for that
    
    protected LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

    @Override
    public <T> T get(String field) {
        return (T) fields.get(field);
    }

    @Override
    public <T> void set(String field, T value) {
        fields.put(field, value);
    }

}
