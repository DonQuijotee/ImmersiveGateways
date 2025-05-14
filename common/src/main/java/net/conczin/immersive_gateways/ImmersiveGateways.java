package net.conczin.immersive_gateways;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ImmersiveGateways {
    public static final String MOD_ID = "immersive_gateways";
    public static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        // nop
    }

    public static ResourceLocation locate(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}
