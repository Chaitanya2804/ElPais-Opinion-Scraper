package com.assignment.scrapping.reporting;

import com.assignment.scrapping.models.Article;

import java.util.List;
import java.util.Map;


public class ConsoleReporter {

    private static final String SEPARATOR =
            "═══════════════════════════════════════════════════════";
    private static final String THIN_SEP  =
            "───────────────────────────────────────────────────────";

    private ConsoleReporter() {}

    public static void printHeader(String title) {
        System.out.println("\n" + SEPARATOR);
        System.out.println("  " + title);
        System.out.println(SEPARATOR);
    }

    public static void printArticleSummary(List<Article> articles) {
        printHeader("SCRAPED ARTICLES — EL PAÍS OPINION");
        for (Article a : articles) {
            System.out.printf("%n  [Article %d]%n", a.getIndex());
            System.out.printf("  ES Title : %s%n", a.getTitleSpanish());
            System.out.printf("  EN Title : %s%n",
                    a.getTitleEnglish() != null ? a.getTitleEnglish() : "Not translated yet");
            System.out.printf("  URL      : %s%n", a.getArticleUrl());
            System.out.printf("  Image    : %s%n",
                    a.hasImage() ? a.getLocalImagePath() : "No image");
            System.out.printf("  Content  : %d chars%n",
                    a.getContent() != null ? a.getContent().length() : 0);
            System.out.println("  " + THIN_SEP);
        }
    }

    public static void printTranslatedTitles(List<Article> articles) {
        printHeader("TRANSLATED TITLES (Spanish → English)");
        for (Article a : articles) {
            System.out.printf("%n  [%d] ES: %s%n", a.getIndex(), a.getTitleSpanish());
            System.out.printf("      EN: %s%n", a.getTitleEnglish());
        }
    }

    public static void printWordFrequency(Map<String, Integer> repeated) {
        printHeader("WORD FREQUENCY ANALYSIS (Words Repeated > 2)");
        if (repeated.isEmpty()) {
            System.out.println("\n  No words repeated more than twice across titles.");
            return;
        }
        System.out.printf("%n  %-25s %s%n", "WORD", "COUNT");
        System.out.println("  " + THIN_SEP);
        repeated.forEach((word, count) ->
                System.out.printf("  %-25s %d%n", word, count));
        System.out.println();
    }

    public static void printExecutionInfo(String browser,
                                          String os,
                                          String device) {
        printHeader("EXECUTION CONTEXT");
        System.out.printf("  Browser : %s%n", browser);
        System.out.printf("  OS/Device: %s %s%n", os, device != null ? device : "");
    }

    public static void printSectionDivider(String label) {
        System.out.printf("%n── %s ──────────────────────────────%n", label);
    }

    public static void printArticlesInSpanish(List<Article> articles) {
        printHeader("EL PAÍS — ARTICLES IN SPANISH");

        for (Article article : articles) {
            System.out.printf("%n  ┌─ ARTICLE %d ─────────────────────────────────────────%n",
                    article.getIndex());
            System.out.printf("  │ TITLE   : %s%n", article.getTitleSpanish());
            System.out.println("  │");
            System.out.println("  │ CONTENT :");

            // Print content with indentation — wrap long lines
            String content = article.getContent();
            if (content == null || content.isBlank()) {
                System.out.println("  │   [Content not available — possible paywall]");
            } else {
                // Print first 500 chars with indent to keep console clean
                String preview = content.length() > 500
                        ? content.substring(0, 500) + "...\n  │   [Full content saved to output/articles/]"
                        : content;
                for (String line : preview.split("\n")) {
                    System.out.println("  │   " + line);
                }
            }

            System.out.println("  │");
            System.out.printf("  │ IMAGE   : %s%n",
                    article.getLocalImagePath() != null
                            ? "✓ Saved → " + article.getLocalImagePath()
                            : "✗ No image available");
            System.out.printf("  │ URL     : %s%n", article.getArticleUrl());
            System.out.println("  └─────────────────────────────────────────────────────");
        }
    }
}