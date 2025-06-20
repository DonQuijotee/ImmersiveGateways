package net.conczin.immersive_gateways.fabric;

import net.conczin.immersive_gateways.BlockEntityTypes;
import net.conczin.immersive_gateways.Client;
import net.conczin.immersive_gateways.block.GatewayBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;


public class ClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Client.init();

        BlockEntityRenderers.register(BlockEntityTypes.GATEWAY, GatewayBlockEntityRenderer::new);
    }
}
