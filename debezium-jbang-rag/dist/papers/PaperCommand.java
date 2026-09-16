package main.papers;

import main.printer.Console;
import picocli.CommandLine;

@CommandLine.Command(name = "/papers", mixinStandardHelpOptions = true, version = "1.0", description = { "command for managing papers" }, subcommands = { AddPaper.class,
        GetPapers.class, DeletePaper.class, CommandLine.HelpCommand.class })
public class PaperCommand implements Runnable {

    @Override
    public void run() {
        new CommandLine(this).usage(Console.out());
    }
}
