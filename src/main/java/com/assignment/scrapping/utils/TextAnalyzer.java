package com.assignment.scrapping.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


public class TextAnalyzer {

    private static final Logger logger = LogManager.getLogger(TextAnalyzer.class);

    // Common English stop words to optionally filter
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on",
            "at", "to", "for", "of", "with", "by", "from",
            "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will",
            "would", "could", "should", "may", "might", "that",
            "this", "these", "those", "it", "its", "as", "not"
    );

    private TextAnalyzer() {}


    public static Map<String, Integer> analyzeWordFrequency(List<String> titles) {
        logger.info("Analyzing word frequency across {} titles.", titles.size());

        // Combine all titles into one text blob
        String combined = String.join(" ", titles);

        // Normalize: lowercase + remove punctuation
        String normalized = normalize(combined);

        // Tokenize into words
        String[] words = normalized.split("\\s+");

        // Count frequency
        Map<String, Integer> frequencyMap = new LinkedHashMap<>();
        for (String word : words) {
            if (!word.isBlank()) {
                frequencyMap.merge(word, 1, Integer::sum);
            }
        }

        // Filter: only words repeated MORE than twice (count > 2)
        Map<String, Integer> repeated = frequencyMap.entrySet().stream()
                .filter(e -> e.getValue() > 2)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        logger.info("Found {} words repeated more than twice.", repeated.size());
        return repeated;
    }


    public static Map<String, Integer> analyzeWordFrequencyFiltered(
            List<String> titles) {
        Map<String, Integer> raw = analyzeWordFrequency(titles);
        return raw.entrySet().stream()
                .filter(e -> !STOP_WORDS.contains(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }


    public static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")  // remove punctuation
                .replaceAll("\\s+", " ")          // collapse spaces
                .trim();
    }

    public static int uniqueWordCount(List<String> texts) {
        String combined = String.join(" ", texts);
        String[] words = normalize(combined).split("\\s+");
        return (int) Arrays.stream(words)
                .filter(w -> !w.isBlank())
                .distinct()
                .count();
    }
}