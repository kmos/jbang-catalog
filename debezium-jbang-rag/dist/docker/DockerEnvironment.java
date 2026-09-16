/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package main.docker;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import io.quarkus.arc.Unremovable;

import main.printer.Console;

@ApplicationScoped
@Unremovable
@SuppressWarnings({ "resource", "deprecation" })
public class DockerEnvironment {

    @ConfigProperty(name = "docker", defaultValue = "true")
    boolean dockerEnabled;

    private Network network;
    private GenericContainer<?> postgres;
    private GenericContainer<?> milvus;
    private GenericContainer<?> debeziumServer;

    public boolean isEnabled() {
        return dockerEnabled;
    }

    public void startAll() {
        if (!dockerEnabled) {
            return;
        }

        Console.header("Starting Docker Containers");

        network = Network.newNetwork();

        postgres = new FixedHostPortGenericContainer<>("quay.io/debezium/example-postgres:3.2")
                .withFixedExposedPort(5432, 5432)
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withEnv("POSTGRES_USER", "postgres")
                .withEnv("POSTGRES_PASSWORD", "postgres")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("docker/config/config-postgres/ai-sample-data.sql"),
                        "/docker-entrypoint-initdb.d/ai-sample-data.sql")
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2)
                        .withStartupTimeout(Duration.ofSeconds(60)));

        milvus = new FixedHostPortGenericContainer<>("milvusdb/milvus:v2.5.4")
                .withFixedExposedPort(19530, 19530)
                .withFixedExposedPort(9091, 9091)
                .withNetwork(network)
                .withNetworkAliases("milvus")
                .withEnv("ETCD_USE_EMBED", "true")
                .withEnv("COMMON_STORAGETYPE", "local")
                .withCommand("milvus", "run", "standalone")
                .waitingFor(Wait.forHttp("/healthz").forPort(9091)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(90)));

        var spinner = Console.spinner("Starting PostgreSQL and Milvus...");
        spinner.start();
        try {
            CompletableFuture.allOf(
                    CompletableFuture.runAsync(() -> postgres.start()),
                    CompletableFuture.runAsync(() -> milvus.start())).join();
        }
        finally {
            spinner.stop();
        }
        Console.checkOk("PostgreSQL");
        Console.checkOk("Milvus");

        var debeziumImage = new ImageFromDockerfile("debezium-server-openai", false)
                .withFileFromClasspath("Dockerfile", "docker/image-server/Dockerfile");
        debeziumServer = new FixedHostPortGenericContainer<>(debeziumImage.get())
                .withFixedExposedPort(8080, 8080)
                .withNetwork(network)
                .withNetworkAliases("debezium-server")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("docker/config/config-server/application.properties"),
                        "/debezium/config/application.properties")
                .withExtraHost("host.docker.internal", "host-gateway")
                .waitingFor(Wait.forLogMessage(".*Connector started.*", 1)
                        .withStartupTimeout(Duration.ofSeconds(120)));

        spinner = Console.spinner("Building and starting Debezium Server...");
        spinner.start();
        try {
            debeziumServer.start();
        }
        finally {
            spinner.stop();
        }
        Console.checkOk("Debezium Server");
    }

    public void stopAll() {
        if (!dockerEnabled) {
            return;
        }

        Console.header("Stopping Docker Containers");

        if (debeziumServer != null) {
            try {
                debeziumServer.stop();
                Console.checkOk("Debezium Server stopped");
            }
            catch (Exception e) {
                Console.error("Failed to stop Debezium Server: %s", e.getMessage());
            }
        }
        if (milvus != null) {
            try {
                milvus.stop();
                Console.checkOk("Milvus stopped");
            }
            catch (Exception e) {
                Console.error("Failed to stop Milvus: %s", e.getMessage());
            }
        }
        if (postgres != null) {
            try {
                postgres.stop();
                Console.checkOk("PostgreSQL stopped");
            }
            catch (Exception e) {
                Console.error("Failed to stop PostgreSQL: %s", e.getMessage());
            }
        }
        if (network != null) {
            try {
                network.close();
            }
            catch (Exception e) {
                Console.error("Failed to close network: %s", e.getMessage());
            }
        }
    }
}
