/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package main.papers;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.arc.Unremovable;

import main.database.DocumentDatabase;
import main.printer.Console;
import picocli.CommandLine;

@Dependent
@Unremovable
@CommandLine.Command(name = "add", description = "Add a paper to the database")
public class AddPaper implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The paper ID (e.g. 2504.05309v1)", completionCandidates = PaperCandidates.class)
    String paperId;

    @Inject
    DocumentDatabase documentDatabase;

    @Override
    public void run() {
        try {
            documentDatabase.insert(paperId);
        }
        catch (Exception e) {
            Console.error("Impossible to insert the paper with id: %s", paperId);
        }
    }

    public static class PaperCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            var classLoader = Thread.currentThread().getContextClassLoader();
            var resource = classLoader.getResource("papers/");
            if (resource == null) {
                return Collections.emptyIterator();
            }
            try {
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

                var ids = new ArrayList<String>();
                try (var stream = Files.list(papersDir)) {
                    stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .sorted()
                            .forEach(p -> {
                                var name = p.getFileName().toString();
                                ids.add(name.substring(0, name.length() - ".json".length()));
                            });
                }
                return ids.iterator();
            }
            catch (Exception e) {
                return Collections.emptyIterator();
            }
        }
    }
}
