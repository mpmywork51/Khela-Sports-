/**
 * LiveKhela Secure Reverse Proxy Server
 * 
 * This Node.js/Express server acts as a secure reverse proxy to bypass 
 * Android Cleartext restrictions and browser mixed-content (HTTP streams in HTTPS pages).
 * It intercepts insecure HTTP streams (.m3u8 playlists and .ts chunks), 
 * downloads them server-side, rewrites any nested relative/absolute HTTP links 
 * to go through this proxy over HTTPS, and streams the content back securely.
 * 
 * Deploy this on platforms like Heroku, Vercel, Render, or Railway.
 */

const express = require('express');
const axios = require('axios');
const url = require('url');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

// Enable CORS for all origins to ensure the Android player can request chunk files
app.use(cors({
    origin: '*',
    methods: ['GET', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Range', 'User-Agent', 'Referer']
}));

// Route for proxying streams
// Usage: https://your-proxy-domain.com/proxy?url=http://insecure-stream.com/live.m3u8
app.get('/proxy', async (req, res) => {
    const streamUrl = req.query.url;
    if (!streamUrl) {
        return res.status(400).send('Missing "url" query parameter');
    }

    try {
        const parsedUrl = url.parse(streamUrl);
        const baseUrl = `${parsedUrl.protocol}//${parsedUrl.host}${parsedUrl.pathname.substring(0, parsedUrl.pathname.lastIndexOf('/'))}`;

        // Forward headers like User-Agent or Referer if requested
        const headers = {
            'User-Agent': req.headers['user-agent'] || 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        };
        if (req.headers['referer']) headers['Referer'] = req.headers['referer'];

        console.log(`[PROXY] Fetching: ${streamUrl}`);

        // Handle .ts segments or binary files (passthrough stream)
        if (streamUrl.endsWith('.ts') || req.query.binary === 'true') {
            const response = await axios({
                method: 'get',
                url: streamUrl,
                responseType: 'stream',
                headers: headers,
                timeout: 10000
            });

            res.setHeader('Content-Type', response.headers['content-type'] || 'video/MP2T');
            response.data.pipe(res);
            return;
        }

        // Fetch .m3u8 playlist file and rewrite nested paths
        const response = await axios.get(streamUrl, { 
            headers: headers,
            timeout: 8000
        });

        let playlistContent = response.data;

        // Content type for HLS is application/x-mpegURL or application/vnd.apple.mpegurl
        res.setHeader('Content-Type', response.headers['content-type'] || 'application/vnd.apple.mpegurl');

        // Parse and rewrite m3u8 playlist lines
        const lines = playlistContent.split('\n');
        const rewrittenLines = lines.map(line => {
            const trimmed = line.trim();
            // If the line is empty or a comment/metadata tag, keep it as is
            if (!trimmed || trimmed.startsWith('#')) {
                return line;
            }

            // If it is an absolute URL (starts with http:// or https://)
            if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
                return `${req.protocol}://${req.get('host')}/proxy?url=${encodeURIComponent(trimmed)}`;
            }

            // If it is a relative path starting with /
            if (trimmed.startsWith('/')) {
                const domainUrl = `${parsedUrl.protocol}//${parsedUrl.host}`;
                const absoluteSegmentUrl = `${domainUrl}${trimmed}`;
                return `${req.protocol}://${req.get('host')}/proxy?url=${encodeURIComponent(absoluteSegmentUrl)}`;
            }

            // Otherwise, it is a relative path relative to the stream directory
            const absoluteSegmentUrl = `${baseUrl}/${trimmed}`;
            return `${req.protocol}://${req.get('host')}/proxy?url=${encodeURIComponent(absoluteSegmentUrl)}`;
        });

        res.send(rewrittenLines.join('\n'));

    } catch (error) {
        console.error(`[PROXY ERROR] Failed to proxy: ${streamUrl}`, error.message);
        res.status(500).send(`Proxy Error: ${error.message}`);
    }
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

app.listen(PORT, () => {
    console.log(`LiveKhela Reverse Proxy running on port ${PORT}`);
});
