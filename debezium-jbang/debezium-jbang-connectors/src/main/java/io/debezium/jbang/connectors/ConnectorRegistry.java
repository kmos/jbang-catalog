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

    private static Map<String, String> instance;

    public static Map<String, String> load() {
        if (instance == null) {
            instance = parse();
        }
        return instance;
    }

    private static Map<String, String> parse() {
        try (InputStream is = ConnectorRegistry.class.getClassLoader().getResourceAsStream("connectors.yaml")) {
            if (is == null) {
                throw new RuntimeException("connectors.yaml not found on classpath");
            }
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ConnectorsConfig config = mapper.readValue(is, ConnectorsConfig.class);
            return config.connectors().stream()
                    .collect(Collectors.toMap(ConnectorEntry::name, ConnectorEntry::connectorClass));
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load connectors.yaml", e);
        }
    }

    public record ConnectorEntry(
            @JsonProperty("name") String name,
            @JsonProperty("class") String connectorClass) {
    }

    public record ConnectorsConfig(
            @JsonProperty("connectors") List<ConnectorEntry> connectors) {
    }
}
