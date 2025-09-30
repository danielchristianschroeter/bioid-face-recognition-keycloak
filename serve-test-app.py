#!/usr/bin/env python3
"""
Simple HTTP server to serve the BioID test application.
This allows testing the OIDC flow without needing a full web server.
"""

import http.server
import socketserver
import webbrowser
import os
import sys

PORT = 3000
HANDLER = http.server.SimpleHTTPRequestHandler

class CORSHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization')
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

def main():
    try:
        with socketserver.TCPServer(("", PORT), CORSHTTPRequestHandler) as httpd:
            print(f"🚀 BioID Test App Server starting on port {PORT}")
            print(f"📱 Open your browser to: http://localhost:{PORT}/test-app.html")
            print(f"🔐 Make sure your Keycloak is running on: http://localhost:8080")
            print(f"⏹️  Press Ctrl+C to stop the server")
            print()
            
            # Try to open browser automatically
            try:
                webbrowser.open(f'http://localhost:{PORT}/test-app.html')
                print("✅ Browser opened automatically")
            except:
                print("ℹ️  Could not open browser automatically")
            
            print()
            httpd.serve_forever()
            
    except KeyboardInterrupt:
        print("\n🛑 Server stopped by user")
        sys.exit(0)
    except OSError as e:
        if e.errno == 48:  # Address already in use
            print(f"❌ Port {PORT} is already in use. Try a different port or stop the other service.")
            sys.exit(1)
        else:
            print(f"❌ Error starting server: {e}")
            sys.exit(1)

if __name__ == "__main__":
    main()