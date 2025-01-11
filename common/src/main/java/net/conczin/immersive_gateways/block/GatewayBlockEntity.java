package net.conczin.immersive_gateways.block;

import net.conczin.immersive_gateways.BlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.List;

public class GatewayBlockEntity extends BlockEntity {
    public static final double MAX_DISTANCE = 5.0;

    public static Vector2f[] offsets = new Vector2f[]{
            new Vector2f(0.25f, 0.25f),
            new Vector2f(0.75f, 0.25f),
            new Vector2f(0.75f, 0.75f),
            new Vector2f(0.25f, 0.75f)
    };

    public static final Quaternionf[] ROTATIONS = new Quaternionf[]{
            new Quaternionf().rotateY(0.0f),
            new Quaternionf().rotateY((float) (Math.PI / 2.0f)),
            new Quaternionf().rotateY((float) Math.PI),
            new Quaternionf().rotateY((float) (Math.PI * 1.5f)),
            new Quaternionf().rotateX((float) (Math.PI / 2.0f)),
            new Quaternionf().rotateX((float) (Math.PI * 1.5f))
    };

    int[] faces = new int[]{0, 1, 2, 3};

    double[] lastDistance = new double[]{1.0, 1.0, 1.0, 1.0};
    double[] distance = new double[]{1.0, 1.0, 1.0, 1.0};

    Quaternionf[] lastRotations = new Quaternionf[]{
            new Quaternionf(),
            new Quaternionf(),
            new Quaternionf(),
            new Quaternionf()
    };

    Quaternionf[] rotations = new Quaternionf[]{
            new Quaternionf(),
            new Quaternionf(),
            new Quaternionf(),
            new Quaternionf()
    };

    public GatewayBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityTypes.GATEWAY.get(), pos, blockState);
    }

    public Vector3d getPosition(BlockPos pos, BlockState state, int i) {
        Direction.Axis value = state.getValue(GatewayBlock.AXIS);
        return new Vector3d(
                pos.getX() + (value == Direction.Axis.X ? offsets[i].x : value == Direction.Axis.Y ? offsets[i].x : 0.5f),
                pos.getY() + (value == Direction.Axis.X ? offsets[i].y : value == Direction.Axis.Y ? 0.5f : offsets[i].y),
                pos.getZ() + (value == Direction.Axis.X ? 0.5f : value == Direction.Axis.Y ? offsets[i].y : offsets[i].x)
        );
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, GatewayBlockEntity blockEntity) {
        for (int i = 0; i < blockEntity.faces.length; i++) {
            Vector3d position = blockEntity.getPosition(pos, state, i);
            Quaternionf target = ROTATIONS[blockEntity.faces[i]];

            blockEntity.lastDistance[i] = blockEntity.distance[i];
            blockEntity.lastRotations[i].set(blockEntity.rotations[i]);

            // If a player is nearby, rotate towards them
            Player player = level.getNearestPlayer(position.x, position.y, position.z, MAX_DISTANCE, false);
            if (player != null) {
                blockEntity.distance[i] = Math.sqrt(player.distanceToSqr(position.x, position.y, position.z)) / MAX_DISTANCE;
                float f = (float) Math.min(1.0f, 2.0f - blockEntity.distance[i]);

                double dx = player.getX() - position.x;
                double dy = player.getEyeY() - position.y;
                double dz = player.getZ() - position.z;
                double yaw = Math.atan2(dz, dx);
                double pitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));

                target = new Quaternionf(target);
                target.rotateLocalZ((float) pitch * f);
                target.rotateLocalY((float) -yaw * f);
            } else {
                blockEntity.distance[i] = 1.0;
            }

            // Rotate towards the target
            blockEntity.rotations[i].slerp(target, 0.2f);

            // Randomly rotate the cubes
            if (level.random.nextInt(100) == 0) {
                blockEntity.faces[i] = level.random.nextInt(6);
            }
        }
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GatewayBlockEntity blockEntity) {
        List<Entity> list = level.getEntitiesOfClass(Entity.class, new AABB(pos), GatewayBlockEntity::canEntityTeleport);
        if (!list.isEmpty()) {
            teleportEntity(level, pos, state, list.get(level.random.nextInt(list.size())), blockEntity);
        }
    }

    public static boolean canEntityTeleport(Entity entity) {
        return EntitySelector.NO_SPECTATORS.test(entity) && !entity.getRootVehicle().isOnPortalCooldown();
    }

    public static void teleportEntity(Level level, BlockPos pos, BlockState state, Entity entity, GatewayBlockEntity blockEntity) {
        // TODO: Maybe moving that to the block collision makes more sense?
    }
}