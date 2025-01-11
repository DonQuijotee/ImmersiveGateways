package net.conczin.immersive_gateways.fabric;

import net.conczin.immersive_gateways.BlockEntityTypes;
import net.conczin.immersive_gateways.ImmersiveGatewaysClient;
import net.conczin.immersive_gateways.block.GatewayBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;


public class ImmersiveGatewaysFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ImmersiveGatewaysClient.init();

        BlockEntityRenderers.register(BlockEntityTypes.GATEWAY.get(), GatewayBlockEntityRenderer::new);
    }
}
