package net.conczin.immersive_gateways.data;

import net.conczin.immersive_gateways.Blocks;
import net.conczin.immersive_gateways.ImmersiveGateways;
import net.conczin.immersive_gateways.Utils;
import net.conczin.immersive_gateways.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class PortalDataManager {
    // The search range, or maximum portal size, in chunks
    private static final int RANGE = 2;

    private static final Utils.TimeBudget budget = new Utils.TimeBudget(Config.getInstance().maxScanningTimePerTickInMS * 1_000_000L);

    public static long toLong(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
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
                Utils.SearchResult result = portal.iterator.next(lazy ? budget : null);
                if (result.structure() != null) {
                    BlockPos candidate = result.pos();

                    // Skip if the portal is already connected
                    if (state.search(candidate) != null) {
                        ImmersiveGateways.LOGGER.info("Portal already connected, skipping...");
                        continue;
                    }

                    // Next phase: remember the candidate and close the iterator
                    portal.setCandidate(candidate);
                    portal.setIterator(null);
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

        // Portal found, once the chunks are loaded, find the exact exit
        if (portal.candidate() != null) {
            // TODO: Check if chunks are loaded, but first try a fully threaded solution

            PortalExit secondPortalExit = findExit(level, portal.candidate());
            ImmersiveGateways.LOGGER.info("Portal found at {}", secondPortalExit.pos());

            // update its final color and position
            portal.setColor(getColor(level, portal.candidate()));
            portal.setPosition(secondPortalExit);
            portal.setResolved();

            // Also add the other side
            PortalExit exit = findExit(level, pos);
            state.add(portal.candidate(), new PortalData(
                    exit,
                    getColor(level, pos),
                    true
            ));
            state.setDirty();
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

    public record PortalExit(BlockPos pos, Direction direction) {
        public PortalExit(int x, int y, int z, Direction direction) {
            this(new BlockPos(x, y, z), direction);
        }
    }

    private static PortalExit findExit(ServerLevel level, BlockPos pos) {
        long time = System.nanoTime();
        List<PortalExit> portalExits = findExits(level, pos);
        long delta = System.nanoTime() - time;
        ImmersiveGateways.LOGGER.info("Exit search took {} ms", delta / 1_000_000);
        for (PortalExit portalExit : portalExits) {
            if (level.getBlockState(portalExit.pos).isAir()) {
                return portalExit;
            }
        }
        return portalExits.get(0);
    }

    private static BlockPos findBlockInArea(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos chunkPos = new BlockPos.MutableBlockPos();
        int range = 2;
        for (int cx = SectionPos.blockToSectionCoord(pos.getX()) - range; cx <= SectionPos.blockToSectionCoord(pos.getX()) + range; cx++) {
            for (int cz = SectionPos.blockToSectionCoord(pos.getZ()) - range; cz <= SectionPos.blockToSectionCoord(pos.getZ()) + range; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int cy = 0; cy < chunk.getSectionsCount(); cy++) {
                    LevelChunkSection section = chunk.getSection(cy);
                    if (section.hasOnlyAir()) continue;
                    if (!section.maybeHas(s -> s.is(Blocks.GATEWAY.get()))) continue;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                chunkPos.set(
                                        SectionPos.sectionToBlockCoord(cx, x),
                                        SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(cy), y),
                                        SectionPos.sectionToBlockCoord(cz, z)
                                );
                                if (chunk.getBlockState(chunkPos).is(Blocks.GATEWAY.get())) {
                                    return chunkPos;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static List<PortalExit> findExits(ServerLevel level, BlockPos pos) {
        BlockPos improvedPos = findBlockInArea(level, pos);

        if (improvedPos == null) {
            ImmersiveGateways.LOGGER.warn("No exit found for portal at {}", pos);
            return List.of(new PortalExit(pos, Direction.NORTH));
        }

        // Found a portal, now estimate the size of the portal and return a valid position
        BoundingBox boundingBox = estimateSize(level, improvedPos);
        if (boundingBox.getYSpan() > 1) {
            return List.of(
                    new PortalExit(
                            boundingBox.minX() - 1,
                            boundingBox.minY(),
                            (boundingBox.minZ() + boundingBox.maxZ()) / 2,
                            Direction.WEST
                    ),
                    new PortalExit(
                            boundingBox.maxX() + 1,
                            boundingBox.minY(),
                            (boundingBox.minZ() + boundingBox.maxZ()) / 2,
                            Direction.EAST
                    ),
                    new PortalExit(
                            (boundingBox.minX() + boundingBox.maxX()) / 2,
                            boundingBox.minY(),
                            boundingBox.minZ() - 1,
                            Direction.NORTH
                    ),
                    new PortalExit(
                            (boundingBox.minX() + boundingBox.maxX()) / 2,
                            boundingBox.minY(),
                            boundingBox.maxZ() + 1,
                            Direction.SOUTH
                    )
            );
        } else {
            return List.of(
                    new PortalExit(
                            boundingBox.maxX() + 1,
                            boundingBox.minY() + 1,
                            (boundingBox.minZ() + boundingBox.maxZ()) / 2,
                            Direction.UP
                    ),
                    new PortalExit(
                            boundingBox.minX() - 1,
                            boundingBox.minY() + 1,
                            (boundingBox.minZ() + boundingBox.maxZ()) / 2,
                            Direction.UP
                    ),
                    new PortalExit(
                            (boundingBox.minX() + boundingBox.maxX()) / 2,
                            boundingBox.minY() + 1,
                            boundingBox.maxZ() + 1,
                            Direction.UP
                    ),
                    new PortalExit(
                            (boundingBox.minX() + boundingBox.maxX()) / 2,
                            boundingBox.minY() + 1,
                            boundingBox.minZ() - 1,
                            Direction.UP
                    )
            );
        }
    }

    private static int getColor(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        ResourceLocation resourceLocation = biome.unwrapKey().map(ResourceKey::location).orElse(new ResourceLocation("minecraft:plains"));
        return Config.getInstance().colors.getOrDefault(resourceLocation.toString(), biome.value().getFoliageColor());
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
            long id = toLong(pos.getX() >> 4, pos.getZ() >> 4);
            if (!portals.containsKey(id)) {
                portals.put(id, data);
                setDirty();
            }
        }

        public PortalData search(BlockPos pos) {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            for (int x = cx - RANGE; x <= cx + RANGE; x++) {
                for (int z = cz - RANGE; z <= cz + RANGE; z++) {
                    long id = toLong(x, z);
                    if (portals.containsKey(id)) {
                        return portals.get(id);
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
        private BlockPos candidate;

        public PortalData(int x, int y, int z, Direction direction, int color, boolean resolved) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.direction = direction;
            this.color = color;
            this.resolved = resolved;
        }

        public PortalData(PortalExit improvedPos, int color, boolean resolved) {
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

        public BlockPos candidate() {
            return candidate;
        }

        public void setCandidate(BlockPos pos) {
            this.candidate = pos;
        }

        public void setPosition(PortalExit point) {
            this.x = point.pos.getX();
            this.y = point.pos.getY();
            this.z = point.pos.getZ();
            this.direction = point.direction;
        }
    }
}
