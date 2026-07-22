package com.example.frostbear.item;

import com.example.frostbear.FrostBear;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/** Diamond-equivalent material for Frostfang, repaired with Glacial Cores. */
public final class FrostfangToolMaterial {
	public static final TagKey<Item> REPAIR_ITEMS = TagKey.create(
			Registries.ITEM, Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, "repairs_frostfang"));

	public static final ToolMaterial INSTANCE = new ToolMaterial(
			BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
			2200,
			8.0F,
			3.0F,
			14,
			REPAIR_ITEMS);

	private FrostfangToolMaterial() {
	}
}