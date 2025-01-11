package net.conczin.immersive_gateways.item;

import net.conczin.immersive_gateways.Blocks;
import net.conczin.immersive_gateways.block.GatewayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GatewayCharge extends Item {
    public static final String PORTAL_TAG = "Portal";

    public GatewayCharge(Properties properties) {
        super(properties);
    }

    public static boolean isLinked(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(PORTAL_TAG);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);

        if (isLinked(stack)) {
            tooltipComponents.add(Component.translatable("item.immersive_gateways.gateway_charge.linked"));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos realPos = pos.offset(direction.getStepX(), direction.getStepY(), direction.getStepZ());

        BlockState blockState = Blocks.GATEWAY.get().defaultBlockState().setValue(GatewayBlock.AXIS, direction.getAxis());
        level.setBlock(realPos, blockState, 3);

        return super.useOn(context);
    }

    public void getPosition(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(PORTAL_TAG)) {
            CompoundTag portalTag = tag.getCompound(PORTAL_TAG);
        }
    }
}
