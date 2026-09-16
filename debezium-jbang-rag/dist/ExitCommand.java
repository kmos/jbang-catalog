/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
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
