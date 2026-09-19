package com.meowaddons.integration.kubejs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import dev.latvian.mods.kubejs.recipe.RecipeScriptContext;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.kubejs.recipe.component.SimpleRecipeComponent;
import dev.latvian.mods.kubejs.util.JsonUtils;
import dev.latvian.mods.rhino.type.TypeInfo;
// Recipe-компонент поверх произвольного DFU codec: JS-объект/строка -> JsonElement -> decode registry ops
// (SimpleRecipeComponent не переопределяет wrap и падает на строках/объектах)
public class KubeCodecComponent<T> extends SimpleRecipeComponent<T> {
	private final Codec<T> codec;
	private final Class<T> clazz;
	private final boolean idFromString;
	public KubeCodecComponent(RecipeComponentType<?> type, Codec<T> codec, Class<T> clazz, boolean idFromString){
		super(type, codec, TypeInfo.of(clazz));
		this.codec = codec;
		this.clazz = clazz;
		this.idFromString = idFromString;
	}
	@Override public T wrap(RecipeScriptContext rcx, Object from){
		if (clazz.isInstance(from)) return clazz.cast(from);
		var el = JsonUtils.of(rcx.cx(), from);
		if (idFromString && el instanceof JsonPrimitive p && p.isString()){
			var o = new JsonObject();
			o.addProperty("id", p.getAsString());
			el = o;
		}
		return codec.parse(rcx.ops().json(), el).getOrThrow(msg -> new RuntimeException("meowaddons kubejs component: " + msg));
	}
}
