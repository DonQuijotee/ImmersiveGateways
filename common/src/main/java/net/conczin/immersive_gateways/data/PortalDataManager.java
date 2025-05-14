package net.conczin.immersive_gateways.data;

import net.conczin.immersive_gateways.Blocks;
import net.conczin.immersive_gateways.ImmersiveGateways;
import net.conczin.immersive_gateways.Utils;
import net.conczin.immersive_gateways.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PortalDataManager {
    // The search range, or maximum portal size, in chunks
    private static final int RANGE = 2;

    public static long toLong(int x, int y, int z) {
        return ((long) x << 37) | ((long) y << 27) | z;
    }

    public static PortalDataLookup getState(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PortalDataLookup::load, PortalDataLookup::new, "immersive_gateways");
    }

    public static PortalData search(ServerLevel level, BlockPos pos, boolean lazy) {
        PortalDataLookup state = getState(level);

        // Check if a known portal is nearby
        PortalData portal = state.search(pos);

        // Create a new draft portal if none is found
        if (portal == null) {
            // Pick a random position
            Config c = Config.getInstance();
            float distance = level.random.nextFloat() * (c.maxDistance - c.minDistance) + c.minDistance;
            double angle = level.random.nextFloat() * Math.PI;

            // Add portal and initialize search
            portal = new PortalData(
                    (int) (pos.getX() + Math.cos(angle) * distance),
                    pos.getY(),
                    (int) (pos.getZ() + Math.sin(angle) * distance),
                    Direction.NORTH,
                    0,
                    false
            );
            state.add(pos, portal);
            ImmersiveGateways.LOGGER.info("New draft portal created.");
        }

        // Create iterator
        if (!portal.isResolved() && portal.iterator == null) {
            BlockPos target = new BlockPos(portal.x, portal.y, portal.z);
            Config c = Config.getInstance();
            portal.iterator = Utils.getStructureSet(level, ImmersiveGateways.locate("portals"))
                    .map(s -> new Utils.NearestMapStructureIterator(level, s, target, 0, c.maxScanDistanceInChunks, false))
                    .orElse(null);

            // In modded scenarios, the structures could be missing
            if (portal.iterator == null) {
                ImmersiveGateways.LOGGER.warn("No portal structures found.");
                portal.setResolved();
            }
        }

        // If the portal is not yet found, continue search
        if (portal.iterator != null) {
            while (portal.iterator.hasNext()) {
                Utils.SearchResult result = portal.iterator.next(lazy ? Config.getInstance().maxScanningTimePerTickInMS : 0);
                if (result.structure() != null) {
                    BlockPos adjustedTarget = result.pos();

                    // Skip if the portal is already connected
                    if (state.search(adjustedTarget) != null) {
                        ImmersiveGateways.LOGGER.info("Portal already connected, skipping...");
                        continue;
                    }

                    // Portal found, update its final color and position
                    ImmersiveGateways.LOGGER.info("Portal found at {}", adjustedTarget);
                    portal.setColor(getColor(level, adjustedTarget));
                    portal.setPosition(improvePosition(level, adjustedTarget));
                    portal.setResolved();
                    state.setDirty();

                    // Also add the other side
                    OutroPoint improvedPos = improvePosition(level, pos);
                    state.add(adjustedTarget, new PortalData(
                            improvedPos,
                            getColor(level, pos),
                            true
                    ));
                    break;
                } else if (lazy) {
                    // Time constraint reached, wait until the next tick
                    break;
                }
            }

            // No portals found, give up
            if (portal.iterator != null && !portal.iterator.hasNext()) {
                ImmersiveGateways.LOGGER.warn("No nearby portal not found, giving up...");
                portal.setResolved();
            }
        }

        return portal;
    }

    private static BoundingBox estimateSize(ServerLevel level, BlockPos pos) {
        int minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
        int maxX = pos.getX(), maxY = pos.getY(), maxZ = pos.getZ();

        Set<BlockPos> open = new HashSet<>();
        Set<BlockPos> closed = new HashSet<>();
        open.add(pos);
        closed.add(pos);
        while (!open.isEmpty()) {
            BlockPos current = open.iterator().next();
            open.remove(current);

            // Check if the block is a portal
            if (level.getBlockState(current).is(Blocks.GATEWAY.get())) {
                minX = Math.min(minX, current.getX());
                minY = Math.min(minY, current.getY());
                minZ = Math.min(minZ, current.getZ());
                maxX = Math.max(maxX, current.getX());
                maxY = Math.max(maxY, current.getY());
                maxZ = Math.max(maxZ, current.getZ());

                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos neighbor = current.offset(x, y, z);
                            if (!closed.contains(neighbor)) {
                                open.add(neighbor);
                                closed.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        return new BoundingBox(
                minX, minY, minZ,
                maxX, maxY, maxZ
        );
    }

    public record OutroPoint(BlockPos pos, Direction direction) {
        public OutroPoint(int x, int y, int z, Direction direction) {
            this(new BlockPos(x, y, z), direction);
        }
    }

    private static OutroPoint improvePosition(ServerLevel level, BlockPos pos) {
        // The position is known to be next to a portal, but the height can be anything
        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int maxOffset = Math.max(height - minY, maxY - 1 - height);

        for (int dy = 0; dy <= maxOffset; dy++) {
            int y = height + ((dy % 2 == 0) ? dy / 2 : -(dy / 2 + 1));
            if (y < minY || y >= maxY) continue;
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos improvedPos = new BlockPos(pos.getX() + x, y, pos.getZ() + z);
                    if (level.getBlockState(improvedPos).is(Blocks.GATEWAY.get())) {
                        // Found a portal, now estimate the size of the portal and return a valid position
                        BoundingBox boundingBox = estimateSize(level, improvedPos);
                        if (boundingBox.getXSpan() == 1) {
                            return new OutroPoint(
                                    improvedPos.getX() + 1,
                                    boundingBox.minY(),
                                    (boundingBox.minZ() + boundingBox.maxZ()) / 2,
                                    Direction.EAST
                            );
                        } else if (boundingBox.getYSpan() == 1) {
                            return new OutroPoint(
                                    boundingBox.maxX() + 1,
                                    improvedPos.getY() + 1,
                                    (boundingBox.minZ() + boundingBox.maxZ()) / 2,
                                    Direction.UP
                            );
                        } else {
                            return new OutroPoint(
                                    (boundingBox.minX() + boundingBox.maxX()) / 2,
                                    boundingBox.minY(),
                                    improvedPos.getZ() + 1,
                                    Direction.SOUTH
                            );
                        }
                    }
                }
            }
        }
        return new OutroPoint(pos, Direction.NORTH);
    }

    private static int getColor(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        ResourceLocation resourceLocation = biome.unwrapKey().map(ResourceKey::location).orElse(new ResourceLocation("minecraft:plains"));
        return Config.getInstance().colors.getOrDefault(resourceLocation.toString(), 0x00FF00);
    }

    public static class PortalDataLookup extends SavedData {
        final Map<Long, PortalData> portals = new HashMap<>();

        public static PortalDataLookup load(CompoundTag nbt) {
            PortalDataLookup c = new PortalDataLookup();
            for (String key : nbt.getAllKeys()) {
                c.portals.put(Long.parseLong(key), PortalData.load(nbt.getCompound(key)));
            }
            return c;
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            CompoundTag c = new CompoundTag();
            for (Map.Entry<Long, PortalData> entry : portals.entrySet()) {
                c.put(Long.toString(entry.getKey()), entry.getValue().save());
            }
            return c;
        }

        public void add(BlockPos pos, PortalData data) {
            long id = toLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            if (!portals.containsKey(id)) {
                portals.put(id, data);
                setDirty();
            }
        }

        public PortalData search(BlockPos pos) {
            int cx = pos.getX() >> 4;
            int cy = pos.getY() >> 4;
            int cz = pos.getZ() >> 4;
            for (int x = cx - RANGE; x <= cx + RANGE; x++) {
                for (int y = cy - RANGE; y <= cy + RANGE; y++) {
                    for (int z = cz - RANGE; z <= cz + RANGE; z++) {
                        long id = toLong(x, y, z);
                        if (portals.containsKey(id)) {
                            return portals.get(id);
                        }
                    }
                }
            }
            return null;
        }
    }

    public static final class PortalData {
        private int x;
        private int y;
        private int z;
        private Direction direction;

        private int color;
        private boolean resolved;
        private Utils.NearestMapStructureIterator iterator;

        public PortalData(int x, int y, int z, Direction direction, int color, boolean resolved) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.direction = direction;
            this.color = color;
            this.resolved = resolved;
        }

        public PortalData(OutroPoint improvedPos, int color, boolean resolved) {
            this(improvedPos.pos.getX(), improvedPos.pos.getY(), improvedPos.pos.getZ(), improvedPos.direction, color, resolved);
        }

        public static PortalData load(CompoundTag nbt) {
            return new PortalData(
                    nbt.getInt("x"),
                    nbt.getInt("y"),
                    nbt.getInt("z"),
                    Direction.CODEC.byName(nbt.getString("direction")),
                    nbt.getInt("color"),
                    nbt.getBoolean("resolved")
            );
        }

        public CompoundTag save() {
            CompoundTag c = new CompoundTag();
            c.putInt("x", x);
            c.putInt("y", y);
            c.putInt("z", z);
            c.putString("direction", direction.getName());
            c.putInt("color", color);
            c.putBoolean("resolved", resolved);
            return c;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }

        public Direction direction() {
            return direction;
        }

        public int color() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public void setIterator(Utils.NearestMapStructureIterator iterator) {
            this.iterator = iterator;
        }

        public void setResolved() {
            this.resolved = true;
            setIterator(null);
        }

        public boolean isResolved() {
            return resolved;
        }

        public void setPosition(OutroPoint point) {
            this.x = point.pos.getX();
            this.y = point.pos.getY();
            this.z = point.pos.getZ();
            this.direction = point.direction;
        }
    }
}
