/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.connectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConnectorRegistryTest {

    @Test
    void connectorRegistryReturnsArtifactForKnownConnectors() {
        assertThat(ConnectorRegistry.getConnector("postgres").artifact())
                .isEqualTo("io.debezium:debezium-connector-postgres");
        assertThat(ConnectorRegistry.getConnector("mysql").artifact())
                .isEqualTo("io.debezium:debezium-connector-mysql");
        assertThat(ConnectorRegistry.getConnector("mongodb").artifact())
                .isEqualTo("io.debezium:debezium-connector-mongodb");
        assertThat(ConnectorRegistry.getConnector("sqlserver").artifact())
                .isEqualTo("io.debezium:debezium-connector-sqlserver");
        assertThat(ConnectorRegistry.getConnector("oracle").artifact())
                .isEqualTo("io.debezium:debezium-connector-oracle");
    }

    @Test
    void connectorRegistryReturnsClassForKnownConnectors() {
        assertThat(ConnectorRegistry.getConnector("postgres").connectorClass())
                .isEqualTo("io.debezium.connector.postgresql.PostgresConnector");
    }

    @Test
    void connectorRegistryReturnsSinksForKnownConnectors() {
        assertThat(ConnectorRegistry.getConnector("postgres").sinks())
                .containsExactlyInAnyOrder("kafka", "http", "pulsar", "redis");
    }

    @Test
    void connectorRegistryThrowsForUnknownConnector() {
        assertThatThrownBy(() -> ConnectorRegistry.getConnector("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown connector: unknown");
    }

    @Test
    void sinkRegistryReturnsArtifactForKnownSinks() {
        assertThat(ConnectorRegistry.getSinkArtifact("kafka"))
                .isEqualTo("io.debezium:debezium-server-kafka");
        assertThat(ConnectorRegistry.getSinkArtifact("http"))
                .isEqualTo("io.debezium:debezium-server-http");
    }

    @Test
    void sinkRegistryThrowsForUnknownSink() {
        assertThatThrownBy(() -> ConnectorRegistry.getSinkArtifact("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown sink: unknown");
    }

    @Test
    void loadReturnsClassMap() {
        assertThat(ConnectorRegistry.load())
                .containsEntry("postgres", "io.debezium.connector.postgresql.PostgresConnector")
                .containsEntry("mysql", "io.debezium.connector.mysql.MySqlConnector")
                .hasSize(5);
    }

    @Test
    void connectorRegistryReturnsVersionsForKnownConnectors() {
        assertThat(ConnectorRegistry.getConnector("postgres").versions())
                .contains("3.7.x", "3.0.x", "2.7.x");
    }

    @Test
    void connectorManifestMatchesRegistry() {
        ConnectorManifest manifest = ConnectorRegistry.getManifest("postgres");
        assertThat(manifest.name()).isEqualTo("postgres");
        assertThat(manifest.connectorClass()).isEqualTo("io.debezium.connector.postgresql.PostgresConnector");
        assertThat(manifest.artifact()).isEqualTo("io.debezium:debezium-connector-postgres");
        assertThat(manifest.supportedVersions()).contains("3.7.x");
        assertThat(manifest.sinks()).containsExactlyInAnyOrder("kafka", "http", "pulsar", "redis");
    }

    @Test
    void connectorProfileMapsToDistProfileName() {
        assertThat(ConnectorRegistry.getConnector("postgres").profile()).isEqualTo("connector-postgres");
        assertThat(ConnectorRegistry.getConnector("mysql").profile()).isEqualTo("connector-mysql");
        assertThat(ConnectorRegistry.getConnector("oracle").profile()).isEqualTo("connector-oracle");
        assertThat(ConnectorRegistry.getSink("kafka").profile()).isEqualTo("sink-kafka");
        assertThat(ConnectorRegistry.getSink("redis").profile()).isEqualTo("sink-redis");
    }
}
