package net.conczin.immersive_gateways;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.conczin.immersive_gateways.mixin.ChunkGeneratorInvoker;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.joml.Vector3f;

import java.util.*;

public class Utils {
    public record SearchResult(BlockPos pos, Holder<Structure> structure) {
    }

    public static Optional<HolderSet.Named<Structure>> getStructureSet(ServerLevel level, ResourceLocation structures) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return registry.getTag(TagKey.create(Registries.STRUCTURE, structures));
    }

    public static class TimeBudget {
        private final long budget;
        private long currentBudget;
        private long lastTick;
        private double totalTime;
        private long totalCount;

        public TimeBudget(long budget) {
            this.budget = budget;
            this.currentBudget = budget;
        }

        public boolean isTimeLeft(ServerLevel level) {
            long time = level.getGameTime();
            if (time != lastTick) {
                lastTick = time;
                currentBudget = budget;
            }
            return currentBudget > 0;
        }

        public void consume(long time) {
            currentBudget -= time;
            totalTime += time;
            totalCount++;

            if (totalCount % 1000 == 0) {
                ImmersiveGateways.LOGGER.info("Scanned {} chunks, with {} ms per chunk.", totalCount, getAverageTime());
            }
        }

        public double getAverageTime() {
            return totalCount > 0 ? totalTime / totalCount / 1_000_000.0 : 0;
        }
    }

    public static class NearestMapStructureIterator {
        private final ServerLevel level;
        private final boolean skipKnownStructures;
        private final Map<RandomSpreadStructurePlacement, Set<Holder<Structure>>> placements;
        private final Iterator<ChunkPos> chunkPosIterator;

        public NearestMapStructureIterator(ServerLevel level, HolderSet<Structure> structure, BlockPos pos, int minSize, int maxSize, boolean skipKnownStructures) {
            this.level = level;
            this.skipKnownStructures = skipKnownStructures;

            // Find all placements for the structure
            ChunkGeneratorStructureState generatorState = level.getChunkSource().getGeneratorState();
            this.placements = new Object2ObjectArrayMap<>();
            for (Holder<Structure> holder : structure) {
                for (StructurePlacement placement : generatorState.getPlacementsForStructure(holder)) {
                    if (placement instanceof RandomSpreadStructurePlacement randomSpreadStructurePlacement) {
                        this.placements.computeIfAbsent(randomSpreadStructurePlacement, p -> new ObjectArraySet<>()).add(holder);
                    }
                }
            }

            // Create chunk position iterator
            if (this.placements.isEmpty()) {
                this.chunkPosIterator = Collections.emptyIterator();
            } else {
                int x = SectionPos.blockToSectionCoord(pos.getX());
                int y = SectionPos.blockToSectionCoord(pos.getZ());
                this.chunkPosIterator = generateOutwardPositions(x, y, minSize, maxSize).iterator();
            }
        }

        public boolean hasNext() {
            return chunkPosIterator.hasNext();
        }

        public SearchResult next(TimeBudget budget) {
            while (hasNext()) {
                // Timeout
                if (budget != null && !budget.isTimeLeft(level)) {
                    return new SearchResult(null, null);
                }

                long time = System.nanoTime();
                ChunkPos position = chunkPosIterator.next();
                StructureManager structureManager = level.structureManager();
                for (Map.Entry<RandomSpreadStructurePlacement, Set<Holder<Structure>>> entry : placements.entrySet()) {
                    Pair<BlockPos, Holder<Structure>> pair = ChunkGeneratorInvoker.invokeGetStructureGeneratingAt(entry.getValue(), level, structureManager, skipKnownStructures, entry.getKey(), position);
                    if (pair != null) {
                        if (budget != null) budget.consume(System.nanoTime() - time);
                        return new SearchResult(pair.getFirst(), pair.getSecond());
                    }
                }
                if (budget != null) budget.consume(System.nanoTime() - time);
            }

            return new SearchResult(null, null);
        }
    }

    private static Iterable<ChunkPos> generateOutwardPositions(int centerX, int centerY, int minSize, int maxSize) {
        return () -> new Iterator<>() {
            private int x = minSize;
            private int y = -minSize;
            private int dx = 0;
            private int dy = -1;
            private int layer = 0;
            private int steps = 0;

            @Override
            public boolean hasNext() {
                return Math.abs(x) <= maxSize && Math.abs(y) <= maxSize;
            }

            @Override
            public ChunkPos next() {
                if (!hasNext()) throw new NoSuchElementException();

                ChunkPos position = new ChunkPos(x + centerX, y + centerY);

                if (steps++ == layer) {
                    steps = 0;
                    layer++;

                    int temp = dx;
                    dx = -dy;
                    dy = temp;
                }

                x += dx;
                y += dy;

                return position;
            }
        };
    }

    public static Vector3f calculateQuadraticBezier(Vector3f p0, Vector3f p1, Vector3f p2, float f) {
        float f2 = 1.0f - f;

        Vector3f term1 = new Vector3f(p0).mul(f2 * f2);
        Vector3f term2 = new Vector3f(p1).mul(2.0f * f2 * f);
        Vector3f term3 = new Vector3f(p2).mul(f * f);

        return term1.add(term2).add(term3);
    }
}
