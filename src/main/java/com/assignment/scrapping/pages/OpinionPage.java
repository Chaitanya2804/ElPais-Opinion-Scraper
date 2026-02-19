package com.assignment.scrapping.pages;

import com.assignment.scrapping.config.ConfigManager;
import com.assignment.scrapping.models.Article;
import com.assignment.scrapping.utils.ImageDownloader;
import com.assignment.scrapping.utils.WaitUtil;
import org.openqa.selenium.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Page Object for El País Opinion Section.
 * Responsible for scraping first 5 articles:
 * - Title (Spanish)
 * - Full content
 * - Cover image URL
 * - Article URL
 */
public class OpinionPage extends BasePage {

    private final ConfigManager config = ConfigManager.getInstance();

    // ── Locators ─────────────────────────────────────

    // Article cards on the opinion listing page
    private static final By ARTICLE_CARDS =
            By.cssSelector("article, .article-item, [class*='article']");

    // More specific — opinion section article links
    private static final By OPINION_ARTICLE_LINKS =
            By.cssSelector("section article h2 a, "
                    + ".opinion article h2 a, "
                    + "article.opinion-article h2 a, "
                    + "h2.article-title a");

    // Article title on detail page
    private static final By ARTICLE_TITLE =
            By.cssSelector("h1.article-title, h1[class*='title'], header h1, h1");

    // Article body content
    private static final By ARTICLE_BODY =
            By.cssSelector(".article-body, [class*='article-body'], "
                    + ".article_body, [itemprop='articleBody'], "
                    + "div[data-dtm-region='articulo_cuerpo']");

    // Cover image on detail page
    private static final By COVER_IMAGE =
            By.cssSelector("figure img, "
                    + ".article-cover img, "
                    + "[class*='cover'] img, "
                    + "header img, "
                    + ".lead-image img");

    public OpinionPage(WebDriver driver) {
        super(driver);
    }

    // ── Core Scraping Method ─────────────────────────

    /**
     * Scrapes the first N articles from the Opinion section.
     * Opens each article, extracts data, returns to listing.
     *
     * @return List of populated Article objects
     */
    public List<Article> scrapeArticles() {
        int count = config.getArticleCount();
        logger.info("Starting scrape of {} articles from Opinion section...", count);

        List<Article> articles = new ArrayList<>();

        // Collect article URLs first — avoids stale element issues
        List<String> articleUrls = collectArticleUrls(count);

        if (articleUrls.isEmpty()) {
            logger.error("No article URLs found. Check Opinion page locators.");
            return articles;
        }

        logger.info("Found {} article URLs. Scraping each...", articleUrls.size());

        for (int i = 0; i < articleUrls.size(); i++) {
            Article article = new Article(i + 1);
            article.setArticleUrl(articleUrls.get(i));

            try {
                scrapeArticleDetail(article);
                articles.add(article);
                logger.info("Scraped article {}/{}: {}",
                        i + 1, articleUrls.size(), article.getTitleSpanish());
            } catch (Exception e) {
                logger.error("Failed to scrape article {}: {}",
                        i + 1, e.getMessage());
                // Continue with remaining articles
            }

            // Navigate back to opinion section for next article
            if (i < articleUrls.size() - 1) {
                String opinionUrl = config.getAppUrl() + config.getOpinionPath();
                driver.get(opinionUrl);
                WaitUtil.waitForPageLoad(driver);
            }
        }

        logger.info("Scraping complete. Successfully scraped {} articles.",
                articles.size());
        return articles;
    }

    // ── Private Helpers ──────────────────────────────

    /**
     * Collects article URLs from the opinion listing page.
     * Collecting URLs first prevents StaleElementReferenceException
     * when we navigate away and back.
     */
    private List<String> collectArticleUrls(int count) {
        List<String> urls = new ArrayList<>();

        try {
            // Wait for page to have article content
            WaitUtil.waitForPresence(driver, OPINION_ARTICLE_LINKS);

            List<WebElement> links = driver.findElements(OPINION_ARTICLE_LINKS);

            // Deduplicate URLs while preserving order
            for (WebElement link : links) {
                if (urls.size() >= count) break;

                String href = link.getAttribute("href");
                if (href != null && !href.isBlank()
                        && !urls.contains(href)
                        && href.contains("elpais.com")) {
                    urls.add(href);
                    logger.debug("Collected URL: {}", href);
                }
            }

        } catch (TimeoutException e) {
            logger.warn("Timeout collecting article links. Trying fallback locators...");
            urls.addAll(collectUrlsFallback(count));
        }

        return urls;
    }

    /**
     * Fallback URL collection using broader selectors.
     * Used when primary locators don't match.
     */
    private List<String> collectUrlsFallback(int count) {
        List<String> urls = new ArrayList<>();
        try {
            // Broad selector — any article link in the page
            List<WebElement> allLinks = driver.findElements(
                    By.cssSelector("article a[href], h2 a[href], h3 a[href]"));

            for (WebElement link : allLinks) {
                if (urls.size() >= count) break;
                String href = link.getAttribute("href");
                if (href != null
                        && href.contains("elpais.com")
                        && !href.contains("#")
                        && !urls.contains(href)) {
                    urls.add(href);
                }
            }
            logger.info("Fallback collected {} URLs.", urls.size());
        } catch (Exception e) {
            logger.error("Fallback URL collection failed: {}", e.getMessage());
        }
        return urls;
    }

    /**
     * Navigates to an article page and extracts all data.
     */
    private void scrapeArticleDetail(Article article) {
        logger.debug("Opening article: {}", article.getArticleUrl());
        driver.get(article.getArticleUrl());
        WaitUtil.waitForPageLoad(driver);

        // Handle any cookie overlay on article page
        WaitUtil.dismissOverlayIfPresent(driver,
                By.id("didomi-notice-agree-button"));

        // Extract title
        article.setTitleSpanish(extractTitle());

        // Extract content
        article.setContent(extractContent());

        // Extract and download cover image
        String imageUrl = extractCoverImageUrl();
        article.setImageUrl(imageUrl);

        if (imageUrl != null) {
            String localPath = ImageDownloader.download(imageUrl, article.getIndex());
            article.setLocalImagePath(localPath);
        }
    }

    private String extractTitle() {
        try {
            WebElement titleEl = WaitUtil.waitForPresence(driver, ARTICLE_TITLE);
            String title = titleEl.getText().trim();
            if (!title.isBlank()) return title;
        } catch (Exception e) {
            logger.warn("Primary title locator failed: {}", e.getMessage());
        }

        // Fallback: try <title> tag
        try {
            return driver.getTitle().split("\\|")[0].trim();
        } catch (Exception e) {
            logger.error("All title extraction attempts failed.");
            return "Title Not Found";
        }
    }

    private String extractContent() {

        // ── Strategy 1: Full article body ────────────────
        try {
            List<WebElement> bodyElements = driver.findElements(By.cssSelector(
                    "[data-dtm-region='articulo_cuerpo'], " +
                            ".article-body, " +
                            "[class*='article-body'], " +
                            "[class*='article_body'], " +
                            "[itemprop='articleBody']"
            ));

            for (WebElement body : bodyElements) {
                scrollTo(body);
                String text = body.getText().trim();
                if (text.length() > 100) return text;
            }
        } catch (Exception e) {
            logger.warn("Strategy 1 failed: {}", e.getMessage());
        }

        // ── Strategy 2: All article paragraphs ───────────
        try {
            List<WebElement> paragraphs = driver.findElements(
                    By.cssSelector("article p, " +
                            ".article-text p, " +
                            "[class*='body'] p, " +
                            ".story-body p"));
            StringBuilder sb = new StringBuilder();
            for (WebElement p : paragraphs) {
                String text = p.getText().trim();
                // Skip short nav/label paragraphs
                if (text.length() > 40) {
                    sb.append(text).append("\n\n");
                }
            }
            if (sb.length() > 100) return sb.toString().trim();
        } catch (Exception e) {
            logger.warn("Strategy 2 failed: {}", e.getMessage());
        }

        // ── Strategy 3: Meta description (paywall fallback) ──
        // Always available even on paywalled articles
        try {
            // Try og:description first — richer than meta description
            WebElement ogDesc = findSafe(
                    By.cssSelector("meta[property='og:description']"));
            if (ogDesc != null) {
                String desc = getAttribute(ogDesc, "content");
                if (desc != null && desc.length() > 30) {
                    logger.info("Using og:description as content fallback.");
                    return "[Article Preview — Full content behind paywall]\n\n"
                            + desc;
                }
            }

            // Standard meta description
            WebElement metaDesc = findSafe(
                    By.cssSelector("meta[name='description']"));
            if (metaDesc != null) {
                String desc = getAttribute(metaDesc, "content");
                if (desc != null && desc.length() > 30) {
                    logger.info("Using meta description as content fallback.");
                    return "[Article Preview — Full content behind paywall]\n\n"
                            + desc;
                }
            }
        } catch (Exception e) {
            logger.warn("Strategy 3 failed: {}", e.getMessage());
        }

        // ── Strategy 4: JSON-LD structured data ──────────
        // El País embeds article data in JSON-LD scripts
        try {
            List<WebElement> scripts = driver.findElements(
                    By.cssSelector("script[type='application/ld+json']"));
            for (WebElement script : scripts) {
                String json = script.getAttribute("innerHTML");
                if (json != null && json.contains("description")) {
                    // Quick extract without full JSON parse
                    int start = json.indexOf("\"description\":\"") + 16;
                    int end   = json.indexOf("\"", start);
                    if (start > 16 && end > start) {
                        String desc = json.substring(start, end);
                        if (desc.length() > 30) {
                            logger.info("Using JSON-LD description as fallback.");
                            return "[Article Preview — Paywall]\n\n" + desc;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Strategy 4 failed: {}", e.getMessage());
        }

        logger.warn("All content extraction strategies failed — paywall article.");
        return "[Content not available — article is behind paywall]";
    }

    private String extractCoverImageUrl() {
        try {
            // ── Strategy 1: Article lead/hero image (JPG/PNG/WebP only) ──
            // Explicitly exclude SVG — those are logos, not cover photos
            List<WebElement> images = driver.findElements(
                    By.cssSelector(
                            "figure img, " +
                                    ".article-cover img, " +
                                    "[class*='cover'] img, " +
                                    "[class*='lead'] img, " +
                                    "[class*='hero'] img, " +
                                    "header picture img, " +
                                    "[class*='main-image'] img, " +
                                    "[class*='featured'] img"
                    )
            );

            for (WebElement img : images) {
                String src     = getAttribute(img, "src");
                String dataSrc = getAttribute(img, "data-src");
                String srcset  = getAttribute(img, "srcset");

                // Resolve the real URL
                String resolved = resolveRealImageUrl(src, dataSrc, srcset);

                if (resolved != null) {
                    logger.info("Cover image found: {}", resolved);
                    return resolved;
                }
            }

            // ── Strategy 2: Open Graph meta image (always article-specific) ──
            // This is the most reliable fallback — El País sets og:image
            // to the actual article cover photo, not the logo
            WebElement ogImage = findSafe(
                    By.cssSelector("meta[property='og:image']"));
            if (ogImage != null) {
                String ogUrl = getAttribute(ogImage, "content");
                if (ogUrl != null && !ogUrl.isBlank()
                        && !ogUrl.endsWith(".svg")) {
                    logger.info("Using og:image as cover: {}", ogUrl);
                    return ogUrl;
                }
            }

            // ── Strategy 3: Twitter card image ──
            WebElement twitterImage = findSafe(
                    By.cssSelector("meta[name='twitter:image']"));
            if (twitterImage != null) {
                String tUrl = getAttribute(twitterImage, "content");
                if (tUrl != null && !tUrl.isBlank()
                        && !tUrl.endsWith(".svg")) {
                    logger.info("Using twitter:image as cover: {}", tUrl);
                    return tUrl;
                }
            }

            logger.info("No article cover image found.");
            return null;

        } catch (Exception e) {
            logger.warn("Cover image extraction failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Resolves the real image URL from src/data-src/srcset.
     * Strictly rejects:
     *  - SVG files (logos, icons)
     *  - base64 data URIs
     *  - blank/null values
     *  - tracking pixels (< 10px implied by 1x1 patterns)
     */
    private String resolveRealImageUrl(String src,
                                       String dataSrc,
                                       String srcset) {
        // Priority 1: data-src (lazy-loaded real image)
        if (isValidImageUrl(dataSrc)) return dataSrc.trim();

        // Priority 2: first URL from srcset
        if (srcset != null && !srcset.isBlank()) {
            String firstSrcset = srcset.split(",")[0].trim().split("\\s+")[0];
            if (isValidImageUrl(firstSrcset)) return firstSrcset;
        }

        // Priority 3: src (only if real photo URL)
        if (isValidImageUrl(src)) return src.trim();

        return null;
    }

    /**
     * Returns true only for real photo URLs.
     * Rejects SVGs, data URIs, blanks, and relative paths.
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.isBlank()) return false;
        if (url.startsWith("data:"))      return false;  // base64
        if (url.endsWith(".svg"))         return false;  // logo/icon
        if (url.endsWith(".gif"))         return false;  // animation
        if (!url.startsWith("http"))      return false;  // relative path

        // Must look like a photo (jpg, jpeg, png, webp)
        String lower = url.toLowerCase();
        return lower.contains(".jpg")
                || lower.contains(".jpeg")
                || lower.contains(".png")
                || lower.contains(".webp")
                || lower.contains("image")   // CDN URLs often have /image/ in path
                || lower.contains("foto")    // El País CDN pattern
                || lower.contains("media");  // Common media CDN pattern
    }
}
