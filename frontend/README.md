# JCrawler Frontend - Electron Application

A beautiful Electron desktop application for JCrawler web crawler with real-time progress visualization.

## Features

- **Modern UI** - Dark theme with gradient accents
- **Real-time Updates** - WebSocket integration for live progress
- **Configuration Panel** - Easy setup for crawl parameters
- **Live Dashboard** - Visual stats and metrics
- **Flow Visualization** - D3.js powered flow graph
- **Activity Log** - Real-time crawl activity monitoring
- **Multi-format Export** - JSON, CSV, Excel, PDF support
- **Extraction Rules** - User-defined CSS/XPath selectors

## Prerequisites

- Node.js 16 or higher
- npm or yarn
- JCrawler backend running on `http://localhost:8080`

## Installation

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install
```

## Running the Application

```bash
# Development mode (with DevTools)
npm run dev

# Production mode
npm start
```

## Building

```bash
# Build for Windows
npm run build
```

The built application will be in the `dist` folder.

## Project Structure

```
frontend/
├── main.js                    # Electron main process
├── public/
│   └── index.html            # HTML entry point
├── src/
│   ├── App.js                # Main React application
│   ├── services/
│   │   ├── api.js           # Backend API service
│   │   └── websocket.js     # WebSocket service
│   └── styles/
│       └── main.css         # Application styles
└── package.json
```

## Usage

1. **Start Backend**: Ensure JCrawler backend is running
   ```bash
   cd ..
   mvn spring-boot:run
   ```

2. **Launch Frontend**: Start the Electron app
   ```bash
   npm start
   ```

3. **Configure Crawl**:
   - Enter start URL
   - Set max depth and pages
   - Configure request delay and threads
   - Add extraction rules (optional)
   - Paste cookies if needed

4. **Start Crawling**: Click "START CRAWL" button

5. **Monitor Progress**:
   - Watch stats update in real-time
   - View discovered flows
   - Check extracted data
   - Monitor activity log

6. **Export Results**: Select formats and export when complete

## Configuration Options

### Basic Settings

- **Start URL**: The URL where crawling begins
- **Max Depth**: Maximum crawl depth from start URL
- **Max Pages**: Maximum number of pages to crawl
- **Request Delay**: Delay between requests (seconds)
- **Concurrent Threads**: Number of parallel crawl threads

### Advanced Features

- **Browser Session**: Import cookies from existing browser session
- **Download Files**: Automatically download attachments (.pdf, .docx, etc.)
- **Extraction Rules**: Define CSS/XPath selectors to extract specific data

## WebSocket Events

The frontend listens to these real-time events:

- `PAGE_DISCOVERED` - New page found
- `FLOW_DISCOVERED` - New navigation path detected
- `DATA_EXTRACTED` - Data extracted via rules
- `FILE_DOWNLOADED` - File downloaded
- `METRICS` - Performance metrics update
- `LOG` - Log message
- `CRAWL_COMPLETED` - Crawl finished
- `CRAWL_ERROR` - Error occurred

## Troubleshooting

### Backend Connection Error

Ensure the backend is running on port 8080:
```bash
curl http://localhost:8080/api/crawler/1/status
```

### WebSocket Connection Failed

Check CORS settings in backend `application.properties`:
```properties
spring.websocket.allowed-origins=*
```

### Blank Screen

Open DevTools (npm run dev) and check console for errors.

## Development

### Adding New Components

1. Create component in `src/components/`
2. Import in `App.js`
3. Add to HTML if needed

### Modifying Styles

Edit `src/styles/main.css` - uses CSS variables for theming.

### API Integration

Add new endpoints in `src/services/api.js`:
```javascript
async newEndpoint() {
    const response = await axios.get(`${API_BASE_URL}/endpoint`);
    return response.data;
}
```

## Technologies Used

- **Electron** - Desktop application framework
- **React 18** - UI library (via CDN)
- **D3.js** - Flow visualization
- **Axios** - HTTP client
- **STOMP.js** - WebSocket protocol
- **SockJS** - WebSocket fallback

## License

MIT
