package net.conczin.immersive_gateways;

import net.minecraft.resources.ResourceLocation;

public class ImmersiveGateways {
    public static final String MOD_ID = "immersive_gateways";

    public static void init() {
		// nop
    }

    public static ResourceLocation locate(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}
