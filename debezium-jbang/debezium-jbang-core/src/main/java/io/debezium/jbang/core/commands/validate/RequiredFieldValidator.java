/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.validate;

import java.util.List;
import java.util.Map;

import io.debezium.jbang.core.platform.catalog.dto.Property;

public class RequiredFieldValidator implements PropertyValidator {

    @Override
    public List<String> validate(Property property, Map<String, Object> config) {
        if (Boolean.TRUE.equals(property.required()) && !config.containsKey(property.name())) {
            return List.of("source.config." + property.name() + ": required field '" + getLabel(property) + "' is missing");
        }
        return List.of();
    }
}
