/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package main.papers;

import main.out.Console;
import picocli.CommandLine;

@CommandLine.Command(name = "/papers", mixinStandardHelpOptions = true, version = "1.0", description = { "command for managing papers" }, subcommands = { AddPaper.class,
        GetPapers.class, DeletePaper.class, CommandLine.HelpCommand.class })
public class PaperCommand implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(Console.out());
    }
}
