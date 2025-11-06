# JCrawler - Web Crawler with Flow Detection

A powerful standalone JavaFX desktop application for crawling websites, tracking navigation flows, extracting data, and exporting results in multiple formats. Built with Spring Boot and JavaFX for true portability.

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
- **Single executable JAR** - truly portable

## Tech Stack

**Core Framework:**
- Spring Boot 3.2.0 (headless mode, no web server)
- Spring Data JPA with H2 Database
- JavaFX 21.0.1 for desktop UI

**Crawler & Processing:**
- JSoup for HTML parsing
- Playwright for JavaScript rendering
- OkHttp for HTTP requests

**Export & Formats:**
- Apache POI (Excel)
- PDFBox (PDF)
- OpenCSV (CSV)
- Jackson (JSON)

## Getting Started

### Prerequisites

**Backend:**
- Java 17 or higher
- Maven 3.6+

**Frontend:**
- Node.js 16 or higher
- npm or yarn

### Quick Start

**1. Start Backend:**
```bash
mvn spring-boot:run
```

**2. Start Frontend:**
```bash
cd frontend
npm install
npm start
```

**For detailed setup instructions, see [GETTING_STARTED.md](GETTING_STARTED.md)**

The backend will start on `http://localhost:8080`
The Electron app will launch automatically

### H2 Database Console

Access the H2 console at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:file:./data/jcrawler`
- Username: `sa`
- Password: (leave empty)

## API Endpoints

### Crawler Operations

```
POST   /api/crawler/start          # Start new crawl
POST   /api/crawler/{id}/pause     # Pause crawl
POST   /api/crawler/{id}/resume    # Resume crawl
POST   /api/crawler/{id}/stop      # Stop crawl
GET    /api/crawler/{id}/status    # Get crawl status
GET    /api/crawler/{id}/pages     # Get discovered pages
GET    /api/crawler/{id}/flows     # Get navigation flows
GET    /api/crawler/{id}/extracted # Get extracted data
GET    /api/crawler/{id}/downloads # Get downloaded files
POST   /api/crawler/{id}/export    # Export results
GET    /api/crawler/{id}/download/{fileId}  # Download specific file
```

### Session & Rules

```
POST   /api/session/import         # Import browser cookies
POST   /api/rules                  # Add extraction rule
GET    /api/rules/{sessionId}      # Get rules for session
DELETE /api/rules/{ruleId}         # Delete rule
PUT    /api/rules/{ruleId}/toggle  # Enable/disable rule
```

### WebSocket

Connect to: `ws://localhost:8080/ws`

Subscribe to progress: `/topic/crawler/{sessionId}/progress`

## Example Usage

### Start a Crawl

```json
POST /api/crawler/start

{
  "startUrl": "https://example.com",
  "maxDepth": 5,
  "maxPages": 100,
  "requestDelay": 1.0,
  "concurrentThreads": 5,
  "downloadFiles": true,
  "allowedFileExtensions": [".pdf", ".docx", ".xlsx"],
  "cookies": {
    "sessionId": "abc123",
    "userId": "user456"
  },
  "extractionRules": [
    {
      "ruleName": "Page Titles",
      "selectorType": "CSS",
      "selectorValue": "h1",
      "attributeToExtract": "text"
    },
    {
      "ruleName": "Product Links",
      "selectorType": "CSS",
      "selectorValue": "a.product-link",
      "attributeToExtract": "href"
    }
  ]
}
```

### Export Results

```json
POST /api/crawler/{sessionId}/export

{
  "formats": ["JSON", "CSV", "EXCEL", "PDF"],
  "includePages": true,
  "includeFlows": true,
  "includeExtractedData": true,
  "includeDownloadedFiles": true
}
```

## WebSocket Progress Updates

The crawler sends real-time updates via WebSocket:

```javascript
// Message types:
{
  "type": "PAGE_DISCOVERED",
  "sessionId": 1,
  "timestamp": "2024-01-01T12:00:00",
  "data": {
    "url": "https://example.com/page",
    "depth": 2,
    "totalPages": 45
  }
}

{
  "type": "FLOW_DISCOVERED",
  "data": {
    "flowId": 123,
    "path": ["url1", "url2", "url3"],
    "totalFlows": 12
  }
}

{
  "type": "DATA_EXTRACTED",
  "data": {
    "ruleName": "Product Titles",
    "count": 25,
    "value": "Sample Product"
  }
}

{
  "type": "METRICS",
  "data": {
    "pagesPerSecond": 3.5,
    "activeThreads": 5,
    "queueSize": 20
  }
}
```

## Project Structure

```
src/main/java/com/jcrawler/
├── config/          # Configuration classes
├── controller/      # REST API controllers
├── dto/             # Data Transfer Objects
├── engine/          # Core crawler engine
├── model/           # JPA entities
├── repository/      # Data repositories
└── service/         # Business logic
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# Database
spring.datasource.url=jdbc:h2:file:./data/jcrawler

# Download directory
jcrawler.download.directory=downloads

# Thread pool configuration
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=20
```

## Database Schema

- **crawl_session**: Crawl session metadata
- **page**: Discovered pages
- **navigation_flow**: Navigation paths
- **extraction_rule**: User-defined extraction rules
- **extracted_data**: Extracted content
- **downloaded_file**: Downloaded files metadata

## Development Roadmap

- [x] Backend REST API
- [x] Core crawler engine
- [x] Data extraction
- [x] Multi-format export
- [x] WebSocket real-time updates
- [x] Electron frontend UI
- [x] Flow visualization (D3.js)
- [x] Real-time dashboard
- [x] Configuration panel
- [x] Export controls
- [ ] Advanced XPath support
- [ ] Crawl resume functionality
- [ ] Distributed crawling
- [ ] Browser DevTools integration
- [ ] Scheduled crawls

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License.

## Author

Built with Spring Boot and ❤️
