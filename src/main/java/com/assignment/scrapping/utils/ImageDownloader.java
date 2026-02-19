package com.assignment.scrapping.utils;

import com.assignment.scrapping.config.ConfigManager;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Downloads cover images from URLs and saves them locally.
 * Handles missing images, lazy-loaded src, and network failures gracefully.
 */
public class ImageDownloader {

    private static final Logger logger =
            LogManager.getLogger(ImageDownloader.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 15_000;

    private ImageDownloader() {}

    /**
     * Downloads image from URL and saves to output/images/
     *
     * @param imageUrl  The image URL (http/https)
     * @param index     Article index (used in filename)
     * @return          Local file path if successful, null otherwise
     */
    public static String download(String imageUrl, int index) {

        if (imageUrl == null || imageUrl.isBlank()) {
            logger.warn("Article {}: No image URL provided. Skipping.", index);
            return null;
        }

        if (imageUrl.toLowerCase().endsWith(".svg")) {
            logger.warn("Article {}: Skipping SVG (logo/icon, not cover photo).", index);
            return null;
        }

        // Handle protocol-relative URLs e.g. //images.elpais.com/...
        if (imageUrl.startsWith("//")) {
            imageUrl = "https:" + imageUrl;
        }

        // Skip data URIs (base64 embedded images)
        if (imageUrl.startsWith("data:")) {
            logger.warn("Article {}: Skipping base64 data URI image.", index);
            return null;
        }

        String imageDir = ConfigManager.getInstance().getImagesDir();
        FileUtil.ensureDirectory(imageDir);

        // Extract extension from URL
        String ext = extractExtension(imageUrl);
        String filename = String.format("%s/article_%d_cover%s",
                imageDir, index, ext);

        try {
            URL url = URI.create(imageUrl).toURL();
            File destination = new File(filename);

            // Apache Commons IO handles connection + streaming
            FileUtils.copyURLToFile(url, destination,
                    CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);

            logger.info("Article {}: Image saved → {}", index, filename);
            return filename;

        } catch (Exception e) {
            logger.error("Article {}: Image download failed for URL '{}': {}",
                    index, imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Attempts to resolve lazy-loaded image.
     * El País often uses data-src instead of src for lazy loading.
     *
     * @param src      value of 'src' attribute
     * @param dataSrc  value of 'data-src' attribute
     * @return         The real image URL, or null
     */
    public static String resolveImageUrl(String src, String dataSrc) {
        // Prefer data-src (lazy-loaded actual URL)
        if (dataSrc != null && !dataSrc.isBlank()
                && !dataSrc.startsWith("data:")) {
            return dataSrc.trim();
        }
        // Fall back to src if it's a real URL
        if (src != null && !src.isBlank()
                && !src.startsWith("data:")
                && src.startsWith("http")) {
            return src.trim();
        }
        return null;
    }

    private static String extractExtension(String url) {
        try {
            String path = URI.create(url).getPath();
            int dotIdx = path.lastIndexOf('.');
            if (dotIdx > 0) {
                String ext = path.substring(dotIdx).toLowerCase();
                // Only allow known image extensions
                if (ext.matches("\\.(jpg|jpeg|png|gif|webp|svg)")) {
                    return ext;
                }
            }
        } catch (Exception ignored) {}
        return ".jpg"; // safe default
    }
}