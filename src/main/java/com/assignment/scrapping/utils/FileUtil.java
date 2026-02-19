package com.assignment.scrapping.utils;

import com.assignment.scrapping.config.ConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class FileUtil {

    private static final Logger logger = LogManager.getLogger(FileUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private FileUtil() {}

    public static void ensureDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                logger.info("Created directory: {}", path);
            }
        }
    }

    public static void saveArticleText(int index,
                                       String title,
                                       String content) {
        String dir = ConfigManager.getInstance().getArticlesDir();
        ensureDirectory(dir);

        String safeTitle = title.replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "_")
                .toLowerCase();
        if (safeTitle.length() > 50) {
            safeTitle = safeTitle.substring(0, 50);
        }

        String filename = String.format("%s/article_%d_%s.txt", dir, index, safeTitle);
        String fileContent = String.format(
                "=== ARTICLE %d ===\nTITLE: %s\n\n%s", index, title, content);

        try {
            FileUtils.writeStringToFile(
                    new File(filename), fileContent, StandardCharsets.UTF_8);
            logger.info("Saved article {} to: {}", index, filename);
        } catch (IOException e) {
            logger.error("Failed to save article {}: {}", index, e.getMessage());
        }
    }

    public static void saveAsJson(Object obj, String filePath) {
        try {
            ensureDirectory(new File(filePath).getParent());
            mapper.writeValue(new File(filePath), obj);
            logger.info("Saved JSON to: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to save JSON to {}: {}", filePath, e.getMessage());
        }
    }

    public static String readFile(String path) {
        try {
            return FileUtils.readFileToString(
                    new File(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read file {}: {}", path, e.getMessage());
            return "";
        }
    }
}