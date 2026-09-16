/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

//JAVA 21+
//REPOS central=https://repo1.maven.org/maven2,apache-snapshot=https://repository.apache.org/content/groups/snapshots/
//JAVA_OPTIONS -Ddebezium.jbang.quarkusVersion=3.27.5 --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.misc=ALL-UNNAMED --enable-native-access=ALL-UNNAMED
//JAVA_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager
//DEPS io.quarkus.platform:quarkus-bom:3.27.5@pom
//DEPS io.quarkus.platform:quarkus-langchain4j-bom:3.27.5@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS info.picocli:picocli-shell-jline3:4.7.7
//DEPS io.quarkus:quarkus-arc
//DEPS io.quarkus:quarkus-jdbc-postgresql
//DEPS jakarta.enterprise:jakarta.enterprise.cdi-api
//DEPS io.milvus:milvus-sdk-java:2.5.9
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-milvus
//DEPS io.quarkiverse.langchain4j:quarkus-langchain4j-openai
//DEPS io.smallrye:jandex:3.5.3
//DEPS org.testcontainers:testcontainers:1.21.1
//SOURCES RagCommands.java
//SOURCES ai/Chat.java
//SOURCES ai/MilvusRetrievalAugmentor.java
//SOURCES database/DocumentDatabase.java
//SOURCES health/StartupChecks.java
//SOURCES milvus/MilvusStore.java
//SOURCES printer/Console.java
//SOURCES papers/AddPaper.java
//SOURCES papers/DeletePaper.java
//SOURCES papers/GetPapers.java
//SOURCES papers/PaperCommand.java
//SOURCES scenario/MilvusList.java
//SOURCES scenario/ScenarioCommand.java
//SOURCES scenario/ScenarioInit.java
//SOURCES docker/DockerEnvironment.java
//SOURCES ExitCommand.java
//FILES papers/2504.05309v1.json=data/papers/2504.05309v1.json
//FILES papers/2609.13082.json=data/papers/2609.13082.json
//FILES papers/2609.13118.json=data/papers/2609.13118.json
//FILES docker/image-server/Dockerfile=docker/image-server/Dockerfile
//FILES docker/config/config-postgres/ai-sample-data.sql=docker/config/config-postgres/ai-sample-data.sql
//FILES docker/config/config-server/application.properties=docker/config/config-server/application.properties
//Q:CONFIG quarkus.banner.enabled=false
//FILES config/application.properties

package main;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.TerminalBuilder;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import main.ai.Chat;
import main.docker.DockerEnvironment;
import main.health.StartupChecks;
import main.printer.Console;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliJLineCompleter;


@QuarkusMain
@ApplicationScoped
public class RagCLI implements QuarkusApplication {

    @Inject
    Chat chat;

    @Inject
    DockerEnvironment dockerEnvironment;

    @Inject
    StartupChecks startupChecks;

    @Override
    public int run(String... args) throws Exception {
        CommandLine.IFactory cdiFactory = new CommandLine.IFactory() {
            @Override
            public <K> K create(Class<K> cls) throws Exception {
                var handle = Arc.container().instance(cls);
                if (handle.isAvailable()) {
                    return handle.get();
                }
                return CommandLine.defaultFactory().create(cls);
            }
        };

        RagCommands ragCommands = cdiFactory.create(RagCommands.class);
        CommandLine cmd = new CommandLine(ragCommands, cdiFactory);

        Console.welcome();
        dockerEnvironment.startAll();

        try (var terminal = TerminalBuilder.builder().build()) {
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new PicocliJLineCompleter(cmd.getCommandSpec()))
                    .parser(new DefaultParser())
                    .build();
            ragCommands.setReader(reader);
            startupChecks.runAll();

            while (true) {
                try {
                    String line = reader.readLine("> ").trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    if (line.startsWith("/")) {
                        cmd.execute(line.split("\\s+"));
                        if (ragCommands.isExitRequested()) {
                            break;
                        }
                    }
                    else {
                        handleChat(chat, line);
                    }
                }
                catch (UserInterruptException ignored) {
                }
                catch (EndOfFileException e) {
                    break;
                }
                catch (Exception e) {
                    Console.error(e.getMessage());
                }
            }
        }

        dockerEnvironment.stopAll();
        return 0;
    }

    private void handleChat(Chat chat, String query) {
        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        var spinner = Console.spinner("Thinking...");
        try {
            long start = System.currentTimeMillis();
            spinner.start();
            var reply = chat.chat(query);
            long elapsed = System.currentTimeMillis() - start;
            spinner.stop();
            Console.reply(reply);
            Console.cooked(elapsed);
        }
        finally {
            spinner.stop();
            requestContext.terminate();
        }
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("-D") && arg.contains("=")) {
                String key = arg.substring(2, arg.indexOf('='));
                String value = arg.substring(arg.indexOf('=') + 1);
                System.setProperty(key, value);
            }
        }
        Quarkus.run(RagCLI.class, args);
    }
}
