const { useState, useEffect, useRef } = React;

function App() {
    // State management
    const [sessionId, setSessionId] = useState(null);
    const [crawlStatus, setCrawlStatus] = useState('IDLE');
    const [stats, setStats] = useState({
        totalPages: 0,
        totalFlows: 0,
        totalExtracted: 0,
        totalDownloaded: 0,
        totalExternalUrls: 0,
        pagesPerSecond: 0,
        queueSize: 0
    });
    const [logs, setLogs] = useState([]);
    const [flows, setFlows] = useState([]);
    const [extractedData, setExtractedData] = useState([]);
    const [pages, setPages] = useState([]);
    const [activeTab, setActiveTab] = useState('flows');

    // Configuration state
    const [config, setConfig] = useState({
        startUrl: '',
        maxDepth: 0, // 0 means infinite
        maxPages: 0, // 0 means infinite
        requestDelay: 1.0,
        concurrentThreads: 5,
        downloadFiles: true,
        enableJavaScript: false,
        cookies: '',
        extractionRules: []
    });

    // Add log entry
    const addLog = (level, message) => {
        const timestamp = new Date().toLocaleTimeString();
        setLogs(prev => [{
            timestamp,
            level,
            message
        }, ...prev].slice(0, 100)); // Keep last 100 logs
    };

    // Start crawl
    const handleStartCrawl = async () => {
        try {
            addLog('INFO', 'Starting crawl...');

            // Parse cookies
            let cookiesObj = {};
            if (config.cookies) {
                config.cookies.split(';').forEach(cookie => {
                    const [key, value] = cookie.trim().split('=');
                    if (key && value) {
                        cookiesObj[key.trim()] = value.trim();
                    }
                });
            }

            const crawlConfig = {
                startUrl: config.startUrl,
                maxDepth: parseInt(config.maxDepth),
                maxPages: parseInt(config.maxPages),
                requestDelay: parseFloat(config.requestDelay),
                concurrentThreads: parseInt(config.concurrentThreads),
                downloadFiles: config.downloadFiles,
                enableJavaScript: config.enableJavaScript,
                cookies: cookiesObj,
                extractionRules: config.extractionRules.map(rule => ({
                    ruleName: rule.name,
                    selectorType: rule.type,
                    selectorValue: rule.selector,
                    attributeToExtract: rule.attribute
                }))
            };

            console.log('=== CRAWL CONFIG DEBUG ===');
            console.log('enableJavaScript in state:', config.enableJavaScript);
            console.log('enableJavaScript in payload:', crawlConfig.enableJavaScript);
            console.log('Full payload:', JSON.stringify(crawlConfig, null, 2));
            console.log('========================');

            const response = await api.startCrawl(crawlConfig);
            setSessionId(response.sessionId);
            setCrawlStatus('RUNNING');
            addLog('SUCCESS', `Crawl started with session ID: ${response.sessionId}`);

            // Connect WebSocket
            wsService.connect(response.sessionId, () => {
                addLog('INFO', 'WebSocket connected');
            });

            // Set up WebSocket listeners
            setupWebSocketListeners();

        } catch (error) {
            addLog('ERROR', `Failed to start crawl: ${error.message}`);
        }
    };

    // Setup WebSocket listeners
    const setupWebSocketListeners = () => {
        wsService.on('PAGE_DISCOVERED', (data) => {
            setStats(prev => ({ ...prev, totalPages: data.totalPages }));
            addLog('INFO', `Discovered: ${data.url}`);
        });

        wsService.on('FLOW_DISCOVERED', (data) => {
            setStats(prev => ({ ...prev, totalFlows: data.totalFlows }));
            setFlows(prev => [...prev, data]);
            addLog('INFO', `Flow discovered: depth ${data.path.length}`);
        });

        wsService.on('DATA_EXTRACTED', (data) => {
            setStats(prev => ({ ...prev, totalExtracted: prev.totalExtracted + data.count }));
            setExtractedData(prev => [...prev, data]);
            addLog('SUCCESS', `Extracted ${data.count} items (${data.ruleName})`);
        });

        wsService.on('FILE_DOWNLOADED', (data) => {
            setStats(prev => ({ ...prev, totalDownloaded: data.totalDownloaded }));
            addLog('SUCCESS', `Downloaded: ${data.fileName}`);
        });

        wsService.on('EXTERNAL_URL_FOUND', (data) => {
            setStats(prev => ({ ...prev, totalExternalUrls: data.totalExternalUrls }));
            addLog('INFO', `External URL found: ${data.url} (Total: ${data.totalExternalUrls})`);
        });

        wsService.on('METRICS', (data) => {
            setStats(prev => ({
                ...prev,
                pagesPerSecond: data.pagesPerSecond.toFixed(2),
                queueSize: data.queueSize
            }));
        });

        wsService.on('LOG', (data) => {
            addLog(data.level, data.message);
        });

        wsService.on('CRAWL_COMPLETED', () => {
            setCrawlStatus('COMPLETED');
            addLog('SUCCESS', 'Crawl completed successfully!');
            loadFinalData();
        });

        wsService.on('CRAWL_ERROR', (data) => {
            setCrawlStatus('FAILED');
            addLog('ERROR', `Crawl failed: ${data.error}`);
        });
    };

    // Load final data after crawl completion
    const loadFinalData = async () => {
        if (!sessionId) return;

        try {
            const [pagesData, flowsData, extractedData] = await Promise.all([
                api.getPages(sessionId),
                api.getFlows(sessionId),
                api.getExtractedData(sessionId)
            ]);

            setPages(pagesData);
            setFlows(flowsData);
            setExtractedData(extractedData);
        } catch (error) {
            addLog('ERROR', `Failed to load final data: ${error.message}`);
        }
    };

    // Pause crawl
    const handlePauseCrawl = async () => {
        try {
            await api.pauseCrawl(sessionId);
            setCrawlStatus('PAUSED');
            addLog('INFO', 'Crawl paused');
        } catch (error) {
            addLog('ERROR', `Failed to pause: ${error.message}`);
        }
    };

    // Resume crawl
    const handleResumeCrawl = async () => {
        try {
            await api.resumeCrawl(sessionId);
            setCrawlStatus('RUNNING');
            addLog('INFO', 'Crawl resumed');
        } catch (error) {
            addLog('ERROR', `Failed to resume: ${error.message}`);
        }
    };

    // Stop crawl
    const handleStopCrawl = async () => {
        try {
            await api.stopCrawl(sessionId);
            setCrawlStatus('STOPPED');
            addLog('INFO', 'Crawl stopped');
            wsService.disconnect();
        } catch (error) {
            addLog('ERROR', `Failed to stop: ${error.message}`);
        }
    };

    // Reset for new crawl
    const handleNewCrawl = () => {
        setSessionId(null);
        setCrawlStatus('IDLE');
        setStats({
            totalPages: 0,
            totalFlows: 0,
            totalExtracted: 0,
            totalDownloaded: 0,
            totalExternalUrls: 0,
            pagesPerSecond: 0,
            queueSize: 0
        });
        setLogs([]);
        setFlows([]);
        setExtractedData([]);
        setPages([]);
        wsService.disconnect();
        addLog('INFO', 'Ready for new crawl');
    };

    // Export results
    const handleExport = async (formats) => {
        try {
            addLog('INFO', `Exporting to ${formats.join(', ')}...`);
            const result = await api.exportResults(sessionId, {
                sessionId,
                formats,
                includePages: true,
                includeFlows: true,
                includeExtractedData: true,
                includeDownloadedFiles: true
            });
            addLog('SUCCESS', 'Export completed!');
            console.log('Export files:', result);
        } catch (error) {
            addLog('ERROR', `Export failed: ${error.message}`);
        }
    };

    // Add extraction rule
    const addExtractionRule = () => {
        setConfig(prev => ({
            ...prev,
            extractionRules: [...prev.extractionRules, {
                name: 'New Rule',
                type: 'CSS',
                selector: '',
                attribute: 'text'
            }]
        }));
    };

    // Remove extraction rule
    const removeExtractionRule = (index) => {
        setConfig(prev => ({
            ...prev,
            extractionRules: prev.extractionRules.filter((_, i) => i !== index)
        }));
    };

    // Update extraction rule
    const updateExtractionRule = (index, field, value) => {
        setConfig(prev => ({
            ...prev,
            extractionRules: prev.extractionRules.map((rule, i) =>
                i === index ? { ...rule, [field]: value } : rule
            )
        }));
    };

    return (
        <div id="root">
            <header className="app-header">
                <h1>JCrawler - Web Crawler</h1>
                <div className="header-actions">
                    {sessionId && (
                        <span className={`status-badge ${crawlStatus.toLowerCase()}`}>
                            {crawlStatus}
                        </span>
                    )}
                </div>
            </header>

            <div className="app-container">
                {/* Configuration Panel */}
                <ConfigPanel
                    config={config}
                    setConfig={setConfig}
                    onStartCrawl={handleStartCrawl}
                    onPauseCrawl={handlePauseCrawl}
                    onResumeCrawl={handleResumeCrawl}
                    onStopCrawl={handleStopCrawl}
                    onNewCrawl={handleNewCrawl}
                    crawlStatus={crawlStatus}
                    sessionId={sessionId}
                    addExtractionRule={addExtractionRule}
                    removeExtractionRule={removeExtractionRule}
                    updateExtractionRule={updateExtractionRule}
                />

                {/* Dashboard */}
                <Dashboard
                    stats={stats}
                    logs={logs}
                    flows={flows}
                    extractedData={extractedData}
                    pages={pages}
                    activeTab={activeTab}
                    setActiveTab={setActiveTab}
                    sessionId={sessionId}
                    onExport={handleExport}
                />
            </div>
        </div>
    );
}

// Configuration Panel Component
function ConfigPanel({
    config,
    setConfig,
    onStartCrawl,
    onPauseCrawl,
    onResumeCrawl,
    onStopCrawl,
    onNewCrawl,
    crawlStatus,
    sessionId,
    addExtractionRule,
    removeExtractionRule,
    updateExtractionRule
}) {
    return (
        <div className="config-panel">
            <h2>Configuration</h2>

            <div className="form-group">
                <label>Start URL</label>
                <input
                    type="text"
                    value={config.startUrl}
                    onChange={(e) => setConfig({ ...config, startUrl: e.target.value })}
                    placeholder="https://example.com"
                    disabled={sessionId}
                />
            </div>

            <div className="checkbox-group">
                <input
                    type="checkbox"
                    id="useCookies"
                    checked={!!config.cookies}
                    onChange={(e) => setConfig({ ...config, cookies: e.target.checked ? '' : config.cookies })}
                    disabled={sessionId}
                />
                <label htmlFor="useCookies">Use existing browser session</label>
            </div>

            {config.cookies !== null && (
                <div className="form-group">
                    <label>Cookies (sessionId=abc; userId=123)</label>
                    <textarea
                        value={config.cookies}
                        onChange={(e) => setConfig({ ...config, cookies: e.target.value })}
                        placeholder="Paste cookie string here"
                        disabled={sessionId}
                    />
                </div>
            )}

            <div className="form-group">
                <label>Max Depth <span style={{color: '#999', fontSize: '12px', fontWeight: 'normal'}}>(0 = infinite)</span></label>
                <input
                    type="number"
                    value={config.maxDepth}
                    onChange={(e) => setConfig({ ...config, maxDepth: e.target.value })}
                    min="0"
                    max="50"
                    placeholder="0 for infinite"
                    disabled={sessionId}
                />
            </div>

            <div className="form-group">
                <label>Max Pages <span style={{color: '#999', fontSize: '12px', fontWeight: 'normal'}}>(0 = infinite)</span></label>
                <input
                    type="number"
                    value={config.maxPages}
                    onChange={(e) => setConfig({ ...config, maxPages: e.target.value })}
                    min="0"
                    max="10000"
                    placeholder="0 for infinite"
                    disabled={sessionId}
                />
            </div>

            <div className="form-group">
                <label>Request Delay (seconds)</label>
                <input
                    type="number"
                    value={config.requestDelay}
                    onChange={(e) => setConfig({ ...config, requestDelay: e.target.value })}
                    min="0"
                    step="0.1"
                    disabled={sessionId}
                />
            </div>

            <div className="form-group">
                <label>Concurrent Threads</label>
                <input
                    type="number"
                    value={config.concurrentThreads}
                    onChange={(e) => setConfig({ ...config, concurrentThreads: e.target.value })}
                    min="1"
                    max="20"
                    disabled={sessionId}
                />
            </div>

            <div className="checkbox-group">
                <input
                    type="checkbox"
                    id="downloadFiles"
                    checked={config.downloadFiles}
                    onChange={(e) => setConfig({ ...config, downloadFiles: e.target.checked })}
                    disabled={sessionId}
                />
                <label htmlFor="downloadFiles">Download attachments</label>
            </div>

            <div className="checkbox-group">
                <input
                    type="checkbox"
                    id="enableJavaScript"
                    checked={config.enableJavaScript}
                    onChange={(e) => {
                        console.log('Checkbox clicked! New value:', e.target.checked);
                        setConfig({ ...config, enableJavaScript: e.target.checked });
                        console.log('State updated to:', { ...config, enableJavaScript: e.target.checked });
                    }}
                    disabled={sessionId}
                />
                <label htmlFor="enableJavaScript">Enable JavaScript rendering (for React/SPA sites)</label>
            </div>

            <div className="extraction-rules">
                <h3 style={{fontSize: '14px', marginBottom: '10px'}}>Extraction Rules</h3>
                {config.extractionRules.map((rule, index) => (
                    <div key={index} className="rule-item">
                        <div className="rule-header">
                            <input
                                type="text"
                                value={rule.name}
                                onChange={(e) => updateExtractionRule(index, 'name', e.target.value)}
                                style={{width: '70%', padding: '5px'}}
                                disabled={sessionId}
                            />
                            <button
                                className="btn btn-danger btn-small"
                                onClick={() => removeExtractionRule(index)}
                                disabled={sessionId}
                            >
                                Remove
                            </button>
                        </div>
                        <select
                            value={rule.type}
                            onChange={(e) => updateExtractionRule(index, 'type', e.target.value)}
                            style={{width: '100%', padding: '5px', marginBottom: '5px'}}
                            disabled={sessionId}
                        >
                            <option value="CSS">CSS Selector</option>
                            <option value="XPATH">XPath</option>
                        </select>
                        <input
                            type="text"
                            value={rule.selector}
                            onChange={(e) => updateExtractionRule(index, 'selector', e.target.value)}
                            placeholder="Selector (e.g., h1.title)"
                            style={{width: '100%', padding: '5px', marginBottom: '5px'}}
                            disabled={sessionId}
                        />
                        <input
                            type="text"
                            value={rule.attribute}
                            onChange={(e) => updateExtractionRule(index, 'attribute', e.target.value)}
                            placeholder="Attribute (text, href, src)"
                            style={{width: '100%', padding: '5px'}}
                            disabled={sessionId}
                        />
                    </div>
                ))}
                <button
                    className="btn btn-secondary btn-full btn-small"
                    onClick={addExtractionRule}
                    disabled={sessionId}
                    style={{marginTop: '10px'}}
                >
                    + Add Rule
                </button>
            </div>

            <div className="btn-group">
                {!sessionId && (
                    <button className="btn btn-primary btn-full" onClick={onStartCrawl}>
                        START CRAWL
                    </button>
                )}

                {sessionId && crawlStatus === 'RUNNING' && (
                    <>
                        <button className="btn btn-warning" onClick={onPauseCrawl}>
                            Pause
                        </button>
                        <button className="btn btn-danger" onClick={onStopCrawl}>
                            Stop
                        </button>
                    </>
                )}

                {sessionId && crawlStatus === 'PAUSED' && (
                    <>
                        <button className="btn btn-success" onClick={onResumeCrawl}>
                            Resume
                        </button>
                        <button className="btn btn-danger" onClick={onStopCrawl}>
                            Stop
                        </button>
                    </>
                )}

                {sessionId && (crawlStatus === 'COMPLETED' || crawlStatus === 'STOPPED') && (
                    <button className="btn btn-primary btn-full" onClick={onNewCrawl}>
                        NEW CRAWL
                    </button>
                )}
            </div>
        </div>
    );
}

// Dashboard Component
function Dashboard({ stats, logs, flows, extractedData, pages, activeTab, setActiveTab, sessionId, onExport }) {
    return (
        <div className="dashboard">
            {/* Stats Bar */}
            <div className="stats-bar">
                <StatCard label="Pages" value={stats.totalPages} />
                <StatCard label="Flows" value={stats.totalFlows} />
                <StatCard label="Extracted" value={stats.totalExtracted} className="success" />
                <StatCard label="Downloads" value={stats.totalDownloaded} className="success" />
                <StatCard label="URLs" value={stats.totalExternalUrls} className="success" />
                <StatCard label="Speed" value={`${stats.pagesPerSecond}/s`} className="warning" />
                <StatCard label="Queue" value={stats.queueSize} />
            </div>


            {/* Activity Log */}
            <div style={{padding: '20px', paddingTop: '10px'}}>
                <h3 style={{fontSize: '16px', color: '#667eea', marginBottom: '10px'}}>Activity Log</h3>
                <div className="activity-log">
                    {logs.map((log, index) => (
                        <div key={index} className={`log-entry ${log.level.toLowerCase()}`}>
                            <span className="log-timestamp">[{log.timestamp}]</span>
                            <span>[{log.level}]</span> {log.message}
                        </div>
                    ))}
                </div>
            </div>

            {/* Export Panel */}
            {sessionId && (
                <ExportPanel onExport={onExport} />
            )}
        </div>
    );
}

// Stat Card Component
function StatCard({ label, value, className = '' }) {
    return (
        <div className="stat-card">
            <div className="stat-label">{label}</div>
            <div className={`stat-value ${className}`}>{value}</div>
        </div>
    );
}

// Flow Visualization Component
function FlowVisualization({ flows }) {
    const svgRef = useRef(null);

    useEffect(() => {
        if (flows.length === 0 || !svgRef.current) return;

        // Clear previous visualization
        d3.select(svgRef.current).selectAll('*').remove();

        const width = svgRef.current.clientWidth;
        const height = svgRef.current.clientHeight;

        const svg = d3.select(svgRef.current)
            .attr('width', width)
            .attr('height', height);

        // Create simple tree layout
        const nodes = flows.slice(0, 20).map((flow, i) => ({
            id: i,
            flow: flow,
            x: 50,
            y: 50 + i * 30
        }));

        // Draw flow paths
        nodes.forEach(node => {
            const g = svg.append('g')
                .attr('transform', `translate(${node.x}, ${node.y})`);

            g.append('text')
                .attr('fill', '#667eea')
                .attr('font-size', '12px')
                .text(`Flow ${node.id + 1}: ${node.flow.path ? node.flow.path.join(' → ').substring(0, 80) + '...' : 'Loading...'}`);
        });

    }, [flows]);

    if (flows.length === 0) {
        return <div style={{color: '#666', textAlign: 'center', marginTop: '50px'}}>No flows discovered yet</div>;
    }

    return <svg ref={svgRef} style={{width: '100%', height: '100%'}}></svg>;
}

// Pages List Component
function PagesList({ pages }) {
    if (pages.length === 0) {
        return <div style={{color: '#666', textAlign: 'center', marginTop: '50px'}}>No pages discovered yet</div>;
    }

    return (
        <div style={{padding: '10px'}}>
            {pages.map((page, index) => (
                <div key={index} className="data-item">
                    <div className="data-item-header">{page.title || 'Untitled'}</div>
                    <div className="data-item-content">
                        <div>URL: {page.url}</div>
                        <div>Depth: {page.depthLevel} | Status: {page.statusCode}</div>
                    </div>
                </div>
            ))}
        </div>
    );
}

// Export Panel Component
function ExportPanel({ onExport }) {
    const [selectedFormats, setSelectedFormats] = useState(['CSV']);

    const toggleFormat = (format) => {
        setSelectedFormats(prev =>
            prev.includes(format)
                ? prev.filter(f => f !== format)
                : [...prev, format]
        );
    };

    return (
        <div className="export-panel">
            <div className="export-options">
                <strong style={{color: '#667eea'}}>Export:</strong>
                <div className="export-format-group">
                    {['CSV', 'EXCEL', 'PDF'].map(format => (
                        <div key={format} className="checkbox-group">
                            <input
                                type="checkbox"
                                id={format}
                                checked={selectedFormats.includes(format)}
                                onChange={() => toggleFormat(format)}
                            />
                            <label htmlFor={format}>{format}</label>
                        </div>
                    ))}
                </div>
                <button
                    className="btn btn-primary btn-small"
                    onClick={() => onExport(selectedFormats)}
                    disabled={selectedFormats.length === 0}
                >
                    Export Results
                </button>
            </div>
        </div>
    );
}

// Render the app
ReactDOM.render(<App />, document.getElementById('root'));
