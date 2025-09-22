package com.rtb.k8gen.util;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting file definitions from text blocks
 * with markers like -----BEGIN_FILE: <filename>----- and
 * -----END_FILE: <filename>-----.
 *
 * It can also export the extracted mapping into a YAML file.
 */
public class FileExtractionUtil {

    private static final Pattern FILE_BLOCK_PATTERN =
            Pattern.compile("-----BEGIN_FILE: (.+?)-----([\\s\\S]*?)-----END_FILE: \\1---");

    /**
     * Extracts file contents from the given text.
     *
     * @param inputText the text containing file definitions
     * @return map of filename -> file content
     */
    public static Map<String, String> extractFiles(String inputText) {
        Map<String, String> files = new LinkedHashMap<>();
        Matcher matcher = FILE_BLOCK_PATTERN.matcher(inputText);

        while (matcher.find()) {
            String fileName = matcher.group(1).trim();
            String content = matcher.group(2).trim();
            files.put(fileName, content);
        }

        return files;
    }

    /**
     * Writes extracted files to disk under the given output directory.
     *
     * @param files     map of filename -> content
     * @param outputDir target directory
     * @throws IOException if file write fails
     */
    public static void writeFilesToDisk(Map<String, String> files, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir);
        }

        for (Map.Entry<String, String> entry : files.entrySet()) {
            File outFile = new File(outputDir, entry.getKey());
            try (FileWriter writer = new FileWriter(outFile)) {
                writer.write(entry.getValue());
            }
        }
    }

    /**
     * Converts the extracted mapping to a YAML string.
     *
     * Example structure:
     * files:
     *   Dockerfile: "<content>"
     *   .dockerignore: "<content>"
     *
     * @param files map of filename -> content
     * @return YAML string
     */
    public static String toYaml(Map<String, String> files) {
        Map<String, Object> yamlRoot = new LinkedHashMap<>();
        yamlRoot.put("files", files);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        return yaml.dump(yamlRoot);
    }
}
