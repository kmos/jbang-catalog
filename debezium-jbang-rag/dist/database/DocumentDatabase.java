/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package main.database;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Unremovable;

import main.printer.Console;

@ApplicationScoped
@Unremovable
public class DocumentDatabase {

    @ConfigProperty(name = "debezium.rag.demo.document.truncate", defaultValue = "2048")
    int documentSize;

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    public void init() throws Exception {
        try (var conn = dataSource.getConnection()) {
            conn.createStatement().execute("TRUNCATE TABLE ai.documents");
        }
    }

    public void delete(String paperId) throws Exception {
        var sql = "DELETE FROM ai.documents WHERE id = ?";

        try (var conn = dataSource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paperId);
            stmt.executeUpdate();
        }
    }

    public void insert(String paperId) throws Exception {
        final var paper = loadPaper(paperId);

        final var text = "# Title\n%s\n\n# Authors\n%s\n\n# Abstract\n%s"
                .formatted(paper.title, paper.authors, paper.abstractText);
        final var truncated = text.substring(0, Math.min(text.length(), documentSize));

        final var sql = "INSERT INTO ai.documents VALUES (?, ?::json, ?)";
        try (var conn = dataSource.getConnection();
                var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, paperId);
            stmt.setString(2, objectMapper.writeValueAsString(new PaperMetadata(paperId, paper.title)));
            stmt.setString(3, truncated);
            stmt.executeUpdate();
        }
        Console.success("Inserted paper '%s': %s", paperId, paper.title);
    }

    public void listPapers() throws IOException, URISyntaxException {
        var classLoader = Thread.currentThread().getContextClassLoader();
        var resource = classLoader.getResource("papers/");
        if (resource == null) {
            Console.warn("No papers directory found.");
            return;
        }

        Path papersDir;
        var uri = resource.toURI();
        if ("jar".equals(uri.getScheme())) {
            try {
                papersDir = FileSystems.getFileSystem(uri).getPath("papers/");
            }
            catch (Exception e) {
                papersDir = FileSystems.newFileSystem(uri, Collections.emptyMap()).getPath("papers/");
            }
        }
        else {
            papersDir = Paths.get(uri);
        }

        Console.header("Available Papers");
        try (var stream = Files.list(papersDir)) {
            stream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(p -> {
                        var fileName = p.getFileName().toString();
                        var id = fileName.substring(0, fileName.length() - ".json".length());
                        try {
                            var paper = loadPaper(id);
                            Console.label("  " + Console.BOLD + Console.CYAN + id + Console.RESET, paper.title());
                        }
                        catch (IOException e) {
                            Console.error("  %-20s (error reading: %s)", id, e.getMessage());
                        }
                    });
        }
    }

    private Paper loadPaper(String paperId) throws IOException {
        final var path = "papers/" + paperId + ".json";
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Paper not found in resources: " + path);
            }
            return objectMapper.readValue(is, Paper.class);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Paper(String title, String authors, @com.fasterxml.jackson.annotation.JsonProperty("abstract") String abstractText) {
    }

    record PaperMetadata(String id, String title) {
    }
}
