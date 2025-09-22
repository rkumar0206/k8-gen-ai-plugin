package io.github.rkumar0206.k8gen.util;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaToolchainService;

/**
 * A utility class for retrieving version information related to a Gradle project.
 *
 * <p>This class provides static methods to get the version of the Gradle distribution
 * and the Java version configured for a project. It prioritizes reading the Java
 * toolchain configuration if specified in the project's build file, and falls back to
 * the currently running JVM's version otherwise.
 */
public class VersionUtils {

    /**
     * Retrieves the version of the Gradle distribution being used by the project.
     *
     * @param project The Gradle project instance.
     * @return A {@code String} representing the Gradle version.
     */
    public static String getGradleVersion(Project project) {
        return project.getGradle().getGradleVersion();
    }

    /**
     * Retrieves the Java version configured for the project.
     *
     * <p>This method first attempts to read the Java version from the project's toolchain
     * configuration. If a toolchain is not specified or cannot be determined, it falls back
     * to the version of the JVM currently running the Gradle build.
     *
     * @param project The Gradle project instance.
     * @return A {@code String} representing the Java version.
     */
    public static String getJavaVersion(Project project) {
        // Try to read Java toolchain config if available
        JavaPluginExtension javaExt = project.getExtensions().findByType(JavaPluginExtension.class);
        if (javaExt != null) {
            javaExt.getToolchain();
            JavaToolchainService service = project.getExtensions().findByType(JavaToolchainService.class);
            if (service != null) {
                try {
                    int toolchainVersion = service
                            .launcherFor(javaExt.getToolchain())
                            .get()
                            .getMetadata()
                            .getLanguageVersion()
                            .asInt();
                    return String.valueOf(toolchainVersion);
                } catch (Exception ignored) {
                    // fall through to default
                }
            }
        }

        // Fallback to current JVM version
        return JavaVersion.current().toString();
    }

    /** Private constructor to prevent instantiation. */
    private VersionUtils() {
        // utility class
    }
}