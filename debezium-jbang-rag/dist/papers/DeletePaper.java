package main.papers;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.arc.Unremovable;

import main.database.DocumentDatabase;
import main.printer.Console;
import picocli.CommandLine;

@Dependent
@Unremovable
@CommandLine.Command(name = "delete", description = "Add a paper to the database")
public class DeletePaper implements Runnable {

    @CommandLine.Parameters(index = "0", description = "The paper ID (e.g. 2504.05309v1)")
    String paperId;

    @Inject
    DocumentDatabase documentDatabase;

    @Override
    public void run() {
        try {
            documentDatabase.delete(paperId);
        }
        catch (Exception e) {
            Console.error("Impossible to delete paper with id: %s", paperId);
        }
    }
}
