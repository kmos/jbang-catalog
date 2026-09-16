package main.scenario;

import main.printer.Console;
import picocli.CommandLine;

@CommandLine.Command(name = "/scenario", mixinStandardHelpOptions = true, version = "1.0", description = { "command for managing Milvus Instance" }, subcommands = {
        ScenarioInit.class, MilvusList.class, CommandLine.HelpCommand.class })
public class ScenarioCommand implements Runnable {
    @Override
    public void run() {
        new CommandLine(this).usage(Console.out());
    }
}
