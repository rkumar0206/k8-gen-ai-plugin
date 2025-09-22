package com.rtb.k8gen.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtb.k8gen.ai.agents.K8ConfigGeneratorAgent;
import com.rtb.k8gen.model.DeploymentConfig;
import com.rtb.k8gen.util.EnvVarExtractor;
import com.rtb.k8gen.util.FileExtractionUtil;
import com.rtb.k8gen.util.VersionUtils;
import org.gradle.api.DefaultTask;
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

@CacheableTask
public abstract class GenerateK8DeploymentConfigTask extends DefaultTask {

    @OutputDirectory
    public abstract Property<@NotNull String> getOutputDir();

    @Input
    public abstract Property<@NotNull String> getConfigFilePath();

    @Input
    @Optional
    public abstract Property<String> getModel();

    @TaskAction
    public void generate() throws IOException {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("GEMINI_API_KEY not set in environment");
        }

        DirectoryProperty directoryProperty = getProject().getObjects().directoryProperty();
        String outputDir = (getOutputDir().get().isEmpty() || getOutputDir().get().equals("/")) ? "/k8s" : getOutputDir().get();
        directoryProperty.set(new File(outputDir));

        File outoutDirectory = directoryProperty.get().getAsFile();
        outoutDirectory.mkdirs();

        DeploymentConfig deploymentConfig = getDeploymentConfig();
        //Files.writeString(new File(outoutDirectory, "deploymentConfig.txt").toPath(), deploymentConfig.getConfigd().toString());

        K8ConfigGeneratorAgent agent = new K8ConfigGeneratorAgent(apiKey, getModel().get());
        String prompt = agent.generatePrompt(deploymentConfig, 1);
        Files.writeString(new File(outoutDirectory, "prompt.txt").toPath(), prompt);

        String generatedConfigs = agent.generateConfigs(deploymentConfig, 1);
        Files.writeString(new File(outoutDirectory, "generatedConfig.txt").toPath(), generatedConfigs);

        Map<String, String> filesAndThereContent = FileExtractionUtil.extractFiles(generatedConfigs);
        FileExtractionUtil.writeFilesToDisk(filesAndThereContent, outoutDirectory);

    }

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