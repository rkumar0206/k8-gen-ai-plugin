package io.github.rkumar0206.k8gen.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * A data class representing the comprehensive configuration for a Kubernetes deployment.
 *
 * <p>This class encapsulates all the necessary parameters required to generate a complete
 * set of Kubernetes manifests, including Deployment, Service, Ingress, and optional
 * resources like Horizontal Pod Autoscaler (HPA), database configurations, and custom
 * secrets. It serves as the primary input for the AI-powered generation process.
 */
@Data
public class DeploymentConfig {
    /**
     * The Gradle version of the project.
     */
    private String gradleVersion;
    /**
     * The Java version used by the project.
     */
    private String javaVersion;
    /**
     * A list of additional Docker images to be included in the deployment.
     */
    private List<DockerImage> additionalDockerImages;
    /**
     * The name of the application. This is typically used as a prefix for Kubernetes resource names.
     */
    private String applicationName;
    /**
     * The database username.
     */
    private String dbUsername;
    /**
     * The database password.
     */
    private String dbPassword;
    /**
     * The name of the database.
     */
    private String dbName;
    /**
     * The port on which the application listens.
     */
    private int port;
    /**
     * The number of replicas for the deployment.
     */
    private int replicas;
    /**
     * The Kubernetes namespace where the resources will be deployed.
     */
    private String namespace;
    /**
     * The Docker image registry to pull the application image from.
     */
    private String imageRegistry;
    /**
     * The tag of the Docker image to be deployed.
     */
    private String imageTag;
    /**
     * A flag indicating whether to enable Horizontal Pod Autoscaling (HPA).
     */
    private boolean enableHPA;
    /**
     * The minimum number of replicas for HPA.
     */
    private int hpaMinReplicas;
    /**
     * The maximum number of replicas for HPA.
     */
    private int hpaMaxReplicas;
    /**
     * The CPU request for the application's container.
     */
    private String cpuRequest;
    /**
     * The memory request for the application's container.
     */
    private String memoryRequest;
    /**
     * The CPU limit for the application's container.
     */
    private String cpuLimit;
    /**
     * The memory limit for the application's container.
     */
    private String memoryLimit;
    /**
     * The host name for the Ingress resource.
     */
    private String ingressHost;
    /**
     * The name of the TLS secret to be used with the Ingress.
     */
    private String tlsSecretName;
    /**
     * A flag indicating whether to include database-related resources in the deployment.
     */
    private boolean includeDatabase;
    /**
     * Configuration for database migrations.
     */
    private Migrations migrations;
    /**
     * A map of secrets to be created in Kubernetes. The key is the secret name, and the value is the secret data.
     */
    private Map<String, String> secrets;
    /**
     * A map of configuration data to be created as a ConfigMap.
     */
    private Map<String, String> configd;
    /**
     * A list of paths to additional Kubernetes resource files to be included.
     */
    private List<String> extraK8sResources;
}