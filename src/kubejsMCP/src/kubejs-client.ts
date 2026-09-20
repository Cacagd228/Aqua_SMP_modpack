import { z } from "zod";
import * as fs from "node:fs";
import * as path from "node:path";
import { fileURLToPath } from "node:url";

export const KubeJSConfigSchema = z.object({
  host: z.string().default("localhost"),
  port: z.number().default(61423),
  auth: z.string(),
  https: z.boolean().default(false),
});

export type KubeJSConfig = z.infer<typeof KubeJSConfigSchema>;

export const RegistryItemSchema = z.object({
  id: z.string(),
  name: z.string().optional(),
  block: z.string().optional(),
  tags: z.array(z.string()).optional(),
});
export type RegistryItem = z.infer<typeof RegistryItemSchema>;

export const RegistryBlockSchema = z.object({
  id: z.string(),
  name: z.string().optional(),
});
export type RegistryBlock = z.infer<typeof RegistryBlockSchema>;

export const RegistryFluidSchema = z.object({
  id: z.string(),
  name: z.string().optional(),
});
export type RegistryFluid = z.infer<typeof RegistryFluidSchema>;

export const RecipeSchema = z.object({
  type: z.string(),
  id: z.string().optional(),
  input: z.unknown(),
  output: z.unknown(),
  conditions: z.array(z.unknown()).optional(),
});
export type Recipe = z.infer<typeof RecipeSchema>;

type RegistryKind = "minecraft/item" | "minecraft/block" | "minecraft/fluid";

function escapeRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function findGameRoot(): string {
  // 1. explicit env
  if (process.env.KUBEJS_GAME_ROOT && fs.existsSync(path.join(process.env.KUBEJS_GAME_ROOT, "kubejs"))) {
    return process.env.KUBEJS_GAME_ROOT;
  }
  // 2. walk up from this file: src/kubejsMCP/src -> ... -> pack root (contains kubejs/)
  try {
    const here = path.dirname(fileURLToPath(import.meta.url));
    let dir = here;
    for (let i = 0; i < 6; i++) {
      if (fs.existsSync(path.join(dir, "kubejs", "server_scripts"))) return dir;
      if (fs.existsSync(path.join(dir, "kubejs")) && fs.existsSync(path.join(dir, "mods"))) return dir;
      dir = path.dirname(dir);
    }
  } catch {
    // ignore
  }
  // 3. cwd fallback
  if (fs.existsSync(path.join(process.cwd(), "kubejs"))) return process.cwd();
  return process.cwd();
}

export class KubeJSApiClient {
  private baseUrl: string;
  private auth: string;

  constructor(config: KubeJSConfig) {
    const protocol = config.https ? "https" : "http";
    this.baseUrl = `${protocol}://${config.host}:${config.port}`;
    this.auth = config.auth;
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    const response = await fetch(url, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${this.auth}`,
        ...options.headers,
      },
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`KubeJS API error: ${response.status} - ${error.slice(0, 500)}`);
    }

    const text = await response.text();
    if (!text) return undefined as T;
    try {
      return JSON.parse(text) as T;
    } catch {
      return text as unknown as T;
    }
  }

  private async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: "GET" });
  }

  // ---------- registries (real endpoints) ----------

  async getRegistryKeys(registry: RegistryKind): Promise<string[]> {
    return this.get<string[]>(`/api/registries/${registry}/keys`);
  }

  async matchRegistry(registry: RegistryKind, query: string): Promise<string[]> {
    // /match/{regex} requires /.../ JS-regex syntax; plain text -> 400
    const rx = `/${escapeRegex(query)}/`;
    return this.get<string[]>(`/api/registries/${registry}/match/${encodeURIComponent(rx)}`);
  }

  async getItems(filter?: string): Promise<RegistryItem[]> {
    const ids = filter ? await this.matchRegistry("minecraft/item", filter) : await this.getRegistryKeys("minecraft/item");
    return ids.map((id) => ({ id }));
  }

  async getBlocks(filter?: string): Promise<RegistryBlock[]> {
    const ids = filter ? await this.matchRegistry("minecraft/block", filter) : await this.getRegistryKeys("minecraft/block");
    return ids.map((id) => ({ id }));
  }

  async getFluids(filter?: string): Promise<RegistryFluid[]> {
    const ids = filter ? await this.matchRegistry("minecraft/fluid", filter) : await this.getRegistryKeys("minecraft/fluid");
    return ids.map((id) => ({ id }));
  }

  // ---------- rich search (with display names) ----------

  async searchItems(query: string): Promise<RegistryItem[]> {
    // client search = by localized display name; registry match = by id. Merge both.
    const byId = await this.getItems(query).catch(() => [] as RegistryItem[]);
    let byName: RegistryItem[] = [];
    try {
      const res = await this.get<{ results: Array<{ id: string; name?: string; block?: string; tags?: string[] }> }>(
        `/api/client/search/items?search=${encodeURIComponent(query)}&tags=true`
      );
      byName = (res.results ?? []).map((r) => ({ id: r.id, name: r.name, block: r.block, tags: r.tags }));
    } catch {
      byName = [];
    }
    const seen = new Set<string>();
    const merged: RegistryItem[] = [];
    for (const r of [...byName, ...byId]) {
      if (!seen.has(r.id)) {
        seen.add(r.id);
        merged.push(r);
      }
    }
    return merged;
  }

  async searchBlocks(query: string): Promise<RegistryBlock[]> {
    try {
      // NOTE: server ignores search for blocks/fluids and returns full list; filter client-side
      const res = await this.get<Array<{ id: string; name?: string }>>(
        `/api/client/search/blocks?search=${encodeURIComponent(query)}`
      );
      const q = query.toLowerCase();
      return res
        .filter((r) => r.id.toLowerCase().includes(q) || (r.name ?? "").toLowerCase().includes(q))
        .map((r) => ({ id: r.id, name: r.name }));
    } catch {
      return this.getBlocks(query);
    }
  }

  async searchFluids(query: string): Promise<RegistryFluid[]> {
    try {
      const res = await this.get<Array<{ id: string; name?: string }>>(
        `/api/client/search/fluids?search=${encodeURIComponent(query)}`
      );
      const q = query.toLowerCase();
      return res
        .filter((r) => r.id.toLowerCase().includes(q) || (r.name ?? "").toLowerCase().includes(q))
        .map((r) => ({ id: r.id, name: r.name }));
    } catch {
      return this.getFluids(query);
    }
  }

  // ---------- tags (real endpoints) ----------

  async getTagList(registry: "minecraft/item" | "minecraft/block" | "minecraft/fluid"): Promise<string[]> {
    return this.get<string[]>(`/api/tags/${registry}`);
  }

  private toPath(id: string): string {
    const i = id.indexOf(":");
    return i < 0 ? id : `${id.slice(0, i)}/${id.slice(i + 1)}`;
  }

  async getTagValues(registry: "minecraft/item" | "minecraft/block" | "minecraft/fluid", tag: string): Promise<string[]> {
    // server route is /values/{tag-namespace}/{tag-path}; tags with nested paths (c:gems/diamond) are unsupported server-side -> []
    if (tag.split("/").length > 1 && tag.includes(":")) {
      // e.g. c:gems/diamond -> would need 3 segments; server only accepts 2
      return [];
    }
    return this.get<string[]>(`/api/tags/${registry}/values/${this.toPath(tag)}`);
  }

  async getTagsForValue(
    registry: "minecraft/item" | "minecraft/block" | "minecraft/fluid",
    valueId: string
  ): Promise<string[]> {
    return this.get<string[]>(`/api/tags/${registry}/keys/${this.toPath(valueId)}`);
  }

  async getTags(
    registry: "items" | "blocks" | "fluids" | "entities",
    filter?: string,
    maxTags = 20
  ): Promise<Record<string, string[]>> {
    const regMap = {
      items: "minecraft/item",
      blocks: "minecraft/block",
      fluids: "minecraft/fluid",
      entities: "minecraft/entity_type",
    } as const;
    const reg = regMap[registry];
    const all = await this.get<string[]>(`/api/tags/${reg}`);
    const picked = (filter ? all.filter((t) => t.includes(filter)) : all).slice(0, filter ? maxTags : 50);
    const out: Record<string, string[]> = {};
    // fetch values with limited concurrency
    const queue = [...picked];
    const workers = Array.from({ length: 5 }, async () => {
      while (queue.length) {
        const tag = queue.shift()!;
        try {
          out[tag] = await this.getTagValues(reg as "minecraft/item" | "minecraft/block" | "minecraft/fluid", tag);
        } catch {
          out[tag] = [];
        }
      }
    });
    await Promise.all(workers);
    return out;
  }

  // ---------- recipes via files + /reload ----------
  // Recipes go ONLY to the running Prism instance (KUBEJS_LIVE_ROOT/.../minecraft).
  // The repo (gameRoot) is kept clean; working recipes are copied there manually.
  // If KUBEJS_LIVE_ROOT is unset, falls back to the repo.

  private gameRoot(): string {
    return findGameRoot();
  }

  private liveRoot(): string | null {
    const live = process.env.KUBEJS_LIVE_ROOT;
    if (live && fs.existsSync(path.join(live, "kubejs"))) return live;
    return null;
  }

  private generatedDir(root?: string): string {
    return path.join(root ?? this.gameRoot(), "kubejs", "server_scripts", "mcp_generated");
  }

  private storePath(root?: string): string {
    return path.join(this.generatedDir(root), "mcp_recipes.json");
  }

  private scriptPath(root?: string): string {
    return path.join(this.generatedDir(root), "mcp_recipes.js");
  }

  private loadStore(): { added: Recipe[]; removed: Array<{ output: string; type?: string }> } {
    try {
      const p = this.storePath(this.liveRoot() ?? undefined);
      if (fs.existsSync(p)) {
        return JSON.parse(fs.readFileSync(p, "utf-8"));
      }
    } catch {
      // ignore
    }
    return { added: [], removed: [] };
  }

  private jsLit(v: unknown): string {
    if (typeof v === "string") return JSON.stringify(v);
    if (v === null || v === undefined) return "null";
    if (typeof v === "number" || typeof v === "boolean") return String(v);
    if (Array.isArray(v)) return `[${v.map((e) => this.jsLit(e)).join(", ")}]`;
    if (typeof v === "object") {
      return `{${Object.entries(v as Record<string, unknown>)
        .map(([k, val]) => `${JSON.stringify(k)}: ${this.jsLit(val)}`)
        .join(", ")}}`;
    }
    return JSON.stringify(String(v));
  }

  /** Ingredient -> KubeJS JS expression. Strings pass through, {tag} -> '#tag', {item,count} -> 'Nx item' / Item.of */
  private ingExpr(v: unknown): string {
    if (typeof v === "string") return JSON.stringify(v);
    if (v == null) return "null";
    const o = v as Record<string, unknown>;
    if (typeof o.fluid === "string") return `Fluid.of(${JSON.stringify(o.fluid)}, ${Number(o.amount ?? 250)})`;
    if (typeof o.tag === "string") {
      const t = o.tag as string;
      return JSON.stringify(t.startsWith("#") ? t : `#${t}`);
    }
    if (typeof o.item === "string") {
      const count = Number(o.count ?? 1);
      if (o.chance !== undefined) return `Item.of(${JSON.stringify(o.item)}, ${count}).withChance(${Number(o.chance)})`;
      if (count !== 1) return JSON.stringify(`${count}x ${o.item}`);
      return JSON.stringify(o.item);
    }
    return this.jsLit(v);
  }

  private outExpr(v: unknown): string {
    if (Array.isArray(v)) {
      if (v.length === 1) return this.ingExpr(v[0]);
      return `[${v.map((e) => this.ingExpr(e)).join(", ")}]`;
    }
    if (v != null && typeof v === "object" && !("item" in (v as object) || "tag" in (v as object) || "fluid" in (v as object))) {
      return this.jsLit(v);
    }
    return this.ingExpr(v);
  }

  private recipeStatement(r: Recipe): string {
    const input = (r.input ?? {}) as Record<string, unknown>;
    const idLine = r.id ? `  .id(${JSON.stringify(r.id)})` : "";
    const withId = (expr: string) => (idLine ? `${expr}\n${idLine}` : expr);
    if (r.type.startsWith("meowaddons:")) {
      return withId(this.meowStatement(r));
    }
    switch (r.type) {
      case "minecraft:crafting_shaped": {
        const p = input as { pattern: string[]; key: Record<string, unknown> };
        return withId(`event.shaped(${this.outExpr(r.output)}, ${this.jsLit(p.pattern)}, ${this.jsLit(p.key)})`);
      }
      case "minecraft:crafting_shapeless": {
        const p = input as { ingredients: unknown[] };
        return withId(`event.shapeless(${this.outExpr(r.output)}, ${this.jsLit(p.ingredients ?? [])})`);
      }
      case "minecraft:smelting":
      case "minecraft:blasting":
      case "minecraft:smoking":
      case "minecraft:campfire_cooking": {
        const p = input as { ingredient: unknown; experience?: number; cookingTime?: number };
        const kind = r.type === "minecraft:campfire_cooking" ? "campfireCooking" : r.type.split(":")[1];
        return withId(
          `event.${kind}(${this.outExpr(r.output)}, ${this.ingExpr(p.ingredient)}, ${Number(p.experience ?? 0)}, ${Number(p.cookingTime ?? (r.type === "minecraft:smelting" ? 200 : r.type === "minecraft:blasting" || r.type === "minecraft:smoking" ? 100 : 600))})`
        );
      }
      case "minecraft:stonecutting": {
        const p = input as { ingredient: unknown };
        return withId(`event.stonecutting(${this.outExpr(r.output)}, ${this.ingExpr(p.ingredient)})`);
      }
      case "create:mixing":
      case "create:compacting": {
        const p = input as { ingredients: unknown[]; heat?: string };
        const kind = r.type.split(":")[1];
        const ings = `[${(p.ingredients ?? []).map((e) => this.ingExpr(e)).join(", ")}]`;
        let expr = `event.recipes.create.${kind}(${this.outExpr(r.output)}, ${ings})`;
        if (p.heat === "heated") expr += ".heated()";
        if (p.heat === "superheated") expr += ".superheated()";
        return withId(expr);
      }
      case "create:pressing":
      case "create:milling":
      case "create:splashing":
      case "create:haunting":
      case "create:sandpaper_polishing": {
        const p = input as { ingredient: unknown };
        const kind = (r.type.split(":")[1] === "sandpaper_polishing" ? "sandpaperPolishing" : r.type.split(":")[1]) as string;
        return withId(`event.recipes.create.${kind}(${this.outExpr(r.output)}, ${this.ingExpr(p.ingredient)})`);
      }
      case "create:cutting":
      case "create:crushing": {
        const p = input as { ingredient: unknown; processingTime?: number };
        const kind = r.type.split(":")[1];
        let expr = `event.recipes.create.${kind}(${this.outExpr(r.output)}, ${this.ingExpr(p.ingredient)})`;
        if (p.processingTime) expr += `.processingTime(${Number(p.processingTime)})`;
        return withId(expr);
      }
      case "create:deploying": {
        const p = input as { ingredients: unknown[]; keepHeldItem?: boolean };
        let expr = `event.recipes.create.deploying(${this.outExpr(r.output)}, [${(p.ingredients ?? []).map((e) => this.ingExpr(e)).join(", ")}])`;
        if (p.keepHeldItem) expr += ".keepHeldItem()";
        return withId(expr);
      }
      case "create:filling": {
        const p = input as { ingredients: unknown[] };
        return withId(
          `event.recipes.create.filling(${this.ingExpr(r.output)}, [${(p.ingredients ?? []).map((e) => this.ingExpr(e)).join(", ")}])`
        );
      }
      case "create:emptying": {
        const p = input as { ingredient: unknown };
        return withId(`event.recipes.create.emptying(${this.outExpr(r.output)}, ${this.ingExpr(p.ingredient)})`);
      }
      case "create:mechanical_crafting": {
        const p = input as { pattern: string[]; key: Record<string, unknown> };
        return withId(`event.recipes.create.mechanicalCrafting(${this.outExpr(r.output)}, ${this.jsLit(p.pattern)}, ${this.jsLit(p.key)})`);
      }
      case "create:sequenced_assembly": {
        const p = input as { ingredient: unknown; transitionalItem: string; sequence: Array<{ type: string; input: unknown; output: unknown }>; loops?: number };
        const normIn = (sIn: unknown): string => {
          if (sIn == null) return "null";
          if (Array.isArray(sIn)) return `[${sIn.map((e) => this.ingExpr(e)).join(", ")}]`;
          if (typeof sIn === "object") {
            const o = sIn as Record<string, unknown>;
            if ("ingredients" in o) return `[${((o.ingredients as unknown[]) ?? []).map((e) => this.ingExpr(e)).join(", ")}]`;
            if ("ingredient" in o) return this.ingExpr(o.ingredient);
          }
          return this.ingExpr(sIn);
        };
        const normOut = (sOut: unknown): string => {
          if (Array.isArray(sOut)) return this.outExpr(sOut);
          if (sOut != null && typeof sOut === "object") {
            const o = sOut as Record<string, unknown>;
            if ("ingredients" in o || "ingredient" in o) return normIn(sOut);
          }
          return this.outExpr(sOut);
        };
        const seq = (p.sequence ?? [])
          .map((s) => {
            const t = String(s.type ?? "").replace(/^create:/, "");
            return `event.recipes.create.${t}(${normOut(s.output)}, ${normIn(s.input)})`;
          })
          .join(", ");
        const expr =
          `event.recipes.create.sequencedAssembly([${Array.isArray(r.output) ? (r.output as unknown[]).map((e) => this.ingExpr(e)).join(", ") : this.ingExpr(r.output)}], ` +
          `${this.ingExpr(p.ingredient)}, [${seq}])` +
          `.transitionalItem(${JSON.stringify(p.transitionalItem)}).loops(${Number(p.loops ?? 1)})`;
        return withId(expr);
      }
      default:
        return withId(`event.custom(${this.jsLit({ type: r.type, ...(input as object), ...(r.output !== undefined ? { result: r.output } : {}) })})`);
    }
  }

  // ---------- meowaddons tiered Create machines ----------
  // Recipes are emitted as event.custom() with Create-style JSON, parsed directly by
  // the mod's serializer codecs (ProcessingRecipeParams / TieredSequencedAssemblyRecipeSerializer).
  // Types: meowaddons:<pressing|crushing|milling|mixing|cutting|deploying|sequenced_assembly> + tier 1-6
  // (1=andesite 2=brass 3=steel 4=shadow_steel 5=refined_radiance 6=chromatic).

  private meowTier(r: Recipe): number {
    const tier = Number((r as unknown as Record<string, unknown>).tier);
    if (!Number.isInteger(tier) || tier < 1 || tier > 6) {
      throw new Error(`meowaddons: missing/invalid "tier" (1-6) for ${r.type}`);
    }
    return tier;
  }

  /** Ingredient -> vanilla ingredient JSON list (counts expand into repeated entries, capped). */
  private meowIngredients(v: unknown): Array<Record<string, unknown>> {
    const out: Array<Record<string, unknown>> = [];
    const push = (e: unknown, n: number) => {
      const times = Math.min(64, Math.max(1, Math.floor(n) || 1));
      for (let i = 0; i < times; i++) out.push(e as Record<string, unknown>);
    };
    const one = (e: unknown): void => {
      if (typeof e === "string") {
        const m = e.match(/^(\d+)\s*x\s+(.+)$/i);
        if (m) {
          const id = m[2].trim();
          push(id.startsWith("#") ? { tag: id.slice(1) } : { item: id }, Number(m[1]));
          return;
        }
        push(e.startsWith("#") ? { tag: e.slice(1) } : { item: e }, 1);
        return;
      }
      if (e == null) return;
      const o = e as Record<string, unknown>;
      if (typeof o.fluid === "string") {
        push({ fluid: o.fluid, amount: Number(o.amount ?? 250) }, 1);
        return;
      }
      if (typeof o.tag === "string") {
        const t = o.tag as string;
        push({ tag: t.startsWith("#") ? t.slice(1) : t }, 1);
        return;
      }
      if (typeof o.item === "string") {
        push({ item: o.item }, Number(o.count ?? 1));
        return;
      }
      throw new Error(`meowaddons: unsupported ingredient ${JSON.stringify(e)}`);
    };
    if (Array.isArray(v)) v.forEach(one);
    else one(v);
    return out;
  }

  /** Output -> Create ProcessingOutput JSON list ({id, count?, chance?}). */
  private meowResults(v: unknown): Array<Record<string, unknown>> {
    const one = (e: unknown): Record<string, unknown> => {
      if (typeof e === "string") {
        const m = e.match(/^(\d+)\s*x\s+(.+)$/i);
        if (m) return { id: m[2].trim(), count: Number(m[1]) };
        return { id: e };
      }
      if (e != null && typeof e === "object") {
        const o = e as Record<string, unknown>;
        if (typeof o.item === "string") {
          const res: Record<string, unknown> = { id: o.item };
          if (o.count !== undefined) res.count = Number(o.count);
          if (o.chance !== undefined) res.chance = Number(o.chance);
          return res;
        }
      }
      throw new Error(`meowaddons: results need item ids (tags/fluids unsupported): ${JSON.stringify(e)}`);
    };
    return (Array.isArray(v) ? v : [v]).map(one);
  }

  private meowStepType(step: { type?: string; tier?: number }): string {
    const t = String(step.type ?? "");
    const m = t.match(/^meowaddons:([a-z_]+?)(_t[1-6])?$/);
    if (m) {
      if (m[2]) return t;
      const tier = Number(step.tier);
      if (!Number.isInteger(tier) || tier < 1 || tier > 6) {
        throw new Error(`meowaddons sequence step "${t}" needs tier 1-6 (or use "${t}_tN")`);
      }
      return `meowaddons:${m[1]}_t${tier}`;
    }
    return t;
  }

  private meowStatement(r: Recipe): string {
    const tier = this.meowTier(r);
    const input = (r.input ?? {}) as Record<string, unknown>;
    const cat = r.type.split(":")[1];
    if (cat === "sequenced_assembly") {
      const p = input as {
        ingredient: unknown;
        transitionalItem: string;
        sequence: Array<{ type?: string; tier?: number; input?: unknown; output?: unknown; processingTime?: number; heat?: string }>;
        loops?: number;
      };
      const seq = (p.sequence ?? []).map((s) => {
        const sIn = (s.input ?? {}) as Record<string, unknown>;
        const list = Array.isArray(sIn)
          ? sIn
          : Array.isArray(sIn.ingredients)
            ? (sIn.ingredients as unknown[])
            : sIn.ingredient !== undefined
              ? [sIn.ingredient]
              : [s.input];
        const step: Record<string, unknown> = {
          type: this.meowStepType(s),
          ingredients: this.meowIngredients(list),
          results: this.meowResults(s.output),
        };
        if (typeof sIn.processingTime === "number" || typeof s.processingTime === "number") {
          step.processingTime = Number(sIn.processingTime ?? s.processingTime);
        }
        const heat = (sIn.heat ?? s.heat) as string | undefined;
        if (heat && heat !== "none") step.heatRequirement = heat;
        return step;
      });
      if (tier >= 2 && !seq.some((s) => String(s.type).startsWith("meowaddons:"))) {
        throw new Error(`meowaddons:sequenced_assembly tier ${tier} requires at least one meowaddons:* step (mod rejects untiered-only assemblies at t2+)`);
      }
      const json: Record<string, unknown> = {
        type: `meowaddons:sequenced_assembly_t${tier}`,
        ingredient: this.meowIngredients(p.ingredient)[0],
        transitional_item: { id: p.transitionalItem },
        sequence: seq,
        results: this.meowResults(r.output),
        loops: Math.min(16, Math.max(1, Number(p.loops ?? 5))),
      };
      return `event.custom(${JSON.stringify(json)})`;
    }
    const p = input as { ingredients?: unknown; heat?: string; processingTime?: number };
    const json: Record<string, unknown> = {
      type: `meowaddons:${cat}_t${tier}`,
      ingredients: this.meowIngredients(p.ingredients ?? []),
      results: this.meowResults(r.output),
    };
    if (typeof p.processingTime === "number" && (cat === "crushing" || cat === "milling" || cat === "cutting" || cat === "mixing")) {
      json.processingTime = Math.max(0, Math.floor(p.processingTime));
    }
    if (cat === "mixing" && p.heat && p.heat !== "none") {
      json.heatRequirement = p.heat;
    }
    return `event.custom(${JSON.stringify(json)})`;
  }

  private writeStoreAndScript(store: { added: Recipe[]; removed: Array<{ output: string; type?: string }> }): string[] {
    const live = this.liveRoot();
    const targets = (live ? [live] : [undefined]) as Array<string | undefined>;
    const written: string[] = [];
    for (const root of targets) {
      fs.mkdirSync(this.generatedDir(root ?? undefined), { recursive: true });
      fs.writeFileSync(this.storePath(root ?? undefined), JSON.stringify(store, null, 2), "utf-8");
      const lines: string[] = [
        "// AUTO-GENERATED by kubejs-mcp. Do not edit manually.",
        "// Re-applied on every add_recipe/remove_recipe. Game root reload via POST /api/reload/server.",
        "ServerEvents.recipes((event) => {",
      ];
      for (const rem of store.removed) {
        lines.push(`  event.remove(${this.jsLit(rem)});`);
      }
      for (const r of store.added) {
        lines.push(`  ${this.recipeStatement(r)};`);
      }
      lines.push("});", "");
      const script = this.scriptPath(root ?? undefined);
      fs.writeFileSync(script, lines.join("\n"), "utf-8");
      written.push(script);
    }
    return written;
  }

  private async tryReloadServer(): Promise<boolean> {
    try {
      const res = await fetch(`${this.baseUrl}/api/reload/server`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${this.auth}`, "Content-Type": "application/json" },
        body: "{}",
      });
      return res.ok;
    } catch {
      return false;
    }
  }

  async addRecipe(recipe: Recipe): Promise<{ saved: boolean; reloaded: boolean; files: string[] }> {
    const store = this.loadStore();
    if (recipe.id) store.added = store.added.filter((r) => r.id !== recipe.id);
    store.added.push(recipe);
    const files = this.writeStoreAndScript(store);
    const reloaded = await this.tryReloadServer();
    return { saved: true, reloaded, files };
  }

  async removeRecipe(outputId: string, recipeType?: string): Promise<{ saved: boolean; reloaded: boolean; files: string[] }> {
    const store = this.loadStore();
    // drop previously added recipes with same output to avoid re-adding deleted ones
    store.added = store.added.filter((r) => {
      const out = r.output as unknown;
      const outId =
        typeof out === "string" ? out : (out as Record<string, unknown> | undefined)?.item as string | undefined;
      return outId !== outputId;
    });
    store.removed.push(recipeType ? { output: outputId, type: recipeType } : { output: outputId });
    const files = this.writeStoreAndScript(store);
    const reloaded = await this.tryReloadServer();
    return { saved: true, reloaded, files };
  }
}
