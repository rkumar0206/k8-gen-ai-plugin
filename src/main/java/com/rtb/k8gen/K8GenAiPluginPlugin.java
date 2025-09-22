package com.rtb.k8gen;

import com.rtb.k8gen.extension.K8GenExtension;
import com.rtb.k8gen.tasks.GenerateK8DeploymentConfigTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class K8GenAiPluginPlugin implements Plugin<@NotNull Project> {

    @Override
    public void apply(Project project) {
        // Register a task
        K8GenExtension extension = project.getExtensions()
                .create("k8Gen", K8GenExtension.class);

        project.getTasks().register("generateK8DeploymentConfig", GenerateK8DeploymentConfigTask.class, task -> {
            task.getOutputDir().set(extension.getOutputDir());
            task.getConfigFilePath().set(extension.getJsonConfigFilePath());
            task.getModel().set(extension.getModel());
        });
    }
}
