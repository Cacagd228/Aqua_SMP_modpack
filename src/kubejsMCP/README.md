# KubeJS MCP Server

MCP server for accessing Minecraft item/block/fluid registries and managing recipes via KubeJS on NeoForge.

## Features

- **Registry Access**: List and search items, blocks, fluids
- **Tag Access**: Get all tags for items, blocks, fluids
- **Recipe Management**: Add/remove crafting, smelting, blasting, smoking, campfire, and stonecutting recipes

## Setup

### 1. Configure KubeJS Web Server

Ensure your `kubejs/config/web_server.json` has:
```json
{
  "enabled": true,
  "port": 61423,
  "public_address": "",
  "auth": "your_secure_token_here"
}
```

### 2. Install Dependencies

```bash
cd src/kubejsMCP
npm install
npm run build
```

### 3. Configure Environment

Copy `.env.example` to `.env` and update with your KubeJS web server credentials:
```env
KUBEJS_HOST=localhost
KUBEJS_PORT=61423
KUBEJS_AUTH=your_auth_token_from_web_server.json
KUBEJS_HTTPS=false
```

### 4. Add to MCP Client

Add to your MCP client config (e.g., `claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "kubejs-registry": {
      "command": "node",
      "args": ["E:/games/minecraft/Aqua_SMP_modpack/src/kubejsMCP/dist/index.js"],
      "env": {
        "KUBEJS_HOST": "localhost",
        "KUBEJS_PORT": "61423",
        "KUBEJS_AUTH": "your_auth_token",
        "KUBEJS_HTTPS": "false"
      }
    }
  }
}
```

## How it works

Uses only real KubeJS Local Web Server endpoints (no `/api/v1/execute` — it doesn't exist):

- `GET /api/registries/{ns}/{path}/keys` — list IDs
- `GET /api/registries/{ns}/{path}/match/{/regex/}` — filter by ID (note `/.../` wrapping)
- `GET /api/client/search/items|blocks|fluids?search=` — display names (items: name search, blocks/fluids: filtered client-side)
- `GET /api/tags/{ns}/{path}` + `/values/{tag-ns}/{tag-path}` — tags (nested tag paths like `c:gems/diamond` are unsupported server-side)
- Recipes: written ONLY to the live instance (`KUBEJS_LIVE_ROOT/.../kubejs/server_scripts/mcp_generated/mcp_recipes.js` + json store), then `POST /api/reload/server`. Repo stays clean; working recipes are copied to the pack manually. Without `KUBEJS_LIVE_ROOT`, falls back to the repo.

## Available Tools

### Registry Tools

- `list_items` - Get all registered items (with optional filter/limit)
- `list_blocks` - Get all registered blocks (with optional filter/limit)
- `list_fluids` - Get all registered fluids (with optional filter/limit)
- `search_items` - Search items by name/ID
- `search_blocks` - Search blocks by name/ID
- `search_fluids` - Search fluids by name/ID

### Tag Tools

- `get_item_tags` - Get all item tags with entries
- `get_block_tags` - Get all block tags with entries
- `get_fluid_tags` - Get all fluid tags with entries

### Recipe Tools

- `add_recipe` - Add a new recipe (shaped, shapeless, smelting, blasting, smoking, campfire, stonecutting)
- `remove_recipe` - Remove recipes by output item

## Recipe Examples

### Shaped Crafting
```json
{
  "type": "minecraft:crafting_shaped",
  "id": "mymod:custom_sword",
  "input": {
    "pattern": [" I ", " I ", " S "],
    "key": {
      "I": { "item": "minecraft:iron_ingot" },
      "S": { "item": "minecraft:stick" }
    }
  },
  "output": { "item": "minecraft:iron_sword", "count": 1 }
}
```

### Shapeless Crafting
```json
{
  "type": "minecraft:crafting_shapeless",
  "input": {
    "ingredients": [
      { "item": "minecraft:diamond" },
      { "item": "minecraft:diamond" },
      { "item": "minecraft:diamond" }
    ]
  },
  "output": { "item": "minecraft:diamond_block", "count": 1 }
}
```

### Smelting
```json
{
  "type": "minecraft:smelting",
  "input": {
    "ingredient": { "item": "minecraft:iron_ore" },
    "experience": 0.7,
    "cookingTime": 200
  },
  "output": { "item": "minecraft:iron_ingot", "count": 1 }
}
```

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.248
- KubeJS with web server enabled
- Node.js 20+

## Development

```bash
npm run dev    # Watch mode with tsx
npm run inspect # Test with MCP Inspector
```