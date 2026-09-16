package main.health;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Unremovable;

import main.printer.Console;

@ApplicationScoped
@Unremovable
public class StartupChecks {

    private static final int MAX_ATTEMPTS = 30;
    private static final int INTERVAL_MS = 2000;

    @Inject
    DataSource dataSource;

    @ConfigProperty(name = "milvus.uri")
    String milvusUri;

    @ConfigProperty(name = "debezium.url", defaultValue = "http://localhost:8080")
    String debeziumUrl;

    @ConfigProperty(name = "docker", defaultValue = "true")
    boolean dockerEnabled;

    @ConfigProperty(name = "quarkus.langchain4j.openai.base-url")
    String chatModelUrl;

    @ConfigProperty(name = "quarkus.langchain4j.openai.granite.base-url")
    String embeddingModelUrl;

    public void runAll() {
        Console.header("Service Health Checks");

        if (dockerEnabled) {
            pollUntilReady("PostgreSQL", this::pingPostgres);
            pollUntilReady("Milvus", () -> httpPing(milvusUri + "/v2/vectordb/collections/list"));
            pollUntilReady("Debezium Server", () -> httpPing(debeziumUrl));
        }
        else {
            checkService("PostgreSQL", this::pingPostgres, "Start PostgreSQL: docker compose up -d postgres");
            checkService("Milvus", () -> httpPing(milvusUri + "/v2/vectordb/collections/list"),
                    "Start Milvus: docker compose up -d milvus");
            checkService("Debezium Server", () -> httpPing(debeziumUrl),
                    "Start Debezium: docker compose up -d debezium");
        }

        checkChatModel();
        checkEmbeddingModel();

        // CHECKSTYLE:OFF
        System.out.println();
        // CHECKSTYLE:ON
    }

    private void checkService(String serviceName, Supplier<Boolean> check, String fix) {
        if (check.get()) {
            Console.checkOk(serviceName);
        }
        else {
            Console.checkFail(serviceName, fix);
        }
    }

    private void pollUntilReady(String serviceName, Supplier<Boolean> check) {
        var spinner = Console.spinner("Waiting for " + serviceName + "...");
        spinner.start();
        try {
            for (int i = 0; i < MAX_ATTEMPTS; i++) {
                if (check.get()) {
                    spinner.stop();
                    Console.checkOk(serviceName);
                    return;
                }
                Thread.sleep(INTERVAL_MS);
            }
            spinner.stop();
            Console.checkFail(serviceName, "Timed out waiting for " + serviceName + ". Check Docker logs.");
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            spinner.stop();
            Console.checkFail(serviceName, "Interrupted while waiting for " + serviceName);
        }
    }

    private boolean pingPostgres() {
        try (var conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
            return true;
        }
        catch (Exception e) {
            return false;
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
