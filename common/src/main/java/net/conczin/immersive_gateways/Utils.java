package net.conczin.immersive_gateways;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

public class Utils {
    static Optional<BlockPos> getClosestStructurePosition(ServerLevel world, BlockPos center, ResourceLocation structure, int radius) {
        Registry<Structure> registry = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure feature = registry.get(structure);
        Optional<Holder.Reference<Structure>> entry = registry.getHolder(registry.getId(feature));
        if (entry.isPresent()) {
            HolderSet<Structure> of = HolderSet.direct(entry.get());
            Pair<BlockPos, Holder<Structure>> pair = world.getChunkSource().getGenerator().findNearestMapStructure(world, of, center, radius, false);
            return pair == null ? Optional.empty() : Optional.ofNullable(pair.getFirst());
        } else {
            return Optional.empty();
        }
    }
}
