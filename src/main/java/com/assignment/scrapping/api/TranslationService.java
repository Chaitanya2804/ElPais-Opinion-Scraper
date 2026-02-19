package com.assignment.scrapping.api;

import com.assignment.scrapping.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Translation using "Top Google Translate" on RapidAPI.
 *
 * Endpoint : POST https://top-google-translate.p.rapidapi.com/v3/translate
 * Headers  : X-RapidAPI-Key, X-RapidAPI-Host
 * Body     : JSON { "sl": "es", "tl": "en", "text": "your text" }
 * Response : { "translation": "translated text" }
 */
public class TranslationService {

    private static final Logger logger =
            LogManager.getLogger(TranslationService.class);

    private static final String API_URL =
            "https://top-google-translate.p.rapidapi.com/v3/translate";

    private static final int TIMEOUT_SECONDS = 15;

    private final ConfigManager config;
    private final ObjectMapper  mapper;
    private final CloseableHttpClient httpClient;

    public TranslationService() {
        this.config     = ConfigManager.getInstance();
        this.mapper     = new ObjectMapper();
        this.httpClient = buildHttpClient();
    }

    public String translate(String spanishText) {
        if (spanishText == null || spanishText.isBlank()) {
            return spanishText;
        }

        String apiKey = config.getTranslationApiKey();
        String host   = config.getRapidApiHost();

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("RapidAPI key not set.");
            return "[NO API KEY] " + spanishText;
        }

        try {
            // Build JSON body: {"sl":"es","tl":"en","text":"..."}
            String jsonBody = mapper.writeValueAsString(
                    new TranslateRequest("es", "en", spanishText));

            HttpPost request = new HttpPost(API_URL);
            request.setHeader("Content-Type",    "application/json");
            request.setHeader("X-RapidAPI-Key",  apiKey);
            request.setHeader("X-RapidAPI-Host", host);
            request.setEntity(new StringEntity(
                    jsonBody, ContentType.APPLICATION_JSON));

            logger.info("Translating: '{}'", spanishText);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int    status       = response.getCode();
                String responseBody = new String(
                        response.getEntity().getContent().readAllBytes(),
                        StandardCharsets.UTF_8);

                logger.debug("Response [{}]: {}", status, responseBody);

                if (status == 200 || status == 221) {
                    return parseTranslation(responseBody, spanishText);
                } else if (status == 429) {
                    logger.warn("Rate limited. Waiting 2s...");
                    Thread.sleep(2000);
                    return fallback(spanishText);
                } else {
                    logger.error("API error {}: {}", status, responseBody);
                    return fallback(spanishText);
                }
            }

        } catch (Exception e) {
            logger.error("Translation error: {}", e.getMessage());
            return fallback(spanishText);
        }
    }

    /**
     * Handles multiple possible response structures:
     *
     * Format 1: { "translation": "..." }
     * Format 2: { "data": { "translation": "..." } }
     * Format 3: { "data": { "translations": [{ "translatedText": "..." }] } }
     */
    private String parseTranslation(String json, String original) {
        try {
            JsonNode root = mapper.readTree(json);

            // This API returns: { "response": "translated text" }
            JsonNode r1 = root.path("response");
            if (!r1.isMissingNode() && !r1.asText().isBlank()) {
                logger.info("✓ '{}' → '{}'", original, r1.asText());
                return r1.asText();
            }

            // Fallback: standard Google format
            JsonNode r2 = root.path("data")
                    .path("translations")
                    .get(0);
            if (r2 != null) {
                String result = r2.path("translatedText").asText();
                if (!result.isBlank()) return result;
            }

            // Fallback: direct translation field
            JsonNode r3 = root.path("translation");
            if (!r3.isMissingNode() && !r3.asText().isBlank()) {
                return r3.asText();
            }

            logger.warn("Unknown response structure: {}", json);
            return fallback(original);

        } catch (Exception e) {
            logger.error("Parse error: {}", e.getMessage());
            return fallback(original);
        }
    }

    private String fallback(String original) {
        return "[TRANSLATION FAILED] " + original;
    }

    private CloseableHttpClient buildHttpClient() {
        RequestConfig rc = RequestConfig.custom()
                .setConnectionRequestTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setResponseTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        return HttpClients.custom()
                .setDefaultRequestConfig(rc)
                .build();
    }

    public void close() {
        try { httpClient.close(); }
        catch (Exception e) {
            logger.warn("HTTP client close: {}", e.getMessage());
        }
    }

    // ── Request POJO ─────────────────────────────────
    static class TranslateRequest {
        public String sl;
        public String tl;
        public String text;

        TranslateRequest(String sl, String tl, String text) {
            this.sl   = sl;
            this.tl   = tl;
            this.text = text;
        }
    }
}
