package main;

import jakarta.enterprise.context.Dependent;

import io.quarkus.arc.Unremovable;

import picocli.CommandLine;

@Dependent
@Unremovable
@CommandLine.Command(name = "/exit", description = "Stop all containers and exit")
public class ExitCommand implements Runnable {

    @CommandLine.ParentCommand
    RagCommands parent;

    @Override
    public void run() {
        parent.requestExit();
    }
}
