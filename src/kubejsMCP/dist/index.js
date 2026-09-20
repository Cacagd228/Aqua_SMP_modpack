import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { KubeJSApiClient, KubeJSConfigSchema } from "./kubejs-client.js";
const config = KubeJSConfigSchema.parse({
    host: process.env.KUBEJS_HOST || "localhost",
    port: parseInt(process.env.KUBEJS_PORT || "61423"),
    auth: process.env.KUBEJS_AUTH || "",
    https: process.env.KUBEJS_HTTPS === "true",
});
const client = new KubeJSApiClient(config);
const server = new McpServer({
    name: "kubejs-registry",
    version: "1.0.0",
});
server.tool("list_items", "List item IDs from live game registry (use search_items for names). No filter = first N of 21k+", {
    filter: z.string().optional().describe("Filter items by substring of ID"),
    limit: z.number().optional().default(100).describe("Maximum number of items to return"),
}, async ({ filter, limit }) => {
    const items = await client.getItems(filter);
    const limited = items.slice(0, limit);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify({ total: items.length, items: limited }, null, 2),
            },
        ],
    };
});
server.tool("list_blocks", "List block IDs from live game registry (use search_blocks for names)", {
    filter: z.string().optional().describe("Filter blocks by substring of ID"),
    limit: z.number().optional().default(100).describe("Maximum number of blocks to return"),
}, async ({ filter, limit }) => {
    const blocks = await client.getBlocks(filter);
    const limited = blocks.slice(0, limit);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify({ total: blocks.length, blocks: limited }, null, 2),
            },
        ],
    };
});
server.tool("list_fluids", "List fluid IDs from live game registry (use search_fluids for names)", {
    filter: z.string().optional().describe("Filter fluids by substring of ID"),
    limit: z.number().optional().default(100).describe("Maximum number of fluids to return"),
}, async ({ filter, limit }) => {
    const fluids = await client.getFluids(filter);
    const limited = fluids.slice(0, limit);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify({ total: fluids.length, fluids: limited }, null, 2),
            },
        ],
    };
});
server.tool("get_item_tags", "List item tags; with filter returns up to 20 tags with their values (unfiltered = first 50 tags with values, full list is 2400+)", {
    filter: z.string().optional().describe("Filter tags by partial name match"),
}, async ({ filter }) => {
    const tags = await client.getTags("items", filter);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify(tags, null, 2),
            },
        ],
    };
});
server.tool("get_block_tags", "List block tags; with filter returns up to 20 tags with their values", {
    filter: z.string().optional().describe("Filter tags by partial name match"),
}, async ({ filter }) => {
    const tags = await client.getTags("blocks", filter);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify(tags, null, 2),
            },
        ],
    };
});
server.tool("get_fluid_tags", "List fluid tags; with filter returns up to 20 tags with their values", {
    filter: z.string().optional().describe("Filter tags by partial name match"),
}, async ({ filter }) => {
    const tags = await client.getTags("fluids", filter);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify(tags, null, 2),
            },
        ],
    };
});
server.tool("search_items", "Search items by ID/name via live client search (returns id, localized name, tags)", {
    query: z.string().describe("Search query"),
    limit: z.number().optional().default(50).describe("Maximum results"),
}, async ({ query, limit }) => {
    const items = await client.searchItems(query);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify({ total: items.length, items: items.slice(0, limit) }, null, 2),
            },
        ],
    };
});
server.tool("search_blocks", "Search blocks by ID/name via live client search", {
    query: z.string().describe("Search query"),
    limit: z.number().optional().default(50).describe("Maximum results"),
}, async ({ query, limit }) => {
    const blocks = await client.searchBlocks(query);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify({ total: blocks.length, blocks: blocks.slice(0, limit) }, null, 2),
            },
        ],
    };
});
server.tool("search_fluids", "Search fluids by ID/name via live client search", {
    query: z.string().describe("Search query"),
    limit: z.number().optional().default(50).describe("Maximum results"),
}, async ({ query, limit }) => {
    const fluids = await client.searchFluids(query);
    return {
        content: [
            {
                type: "text",
                text: JSON.stringify({ total: fluids.length, fluids: fluids.slice(0, limit) }, null, 2),
            },
        ],
    };
});
const RecipeInputSchema = z.object({
    type: z.enum([
        "minecraft:crafting_shaped",
        "minecraft:crafting_shapeless",
        "minecraft:smelting",
        "minecraft:blasting",
        "minecraft:smoking",
        "minecraft:campfire_cooking",
        "minecraft:stonecutting",
        "create:mixing",
        "create:compacting",
        "create:pressing",
        "create:cutting",
        "create:milling",
        "create:crushing",
        "create:deploying",
        "create:filling",
        "create:emptying",
        "create:splashing",
        "create:haunting",
        "create:mechanical_crafting",
        "create:sandpaper_polishing",
        "create:sequenced_assembly",
        "meowaddons:pressing",
        "meowaddons:crushing",
        "meowaddons:milling",
        "meowaddons:mixing",
        "meowaddons:cutting",
        "meowaddons:deploying",
        "meowaddons:sequenced_assembly",
    ]),
    id: z.string().optional().describe("Optional recipe ID"),
});
// Create ingredients accept strings ('minecraft:iron_ingot', '#forge:ingots')
// or objects ({item, tag, fluid, amount, count, chance})
const CreateIngredient = z
    .union([
    z.string(),
    z.object({
        item: z.string().optional(),
        tag: z.string().optional(),
        fluid: z.string().optional(),
        amount: z.number().optional().describe("Fluid amount in mB"),
        count: z.number().optional(),
        chance: z.number().optional().describe("0.0-1.0 chance for outputs"),
    }),
])
    .describe("Item string, tag string, or {item/tag/fluid, amount, chance}");
const CreateIngredientsInput = z.object({
    ingredients: z.array(CreateIngredient).describe("List of inputs (items, tags, fluids)"),
    heat: z.enum(["none", "heated", "superheated"]).optional().describe("For mixing/compacting: heat requirement"),
    processingTime: z.number().optional().describe("For crushing/cutting: ticks"),
    keepHeldItem: z.boolean().optional().describe("For deploying: don't consume held item"),
});
const CreateOutput = z
    .union([CreateIngredient, z.array(CreateIngredient)])
    .describe("Single result or array of results (supports chance)");
const ShapedRecipeSchema = RecipeInputSchema.extend({
    type: z.literal("minecraft:crafting_shaped"),
    input: z.object({
        pattern: z.array(z.string()).length(3),
        key: z.record(z.object({ item: z.string().optional(), tag: z.string().optional() })),
    }),
    output: z.object({ item: z.string(), count: z.number().optional() }),
});
const ShapelessRecipeSchema = RecipeInputSchema.extend({
    type: z.literal("minecraft:crafting_shapeless"),
    input: z.object({
        ingredients: z.array(z.object({ item: z.string().optional(), tag: z.string().optional(), count: z.number().optional() })),
    }),
    output: z.object({ item: z.string(), count: z.number().optional() }),
});
const SmeltingRecipeSchema = RecipeInputSchema.extend({
    type: z.literal("minecraft:smelting"),
    input: z.object({
        ingredient: z.object({ item: z.string().optional(), tag: z.string().optional() }),
        experience: z.number().optional(),
        cookingTime: z.number().optional(),
    }),
    output: z.object({ item: z.string(), count: z.number().optional() }),
});
const BlastingRecipeSchema = RecipeInputSchema.extend({
    type: z.literal("minecraft:blasting"),
    input: z.object({
        ingredient: z.object({ item: z.string().optional(), tag: z.string().optional() }),
        experience: z.number().optional(),
        cookingTime: z.number().optional(),
    }),
    output: z.object({ item: z.string(), count: z.number().optional() }),
});
const SmokingRecipeSchema = RecipeInputSchema.extend({
    type: z.literal("minecraft:smoking"),
    input: z.object({
        ingredient: z.object({ item: z.string().optional(), tag: z.string().optional() }),
        experience: z.number().optional(),
        cookingTime: z.number().optional(),
    }),
    output: z.object({ item: z.string(), count: z.number().optional() }),
});
const CampfireRecipeSchema = RecipeInputSchema.extend({
    type: z.literal("minecraft:campfire_cooking"),
    input: z.object({
        ingredient: z.object({ item: z.string().optional(), tag: z.string().optional() }),
        experience: z.number().optional(),
        cookingTime: z.number().optional(),
    }),
    output: z.object({ item: z.string(), count: z.number().optional() }),
});
const StonecuttingRecipeSchema = RecipeInputSchema.extend({
    type: z.literal("minecraft:stonecutting"),
    input: z.object({
        ingredient: z.object({ item: z.string().optional(), tag: z.string().optional() }),
    }),
    output: z.object({ item: z.string(), count: z.number().optional() }),
});
// --- Create recipes (kubejs-create, event.recipes.create.*) ---
const CreateMixingSchema = RecipeInputSchema.extend({
    type: z.literal("create:mixing"),
    input: CreateIngredientsInput,
    output: CreateOutput,
});
const CreateCompactingSchema = RecipeInputSchema.extend({
    type: z.literal("create:compacting"),
    input: CreateIngredientsInput,
    output: CreateOutput,
});
const CreatePressingSchema = RecipeInputSchema.extend({
    type: z.literal("create:pressing"),
    input: z.object({ ingredient: CreateIngredient }),
    output: CreateOutput,
});
const CreateCuttingSchema = RecipeInputSchema.extend({
    type: z.literal("create:cutting"),
    input: z.object({
        ingredient: CreateIngredient,
        processingTime: z.number().optional(),
    }),
    output: CreateOutput,
});
const CreateMillingSchema = RecipeInputSchema.extend({
    type: z.literal("create:milling"),
    input: z.object({ ingredient: CreateIngredient }),
    output: CreateOutput,
});
const CreateCrushingSchema = RecipeInputSchema.extend({
    type: z.literal("create:crushing"),
    input: z.object({
        ingredient: CreateIngredient,
        processingTime: z.number().optional(),
    }),
    output: CreateOutput,
});
const CreateDeployingSchema = RecipeInputSchema.extend({
    type: z.literal("create:deploying"),
    input: z.object({
        ingredients: z.array(CreateIngredient).length(2).describe("[target, heldItem] - exactly 2 inputs"),
        keepHeldItem: z.boolean().optional(),
    }),
    output: CreateOutput,
});
const CreateFillingSchema = RecipeInputSchema.extend({
    type: z.literal("create:filling"),
    input: z.object({
        ingredients: z.array(CreateIngredient).length(2).describe("[item, fluid]"),
    }),
    output: CreateIngredient,
});
const CreateEmptyingSchema = RecipeInputSchema.extend({
    type: z.literal("create:emptying"),
    input: z.object({ ingredient: CreateIngredient }),
    output: z.array(CreateIngredient).length(2).describe("[item, fluid]"),
});
const CreateSplashingSchema = RecipeInputSchema.extend({
    type: z.literal("create:splashing"),
    input: z.object({ ingredient: CreateIngredient }),
    output: CreateOutput,
});
const CreateHauntingSchema = RecipeInputSchema.extend({
    type: z.literal("create:haunting"),
    input: z.object({ ingredient: CreateIngredient }),
    output: CreateOutput,
});
const CreateMechanicalCraftingSchema = RecipeInputSchema.extend({
    type: z.literal("create:mechanical_crafting"),
    input: z.object({
        pattern: z.array(z.string()).min(1).max(9).describe("Up to 9x9 pattern"),
        key: z.record(z.object({ item: z.string().optional(), tag: z.string().optional() })),
    }),
    output: z.object({ item: z.string(), count: z.number().optional() }),
});
const CreateSandpaperPolishingSchema = RecipeInputSchema.extend({
    type: z.literal("create:sandpaper_polishing"),
    input: z.object({ ingredient: CreateIngredient }),
    output: CreateOutput,
});
const CreateSequencedAssemblySchema = RecipeInputSchema.extend({
    type: z.literal("create:sequenced_assembly"),
    input: z.object({
        ingredient: CreateIngredient.describe("Base input item"),
        transitionalItem: z.string().describe("Intermediate item, e.g. 'create:incomplete_precision_mechanism'"),
        sequence: z
            .array(z.object({ type: z.string(), input: z.unknown(), output: z.unknown() }))
            .describe("Steps: create:deploying/pressing/filling/cutting sub-recipes with transitional item as in+out"),
        loops: z.number().optional().default(5),
    }),
    output: z.array(CreateIngredient).describe("First = main output, rest = random salvage"),
});
const MeowBase = RecipeInputSchema.extend({
    tier: z.number().int().min(1).max(6).describe("MeowAddons machine tier: 1=andesite 2=brass 3=steel 4=shadow_steel 5=refined_radiance 6=chromatic"),
});
const MeowProcessingSchema = MeowBase.extend({
    type: z.enum([
        "meowaddons:pressing",
        "meowaddons:crushing",
        "meowaddons:milling",
        "meowaddons:mixing",
        "meowaddons:cutting",
        "meowaddons:deploying",
    ]),
    input: CreateIngredientsInput.describe("ingredients list; heat for mixing only (heated/superheated); processingTime for crushing/milling/cutting/mixing"),
    output: CreateOutput,
});
const MeowSequencedAssemblySchema = MeowBase.extend({
    type: z.literal("meowaddons:sequenced_assembly"),
    input: z.object({
        ingredient: CreateIngredient.describe("Base input item"),
        transitionalItem: z.string().describe("Intermediate item id"),
        sequence: z
            .array(z.object({ type: z.string(), tier: z.number().int().min(1).max(6).optional(), input: z.unknown(), output: z.unknown() }))
            .describe("Steps: create:* or meowaddons:* (use 'meowaddons:pressing_t2' or {type:'meowaddons:pressing',tier:2}); tier2+ assemblies require at least one meowaddons step"),
        loops: z.number().optional().default(5),
    }),
    output: z.array(CreateIngredient).describe("First = main output, rest = random salvage"),
});
const AnyRecipeSchema = z.union([
    ShapedRecipeSchema,
    ShapelessRecipeSchema,
    SmeltingRecipeSchema,
    BlastingRecipeSchema,
    SmokingRecipeSchema,
    CampfireRecipeSchema,
    StonecuttingRecipeSchema,
    CreateMixingSchema,
    CreateCompactingSchema,
    CreatePressingSchema,
    CreateCuttingSchema,
    CreateMillingSchema,
    CreateCrushingSchema,
    CreateDeployingSchema,
    CreateFillingSchema,
    CreateEmptyingSchema,
    CreateSplashingSchema,
    CreateHauntingSchema,
    CreateMechanicalCraftingSchema,
    CreateSandpaperPolishingSchema,
    CreateSequencedAssemblySchema,
    MeowProcessingSchema,
    MeowSequencedAssemblySchema,
]);
server.tool("add_recipe", "Add recipe by writing to live instance kubejs/server_scripts/mcp_generated/mcp_recipes.js + POST /api/reload/server (vanilla + Create + meowaddons tiered machines, tier 1-6)", {
    recipe: AnyRecipeSchema,
}, async ({ recipe }) => {
    try {
        const res = await client.addRecipe(recipe);
        return {
            content: [
                {
                    type: "text",
                    text: `Recipe saved to ${res.files.join(" + ")}${recipe.id ? ` (ID: ${recipe.id})` : ""}${res.reloaded ? " + live reload OK" : " (reload skipped - will apply on /reload or restart)"}`,
                },
            ],
        };
    }
    catch (e) {
        return { content: [{ type: "text", text: `Failed to add recipe: ${e instanceof Error ? e.message : String(e)}` }] };
    }
});
server.tool("remove_recipe", "Remove recipe by output: appends event.remove to mcp_recipes.js + reload", {
    output: z.string().describe("Output item ID to remove recipes for"),
    type: z.string().optional().describe("Optional recipe type to filter"),
}, async ({ output, type }) => {
    try {
        const res = await client.removeRecipe(output, type);
        return {
            content: [
                {
                    type: "text",
                    text: `Remove for ${output} saved to ${res.files.join(" + ")}${res.reloaded ? " + live reload OK" : " (reload skipped)"}`,
                },
            ],
        };
    }
    catch (e) {
        return { content: [{ type: "text", text: `Failed to remove recipe: ${e instanceof Error ? e.message : String(e)}` }] };
    }
});
async function main() {
    const transport = new StdioServerTransport();
    await server.connect(transport);
    console.error("KubeJS MCP Server running on stdio");
}
main().catch((err) => {
    console.error("Server error:", err);
    process.exit(1);
});
//# sourceMappingURL=index.js.map