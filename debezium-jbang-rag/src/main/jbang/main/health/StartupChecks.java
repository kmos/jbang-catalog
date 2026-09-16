/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package main.health;

import java.net.HttpURLConnection;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Unremovable;

import main.printer.Console;

@ApplicationScoped
@Unremovable
public class StartupChecks {

    @Inject
    DataSource dataSource;

    @ConfigProperty(name = "milvus.uri")
    String milvusUri;

    @ConfigProperty(name = "debezium.url", defaultValue = "http://localhost:8080")
    String debeziumUrl;

    @ConfigProperty(name = "quarkus.langchain4j.openai.base-url")
    String chatModelUrl;

    @ConfigProperty(name = "quarkus.langchain4j.openai.granite.base-url")
    String embeddingModelUrl;

    public void runAll() {
        Console.header("Service Health Checks");

        checkPostgres();
        checkMilvus();
        checkDebezium();
        checkChatModel();
        checkEmbeddingModel();

        // CHECKSTYLE:OFF
        System.out.println();
        // CHECKSTYLE:ON
    }

    private void checkPostgres() {
        try (var conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
            Console.checkOk("PostgreSQL");
        }
        catch (Exception e) {
            Console.checkFail("PostgreSQL", "Start PostgreSQL: docker compose up -d postgres");
        }
    }

    private void checkMilvus() {
        if (httpPing(milvusUri + "/v2/vectordb/collections/list")) {
            Console.checkOk("Milvus");
        }
        else {
            Console.checkFail("Milvus", "Start Milvus: docker compose up -d milvus");
        }
    }

    private void checkDebezium() {
        if (httpPing(debeziumUrl)) {
            Console.checkOk("Debezium Server");
        }
        else {
            Console.checkFail("Debezium Server", "Start Debezium: docker compose up -d debezium");
        }
    }

    private void checkChatModel() {
        if (httpPing(chatModelUrl + "/models")) {
            Console.checkOk("Chat Model");
        }
        else {
            Console.checkFail("Chat Model", "Start your LLM server on " + chatModelUrl);
        }
    }

    private void checkEmbeddingModel() {
        if (httpPing(embeddingModelUrl + "/models")) {
            Console.checkOk("Embedding Model");
        }
        else {
            Console.checkFail("Embedding Model", "Start your embedding server on " + embeddingModelUrl);
        }
    }

    private boolean httpPing(String url) {
        try {
            var conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 500;
        }
        catch (Exception e) {
            return false;
        }
    }
}
