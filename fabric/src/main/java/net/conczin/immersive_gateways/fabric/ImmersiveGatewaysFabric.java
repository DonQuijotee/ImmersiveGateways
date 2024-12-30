package net.conczin.immersive_gateways.fabric;

import net.conczin.immersive_gateways.ImmersiveGateways;
import net.fabricmc.api.ModInitializer;

public class ImmersiveGatewaysFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ImmersiveGateways.init();
    }
}