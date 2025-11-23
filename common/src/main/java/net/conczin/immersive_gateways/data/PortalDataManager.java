package net.conczin.immersive_gateways.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.conczin.immersive_gateways.Blocks;
import net.conczin.immersive_gateways.Common;
import net.conczin.immersive_gateways.Utils;
import net.conczin.immersive_gateways.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Manages portal data storage and searching.
 * A portal is a pair of portals, each defined as a bounding box.
 */
public class PortalDataManager {
    public static long toLong(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static PortalDataLookup getState(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PortalDataLookup::load, PortalDataLookup::new, "immersive_gateways");
    }

    /**
     * Searches for a portal destination at the given position, or creates a new one if none is found.
     */
    public static PortalPair search(ServerLevel level, BlockPos pos, boolean resolve) {
        PortalDataLookup state = getState(level);

        // Check if a known portal is nearby
        PortalPair portal = state.search(pos);

        // Create a new draft portal if none is found
        if (portal == null) {
            // Pick a random position
            Config c = Config.getInstance();
            float distance = level.random.nextFloat() * (c.maxDistance - c.minDistance) + c.minDistance;
            double angle = level.random.nextFloat() * Math.PI;

            BlockPos target = new BlockPos(
                    (int) (pos.getX() + Math.cos(angle) * distance),
                    pos.getY(),
                    (int) (pos.getZ() + Math.sin(angle) * distance)
            );

            // Add portal and initialize search
            portal = new PortalPair(
                    new Portal(findPortalBoundingBox(level, pos), getColor(level, pos), true),
                    new Portal(BoundingBox.fromCorners(target, target), -1, false)
            );
            state.add(portal);
            Common.LOGGER.info("New draft portal created.");
        }

        // Resolve it
        if (resolve && !portal.second.resolved()) {
            portal.second = resolve(state, portal.second, level);
            state.add(portal);
        }

        return portal;
    }

    /**
     * Looks for the exit position of the portal.
     */
    private static synchronized Portal resolve(PortalDataLookup state, Portal portal, ServerLevel level) {
        // Create an iterator over all nearby portal structures
        BlockPos target = portal.boundingBox().getCenter();
        Config c = Config.getInstance();
        Utils.NearestMapStructureIterator structures = Utils.getStructureSet(level, Common.locate("portal"))
                .map(s -> new Utils.NearestMapStructureIterator(level, s, target, 0, c.maxScanDistanceInChunks, false))
                .orElse(null);

        // In modded scenarios, the structures could be missing
        if (structures == null) {
            Common.LOGGER.warn("No portal structures found.");
            return portal.resolve();
        }

        // Iterate over all structures, skip connected ones
        BlockPos candidate = null;
        while (structures.hasNext()) {
            Utils.SearchResult result = structures.next();

            // Skip if the portal is already connected
            if (state.search(result.pos()) == null) {
                candidate = result.pos();
                break;
            }
        }

        // No portals found, give up
        if (candidate == null) {
            Common.LOGGER.warn("No nearby portal not found, giving up...");
            return portal.resolve();
        }

        // Find the exact portal dimensions
        BoundingBox boundingBox = findPortalBoundingBox(level, candidate);

        // Return resolved portal
        return new Portal(
                boundingBox,
                getColor(level, candidate),
                true
        );
    }

    /**
     * Estimates the bounding box of the portal by searching for connected blocks.
     */
    private static BoundingBox estimateBoundingBox(ServerLevel level, BlockPos pos) {
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
            if (level.getBlockState(current).is(Blocks.GATEWAY)) {
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

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Finds the exact portal bounding box.
     */
    private static BoundingBox findPortalBoundingBox(ServerLevel level, BlockPos pos) {
        long time = System.nanoTime();
        BlockPos improvedPos = findBlockInArea(level, pos);

        if (improvedPos == null) {
            Common.LOGGER.warn("Failed to find gateway block near {}", pos);
            return BoundingBox.fromCorners(pos, pos);
        }

        // Found a portal, now estimate the size of the portal
        BoundingBox boundingBox = estimateBoundingBox(level, improvedPos);

        long delta = System.nanoTime() - time;
        Common.LOGGER.info("Exit search took {} ms", delta / 1_000_000);

        return boundingBox;
    }

    /**
     * Finds a gateway block in the area around the given position.
     * Tries to optimize the search.
     */
    private static BlockPos findBlockInArea(ServerLevel level, BlockPos pos) {
        // TODO: Isn't the structure block next to it?
        BlockPos.MutableBlockPos chunkPos = new BlockPos.MutableBlockPos();
        int range = 2;
        for (int cx = SectionPos.blockToSectionCoord(pos.getX()) - range; cx <= SectionPos.blockToSectionCoord(pos.getX()) + range; cx++) {
            for (int cz = SectionPos.blockToSectionCoord(pos.getZ()) - range; cz <= SectionPos.blockToSectionCoord(pos.getZ()) + range; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int cy = 0; cy < chunk.getSectionsCount(); cy++) {
                    LevelChunkSection section = chunk.getSection(cy);
                    if (section.hasOnlyAir()) continue;
                    if (!section.maybeHas(s -> s.is(Blocks.GATEWAY))) continue;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                chunkPos.set(
                                        SectionPos.sectionToBlockCoord(cx, x),
                                        SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(cy), y),
                                        SectionPos.sectionToBlockCoord(cz, z)
                                );
                                if (chunk.getBlockState(chunkPos).is(Blocks.GATEWAY)) {
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

    private static int getColor(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        ResourceLocation resourceLocation = biome.unwrapKey().map(ResourceKey::location).orElse(new ResourceLocation("minecraft:plains"));
        return Config.getInstance().colors.getOrDefault(resourceLocation.toString(), biome.value().getFoliageColor());
    }

    public static class PortalDataLookup extends SavedData {
        final Set<PortalPair> portals = new HashSet<>();
        final Map<Long, Set<PortalPair>> lookup = new HashMap<>();

        public static PortalDataLookup load(CompoundTag nbt) {
            PortalDataLookup c = new PortalDataLookup();
            for (String key : nbt.getAllKeys()) {
                PortalPair pair = PortalPair.load(nbt.get(key));
                c.portals.add(pair);
                c.populateLookup(pair);
            }
            return c;
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            int index = 0;
            for (PortalPair pair : portals) {
                nbt.put(String.valueOf(index), pair.save());
                index++;
            }
            return nbt;
        }

        public void add(PortalPair data) {
            portals.add(data);
            populateLookup(data);
            setDirty();
        }

        private void populateLookup(PortalPair data) {
            populateLookup(data, data.first);
            populateLookup(data, data.second);
        }

        private void populateLookup(PortalPair data, Portal portal) {
            int minX = portal.boundingBox().minX() >> 4;
            int minZ = portal.boundingBox().minZ() >> 4;
            int maxX = portal.boundingBox().maxX() >> 4;
            int maxZ = portal.boundingBox().maxZ() >> 4;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    long cellId = toLong(x, z);
                    lookup.computeIfAbsent(cellId, k -> new HashSet<>()).add(data);
                }
            }
        }

        public PortalPair search(BlockPos pos) {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            long cellId = toLong(cx, cz);
            Set<PortalPair> candidates = lookup.get(cellId);
            if (candidates != null) {
                for (PortalPair candidate : candidates) {
                    if (candidate.first.boundingBox().isInside(pos) || candidate.second.boundingBox().isInside(pos)) {
                        return candidate;
                    }
                }
            }
            return null;
        }
    }

    public record Portal(BoundingBox boundingBox, int color, boolean resolved) {
        public static final Codec<Portal> CODEC = RecordCodecBuilder.create((portal)
                -> portal.group(
                BoundingBox.CODEC.fieldOf("boundingBox").forGetter(Portal::boundingBox),
                Codec.INT.fieldOf("color").forGetter(Portal::color),
                Codec.BOOL.fieldOf("resolved").forGetter(Portal::resolved)
        ).apply(portal, Portal::new));

        public Portal resolve() {
            return new Portal(boundingBox, color, true);
        }

        public BlockPos getSafePosition(ServerLevel level, Entity entity) {
            List<BlockPos> candidates = new LinkedList<>();

            if (boundingBox.getZSpan() > 1) {
                int z = (boundingBox.minZ() + boundingBox.maxZ()) / 2;
                candidates.add(new BlockPos(boundingBox.minX() - 1, boundingBox.minY(), z));
                candidates.add(new BlockPos(boundingBox.maxX() + 1, boundingBox.minY(), z));
            }

            if (boundingBox.getXSpan() > 1) {
                int x = (boundingBox.minX() + boundingBox.maxX()) / 2;
                candidates.add(new BlockPos(x, boundingBox.minY(), boundingBox.minZ() - 1));
                candidates.add(new BlockPos(x, boundingBox.minY(), boundingBox.maxZ() + 1));
            }

            for (int y = boundingBox.minY(); y <= level.getMaxBuildHeight(); y++) {
                for (BlockPos candidate : candidates) {
                    BlockPos pos = new BlockPos(candidate.getX(), y, candidate.getZ());
                    if (entity.level().noCollision(entity)) {
                        return pos;
                    }
                }
            }

            return boundingBox.getCenter();
        }
    }

    public static final class PortalPair {
        public static final Codec<PortalPair> CODEC = RecordCodecBuilder.create((pair)
                -> pair.group(
                Portal.CODEC.fieldOf("first").forGetter(PortalPair::first),
                Portal.CODEC.fieldOf("second").forGetter(PortalPair::second)
        ).apply(pair, PortalPair::new));

        public Portal first;
        public Portal second;

        public PortalPair(Portal first, Portal second) {
            this.first = first;
            this.second = second;
        }

        public static PortalPair load(Tag nbt) {
            return CODEC.parse(NbtOps.INSTANCE, nbt).resultOrPartial(Common.LOGGER::error).orElseThrow();
        }

        public Tag save() {
            return CODEC.encodeStart(NbtOps.INSTANCE, this).resultOrPartial(Common.LOGGER::error).orElseThrow();
        }

        public Portal first() {
            return first;
        }

        public Portal second() {
            return second;
        }

        public Portal getTarget(BlockPos pos) {
            double dist1 = first.boundingBox.getCenter().distSqr(pos);
            double dist2 = second.boundingBox.getCenter().distSqr(pos);
            return dist1 < dist2 ? second : first;
        }
    }
}
