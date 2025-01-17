package net.conczin.immersive_gateways.forge;

import net.conczin.immersive_gateways.*;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import static net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB;

@Mod(ImmersiveGateways.MOD_ID)
public class ImmersiveGatewaysForge {
    static {
        new RegistrationImpl();
    }

    public ImmersiveGatewaysForge() {
        ImmersiveGateways.init();

        Items.bootstrap();
        Blocks.bootstrap();
        BlockEntityTypes.bootstrap();
        Sounds.bootstrap();

        DEF_REG.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    public static final DeferredRegister<CreativeModeTab> DEF_REG = DeferredRegister.create(CREATIVE_MODE_TAB, ImmersiveGateways.MOD_ID);

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> TAB = DEF_REG.register(ImmersiveGateways.MOD_ID, () -> CreativeModeTab.builder()
            .title(ItemGroups.getDisplayName())
            .icon(ItemGroups::getIcon)
            .displayItems((featureFlags, output) -> output.acceptAll(Items.getSortedItems()))
            .build()
    );
}