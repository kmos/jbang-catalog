/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.debezium.jbang.core.platform.catalog.dto.Property;

public class CompositePropertyValidator implements PropertyValidator {

    private final List<PropertyValidator> validators;

    public CompositePropertyValidator(PropertyValidator... validators) {
        this.validators = List.of(validators);
    }

    @Override
    public List<String> validate(Property property, Map<String, Object> config) {
        List<String> errors = new ArrayList<>();
        for (PropertyValidator validator : validators) {
            errors.addAll(validator.validate(property, config));
        }
        return errors;
    }
}
