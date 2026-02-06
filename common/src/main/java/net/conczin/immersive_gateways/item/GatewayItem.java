package net.conczin.immersive_gateways.item;

import net.conczin.immersive_gateways.Blocks;
import net.conczin.immersive_gateways.block.GatewayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashSet;
import java.util.Set;

import static net.minecraft.world.level.block.Blocks.CAVE_AIR;

public class GatewayItem extends Item {
    public GatewayItem(Properties properties) {
        super(properties);
    }

    private BoundingBox getBoundingBox(Level level, BlockPos pos) {
        Set<BlockPos> done = new HashSet<>();
        Set<BlockPos> todo = new HashSet<>();
        todo.add(pos);

        int minX = pos.getX();
        int minY = pos.getY();
        int minZ = pos.getZ();
        int maxX = pos.getX();
        int maxY = pos.getY();
        int maxZ = pos.getZ();

        while (!todo.isEmpty()) {
            BlockPos current = todo.iterator().next();
            todo.remove(current);
            done.add(current);

            if (!level.getBlockState(current).isAir()) {
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction);
                    if (!done.contains(neighbor)) {
                        done.add(neighbor);
                        todo.add(neighbor);
                    }
                }

                minX = Math.min(minX, current.getX());
                minY = Math.min(minY, current.getY());
                minZ = Math.min(minZ, current.getZ());
                maxX = Math.max(maxX, current.getX());
                maxY = Math.max(maxY, current.getY());
                maxZ = Math.max(maxZ, current.getZ());
            }
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void fillCaveAir(Level level, BlockPos pos) {
        Set<BlockPos> done = new HashSet<>();
        Set<BlockPos> todo = new HashSet<>();
        Set<BlockPos> toReplace = new HashSet<>();
        todo.add(pos);

        while (!todo.isEmpty()) {
            BlockPos current = todo.iterator().next();
            todo.remove(current);
            done.add(current);

            if (level.getBlockState(current).isAir()) {
                toReplace.add(current);
                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction);
                    if (neighbor.getY() <= pos.getY() && !done.contains(neighbor)) {
                        done.add(neighbor);
                        todo.add(neighbor);
                    }
                }
            }

            if (toReplace.size() > 10000) {
                return;
            }
        }

        for (BlockPos p : toReplace) {
            level.setBlock(p, CAVE_AIR.defaultBlockState(), 3);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        Direction.Axis axis = context.getPlayer() != null && direction.getAxis().isVertical() ? Direction.fromYRot(context.getPlayer().getYRot() + 90.0).getAxis() : Direction.Axis.Y;

        // If pressing on a structure block, autodetect the structure
        if (level.getBlockEntity(pos) instanceof StructureBlockEntity structure) {
            structure.setMode(StructureMode.SAVE);
            BoundingBox boundingBox = getBoundingBox(level, pos);
            structure.setStructurePos(new BlockPos(
                    boundingBox.minX() - pos.getX(),
                    boundingBox.minY() - pos.getY(),
                    boundingBox.minZ() - pos.getZ()
            ));
            structure.setStructureSize(new Vec3i(
                    boundingBox.maxX() - boundingBox.minX() + 1,
                    boundingBox.maxY() - boundingBox.minY() + 1,
                    boundingBox.maxZ() - boundingBox.minZ() + 1
            ));
            structure.saveStructure();
            return InteractionResult.CONSUME;
        }

        // If in offhand, fill everything beyond your level with cave air
        if (context.getPlayer().getOffhandItem() == context.getItemInHand()) {
            if (!context.getLevel().isClientSide) {
                fillCaveAir(level, pos.offset(0, 1, 0));
                context.getPlayer().sendSystemMessage(Component.literal("Filled everything below with cave air!"));
            }
            return InteractionResult.CONSUME;
        }

        // Otherwise place gateways in the chosen direction
        for (int i = 0; i < 8; i++) {
            pos = pos.offset(direction.getNormal());
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                BlockState blockState = Blocks.GATEWAY.defaultBlockState().setValue(GatewayBlock.AXIS, axis);
                level.setBlock(pos, blockState, 3);
            } else {
                break;
            }
        }

        return InteractionResult.CONSUME;
    }
}
