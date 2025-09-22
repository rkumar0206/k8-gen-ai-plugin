package com.rtb.k8gen.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DeploymentConfig {
    private String gradleVersion;
    private String javaVersion;
    private List<DockerImage> additionalDockerImages;
    private String applicationName;
    private String dbUsername;
    private String dbPassword;
    private String dbName;
    private int port;
    private int replicas;
    private String namespace;
    private String imageRegistry;
    private String imageTag;
    private boolean enableHPA;
    private int hpaMinReplicas;
    private int hpaMaxReplicas;
    private String cpuRequest;
    private String memoryRequest;
    private String cpuLimit;
    private String memoryLimit;
    private String ingressHost;
    private String tlsSecretName;
    private boolean includeDatabase;
    private Migrations migrations;
    private Map<String, String> secrets;
    private Map<String, String> configd;
    private List<String> extraK8sResources;
}

