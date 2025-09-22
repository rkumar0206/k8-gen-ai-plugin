package com.rtb.k8gen.extension;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

@Getter
@AllArgsConstructor
public class K8GenExtension {
    private final Property<@NotNull String> outputDir;
    private final Property<@NotNull String> jsonConfigFilePath;
    private final Property<String> model;

    @Inject
    public K8GenExtension(ObjectFactory objects) {
        this.outputDir = objects.property(String.class).convention("/");
        this.jsonConfigFilePath = objects.property(String.class).convention("/k8-config.json");
        this.model = objects.property(String.class).convention("gemini-2.5-flash");

    }
}