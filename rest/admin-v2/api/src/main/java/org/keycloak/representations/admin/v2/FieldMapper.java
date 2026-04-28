package org.keycloak.representations.admin.v2;

public interface FieldMapper {
    
    <T> T get(String field);
    
    <T> void set(String field, T value);

}
