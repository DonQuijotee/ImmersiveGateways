package net.conczin.immersive_gateways;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ItemGroups {
    public static ResourceLocation getIdentifier() {
        return ImmersiveGateways.locate(ImmersiveGateways.MOD_ID + "_tab");
    }

    public static Component getDisplayName() {
        return Component.translatable("itemGroup." + ItemGroups.getIdentifier().toLanguageKey());
    }

    public static ItemStack getIcon() {
        return Items.GATEWAY_CHARGE.get().getDefaultInstance();
    }
}
