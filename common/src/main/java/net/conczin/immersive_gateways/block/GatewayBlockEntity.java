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
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GatewayBlockEntity extends BlockEntity {
    public static final double DISTANCE = 18.0;
    public static final int COOLDOWN = 30;
    public static final float OFFSET = 2.5f;

    public static Vector2f[] offsets = new Vector2f[]{
            new Vector2f(0.25f, 0.25f),
            new Vector2f(0.75f, 0.25f),
            new Vector2f(0.75f, 0.75f),
            new Vector2f(0.25f, 0.75f)
    };

    Vector3f[] offsets1;
    Vector3f[] offsets2;

    Quaternionf[] rotations;

    float[] lastTime = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    float[] time = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    boolean[] state = new boolean[]{false, false, false, false};

    Random random = new Random();

    // The last tick a player was close to the gateway
    static final Map<UUID, Long> lastCloseTick = new ConcurrentHashMap<>();

    private Vector3f[] getOffsets() {
        return getOffsets(new Vector3f[]{
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Vector3f(0.0f, 0.0f, 0.0f),
        });
    }

    private float getRandom() {
        return random.nextFloat() * 2.0f - 1.0f;
    }

    private Vector3f offsetVector(Vector3f offset) {
        return new Vector3f(offset.x() + getRandom() * OFFSET, offset.y() + getRandom() * OFFSET, offset.z() + getRandom() * OFFSET);
    }

    private Vector3f @NotNull [] getOffsets(Vector3f[] offset) {
        return new Vector3f[]{
                offsetVector(offset[0]),
                offsetVector(offset[1]),
                offsetVector(offset[2]),
                offsetVector(offset[3])
        };
    }

    public Quaternionf randomQuaternion() {
        float u1 = random.nextFloat();
        float u2 = random.nextFloat();
        float u3 = random.nextFloat();

        double theta1 = 2.0 * Math.PI * u1;
        double theta2 = 2.0 * Math.PI * u2;
        double s1 = Math.sqrt(1.0 - u3);
        double s2 = Math.sqrt(u3);

        float x = (float) (s1 * Math.sin(theta1));
        float y = (float) (s1 * Math.cos(theta1));
        float z = (float) (s2 * Math.sin(theta2));
        float w = (float) (s2 * Math.cos(theta2));

        return new Quaternionf(x, y, z, w).normalize();
    }

    public GatewayBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityTypes.GATEWAY.get(), pos, blockState);

        rotations = new Quaternionf[]{
                randomQuaternion(),
                randomQuaternion(),
                randomQuaternion(),
                randomQuaternion(),
        };

        offsets1 = getOffsets();
        offsets2 = getOffsets(offsets1);
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
        for (int i = 0; i < 4; i++) {
            // Check if a player is nearby, load distributed
            long time = level.getGameTime();
            if ((time ^ (pos.getX() * 77L) ^ (pos.getY() * 66L) ^ (pos.getZ() * 55L) + (i * 44L)) % COOLDOWN == 0) {
                Vector3d position = blockEntity.getPosition(pos, state, i);
                Player player = level.getNearestPlayer(position.x, position.y, position.z, DISTANCE * 2, false);
                boolean isClose = false;
                if (player != null) {
                    double distance = player.distanceToSqr(position.x, position.y, position.z);
                    if (distance < DISTANCE * DISTANCE) {
                        lastCloseTick.put(player.getUUID(), time);
                    }

                    isClose = lastCloseTick.getOrDefault(player.getUUID(), 0L) + COOLDOWN > time;
                }
                blockEntity.state[i] = isClose;
            }

            // Animate the gateway
            blockEntity.lastTime[i] = blockEntity.time[i];
            if (blockEntity.state[i]) {
                blockEntity.time[i] = Math.min(1.0f, blockEntity.time[i] + 1.0f / COOLDOWN);
            } else {
                blockEntity.time[i] = Math.max(0.0f, blockEntity.time[i] - 1.0f / COOLDOWN);
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