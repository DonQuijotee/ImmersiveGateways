package net.conczin.immersive_gateways.forge;

import net.conczin.immersive_gateways.*;
import net.minecraftforge.fml.common.Mod;

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
    }
}