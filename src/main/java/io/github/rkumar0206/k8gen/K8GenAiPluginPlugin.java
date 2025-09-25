package io.github.rkumar0206.k8gen;

import io.github.rkumar0206.k8gen.extension.K8GenExtension;
import io.github.rkumar0206.k8gen.tasks.GenerateK8DeploymentConfigTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

/**
 * A Gradle plugin that automates the generation of Kubernetes deployment configurations.
 *
 * <p>This plugin introduces a new Gradle extension named `k8Gen` and a task called
 * `generateK8DeploymentConfig`. The extension allows users to configure the output directory,
 * the path to a JSON configuration file, and the AI model to be used for generating the
 * Kubernetes manifests. The task uses these configurations to perform the actual generation.
 *
 * <p>By applying this plugin to a Gradle project, developers can streamline their CI/CD
 * pipeline by programmatically creating Kubernetes manifests from a simple configuration,
 * reducing the need for manual YAML authoring and maintenance.
 */
public class K8GenAiPluginPlugin implements Plugin<@NotNull Project> {

    /**
     * Applies the plugin to a given Gradle project.
     *
     * <p>This method performs two main actions:
     * <ul>
     * <li>It registers the `k8Gen` extension, allowing for user-defined configuration
     * of the plugin's behavior.</li>
     * <li>It registers the `generateK8DeploymentConfig` task, which is responsible for
     * generating the Kubernetes configuration files. The task is configured with the
     * properties defined in the `k8Gen` extension.</li>
     * </ul>
     *
     * @param project The Gradle project to which this plugin is being applied.
     */
    @Override
    public void apply(Project project) {
        // Register a task
        K8GenExtension extension = project.getExtensions()
                .create("k8Gen", K8GenExtension.class);

        project.getTasks().register("generateK8DeploymentConfig", GenerateK8DeploymentConfigTask.class, task -> {
            task.getOutputDir().set(extension.getOutputDir());
            task.getConfigFilePath().set(extension.getJsonConfigFilePath());
            task.getModel().set(extension.getModel());
            task.getGeminiAPIKey().set(extension.getGeminiAPIKey());
        });
    }
}
