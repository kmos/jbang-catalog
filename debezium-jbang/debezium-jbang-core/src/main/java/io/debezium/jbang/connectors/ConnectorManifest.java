/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.connectors;

import java.util.List;

public record ConnectorManifest(
        String name,
        String connectorClass,
        String artifact,
        String profile,
        List<String> supportedVersions,
        List<String> sinks) {

    public static ConnectorManifest from(ConnectorRegistry.ConnectorEntry entry) {
        return new ConnectorManifest(
                entry.name(),
                entry.connectorClass(),
                entry.artifact(),
                entry.profile(),
                entry.versions(),
                entry.sinks());
    }
}
