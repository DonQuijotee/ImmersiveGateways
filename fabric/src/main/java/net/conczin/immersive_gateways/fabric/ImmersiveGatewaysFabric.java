package net.conczin.immersive_gateways.fabric;

import net.conczin.immersive_gateways.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;

public class ImmersiveGatewaysFabric implements ModInitializer {
    static {
        new RegistrationImpl();
    }

    @Override
    public void onInitialize() {
        ImmersiveGateways.init();

        Items.bootstrap();
        Blocks.bootstrap();
        BlockEntityTypes.bootstrap();

        CreativeModeTab group = FabricItemGroup.builder()
                .title(ItemGroups.getDisplayName())
                .icon(ItemGroups::getIcon)
                .displayItems((enabledFeatures, entries) -> entries.acceptAll(Items.getSortedItems()))
                .build();

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ImmersiveGateways.locate("group"), group);
    }
}