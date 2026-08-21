/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

public class MavenResolver {

    private static final String CENTRAL_URL = "https://repo1.maven.org/maven2/";

    private final RepositorySystem system;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    public MavenResolver() {
        this.system = newRepositorySystem();
        this.session = newSession(system);
        this.repositories = List.of(
                new RemoteRepository.Builder("central", "default", CENTRAL_URL).build());
    }

    public List<Path> resolve(List<String> artifacts, String version) throws Exception {
        List<Dependency> dependencies = new ArrayList<>();
        for (String artifact : artifacts) {
            dependencies.add(new Dependency(new DefaultArtifact(artifact + ":" + version), "runtime"));
        }

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setDependencies(dependencies);
        collectRequest.setRepositories(repositories);

        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest,
                new ScopeDependencyFilter("test", "provided"));

        DependencyResult result = system.resolveDependencies(session, dependencyRequest);

        List<Path> jars = new ArrayList<>();
        for (var artifactResult : result.getArtifactResults()) {
            Artifact resolved = artifactResult.getArtifact();
            if (resolved != null && resolved.getFile() != null) {
                jars.add(resolved.getFile().toPath());
            }
        }
        return jars;
    }

    @SuppressWarnings("deprecation")
    private static RepositorySystem newRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private static DefaultRepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        String localRepoPath = System.getProperty("user.home") + "/.m2/repository";
        LocalRepository localRepo = new LocalRepository(new File(localRepoPath));
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }
}
