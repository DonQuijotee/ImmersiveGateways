package net.conczin.immersive_gateways.data;

import net.conczin.immersive_gateways.Utils;
import net.conczin.immersive_gateways.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class PortalDataManager {
    // The search range, or maximum portal size, in chunks
    private static final int RANGE = 2;

    public static long toLong(int x, int y, int z) {
        return ((long) x << 37) | ((long) y << 27) | z;
    }

    public static PortalDataLookup getState(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PortalDataLookup::load, PortalDataLookup::new, "immersive_gateways");
    }

    public static PortalData search(ServerLevel level, BlockPos pos) {
        PortalDataLookup state = getState(level);

        // Check if a known portal is nearby
        PortalData portal = state.search(pos);

        // If not portal is found, search for a connection
        if (portal == null) {
            ResourceLocation structure = new ResourceLocation("minecraft:desert_pyramid");

            // TODO: Replace with spiral and accessor into getStructureAt, filter chunks before searching
            // Sample at random positions in the world
            BlockPos position = null;
            for (int attempt = 0; attempt < 32; attempt++) {
                float distanceFactor = 1.0f + attempt / 10.0f;
                float distance = level.random.nextFloat() * (Config.getInstance().maxDistance - Config.getInstance().minDistance) * distanceFactor + Config.getInstance().minDistance;
                double angle = level.random.nextFloat() * Math.PI;
                BlockPos target = new BlockPos((int) (pos.getX() + Math.cos(angle) * distance), pos.getY(), (int) (pos.getZ() + Math.sin(angle) * distance));
                position = Utils.getClosestStructurePosition(level, target, structure, 24).orElse(target);

                // If this portal is unknown, pick it
                if (state.search(pos) == null) break;
            }

            // TODO: This here also only works if we get the structure
            Biome biome = level.getBiome(position).value();

            portal = new PortalData(position.getX(), position.getY(), position.getZ());
        }

        // Also mark
        state.add(pos, portal);

        return portal;
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

    public record PortalData(int x, int y, int z) {
        public static PortalData load(CompoundTag nbt) {
            return new PortalData(
                    nbt.getInt("x"),
                    nbt.getInt("y"),
                    nbt.getInt("z")
            );
        }

        public CompoundTag save() {
            CompoundTag c = new CompoundTag();
            c.putInt("x", x);
            c.putInt("y", y);
            c.putInt("z", z);
            return c;
        }
    }
}
