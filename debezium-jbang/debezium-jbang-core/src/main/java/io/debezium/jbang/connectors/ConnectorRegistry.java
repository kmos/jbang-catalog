/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.connectors;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class ConnectorRegistry {

    private static ConnectorsConfig config;

    private static ConnectorsConfig config() {
        if (config == null) {
            config = parse();
        }
        return config;
    }

    public static Map<String, String> load() {
        return config().connectors().stream()
                .collect(Collectors.toMap(ConnectorEntry::name, ConnectorEntry::connectorClass));
    }

    public static ConnectorEntry getConnector(String name) {
        return config().connectors().stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown connector: " + name));
    }

    public static ConnectorManifest getManifest(String name) {
        return ConnectorManifest.from(getConnector(name));
    }

    public static SinkEntry getSink(String name) {
        return config().sinks().stream()
                .filter(s -> s.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown sink: " + name));
    }

    public static String getSinkArtifact(String name) {
        return getSink(name).artifact();
    }

    public static ExtrasEntry getExtra(String name) {
        return config().extras().stream()
                .filter(e -> e.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown extra: " + name));
    }

    private static ConnectorsConfig parse() {
        try (InputStream is = ConnectorRegistry.class.getClassLoader().getResourceAsStream("connectors.yaml")) {
            if (is == null) {
                throw new RuntimeException("connectors.yaml not found on classpath");
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(is, ConnectorsConfig.class);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load connectors.yaml", e);
        }
    }

    public record ConnectorEntry(
            @JsonProperty("name") String name,
            @JsonProperty("class") String connectorClass,
            @JsonProperty("artifact") String artifact,
            @JsonProperty("profile") String profile,
            @JsonProperty("versions") List<String> versions,
            @JsonProperty("sinks") List<String> sinks) {
    }

    public record SinkEntry(
            @JsonProperty("name") String name,
            @JsonProperty("artifact") String artifact,
            @JsonProperty("profile") String profile) {
    }

    public record ExtrasEntry(
            @JsonProperty("name") String name,
            @JsonProperty("profile") String profile) {
    }

    public record ConnectorsConfig(
            @JsonProperty("connectors") List<ConnectorEntry> connectors,
            @JsonProperty("sinks") List<SinkEntry> sinks,
            @JsonProperty("extras") List<ExtrasEntry> extras) {
    }
}
