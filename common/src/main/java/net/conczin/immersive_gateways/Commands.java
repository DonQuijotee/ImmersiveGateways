package net.conczin.immersive_gateways;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class Commands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal(ImmersiveGateways.MOD_ID)
                .then(net.minecraft.commands.Commands.literal("help")
                        .executes(Commands::displayHelp))
                .then(net.minecraft.commands.Commands.literal("summon")
                        .requires(p -> p.hasPermission(2))
                        .then(net.minecraft.commands.Commands.argument("wave", StringArgumentType.string())
                                .executes(c -> 0)
                        )
                )
        );
    }

    private static int displayHelp(CommandContext<CommandSourceStack> context) {
        sendMessage(context, "Debug commands");
        return 0;
    }

    private static void sendMessage(CommandContext<CommandSourceStack> context, String message) {
        sendMessage(context, Component.literal(message));
    }

    private static void sendMessage(CommandContext<CommandSourceStack> context, Component message) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null) {
            player.sendSystemMessage(message);
        }
    }
}
