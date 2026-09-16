/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package main.scenario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.arc.Unremovable;

import main.milvus.MilvusStore;
import main.printer.Console;
import picocli.CommandLine;

@CommandLine.Command(name = "milvus", mixinStandardHelpOptions = true, subcommands = { CommandLine.HelpCommand.class }, description = "list milvus vectors")
@ApplicationScoped
@Unremovable
public class MilvusList implements Runnable {

    @Inject
    MilvusStore milvusStore;

    @Override
    public void run() {
        try {
            milvusStore.list();
        }
        catch (Exception e) {
            Console.hint("Collection not found", "/scenario init");
        }
    }
}
