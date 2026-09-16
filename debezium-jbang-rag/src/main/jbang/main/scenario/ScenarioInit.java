package main.scenario;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import io.quarkus.arc.Unremovable;

import main.database.DocumentDatabase;
import main.milvus.MilvusStore;
import picocli.CommandLine;

@Dependent
@CommandLine.Command(name = "init", mixinStandardHelpOptions = true, subcommands = { CommandLine.HelpCommand.class }, description = "initialize scenario")
@Unremovable
public class ScenarioInit implements Runnable {

    @Inject
    MilvusStore embeddingStore;

    @Inject
    DocumentDatabase documentDatabase;

    @Override
    public void run() {
        embeddingStore.init();
        try {
            documentDatabase.init();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
