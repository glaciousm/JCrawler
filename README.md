# JCrawler - Lightweight Web Crawler

A lightweight, portable JavaFX desktop application for crawling websites, tracking navigation flows, extracting data, and exporting results in multiple formats. **No Spring Boot overhead!** Built with pure Hibernate and JavaFX for maximum performance and minimal footprint.

## 🎯 Highlights

- **60-70% smaller** than typical Spring Boot apps (~15-20MB vs ~50-60MB)
- **Faster startup** - no Spring Boot initialization
- **Lower memory footprint** - no web server overhead
- **Truly portable** - create native executables with jpackage (no Java required!)
- **Production-ready** - all features fully functional

## Features

- **Same-domain crawling** with configurable depth and page limits
- **Browser session reuse** via cookie import
- **Navigation flow tracking** (path discovery from start URL to destinations)
- **User-defined extraction rules** using CSS/XPath selectors
- **Multi-format export** (JSON, CSV, Excel, PDF)
- **File downloads** with configurable file extensions
- **Desktop GUI** with intuitive tabbed interface
- **Concurrent crawling** with configurable thread pool
- **Optional authentication** (form-based login)
- **Session management** with detailed statistics
- **JavaScript rendering** support via Playwright
- **Native executables** - bundle with jpackage for zero-dependency distribution

## Tech Stack

**Core Framework:**
- Hibernate Core 6.3.1 (ORM)
- JavaFX 21.0.1 (desktop UI)
- H2 Database (embedded)
- HikariCP (connection pooling)

**Crawler & Processing:**
- JSoup for HTML parsing
- Playwright for JavaScript rendering
- OkHttp for HTTP requests

**Export & Formats:**
- Apache POI (Excel)
- PDFBox (PDF)
- OpenCSV (CSV)
- Jackson (JSON)

**Architecture:**
- AppContext (simple dependency injection)
- Hibernate DAOs (data access)
- JavaFX UI tabs
- ExecutorService (async operations)

## Getting Started

### For End Users (True Portability)

**Want a portable executable that requires NO Java installation?**

Use jpackage to create a native executable:

```bash
# Windows
build-native-simple.bat

# This creates: target\installer\JCrawler\JCrawler.exe
# Double-click to run - no Java needed!
```

See **[PORTABLE_BUILD_GUIDE.md](PORTABLE_BUILD_GUIDE.md)** for detailed instructions.

### For Developers

**Prerequisites:**
- Java 17 or higher
- Maven 3.6+

**Development Run:**
```bash
mvn javafx:run
```

**Build JAR (requires JavaFX installed):**
```bash
mvn clean package
# Note: JAR requires JavaFX runtime to run
```

## Usage

### 1. Crawler Tab
- Enter start URL
- Configure crawl parameters (depth, max pages, delay, threads)
- Enable JavaScript rendering if needed
- Click "Start Crawl"
- Monitor progress in real-time

### 2. Sessions Tab
- View all completed crawl sessions
- See session statistics (pages, flows, downloads)
- Export session data to JSON/CSV/Excel/PDF
- Delete old sessions

### 3. Downloads Tab
- View all downloaded files
- Filter by session ID
- See file details (name, size, URL, status)
- Open URLs in browser

## Project Structure

```
src/main/java/com/jcrawler/
├── JCrawlerApplication.java    # Main JavaFX application
├── core/
│   ├── HibernateConfig.java   # Hibernate SessionFactory
│   └── AppContext.java         # Dependency injection
├── dao/                        # Data Access Objects
│   ├── BaseDao.java           # Generic CRUD
│   └── *Dao.java              # Specific DAOs
├── service/                    # Business logic
│   ├── CrawlerService.java
│   ├── ExtractionService.java
│   ├── DownloadService.java
│   └── ExportService.java
├── engine/                     # Crawler engine
│   ├── CrawlerEngine.java
│   ├── PageProcessor.java
│   └── LinkExtractor.java
├── ui/                         # JavaFX UI
│   ├── MainStage.java
│   ├── CrawlerTab.java
│   ├── SessionsTab.java
│   └── DownloadsTab.java
├── model/                      # JPA entities
└── dto/                        # Data transfer objects
```

## Configuration

### Database
Configured in `HibernateConfig.java`:
- Database file: `./data/jcrawler.db` (H2)
- Auto-schema generation enabled
- Connection pooling: 5-20 connections

### Application Settings
Defaults (can be customized in code):
- Download directory: `downloads/`
- Executor service: 20 threads
- Request timeout: 30 seconds

## Building Native Installers

Create platform-specific installers with `jpackage`:

```bash
# Windows .exe
jpackage --input target --name JCrawler \
  --main-jar jcrawler-1.0.0.jar \
  --main-class com.jcrawler.JCrawlerApplication \
  --type exe

# macOS .dmg
jpackage --input target --name JCrawler \
  --main-jar jcrawler-1.0.0.jar \
  --main-class com.jcrawler.JCrawlerApplication \
  --type dmg

# Linux .deb
jpackage --input target --name JCrawler \
  --main-jar jcrawler-1.0.0.jar \
  --main-class com.jcrawler.JCrawlerApplication \
  --type deb
```

## Architecture

JCrawler uses a lightweight architecture with no framework overhead:

1. **AppContext** - Simple service locator for dependency injection
2. **Hibernate DAOs** - Direct database access with transaction management
3. **ExecutorService** - Manual async execution (replaces @Async)
4. **JavaFX UI** - Native desktop interface

No Spring Boot, no web server, no unnecessary dependencies!

## Migration from Spring Boot

This project was successfully migrated from Spring Boot to a lightweight architecture:
- Removed Spring Boot, Spring Web, Spring Data JPA, Spring WebSocket
- Replaced with Hibernate Core + manual DI
- Result: 60-70% smaller JAR, faster startup, lower memory

See `LIGHTWEIGHT_MIGRATION_COMPLETE.md` for full migration details.

## Database Schema

The application automatically creates these tables:
- `crawl_session` - Crawl session metadata
- `page` - Discovered pages
- `navigation_flow` - Navigation flows between pages
- `extraction_rule` - Data extraction rules
- `extracted_data` - Extracted data results
- `downloaded_file` - Downloaded file references
- `external_url` - External URLs found
- `internal_link` - Internal links discovered

## Export Formats

### JSON
Complete crawl session data in JSON format.

### CSV
Page list with URLs, titles, status codes, and depth levels.

### Excel
Multi-sheet workbook with:
- Session info
- Pages
- Navigation flows
- Downloads

### PDF
Summary report with session statistics and page counts.

## Performance

- **Startup time:** <5 seconds
- **Memory usage:** ~100-200MB (vs ~300-500MB with Spring Boot)
- **JAR size:** ~15-20MB (vs ~50-60MB with Spring Boot)
- **Crawl speed:** Depends on site and configuration (typically 5-50 pages/sec)

## Troubleshooting

### "No suitable driver found for jdbc:h2"
- Ensure H2 dependency is in pom.xml
- Check Hibernate configuration

### JavaFX runtime errors
- Verify JavaFX 21+ is installed
- Run with `mvn javafx:run` instead of `java -jar`

### Out of memory during large crawls
- Increase JVM heap: `java -Xmx2g -jar jcrawler-1.0.0.jar`
- Reduce concurrent threads in crawler settings

## Contributing

This is a demonstration project showing how to build a lightweight desktop app without Spring Boot. Feel free to fork and modify!

## License

MIT License - See LICENSE file for details.

## Credits

Built as a demonstration of:
- JavaFX desktop applications
- Hibernate without Spring
- Manual dependency injection
- Lightweight Java architecture

**No Spring Boot required!**
