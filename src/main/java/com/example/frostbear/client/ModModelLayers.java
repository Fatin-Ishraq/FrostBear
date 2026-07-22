package com.example.frostbear.client;

import com.example.frostbear.FrostBear;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class ModModelLayers {
	private ModModelLayers() {}

	public static final ModelLayerLocation FROST_BEAR =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(FrostBear.MOD_ID, "frost_bear"), "main");
}
