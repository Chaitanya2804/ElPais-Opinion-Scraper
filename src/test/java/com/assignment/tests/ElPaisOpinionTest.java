package com.assignment.tests;

import com.assignment.scrapping.api.TranslationService;
import com.assignment.base.BaseTest;
import com.assignment.scrapping.models.Article;
import com.assignment.scrapping.pages.HomePage;
import com.assignment.scrapping.pages.OpinionPage;
import com.assignment.scrapping.reporting.ConsoleReporter;
import com.assignment.scrapping.utils.FileUtil;
import com.assignment.scrapping.utils.TextAnalyzer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main test class for El País Opinion scraping.
 *
 * Flow:
 *  1. Open elpais.com → verify Spanish language
 *  2. Navigate to Opinion section
 *  3. Scrape first 5 articles (title, content, image)
 *  4. Translate titles ES → EN
 *  5. Print translated titles
 *  6. Analyze word frequency across titles
 *  7. Save all output to files
 *
 * Extends BaseTest → driver lifecycle handled automatically.
 * Each parallel thread runs this full flow independently.
 */
public class ElPaisOpinionTest extends BaseTest {

    // TranslationService is instantiated per test instance
    // (one per thread) — no shared state
    private final TranslationService translationService =
            new TranslationService();

    @Test(description = "Scrape El País Opinion articles and analyze titles")
    public void testOpinionArticleScraping() {

        // ── STEP 1: Open Home Page ────────────────────
        ConsoleReporter.printSectionDivider("STEP 1: Opening El País");
        logger.info("Opening El País home page...");

        HomePage homePage = new HomePage(getDriver());
        homePage.open();

        // ── STEP 2: Verify Spanish Language ──────────
        ConsoleReporter.printSectionDivider("STEP 2: Language Verification");
        logger.info("Verifying page is in Spanish...");

        boolean isSpanish = homePage.isInSpanish();
        logger.info("Page in Spanish: {}", isSpanish);

        Assert.assertTrue(isSpanish,
                "El País should be displayed in Spanish. " +
                        "HTML lang attribute did not match 'es'.");

        System.out.println("\n  ╔══════════════════════════════════════════╗");
        System.out.println("  ║  ✓ WEBSITE CONFIRMED IN SPANISH          ║");
        System.out.println("  ║    HTML lang attribute = 'es'            ║");
        System.out.println("  ╚══════════════════════════════════════════╝");

        // ── STEP 3: Navigate to Opinion ──────────────
        ConsoleReporter.printSectionDivider("STEP 3: Navigating to Opinion");
        OpinionPage opinionPage = homePage.navigateToOpinion();

        Assert.assertTrue(
                getDriver().getCurrentUrl().contains("opinion"),
                "URL should contain 'opinion' after navigation. Got: "
                        + getDriver().getCurrentUrl());
        System.out.println("  ✓ Opinion section loaded: "
                + getDriver().getCurrentUrl());

        // ── STEP 4: Scrape Articles ───────────────────
        ConsoleReporter.printSectionDivider("STEP 4: Scraping Articles");
        logger.info("Beginning article scrape...");

        List<Article> articles = opinionPage.scrapeArticles();

        Assert.assertFalse(articles.isEmpty(),
                "No articles were scraped. Check Opinion page locators.");
        Assert.assertTrue(articles.size() >= 1,
                "Expected at least 1 article, got: " + articles.size());

        logger.info("Scraped {} articles successfully.", articles.size());
        ConsoleReporter.printArticleSummary(articles);

        ConsoleReporter.printArticlesInSpanish(articles);

        // Save raw article text to output/articles/
        saveArticlesToFiles(articles);

        // ── STEP 5: Translate Titles ──────────────────
        ConsoleReporter.printSectionDivider("STEP 5: Translating Titles");
        logger.info("Translating {} article titles...", articles.size());

        for (Article article : articles) {
            String spanish = article.getTitleSpanish();
            logger.info("Translating: '{}'", spanish);

            String english = translationService.translate(spanish);
            article.setTitleEnglish(english);

            logger.info("Translated: '{}' → '{}'", spanish, english);
        }

        // ── STEP 6: Print Translated Titles ──────────
        ConsoleReporter.printTranslatedTitles(articles);

        // ── STEP 7: Word Frequency Analysis ──────────
        ConsoleReporter.printSectionDivider("STEP 7: Word Frequency Analysis");
        logger.info("Running word frequency analysis...");

        List<String> englishTitles = articles.stream()
                .map(Article::getTitleEnglish)
                .filter(t -> t != null && !t.startsWith("[TRANSLATION"))
                .collect(Collectors.toList());

        if (englishTitles.isEmpty()) {
            logger.warn("No valid translated titles for analysis.");
            System.out.println(
                    "  ⚠ Skipping analysis — no valid translations available.");
        } else {
            // Full analysis including stop words
            Map<String, Integer> allRepeated =
                    TextAnalyzer.analyzeWordFrequency(englishTitles);

            // Filtered analysis — removes common stop words
            Map<String, Integer> meaningfulRepeated =
                    TextAnalyzer.analyzeWordFrequencyFiltered(englishTitles);

            ConsoleReporter.printWordFrequency(allRepeated);

            if (!meaningfulRepeated.isEmpty()) {
                System.out.println("\n  [Meaningful words only — stop words removed]");
                ConsoleReporter.printWordFrequency(meaningfulRepeated);
            }

            // Save analysis to JSON
            saveAnalysisToJson(articles, allRepeated);
        }

        // ── STEP 8: Final Assertions ──────────────────
        validateResults(articles);

        logger.info("Test completed successfully for thread: {}",
                Thread.currentThread().getName());
    }

    // ── Private Helper Methods ───────────────────────

    /**
     * Saves each article's content to a text file.
     */
    private void saveArticlesToFiles(List<Article> articles) {
        logger.info("Saving {} articles to files...", articles.size());
        for (Article article : articles) {
            if (article.getContent() != null
                    && !article.getContent().isBlank()) {
                FileUtil.saveArticleText(
                        article.getIndex(),
                        article.getTitleSpanish(),
                        article.getContent()
                );

                if (article.getLocalImagePath() != null) {
                    System.out.printf("  ✓ Image saved locally: %s%n",
                            article.getLocalImagePath());
                } else {
                    System.out.printf("  ✗ Article %d: No cover image available%n",
                            article.getIndex());
                }
            }
        }
    }

    /**
     * Serializes final results to a JSON summary file.
     * Named with thread ID to avoid file collisions in parallel runs.
     */
    private void saveAnalysisToJson(List<Article> articles,
                                    Map<String, Integer> wordFrequency) {
        String threadId = String.valueOf(Thread.currentThread().getId());

        // Build a summary object
        java.util.Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("thread",    threadId);
        summary.put("timestamp", java.time.LocalDateTime.now().toString());
        summary.put("articles",  articles);
        summary.put("wordFrequency", wordFrequency);

        String outputPath = String.format(
                "output/articles/analysis_thread_%s.json", threadId);
        FileUtil.saveAsJson(summary, outputPath);
        logger.info("Analysis JSON saved: {}", outputPath);
    }

    /**
     * Final validation assertions on scraped data quality.
     */
    private void validateResults(List<Article> articles) {
        logger.info("Running final validations...");

        for (Article article : articles) {
            // Every article must have a Spanish title
            Assert.assertNotNull(article.getTitleSpanish(),
                    "Article " + article.getIndex() + " missing Spanish title.");
            Assert.assertFalse(article.getTitleSpanish().isBlank(),
                    "Article " + article.getIndex() + " has blank Spanish title.");

            // Every article must have some content
            Assert.assertNotNull(article.getContent(),
                    "Article " + article.getIndex() + " missing content.");

            // English translation must be present
            Assert.assertNotNull(article.getTitleEnglish(),
                    "Article " + article.getIndex() + " was not translated.");

            logger.debug("Article {} validation passed.", article.getIndex());
        }

        System.out.println("\n  ✓ All " + articles.size()
                + " articles passed validation.");
    }
}
