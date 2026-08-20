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
import io.debezium.jbang.core.platform.catalog.dto.Validation;

public class EnumValueValidator implements PropertyValidator {

    @Override
    public List<String> validate(Property property, Map<String, Object> config) {
        if (property.validation() == null || !config.containsKey(property.name())) {
            return List.of();
        }
        String userValue = String.valueOf(config.get(property.name()));
        List<String> errors = new ArrayList<>();
        for (Validation v : property.validation()) {
            if ("enum".equals(v.type()) && v.values() != null && !v.values().contains(userValue)) {
                errors.add("source.config." + property.name() + ": invalid value '" + userValue
                        + "' for field '" + getLabel(property) + "'. Allowed: " + String.join(", ", v.values()));
            }
        }
        return errors;
    }
}
