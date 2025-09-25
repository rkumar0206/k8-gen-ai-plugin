package io.github.rkumar0206.k8gen.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rkumar0206.k8gen.ai.agents.K8ConfigGeneratorAgent;
import io.github.rkumar0206.k8gen.model.DeploymentConfig;
import io.github.rkumar0206.k8gen.util.EnvVarExtractor;
import io.github.rkumar0206.k8gen.util.FileExtractionUtil;
import io.github.rkumar0206.k8gen.util.VersionUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Gradle task for generating Kubernetes deployment configurations using a language model.
 *
 * <p>This task is a core component of the K8Gen Gradle plugin. It reads a deployment
 * configuration from a JSON file, supplements it with project-specific details like
 * Gradle and Java versions, and then uses an AI agent to generate Kubernetes manifests.
 * The generated manifests are then saved to a specified output directory.
 *
 * <p>The task is designed to be cacheable, meaning Gradle can skip its execution if the
 * inputs (the configuration file and the AI model) have not changed, which improves build
 * performance.
 */
@CacheableTask
public abstract class GenerateK8DeploymentConfigTask extends DefaultTask {

    /**
     * The output directory where the generated Kubernetes YAML files will be written.
     * This property is an {@link OutputDirectory}, making it an input for Gradle's build cache.
     */
    @OutputDirectory
    public abstract Property<@NotNull String> getOutputDir();

    /**
     * The path to the JSON file containing the deployment configuration.
     * This property is an {@link Input}, making it a key part of Gradle's build cache.
     */
    @Input
    public abstract Property<@NotNull String> getConfigFilePath();

    /**
     * The name of the AI model to use for generating the configurations.
     * This is an optional {@link Input} property.
     */
    @Input
    @Optional
    public abstract Property<String> getModel();

    @Input
    @Optional
    public abstract Property<String> getGeminiAPIKey();

    public static final String GEMINI_API_KEY = "GEMINI_API_KEY";
    /**
     * The main action method for the task.
     *
     * <p>This method performs the following steps:
     * <ol>
     * <li>Checks for the presence of the required `GEMINI_API_KEY` environment variable.</li>
     * <li>Prepares the output directory.</li>
     * <li>Reads and populates the `DeploymentConfig` object from the JSON file and project context.</li>
     * <li>Initializes the `K8ConfigGeneratorAgent`.</li>
     * <li>Generates the Kubernetes configurations using the agent.</li>
     * <li>Parses the generated string to extract individual files.</li>
     * <li>Writes the extracted files to the output directory.</li>
     * </ol>
     *
     * @throws IOException If an I/O error occurs during file operations.
     * @throws IllegalStateException If the `GEMINI_API_KEY` environment variable is not set.
     */
    @TaskAction
    public void generate() throws IOException {

        try {
            String apiKey = java.util.Optional.ofNullable(System.getenv(GEMINI_API_KEY))
                    .filter(s -> !s.isBlank())
                    .orElseGet(() -> {
                        Object prop = getProject().findProperty(GEMINI_API_KEY);
                        if (prop != null && !prop.toString().isBlank()) {
                            return prop.toString();
                        }
                        if (getGeminiAPIKey().isPresent() && !getGeminiAPIKey().get().isBlank()) {
                            return getGeminiAPIKey().get();
                        }
                        return null;
                    });


            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("GEMINI_API_KEY must be set (env var or -P).");
            }

            DirectoryProperty directoryProperty = getProject().getObjects().directoryProperty();
            String outputDir = (getOutputDir().get().isEmpty() || getOutputDir().get().equals("/")) ? "/k8s" : getOutputDir().get();
            directoryProperty.set(new File(outputDir));

            File outputDirectory = directoryProperty.get().getAsFile();
            boolean isOutputDirectoryCreated = outputDirectory.mkdirs();

            if (Files.exists(outputDirectory.toPath()) || isOutputDirectoryCreated) {
                DeploymentConfig deploymentConfig = getDeploymentConfig();

                K8ConfigGeneratorAgent agent = new K8ConfigGeneratorAgent(apiKey, getModel().get());
//                String prompt = agent.generatePrompt(deploymentConfig, 1);
//                Files.writeString(new File(outputDirectory, "prompt.txt").toPath(), prompt);

                String generatedConfigs = agent.generateConfigs(deploymentConfig, 1);
    //            Files.writeString(new File(outputDirectory, "generatedConfig.txt").toPath(), generatedConfigs);

                Map<String, String> filesAndThereContent = FileExtractionUtil.extractFiles(generatedConfigs);
                FileExtractionUtil.writeFilesToDisk(filesAndThereContent, outputDirectory);
            }else {
                throw new IOException("Unable to create the output directory.");
            }
        }  catch (Exception e) {
            // Check if this was caused by an interrupt
            if (e.getCause() instanceof InterruptedException || e instanceof InterruptedException) {
                Thread.currentThread().interrupt(); // restore interrupt flag
                throw new GradleException("Task was interrupted. Build cancelled.", e);
            }
            // Any other exception handling
            throw new GradleException("Error while generating K8s config", e);
        }
    }

    /**
     * Retrieves and populates the `DeploymentConfig` object.
     *
     * <p>This private helper method reads the configuration from the specified JSON file.
     * It then automatically populates the `gradleVersion` and `javaVersion` fields by
     * querying the Gradle project. Additionally, it extracts environment variables from
     * the project and adds them to the `configd` map with a placeholder value.
     *
     * @return The populated {@link DeploymentConfig} object.
     * @throws IOException If the JSON configuration file cannot be found or read.
     */
    private DeploymentConfig getDeploymentConfig() throws IOException {

//        Set<String> possibleDockerImages = new HashSet<>();
//
//        possibleDockerImages.addAll(DockerImageScanner.scanConfigFilesForDockerImages(getProject()));
//        possibleDockerImages.addAll(DockerImageScanner.scanDependenciesForDockerImages(getProject()));

        String configFilePath = getConfigFilePath().get();

        File file = getProject().file(configFilePath);
        ObjectMapper objectMapper = new ObjectMapper();

        DeploymentConfig deploymentConfig = objectMapper.readValue(file, DeploymentConfig.class);

        if (deploymentConfig.getGradleVersion() == null || deploymentConfig.getGradleVersion().isEmpty()) {

            // get gradle version
            String gradleVersion = VersionUtils.getGradleVersion(getProject());
            deploymentConfig.setGradleVersion(gradleVersion);
        }
        if (deploymentConfig.getJavaVersion() == null || deploymentConfig.getJavaVersion().isEmpty()) {

            // get java version
            String javaVersion = VersionUtils.getJavaVersion(getProject());
            deploymentConfig.setJavaVersion(javaVersion);
        }

        Set<String> envVariables = EnvVarExtractor.extractEnvVars(getProject());

        if (deploymentConfig.getConfigd() == null && !envVariables.isEmpty()) {
            deploymentConfig.setConfigd(new HashMap<>());
        }

        envVariables.forEach(env -> deploymentConfig.getConfigd().put(env, "add-your-value-here"));

        return deploymentConfig;
    }
}