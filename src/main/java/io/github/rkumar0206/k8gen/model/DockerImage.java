package io.github.rkumar0206.k8gen.model;

import lombok.Data;

import java.util.List;

/**
 * A data class representing a single Docker image to be included in a Kubernetes deployment.
 *
 * <p>This class encapsulates the essential details for a Docker container, such as its name,
 * the image tag, its role within the application (e.g., database, cache), and the ports it
 * exposes. This information is used by the AI to generate the appropriate Kubernetes
 * manifest for the container.
 */
@Data
public class DockerImage {
    /**
     * The name of the container within the Kubernetes pod.
     */
    private String name;
    /**
     * The full Docker image name, including the registry and tag (e.g., `nginx:1.21.6`).
     */
    private String image;
    /**
     * The role or purpose of the container within the application (e.g., "backend", "database", "cache").
     */
    private String role;
    /**
     * A list of port numbers that the container exposes.
     */
    private List<String> ports;
}