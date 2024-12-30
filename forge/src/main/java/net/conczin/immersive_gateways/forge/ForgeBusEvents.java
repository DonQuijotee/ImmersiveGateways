package net.conczin.immersive_gateways.forge;

import net.conczin.immersive_gateways.Commands;
import net.conczin.immersive_gateways.ImmersiveGateways;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveGateways.MOD_ID)
public class ForgeBusEvents {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        Commands.register(event.getDispatcher());
    }
}