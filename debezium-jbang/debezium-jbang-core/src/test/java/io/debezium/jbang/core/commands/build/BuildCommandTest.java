/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildCommandTest {

    private BuildCommand silentCommand(String configPath) {
        BuildCommand cmd = new BuildCommand(null) {
            @Override
            public void println(String line) {
            }

            @Override
            public void println() {
            }

            @Override
            public void print(String output) {
            }

            @Override
            public void printf(String format, Object... args) {
            }
        };
        cmd.configPath = configPath;
        return cmd;
    }

    @Test
    void normalizeVersionAppendsFinalSuffix() {
        assertThat(BuildCommand.normalizeVersion("3.7.0")).isEqualTo("3.7.0.Final");
        assertThat(BuildCommand.normalizeVersion("3.7.0.Final")).isEqualTo("3.7.0.Final");
        assertThat(BuildCommand.normalizeVersion("3.7.0.Alpha1")).isEqualTo("3.7.0.Alpha1");
        assertThat(BuildCommand.normalizeVersion("3.7.0.Beta1")).isEqualTo("3.7.0.Beta1");
        assertThat(BuildCommand.normalizeVersion("3.7.0.CR1")).isEqualTo("3.7.0.CR1");
        assertThat(BuildCommand.normalizeVersion("3.7.0-SNAPSHOT")).isEqualTo("3.7.0-SNAPSHOT");
    }

    @Test
    void doCallReturnsOneWhenConfigFileMissing() throws Exception {
        assertThat(silentCommand("nonexistent-config.yaml").doCall()).isEqualTo(1);
    }

    @Test
    void doCallReturnsOneForUnknownConnector(@TempDir Path tempDir) throws Exception {
        Path cfg = tempDir.resolve("dbz.yaml");
        Files.writeString(cfg, "version: \"3.7.0\"\nsource:\n  type: unknowndb\nsink:\n  type: kafka\n");
        assertThat(silentCommand(cfg.toString()).doCall()).isEqualTo(1);
    }

    @Test
    void doCallReturnsOneForUnknownSink(@TempDir Path tempDir) throws Exception {
        Path cfg = tempDir.resolve("dbz.yaml");
        Files.writeString(cfg, "version: \"3.7.0\"\nsource:\n  type: postgres\nsink:\n  type: unknownsink\n");
        assertThat(silentCommand(cfg.toString()).doCall()).isEqualTo(1);
    }
}
