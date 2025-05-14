package net.conczin.immersive_gateways.fabric;

import net.conczin.immersive_gateways.*;
import net.fabricmc.api.ModInitializer;

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
        Sounds.bootstrap();
    }
}