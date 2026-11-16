package com.hmydk.aigit.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the plugin's runtime classpath contract.
 *
 * <p>The plugin is published as a single jar. On the Marketplace, the 2.2.1 artifact
 * ({@code AI_git_commit-2.2.1.jar}) contained the plugin classes only: no {@code lib/} directory
 * and no Jackson jars, while {@code OpenAIUtil} referenced {@code com.fasterxml.jackson.databind}.
 * That worked on IDEs that happened to ship Jackson, but PyCharm 2026.3 no longer provides it, so
 * generating a commit message failed with
 * {@code NoClassDefFoundError: com/fasterxml/jackson/databind/ObjectMapper} (issue #103).</p>
 *
 * <p>The plugin now uses Gson, which the IntelliJ Platform bundles, so the artifact needs no
 * third-party jars at all. This test keeps it that way: a class file that references Jackson means
 * the published jar would fail again on an IDE without Jackson on its classpath.</p>
 */
class PluginDistributionTest {

    private static final String FORBIDDEN_PACKAGE = "com/fasterxml/jackson";

    @Test
    void mainClassesDoNotReferenceJackson() throws IOException {
        Path mainClasses = Paths.get(System.getProperty("user.dir"), "build", "classes", "java", "main");
        assertTrue(Files.isDirectory(mainClasses),
                "compiled main classes are missing; the test must run after compileJava: " + mainClasses);

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(mainClasses)) {
            for (Path classFile : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                if (referencesForbiddenPackage(classFile)) {
                    violations.add(mainClasses.relativize(classFile).toString());
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "the plugin jar must not reference " + FORBIDDEN_PACKAGE
                        + " (not bundled in the published single-jar artifact): " + violations);
    }

    private boolean referencesForbiddenPackage(Path classFile) throws IOException {
        // Class names are stored as UTF-8 constants in the constant pool, so a byte scan is enough
        // to find references without loading the class (which would need the dependency itself).
        String content = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
        return content.contains(FORBIDDEN_PACKAGE.replace('.', '/'));
    }
}
