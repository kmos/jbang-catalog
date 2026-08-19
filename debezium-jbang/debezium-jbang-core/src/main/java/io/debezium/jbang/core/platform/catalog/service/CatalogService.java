/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.platform.catalog.service;

import io.debezium.jbang.core.platform.catalog.dto.ComponentDescriptor;

public interface CatalogService {

    String getCatalog(String type);

    ComponentDescriptor getComponentDescriptor(String type, String componentClass);
}
