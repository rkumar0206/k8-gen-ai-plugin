package io.github.rkumar0206.k8gen.extension;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

/**
 * A Gradle extension that provides configuration properties for the K8Gen Gradle plugin.
 *
 * <p>This class allows users to customize the behavior of the `generateK8DeploymentConfig`
 * task by specifying the output directory for generated files, the path to the
 * JSON configuration file, and the AI model to be used for generation.
 *
 * <p>The properties are managed by Gradle's {@code Property} type, which allows for lazy
 * evaluation and ensures that values can be set by the user in the build script. Default
 * values are provided for each property to ensure the plugin can function out of the box.
 */
@Getter
@AllArgsConstructor
public class K8GenExtension {

    /**
     * The output directory where the generated Kubernetes configuration files will be saved.
     * The default value is the project's root directory (`/`).
     */
    private final Property<@NotNull String> outputDir;

    /**
     * The path to the JSON file containing the application's deployment configuration details.
     * The default value is `k8-config.json` in the project's root directory.
     */
    private final Property<@NotNull String> jsonConfigFilePath;

    /**
     * The name of the AI model to be used for generating the Kubernetes configuration.
     * The default value is `gemini-2.5-flash`.
     */
    private final Property<String> model;

    /**
     * Constructs a new `K8GenExtension` with default values.
     *
     * <p>This constructor is used by Gradle to create an instance of the extension and
     * injects an {@code ObjectFactory} to create the {@code Property} instances.
     *
     * @param objects The {@code ObjectFactory} provided by Gradle for creating property instances.
     */
    @Inject
    public K8GenExtension(ObjectFactory objects) {
        this.outputDir = objects.property(String.class).convention("/");
        this.jsonConfigFilePath = objects.property(String.class).convention("/k8-config.json");
        this.model = objects.property(String.class).convention("gemini-2.5-flash");

    }
}