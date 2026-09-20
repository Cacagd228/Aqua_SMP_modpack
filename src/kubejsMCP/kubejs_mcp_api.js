// KubeJS startup script to enable script execution via web server
// Place this in kubejs/startup_scripts/mcp_api.js

// The KubeJS web server already has an execute endpoint at /api/v1/execute
// but it requires the script to be in a specific format.
// This script ensures the endpoint works correctly.

if (Platform.isServer()) {
  // Register a custom endpoint for more flexible script execution
  HttpServer.onRequest("/api/mcp/execute", (request, response) => {
    if (request.method !== "POST") {
      response.status(405).send("Method not allowed");
      return;
    }
    
    let body = "";
    request.onData((data) => {
      body += data;
    });
    
    request.onEnd(() => {
      try {
        const { script } = JSON.parse(body);
        if (!script) {
          response.status(400).json({ error: "Missing script parameter" });
          return;
        }
        
        // Execute the script in KubeJS context
        const result = eval(script);
        response.json({ result });
      } catch (e) {
        response.status(500).json({ error: e.message });
      }
    });
  });
  
  console.log("[MCP] Custom execute endpoint registered at /api/mcp/execute");
}