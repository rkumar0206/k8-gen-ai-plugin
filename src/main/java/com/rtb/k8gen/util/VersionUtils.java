package com.rtb.k8gen.util;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaToolchainService;

public class VersionUtils {

    /**
     * Returns the Gradle version used to run the current build.
     */
    public static String getGradleVersion(Project project) {
        return project.getGradle().getGradleVersion();
    }

    /**
     * Returns the target Java version of the project.
     * - Prefers toolchain declaration if present.
     * - Falls back to the current JVM (Gradle daemon).
     */
    public static String getJavaVersion(Project project) {
        // Try to read Java toolchain config if available
        JavaPluginExtension javaExt = project.getExtensions().findByType(JavaPluginExtension.class);
        if (javaExt != null && javaExt.getToolchain() != null) {
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
}

