/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package main;

import java.io.PrintWriter;

import org.jline.reader.LineReader;

import main.papers.PaperCommand;
import main.scenario.ScenarioCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "rag", description = "RAG CLI", footer = { "", "Press Ctrl-D to exit." }, subcommands = { ScenarioCommand.class,
        PaperCommand.class
})
public class RagCommands implements Runnable {

    PrintWriter out;

    RagCommands() {
    }

    public void setReader(LineReader reader) {
        out = reader.getTerminal().writer();
    }

    @Override
    public void run() {
        out.println(new CommandLine(this).getUsageMessage());
    }
}
