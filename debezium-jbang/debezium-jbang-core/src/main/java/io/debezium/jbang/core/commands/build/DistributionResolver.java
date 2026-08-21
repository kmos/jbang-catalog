/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.jbang.core.commands.build;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DistributionResolver {

    private static final String CENTRAL = "https://repo1.maven.org/maven2/";
    private static final String DIST_PATH = "io/debezium/debezium-server-dist";

    private final Consumer<String> logger;

    public DistributionResolver(Consumer<String> logger) {
        this.logger = logger;
    }

    public Path resolve(String version, List<String> activeProfiles) throws Exception {
        logger.accept("Fetching debezium-server-dist " + version + " manifest...");

        String pomUrl = CENTRAL + DIST_PATH + "/" + version
                + "/debezium-server-dist-" + version + ".pom";

        Document doc;
        try (InputStream is = URI.create(pomUrl).toURL().openStream()) {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
        doc.getDocumentElement().normalize();

        Map<String, String> effectiveProps = buildEffectiveProperties(doc, activeProfiles);
        List<String> compileDeps = extractCompileDeps(doc, effectiveProps);

        logger.accept("Profiles active: " + activeProfiles);
        logger.accept("Resolving " + compileDeps.size() + " top-level component(s)...");

        List<Path> jars = new MavenResolver().resolve(compileDeps, version);
        return packageZip(jars, version);
    }

    private Map<String, String> buildEffectiveProperties(Document doc, List<String> activeProfiles) {
        Map<String, String> props = new LinkedHashMap<>();

        NodeList propBlocks = doc.getElementsByTagName("properties");
        for (int i = 0; i < propBlocks.getLength(); i++) {
            Node block = propBlocks.item(i);
            if ("project".equals(block.getParentNode().getNodeName())) {
                collectChildText(block, props);
                break;
            }
        }

        NodeList profileNodes = doc.getElementsByTagName("profile");
        for (int i = 0; i < profileNodes.getLength(); i++) {
            Node profile = profileNodes.item(i);
            String id = childText(profile, "id");
            if (id != null && activeProfiles.contains(id)) {
                Node profileProps = childNode(profile, "properties");
                if (profileProps != null) {
                    collectChildText(profileProps, props);
                }
            }
        }
        return props;
    }

    private List<String> extractCompileDeps(Document doc, Map<String, String> props) {
        List<String> coords = new ArrayList<>();
        NodeList depNodes = doc.getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            Node dep = depNodes.item(i);
            Node parent = dep.getParentNode();
            if (parent == null || !"dependencies".equals(parent.getNodeName())) {
                continue;
            }
            if (parent.getParentNode() == null || !"project".equals(parent.getParentNode().getNodeName())) {
                continue;
            }

            String groupId = childText(dep, "groupId");
            String artifactId = childText(dep, "artifactId");
            String rawScope = childText(dep, "scope");
            String scope = resolveProperty(rawScope, props);

            if ("compile".equals(scope) && groupId != null && artifactId != null) {
                coords.add(groupId + ":" + artifactId);
            }
        }
        return coords;
    }

    private String resolveProperty(String value, Map<String, String> props) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("${") && value.endsWith("}")) {
            String key = value.substring(2, value.length() - 1);
            return props.getOrDefault(key, value);
        }
        return value;
    }

    private Path packageZip(List<Path> jars, String version) throws Exception {
        Path zipPath = Path.of("target", "debezium-server-" + version + ".zip");
        Files.createDirectories(zipPath.getParent());
        String libDir = "debezium-server-" + version + "/lib/";

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (Path jar : jars) {
                zos.putNextEntry(new ZipEntry(libDir + jar.getFileName()));
                Files.copy(jar, zos);
                zos.closeEntry();
                logger.accept("  + " + jar.getFileName());
            }
        }
        logger.accept("Build complete: " + jars.size() + " JARs in " + zipPath);
        return zipPath;
    }

    private void collectChildText(Node parent, Map<String, String> target) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                target.put(child.getNodeName(), child.getTextContent().trim());
            }
        }
    }

    private String childText(Node parent, String tagName) {
        Node child = childNode(parent, tagName);
        return child != null ? child.getTextContent().trim() : null;
    }

    private Node childNode(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (tagName.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }
}
