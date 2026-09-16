package main.papers;

import java.io.IOException;
import java.net.URISyntaxException;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.arc.Unremovable;

import main.database.DocumentDatabase;
import main.printer.Console;
import picocli.CommandLine;

@Dependent
@Unremovable
@CommandLine.Command(name = "available", description = "Add a paper to the database")
public class GetPapers implements Runnable {

    @Inject
    DocumentDatabase documentDatabase;

    @Override
    public void run() {
        try {
            documentDatabase.listPapers();
        }
        catch (IOException | URISyntaxException e) {
            Console.error("Impossible to get available papers");
        }
    }
}
