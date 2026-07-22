package com.example.frostbear.registry;

import java.util.function.Function;
import com.example.frostbear.FrostBear;
import com.example.frostbear.item.FrostfangItem;
import com.example.frostbear.item.FrostfangToolMaterial;
import com.example.frostbear.item.FrozenHeartItem;
import com.example.frostbear.item.GlacialCoreItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;

public final class ModItems {
	private ModItems() {
	}

	public static final Item FROST_HEART = register("frost_heart", FrozenHeartItem::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));

	public static final Item GLACIAL_CORE = register("glacial_core", GlacialCoreItem::new,
			new Item.Properties().rarity(Rarity.EPIC));

	public static final Item FROSTFANG = register("frostfang", FrostfangItem::new,
			new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)
					.sword(FrostfangToolMaterial.INSTANCE, 3.0F, -2.4F));

	public static final Item FROST_BEAR_SPAWN_EGG = register("frost_bear_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.FROST_BEAR));

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory,
			Item.Properties properties) {
		Identifier id = Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, name);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void register() {
		FrostBear.LOGGER.info("[FrostBear] Registering items");
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(entries -> entries.accept(FROST_BEAR_SPAWN_EGG));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(entries -> {
					entries.accept(FROST_HEART);
					entries.accept(GLACIAL_CORE);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(entries -> entries.accept(FROSTFANG));
	}
}