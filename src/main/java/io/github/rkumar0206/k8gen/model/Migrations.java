package io.github.rkumar0206.k8gen.model;

import lombok.Data;

import java.util.List;

/**
 * A data class representing the configuration for database migrations within a Kubernetes deployment.
 *
 * <p>This class holds the details necessary to define a one-off Kubernetes Job or similar resource
 * that runs database migrations before the main application starts. This ensures that the database
 * schema is up-to-date with the application's requirements.
 */
@Data
public class Migrations {
    /**
     * The name of the migration tool being used (e.g., "flyway", "liquibase").
     */
    private String tool;
    /**
     * The Docker image for the migration tool.
     */
    private String image;
    /**
     * A list of command-line arguments to pass to the migration tool's container.
     */
    private List<String> args;
}