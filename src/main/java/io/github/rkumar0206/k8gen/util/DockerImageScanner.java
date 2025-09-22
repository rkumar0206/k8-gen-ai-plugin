package io.github.rkumar0206.k8gen.util;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;

/**
 * Utility class for scanning a Spring Boot project to infer Docker images.
 */
public class DockerImageScanner {

    private static final Map<String, String> DEP_TO_IMAGE = Map.of(
            "org.postgresql:postgresql", "postgres:latest",
            "mysql:mysql-connector-java", "mysql:latest",
            "io.lettuce:lettuce-core", "redis:latest",
            "redis.clients:jedis", "redis:latest",
            "org.mongodb:mongodb-driver", "mongo:latest",
            "org.apache.kafka:kafka-clients", "confluentinc/cp-kafka:latest"
    );

    private static final Map<String, String> CONFIG_KEY_TO_IMAGE = Map.of(
            "jdbc:postgresql", "postgres:latest",
            "jdbc:mysql", "mysql:latest",
            "spring.redis.host", "redis:latest",
            "spring.data.mongodb.uri", "mongo:latest",
            "spring.kafka.bootstrap-servers", "confluentinc/cp-kafka:latest"
    );

    /**
     * Scans the Gradle dependencies of the project and infers possible Docker images.
     * @param project consumer project
     * @return list of additional docker images from build.gradle dependencies
     */
    public static List<String> scanDependenciesForDockerImages(Project project) {
        Set<String> images = new HashSet<>();

        Configuration runtimeClasspath = project.getConfigurations().findByName("runtimeClasspath");
        if (runtimeClasspath != null) {
            Set<ResolvedArtifact> artifacts = runtimeClasspath.resolve().stream()
                    .flatMap(f -> runtimeClasspath.getResolvedConfiguration().getResolvedArtifacts().stream())
                    .collect(Collectors.toSet());

            for (ResolvedArtifact artifact : artifacts) {
                String gav = artifact.getModuleVersion().getId().getGroup() + ":" + artifact.getName();
                DEP_TO_IMAGE.forEach((dep, image) -> {
                    if (gav.startsWith(dep)) {
                        images.add(image);
                    }
                });
            }
        }

        return new ArrayList<>(images);
    }

    /**
     * Scans application.properties or application.yml and infers possible Docker images.
     *
     * @param project consumer project
     * @return list of additional docker images from properties file dependencies
     */
    public static List<String> scanConfigFilesForDockerImages(Project project) {
        Set<String> images = new HashSet<>();

        Path resourcesDir = project.getProjectDir().toPath().resolve("src/main/resources");

        // application.properties
        Path propsFile = resourcesDir.resolve("application.properties");
        if (Files.exists(propsFile)) {
            try {
                List<String> lines = Files.readAllLines(propsFile);
                for (String line : lines) {
                    CONFIG_KEY_TO_IMAGE.forEach((key, image) -> {
                        if (line.contains(key)) {
                            images.add(image);
                        }
                    });
                }
            } catch (IOException ignored) {}
        }

        // application.yml
        Path ymlFile = resourcesDir.resolve("application.yml");
        if (Files.exists(ymlFile)) {
            try (FileInputStream fis = new FileInputStream(ymlFile.toFile())) {
                Yaml yaml = new Yaml();
                Object data = yaml.load(fis);
                String yamlContent = data.toString();
                CONFIG_KEY_TO_IMAGE.forEach((key, image) -> {
                    if (yamlContent.contains(key)) {
                        images.add(image);
                    }
                });
            } catch (IOException ignored) {}
        }

        return new ArrayList<>(images);
    }
}
