/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.validate;

import java.util.List;
import java.util.Map;

import io.debezium.jbang.core.platform.catalog.dto.Property;

public interface PropertyValidator {

    List<String> validate(Property property, Map<String, Object> config);

    default String getLabel(Property property) {
        return property.display() != null && property.display().label() != null
                ? property.display().label()
                : property.name();
    }
}
