package net.conczin.immersive_gateways.forge;

import net.conczin.immersive_gateways.BlockEntityTypes;
import net.conczin.immersive_gateways.Client;
import net.conczin.immersive_gateways.Common;
import net.conczin.immersive_gateways.block.GatewayBlockEntityRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Common.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientForge {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        Client.init();
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityTypes.GATEWAY, GatewayBlockEntityRenderer::new);
    }
}
