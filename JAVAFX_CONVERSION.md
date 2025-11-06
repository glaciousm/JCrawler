# JavaFX Desktop Application Conversion

## Overview
JCrawler has been converted from a web application (Spring Boot + Electron) to a standalone JavaFX desktop application.

## Changes Made

### 1. Dependencies Added (pom.xml)
- JavaFX Controls 21.0.1
- JavaFX FXML 21.0.1
- JavaFX Graphics 21.0.1
- JavaFX Web 21.0.1
- JavaFX Maven Plugin 0.0.8

### 2. Application Architecture
**Before:** Spring Boot Web Server + Electron Frontend
**After:** Spring Boot (no web server) + JavaFX Desktop UI

### 3. New UI Components Created

#### MainStage.java
- Main JavaFX window
- Tab-based navigation (Crawler, Sessions, Downloads)
- Integrates with Spring Boot context

#### CrawlerTab.java
- Web crawler configuration interface
- Input fields for:
  - Start URL
  - Max depth and max pages
  - Request delay and concurrent threads
  - JavaScript rendering toggle
  - File download toggle
- Start/Pause/Resume/Stop controls
- Real-time crawl log display
- Progress monitoring

#### SessionsTab.java
- TableView displaying all crawl sessions
- Session details viewer
- Export functionality (JSON, CSV, Excel, PDF)
- Session deletion
- Auto-refresh capability

#### DownloadsTab.java
- TableView displaying downloaded files
- Filter by session ID
- File statistics
- Open URLs in browser
- File size formatting

### 4. Configuration Changes

#### application.properties
- Disabled embedded web server: `spring.main.web-application-type=none`
- Removed web server port configuration
- Kept database (H2) and JPA configuration
- Kept async configuration for crawler engine

#### JCrawlerApplication.java
- Now extends `javafx.application.Application`
- Initializes Spring Boot context in `init()` method
- Launches JavaFX UI in `start()` method
- Properly closes Spring context on shutdown

### 5. Service Layer Updates

#### CrawlerService.java
- Removed `SimpMessagingTemplate` dependency
- Removed all WebSocket message sending
- Core crawler functionality remains unchanged
- Direct service method calls from UI

## Benefits

### ✅ Portability
- Single executable JAR file
- No separate frontend process needed
- No web server overhead

### ✅ Simplicity
- Direct method calls instead of REST APIs
- No HTTP/JSON serialization
- No WebSocket complexity
- Simpler deployment

### ✅ Performance
- Lower memory footprint
- No Tomcat overhead
- Native desktop integration

### ✅ User Experience
- Native file dialogs
- Desktop integration
- No browser/port dependencies
- Faster startup

## How to Build

```bash
mvn clean package
```

This creates an executable JAR: `target/jcrawler-1.0.0.jar`

## How to Run

```bash
java -jar target/jcrawler-1.0.0.jar
```

Or with JavaFX plugin:

```bash
mvn javafx:run
```

## Creating Native Installers

You can use jpackage to create platform-specific installers:

```bash
# Windows .exe
jpackage --input target --name JCrawler --main-jar jcrawler-1.0.0.jar --main-class com.jcrawler.JCrawlerApplication --type exe

# macOS .dmg
jpackage --input target --name JCrawler --main-jar jcrawler-1.0.0.jar --main-class com.jcrawler.JCrawlerApplication --type dmg

# Linux .deb
jpackage --input target --name JCrawler --main-jar jcrawler-1.0.0.jar --main-class com.jcrawler.JCrawlerApplication --type deb
```

## Features Preserved

All core functionality remains intact:
- ✅ Web crawling with depth/page limits
- ✅ JavaScript rendering (Playwright)
- ✅ Data extraction with rules
- ✅ File downloads
- ✅ Session management
- ✅ Export to multiple formats (JSON, CSV, Excel, PDF)
- ✅ H2 database persistence
- ✅ Concurrent crawling
- ✅ Navigation flow detection
- ✅ External URL tracking

## Removed Components

- ❌ Electron frontend (frontend/ directory no longer needed)
- ❌ REST API controllers
- ❌ WebSocket real-time updates
- ❌ Embedded Tomcat web server
- ❌ CORS configuration
- ❌ HTTP endpoints

## Future Enhancements

Potential improvements:
1. Add charts/graphs for crawl statistics using JavaFX Charts
2. Implement drag-and-drop for URL lists
3. Add system tray integration
4. Create extraction rule builder UI
5. Add dark/light theme toggle
6. Implement crawl templates
7. Add keyboard shortcuts

## Database

The application continues to use H2 file-based database stored in `./data/jcrawler.db`. All data persists between runs.

## Requirements

- Java 17 or higher
- JavaFX runtime (included in dependencies)

No additional requirements (no Node.js, no browser, no network services needed)
