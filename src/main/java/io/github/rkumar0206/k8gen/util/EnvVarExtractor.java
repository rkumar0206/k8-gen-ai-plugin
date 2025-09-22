package io.github.rkumar0206.k8gen.util;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting environment variable references
 * from Spring Boot application.properties or application.yml.
 */
public class EnvVarExtractor {

    // Pattern matches ${VAR} or ${VAR:default}
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)(?::[^}]*)?}");

    /**
     * Extracts environment variable names from application.properties or application.yml.
     *
     * @param project consumer project
     * @return set of environment variable names found
     * @throws IOException if file cannot be read
     */
    public static Set<String> extractEnvVars(Project project) throws IOException {
        Set<String> envVars = new HashSet<>();

        File propsFile = project.file("src/main/resources/application.properties");
        File ymlFile = project.file("src/main/resources/application.yml");

        if (propsFile.exists()) {
            envVars.addAll(extractFromFile(propsFile));
        }

        if (ymlFile.exists()) {
            envVars.addAll(extractFromFile(ymlFile));
        }

        if (envVars.isEmpty() && !propsFile.exists() && !ymlFile.exists()) {
            System.out.println("No application.properties or application.yml found in src/main/resources");
        }

        return envVars;
    }

    private static Set<String> extractFromFile(File file) throws IOException {
        Set<String> vars = new HashSet<>();
        String content = Files.readString(file.toPath());

        Matcher matcher = ENV_PATTERN.matcher(content);
        while (matcher.find()) {
            vars.add(matcher.group(1)); // only the variable name
        }
        return vars;
    }

    EnvVarExtractor() {

    }
}
