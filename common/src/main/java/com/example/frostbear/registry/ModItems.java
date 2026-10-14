package com.example.frostbear.registry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import com.example.frostbear.FrostBear;
import com.example.frostbear.item.FrostfangItem;
import com.example.frostbear.item.FrostfangToolMaterial;
import com.example.frostbear.item.FrozenHeartItem;
import com.example.frostbear.item.GlacialCoreItem;
import com.example.frostbear.platform.RegEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ItemLike;

public final class ModItems {
	private ModItems() {}

	public static final RegEntry<Item, FrozenHeartItem> FROST_HEART = register("frost_heart",
			FrozenHeartItem::new, properties -> properties.stacksTo(16).rarity(Rarity.EPIC));

	public static final RegEntry<Item, GlacialCoreItem> GLACIAL_CORE = register("glacial_core",
			GlacialCoreItem::new, properties -> properties.rarity(Rarity.EPIC));

	public static final RegEntry<Item, FrostfangItem> FROSTFANG = register("frostfang",
			FrostfangItem::new, properties -> properties.rarity(Rarity.EPIC).stacksTo(1)
					.sword(FrostfangToolMaterial.INSTANCE, 3.0F, -2.4F));

	public static final RegEntry<Item, SpawnEggItem> FROST_BEAR_SPAWN_EGG = register("frost_bear_spawn_egg",
			SpawnEggItem::new, properties -> properties.spawnEgg(ModEntities.FROST_BEAR.get()));

	// The CreativeModeTabs constants are private in vanilla (both loaders widen them), so name the keys here.
	private static final ResourceKey<CreativeModeTab> SPAWN_EGGS_TAB = vanillaTab("spawn_eggs");
	private static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB = vanillaTab("ingredients");
	private static final ResourceKey<CreativeModeTab> COMBAT_TAB = vanillaTab("combat");

	/** Vanilla creative tabs FrostBear adds items to; see {@link #addToCreativeTab}. */
	public static final List<ResourceKey<CreativeModeTab>> CREATIVE_TABS = List.of(
			SPAWN_EGGS_TAB, INGREDIENTS_TAB, COMBAT_TAB);

	private static ResourceKey<CreativeModeTab> vanillaTab(String name) {
		return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(name));
	}

	private static <T extends Item> RegEntry<Item, T> register(String name, Function<Item.Properties, T> factory,
			UnaryOperator<Item.Properties> properties) {
		return FrostBear.platform().register(BuiltInRegistries.ITEM, name,
				key -> factory.apply(properties.apply(new Item.Properties().setId(key))));
	}

	public static void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Consumer<ItemLike> output) {
		if (tab.equals(SPAWN_EGGS_TAB)) {
			output.accept(FROST_BEAR_SPAWN_EGG.get());
		} else if (tab.equals(INGREDIENTS_TAB)) {
			output.accept(FROST_HEART.get());
			output.accept(GLACIAL_CORE.get());
		} else if (tab.equals(COMBAT_TAB)) {
			output.accept(FROSTFANG.get());
		}
	}

	public static void init() {
		FrostBear.LOGGER.info("[FrostBear] Registering items");
	}
}
