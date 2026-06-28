# LiveKhela Secure Reverse Proxy Backend

This is a production-ready, Node.js-based secure reverse proxy server designed to route insecure streams (`http://...`) through a secure, CORS-enabled HTTPS connection.

## Features
- Bypasses Android's default cleartext/HTTP block.
- Solves Mixed Content issues where secure frontends cannot load insecure `.m3u8` or `.ts` chunks.
- Recursively parses `.m3u8` playlists and rewrites sub-segment/chunk URLs so that all traffic flows securely through this proxy.
- Adds appropriate CORS headers so the video player can buffer files flawlessly.

## Quick Start (Local Deployment)

1. **Install dependencies**:
   ```bash
   npm init -y
   npm install express axios cors
   ```

2. **Run the server**:
   ```bash
   node server.js
   ```

The proxy will run on `http://localhost:3000`. 
To test proxying a stream:
`http://localhost:3000/proxy?url=YOUR_INSECURE_STREAM_URL`

## Production Deployment (Vercel, Render, Heroku)

### Deploying to Render / Railway:
1. Push this folder to a GitHub repository.
2. Link the repository to your host (Render or Railway).
3. Set the start command to `node server.js`.
4. Render will provide a secure `https://your-app-domain.onrender.com` URL.
5. In your Android/Flutter app's settings panel, enter `https://your-app-domain.onrender.com/proxy?url=` as the proxy URL to route insecure streams seamlessly!
