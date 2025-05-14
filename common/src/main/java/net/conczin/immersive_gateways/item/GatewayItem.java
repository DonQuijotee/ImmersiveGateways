package net.conczin.immersive_gateways.item;

import net.conczin.immersive_gateways.Blocks;
import net.conczin.immersive_gateways.block.GatewayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GatewayItem extends Item {
    public GatewayItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        Direction.Axis axis = context.getPlayer() != null && direction.getAxis().isVertical() ? Direction.fromYRot(context.getPlayer().getYRot() + 90.0).getAxis() : Direction.Axis.Y;

        for (int i = 0; i < 5; i++) {
            pos = pos.offset(direction.getNormal());
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                BlockState blockState = Blocks.GATEWAY.get().defaultBlockState().setValue(GatewayBlock.AXIS, axis);
                level.setBlock(pos, blockState, 3);
            }
        }

        return InteractionResult.CONSUME;
    }
}
