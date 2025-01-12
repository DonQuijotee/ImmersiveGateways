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
import org.joml.Vector3f;

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

    public static Vector3f calculateQuadraticBezier(Vector3f p0, Vector3f p1, Vector3f p2, float f) {
        float f2 = 1.0f - f;

        Vector3f term1 = new Vector3f(p0).mul(f2 * f2);
        Vector3f term2 = new Vector3f(p1).mul(2.0f * f2 * f);
        Vector3f term3 = new Vector3f(p2).mul(f * f);

        return term1.add(term2).add(term3);
    }
}
