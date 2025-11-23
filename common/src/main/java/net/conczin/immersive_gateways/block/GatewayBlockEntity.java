package net.conczin.immersive_gateways.block;

import net.conczin.immersive_gateways.BlockEntityTypes;
import net.conczin.immersive_gateways.Sounds;
import net.conczin.immersive_gateways.data.PortalDataManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GatewayBlockEntity extends BlockEntity {
    public static final double DISTANCE = 16.0;
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

    float[] lastTime = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    float[] time = new float[]{0.0f, 0.0f, 0.0f, 0.0f};
    boolean[] state = new boolean[]{false, false, false, false};

    Random random = new Random();

    int color = 0;

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

    private Vector3f[] getOffsets(Vector3f[] offset) {
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
        super(BlockEntityTypes.GATEWAY, pos, blockState);

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

            // Sound
            float threshold = 0.75f;
            if (blockEntity.time[i] > threshold && blockEntity.lastTime[i] <= threshold) {
                playSound(level, pos, Sounds.ASSEMBLE);
            } else if (blockEntity.time[i] <= threshold && blockEntity.lastTime[i] > threshold) {
                playSound(level, pos, Sounds.DISASSEMBLE);
            }
        }
    }

    static Executor executor = Executors.newSingleThreadExecutor();

    public static void serverTick(ServerLevel level, BlockPos pos, @SuppressWarnings("unused") BlockState state, GatewayBlockEntity blockEntity) {
        // If the color is not set yet, lazily search for the second portal
        if (blockEntity.color == 0) {
            blockEntity.color = 1;

            // Run future
            executor.execute(() -> {
                PortalDataManager.PortalPair pair = PortalDataManager.search(level, pos, true);
                blockEntity.setColor(pair.getTarget(pos).color());
                level.getChunkSource().blockChanged(pos);
            });
        }
    }

    public void setColor(int color) {
        this.color = color;
        this.setChanged();
    }

    private static void playSound(Level level, BlockPos pos, SoundEvent sound) {
        float volume = level.random.nextFloat() * 0.1f + 0.1f;
        float pitch = level.random.nextFloat() * 0.4f + 0.8f;
        level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    public static void teleportEntity(ServerLevel level, BlockPos pos, Entity entity) {
        entity.playSound(Sounds.GATEWAY, 1.0f, 1.0f);
        entity.setPortalCooldown();

        // Find exist
        PortalDataManager.PortalPair pair = PortalDataManager.search(level, pos, false);
        PortalDataManager.Portal portal = pair.getTarget(pos);

        if (!portal.resolved()) {
            entity.sendSystemMessage(Component.translatable("immersive_gateways.not_loaded_yet"));
            return;
        }

        // Hide the transition
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
        }

        // Find a safe position to teleport
        BlockPos targetPos = portal.getSafePosition(level, entity);
        double portalCenterX = (portal.boundingBox().maxX() + portal.boundingBox().minX()) / 2.0;
        double portalCenterZ = (portal.boundingBox().maxZ() + portal.boundingBox().minZ()) / 2.0;
        double deltaX = portalCenterX - (targetPos.getX() + 0.5);
        double deltaZ = portalCenterZ - (targetPos.getZ() + 0.5);
        float targetYRot = (float) (Math.toDegrees(Math.atan2(-deltaZ, deltaX)) + 360) % 360;
        targetYRot = Math.round(targetYRot / 90) * 90;
        double targetX = targetPos.getX() + 0.5;
        double targetY = targetPos.getY();
        double targetZ = targetPos.getZ() + 0.5;
        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(level, targetX, targetY, targetZ, targetYRot, serverPlayer.getXRot());
            serverPlayer.setYHeadRot(targetYRot);
            serverPlayer.yHeadRotO = targetYRot;
        } else {
            entity.teleportToWithTicket(targetX, targetY, targetZ);
            entity.setYRot(targetYRot);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (this.level instanceof ClientLevel) {
            color = tag.getInt("Color");
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("Color", IntTag.valueOf(color));
        return tag;
    }

    public int getColor() {
        return color;
    }
}

