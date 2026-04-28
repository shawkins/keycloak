package org.keycloak.models.mapper;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class MappedField<M, T> {

    final Function<M, T> modelGetter;
    final BiConsumer<M, T> modelSetter;

    public MappedField(Function<M, T> modelGetter, BiConsumer<M, T> modelSetter) {
        this.modelGetter = modelGetter;
        this.modelSetter = modelSetter;
    }    
    
}
