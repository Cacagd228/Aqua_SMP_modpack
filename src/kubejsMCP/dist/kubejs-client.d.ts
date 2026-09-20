import { z } from "zod";
export declare const KubeJSConfigSchema: z.ZodObject<{
    host: z.ZodDefault<z.ZodString>;
    port: z.ZodDefault<z.ZodNumber>;
    auth: z.ZodString;
    https: z.ZodDefault<z.ZodBoolean>;
}, "strip", z.ZodTypeAny, {
    host: string;
    port: number;
    auth: string;
    https: boolean;
}, {
    auth: string;
    host?: string | undefined;
    port?: number | undefined;
    https?: boolean | undefined;
}>;
export type KubeJSConfig = z.infer<typeof KubeJSConfigSchema>;
export declare const RegistryItemSchema: z.ZodObject<{
    id: z.ZodString;
    name: z.ZodOptional<z.ZodString>;
    block: z.ZodOptional<z.ZodString>;
    tags: z.ZodOptional<z.ZodArray<z.ZodString, "many">>;
}, "strip", z.ZodTypeAny, {
    id: string;
    name?: string | undefined;
    block?: string | undefined;
    tags?: string[] | undefined;
}, {
    id: string;
    name?: string | undefined;
    block?: string | undefined;
    tags?: string[] | undefined;
}>;
export type RegistryItem = z.infer<typeof RegistryItemSchema>;
export declare const RegistryBlockSchema: z.ZodObject<{
    id: z.ZodString;
    name: z.ZodOptional<z.ZodString>;
}, "strip", z.ZodTypeAny, {
    id: string;
    name?: string | undefined;
}, {
    id: string;
    name?: string | undefined;
}>;
export type RegistryBlock = z.infer<typeof RegistryBlockSchema>;
export declare const RegistryFluidSchema: z.ZodObject<{
    id: z.ZodString;
    name: z.ZodOptional<z.ZodString>;
}, "strip", z.ZodTypeAny, {
    id: string;
    name?: string | undefined;
}, {
    id: string;
    name?: string | undefined;
}>;
export type RegistryFluid = z.infer<typeof RegistryFluidSchema>;
export declare const RecipeSchema: z.ZodObject<{
    type: z.ZodString;
    id: z.ZodOptional<z.ZodString>;
    input: z.ZodUnknown;
    output: z.ZodUnknown;
    conditions: z.ZodOptional<z.ZodArray<z.ZodUnknown, "many">>;
}, "strip", z.ZodTypeAny, {
    type: string;
    id?: string | undefined;
    input?: unknown;
    output?: unknown;
    conditions?: unknown[] | undefined;
}, {
    type: string;
    id?: string | undefined;
    input?: unknown;
    output?: unknown;
    conditions?: unknown[] | undefined;
}>;
export type Recipe = z.infer<typeof RecipeSchema>;
type RegistryKind = "minecraft/item" | "minecraft/block" | "minecraft/fluid";
export declare class KubeJSApiClient {
    private baseUrl;
    private auth;
    constructor(config: KubeJSConfig);
    private request;
    private get;
    getRegistryKeys(registry: RegistryKind): Promise<string[]>;
    matchRegistry(registry: RegistryKind, query: string): Promise<string[]>;
    getItems(filter?: string): Promise<RegistryItem[]>;
    getBlocks(filter?: string): Promise<RegistryBlock[]>;
    getFluids(filter?: string): Promise<RegistryFluid[]>;
    searchItems(query: string): Promise<RegistryItem[]>;
    searchBlocks(query: string): Promise<RegistryBlock[]>;
    searchFluids(query: string): Promise<RegistryFluid[]>;
    getTagList(registry: "minecraft/item" | "minecraft/block" | "minecraft/fluid"): Promise<string[]>;
    private toPath;
    getTagValues(registry: "minecraft/item" | "minecraft/block" | "minecraft/fluid", tag: string): Promise<string[]>;
    getTagsForValue(registry: "minecraft/item" | "minecraft/block" | "minecraft/fluid", valueId: string): Promise<string[]>;
    getTags(registry: "items" | "blocks" | "fluids" | "entities", filter?: string, maxTags?: number): Promise<Record<string, string[]>>;
    private gameRoot;
    private liveRoot;
    private generatedDir;
    private storePath;
    private scriptPath;
    private loadStore;
    private jsLit;
    /** Ingredient -> KubeJS JS expression. Strings pass through, {tag} -> '#tag', {item,count} -> 'Nx item' / Item.of */
    private ingExpr;
    private outExpr;
    private recipeStatement;
    private meowTier;
    /** Ingredient -> vanilla ingredient JSON list (counts expand into repeated entries, capped). */
    private meowIngredients;
    /** Output -> Create ProcessingOutput JSON list ({id, count?, chance?}). */
    private meowResults;
    private meowStepType;
    private meowStatement;
    private writeStoreAndScript;
    private tryReloadServer;
    addRecipe(recipe: Recipe): Promise<{
        saved: boolean;
        reloaded: boolean;
        files: string[];
    }>;
    removeRecipe(outputId: string, recipeType?: string): Promise<{
        saved: boolean;
        reloaded: boolean;
        files: string[];
    }>;
}
export {};
//# sourceMappingURL=kubejs-client.d.ts.map