package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.block.GatewayBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public interface Blocks {
    Block GATEWAY = new GatewayBlock(baseProps().mapColor(MapColor.COLOR_BLACK)
            .strength(-1.0F, 3600000.0F)
            .noLootTable()
            .sound(SoundType.GLASS)
            .lightLevel((blockStatex) -> 11)
            .pushReaction(PushReaction.BLOCK));


    static BlockBehaviour.Properties baseProps() {
        return BlockBehaviour.Properties.of();
    }

    static void registerBlocks(Common.RegisterHelper<Block> helper) {
        helper.register(Common.locate("gateway"), GATEWAY);
    }
}
