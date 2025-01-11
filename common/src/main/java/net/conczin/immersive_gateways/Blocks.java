package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.block.GatewayBlock;
import net.conczin.immersive_gateways.cobalt.Registration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Supplier;

public interface Blocks {
    Supplier<Block> GATEWAY = register("gateway", () -> new GatewayBlock(baseProps().mapColor(MapColor.COLOR_BLACK).noCollission().strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.GLASS).lightLevel((blockStatex) -> 11).pushReaction(PushReaction.BLOCK)));

    static Supplier<Block> register(String name, Supplier<Block> block) {
        return Registration.register(BuiltInRegistries.BLOCK, ImmersiveGateways.locate(name), block);
    }

    static void bootstrap() {
    }

    static BlockBehaviour.Properties baseProps() {
        return BlockBehaviour.Properties.of();
    }
}
