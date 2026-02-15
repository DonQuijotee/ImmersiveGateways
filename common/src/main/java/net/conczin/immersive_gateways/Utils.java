package net.conczin.immersive_gateways;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Vector3f;

import java.util.stream.Stream;

public class Utils {
    public static Vector3f calculateQuadraticBezier(Vector3f p0, Vector3f p1, Vector3f p2, float f) {
        float f2 = 1.0f - f;

        Vector3f term1 = new Vector3f(p0).mul(f2 * f2);
        Vector3f term2 = new Vector3f(p1).mul(2.0f * f2 * f);
        Vector3f term3 = new Vector3f(p2).mul(f * f);

        return term1.add(term2).add(term3);
    }

    public static Stream<ChunkPos> getChunksInBoundingBox(BoundingBox box) {
        ChunkPos fromPos = new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
        ChunkPos toPos = new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
        return ChunkPos.rangeClosed(fromPos, toPos);
    }
}
