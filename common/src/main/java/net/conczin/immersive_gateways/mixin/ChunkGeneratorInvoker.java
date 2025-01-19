package net.conczin.immersive_gateways.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorInvoker {
    @Invoker("getStructureGeneratingAt")
    static Pair<BlockPos, Holder<Structure>> invokeGetStructureGeneratingAt(Set<Holder<Structure>> structureHoldersSet, LevelReader level, StructureManager structureManager, boolean skipKnownStructures, StructurePlacement placement, ChunkPos chunkPos) {
        throw new AssertionError();
    }
}
