# El País Opinion Scraper — Enterprise Automation Framework

![BrowserStack](https://img.shields.io/badge/BrowserStack-5%20Parallel%20Threads-orange?logo=browserstack)
![Selenium](https://img.shields.io/badge/Selenium-4.18.1-green?logo=selenium)
![TestNG](https://img.shields.io/badge/TestNG-7.9.0-red)
![Java](https://img.shields.io/badge/Java-11-blue?logo=java)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?logo=apachemaven)
![Build](https://img.shields.io/badge/Build-Passing-brightgreen)

---

## Project Overview

A **production-grade, enterprise-level Selenium automation framework** built for the BrowserStack Customer Engineer technical evaluation.

The framework automates the following end-to-end workflow:

1. Opens [El País](https://elpais.com) and verifies the site is displayed in **Spanish**
2. Navigates to the **Opinion section**
3. Scrapes the **first 5 articles** — title (Spanish), full content, and cover image
4. **Downloads and saves cover images** locally
5. **Translates** each article title from Spanish → English using RapidAPI (Top Google Translate)
6. Prints all translated titles to console
7. Performs **word frequency analysis** across all titles — normalizes, counts, identifies repeated words
8. Executes **locally** (single Chrome) and on **BrowserStack** (5 parallel threads)

---

## BrowserStack Dashboard — All 5 Tests Passing

> ✅ Build #6 — 5/5 Tests Passed | 0 Failures | 7m 31s Total

![BrowserStack Dashboard](docs/browserstack-dashboard.png)

> **Add your screenshot here:** Save the BrowserStack dashboard screenshot as `docs/browserstack-dashboard.png`

**Parallel Sessions:**

| Thread | Platform | Browser | Status |
|--------|----------|---------|--------|
| 1 | Windows 11 | Chrome 145 | ✅ Passed |
| 2 | Windows 11 | Firefox 147 | ✅ Passed |
| 3 | macOS Sequoia | Safari 18 | ✅ Passed |
| 4 | iPhone 14 (Real Device) | Safari iOS 26.2 | ✅ Passed |
| 5 | Samsung Galaxy S22 (Real Device) | Chrome Android 12 | ✅ Passed |

---

## Architecture

```
elpais-browserstack-automation/
│
├── src/main/java/com/assignment/
│   ├── config/
│   │   └── ConfigManager.java          ← Singleton; loads .properties + .env
│   ├── driver/
│   │   ├── DriverManager.java          ← ThreadLocal<WebDriver> — thread safety
│   │   └── CapabilityFactory.java      ← BrowserStack capabilities per platform
│   ├── pages/
│   │   ├── BasePage.java               ← Abstract POM base; shared interactions
│   │   ├── HomePage.java               ← Cookie consent, language check, nav
│   │   └── OpinionPage.java            ← Article scraping — 4-strategy content extraction
│   ├── models/
│   │   └── Article.java                ← Pure POJO data carrier
│   ├── api/
│   │   └── TranslationService.java     ← RapidAPI HTTP client with fallback
│   ├── utils/
│   │   ├── WaitUtil.java               ← All explicit waits — never Thread.sleep
│   │   ├── FileUtil.java               ← Article text + JSON serialization
│   │   ├── ImageDownloader.java        ← Cover image download + lazy-load handling
│   │   └── TextAnalyzer.java           ← Word frequency + normalization
│   └── reporting/
│       └── ConsoleReporter.java        ← All console output centralized
│
├── src/test/java/com/assignment/
│   ├── base/
│   │   └── BaseTest.java               ← @BeforeMethod/@AfterMethod driver lifecycle
│   └── tests/
│       └── ElPaisOpinionTest.java      ← Main test orchestrator
│
├── src/test/resources/
│   ├── config.properties               ← App config (non-sensitive)
│   ├── log4j2.xml                      ← Async logging config
│   ├── testng-local.xml                ← Local single-thread execution
│   └── testng-browserstack.xml        ← 5 parallel BrowserStack threads
│
├── .env                                ← Credentials (git-ignored)
├── .gitignore
├── pom.xml
└── output/
    ├── articles/                       ← Scraped article text files
    └── images/                         ← Downloaded cover images
```

---

## Design Decisions

### 1. ThreadLocal WebDriver — Thread Safety
```
TestNG Thread-1 → DriverManager.ThreadLocal[T1] = ChromeDriver (Windows)
TestNG Thread-2 → DriverManager.ThreadLocal[T2] = FirefoxDriver (Windows)
TestNG Thread-3 → DriverManager.ThreadLocal[T3] = SafariDriver (macOS)
TestNG Thread-4 → DriverManager.ThreadLocal[T4] = RemoteDriver (iPhone)
TestNG Thread-5 → DriverManager.ThreadLocal[T5] = RemoteDriver (Android)
```
Each thread has its own completely isolated WebDriver instance. Zero shared state. Zero race conditions.

### 2. `parallel="tests"` in TestNG XML
Using `parallel="tests"` (not `parallel="methods"`) ensures each `<test>` node — which carries its own platform/browser configuration — runs in its own thread. This is the correct enterprise pattern for cross-browser grid execution.

### 3. Config Externalized — Two Layers
- `config.properties` → non-sensitive app config (URLs, timeouts, counts)
- `.env` → credentials only (BrowserStack keys, API keys) — never committed to Git
- `ConfigManager` priority: `System.getenv()` (CI/CD) → `.env` (local dev) → warn + null

### 4. Page Load Strategy: EAGER
El País loads heavy third-party ad networks. `EAGER` strategy waits only for DOM ready, ignoring ads/trackers. This reduced page load timeouts from frequent failures to zero.

### 5. 4-Strategy Content Extraction
```
Strategy 1 → article body element (full article)
Strategy 2 → all article <p> tags (partial paywall)
Strategy 3 → og:description meta tag (paywall fallback)
Strategy 4 → JSON-LD structured data (ultimate fallback)
```

### 6. Image URL Resolution Priority
```
data-src (lazy-loaded real URL) → srcset first entry → src attribute
SVGs rejected → only jpg/jpeg/png/webp accepted
og:image as reliable fallback (always article-specific)
```

---

## Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 11 |
| Browser Automation | Selenium WebDriver | 4.18.1 |
| Test Framework | TestNG | 7.9.0 |
| Build Tool | Apache Maven | 3.x |
| Design Pattern | Page Object Model (POM) | — |
| Parallel Execution | TestNG parallel threads | 5 threads |
| Cloud Execution | BrowserStack Automate | — |
| Translation API | RapidAPI Top Google Translate | v3 |
| JSON Parsing | Jackson Databind | 2.17.0 |
| HTTP Client | Apache HttpClient 5 | 5.3.1 |
| Logging | Log4j2 + SLF4J | 2.23.1 |
| File I/O | Apache Commons IO | 2.16.1 |
| Credentials | dotenv-java | 3.0.0 |

---

## Setup

### Prerequisites
- Java 11+
- Maven 3.x
- Google Chrome (for local execution)
- BrowserStack account (for cloud execution)
- RapidAPI account with Top Google Translate subscription

### 1. Clone the Repository
```bash
git clone (https://github.com/Chaitanya2804/ElPais-Opinion-Scraper)
cd elpais-browserstack-automation
```

### 2. Configure Credentials
Create a `.env` file in the project root:
```bash
BROWSERSTACK_USERNAME=your_browserstack_username
BROWSERSTACK_ACCESS_KEY=your_browserstack_access_key
TRANSLATION_API_KEY=your_rapidapi_key
RAPIDAPI_HOST=top-google-translate.p.rapidapi.com
```

> **Get BrowserStack credentials:** [automate.browserstack.com](https://automate.browserstack.com) → top of dashboard
> **Get RapidAPI key:** [rapidapi.com](https://rapidapi.com/search/google-translate)

### 3. Install Dependencies
```bash
mvn dependency:resolve
```

Expected output: `BUILD SUCCESS`

---

## How to Run

### Run Locally (Single Chrome Thread)
```bash
mvn test -P local
```

### Run on BrowserStack (5 Parallel Threads)
```bash
mvn test -P browserstack
```

### Run with Specific Browser Locally
```bash
mvn test -P local -Dbrowser=firefox
mvn test -P local -Dbrowser=chrome
```

### Run via CI/CD (GitHub Actions)
Push to `main` branch — GitHub Actions workflow in `.github/workflows/browserstack.yml` triggers automatically. Credentials are injected from GitHub Secrets.

---

## Sample Output

```
── STEP 1: Opening El País ──────────────────────────────

── STEP 2: Language Verification ──────────────────────────────

  ╔══════════════════════════════════════════╗
  ║  ✓ WEBSITE CONFIRMED IN SPANISH          ║
  ║    HTML lang attribute = 'es'            ║
  ╚══════════════════════════════════════════╝

── STEP 3: Navigating to Opinion ──────────────────────────────
  ✓ Opinion section loaded: https://elpais.com/opinion/

── STEP 4: Scraping Articles ──────────────────────────────

═══════════════════════════════════════════════════════
  EL PAÍS — ARTICLES IN SPANISH
═══════════════════════════════════════════════════════

  ┌─ ARTICLE 1 ─────────────────────────────────────────
  │ TITLE   : Caiga quien caiga
  │ CONTENT : La inmediata dimisión del comisario principal...
  │ IMAGE   : ✓ Saved → output/images/article_1_cover.jpg
  │ URL     : https://elpais.com/opinion/2026-02-19/caiga-quien-caiga.html
  └─────────────────────────────────────────────────────

═══════════════════════════════════════════════════════
  TRANSLATED TITLES (Spanish → English)
═══════════════════════════════════════════════════════

  [1] ES: Caiga quien caiga
      EN: Whoever falls falls

  [2] ES: Negar un techo por el color de piel
      EN: Denying a roof because of skin color

  [3] ES: La clase media no es un invento facha
      EN: The middle class is not a façade invention

  [4] ES: Fronteras 'inteligentes', democracias negligentes
      EN: 'Smart' borders, negligent democracies

  [5] ES: 'Moteras iraníes'
      EN: 'Iranian bikers'

═══════════════════════════════════════════════════════
  WORD FREQUENCY ANALYSIS (Words Repeated > 2)
═══════════════════════════════════════════════════════

  WORD                      COUNT
  ───────────────────────────────
  (results vary by day's articles)

  ✓ All 5 articles passed validation.
```

---

## Output Files

After execution, the following files are created:

```
output/
├── articles/
│   ├── article_1_caiga_quien_caiga.txt
│   ├── article_2_negar_un_techo_por_el.txt
│   ├── article_3_la_clase_media_no_es.txt
│   ├── article_4_fronteras_inteligentes.txt
│   ├── article_5_moteras_iranies.txt
│   └── analysis_thread_1.json          ← Full analysis with word frequency
└── images/
    ├── article_1_cover.jpg
    ├── article_2_cover.jpg
    ├── article_3_cover.jpg
    ├── article_4_cover.jpg
    └── article_5_cover.jpg
```

---

## Edge Cases Handled

| Scenario | Handling Strategy |
|----------|-------------------|
| Lazy-loaded images | `data-src` resolved before `src` |
| SVG logos mistaken for cover photos | Extension filter — only jpg/png/webp accepted |
| Paywalled articles (Article 5) | 4-strategy fallback: body → paragraphs → og:description → JSON-LD |
| Cookie/GDPR consent banner | Auto-dismissed before any interaction |
| Mobile `window.maximize()` failure | Platform detection — maximize skipped on iOS/Android |
| Safari overlay ClassCastException | `findElements()` used instead of `WebDriverWait` |
| Translation API rate limit (429) | 2s backoff + graceful fallback to original text |
| Network timeout on heavy pages | EAGER page load strategy + configurable timeouts |
| StaleElementReferenceException | URLs collected first, then navigated — prevents stale refs |

---

## Scalability Considerations

**Adding a new browser/platform:**
Add one `<test>` block to `testng-browserstack.xml` and one `case` in `CapabilityFactory.java`. Zero other changes required.

**Adding a new test:**
Extend `BaseTest` — driver lifecycle is fully managed. Just write test logic.

**Increasing article count:**
Change `scrape.article.count` in `config.properties`. No code changes.

**Switching translation provider:**
Replace `TranslationService.java` only. All other classes are unaffected.

**CI/CD integration:**
GitHub Actions workflow is pre-configured. Add `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` to GitHub Secrets and push.

---

## Logging

- **Console:** INFO level — clean test execution output
- **File:** DEBUG level — full detail in `logs/automation.log`
- **BrowserStack Dashboard:** Network logs, console logs, and screenshots auto-captured

---

## Credential Security

```
Priority 1: System.getenv()     ← CI/CD environment variables (GitHub Secrets)
Priority 2: .env file           ← Local development only
Priority 3: Warning logged      ← Never crashes silently with missing key
```

The `.env` file is in `.gitignore` — credentials are **never committed** to version control.

---

