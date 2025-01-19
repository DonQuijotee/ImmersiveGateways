package net.conczin.immersive_gateways.data;

import net.conczin.immersive_gateways.ImmersiveGateways;
import net.conczin.immersive_gateways.Utils;
import net.conczin.immersive_gateways.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

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

        // Create a new portal if none is found
        if (portal == null) {
            // Pick random position
            Config c = Config.getInstance();
            float distance = level.random.nextFloat() * (c.maxDistance - c.minDistance) + c.minDistance;
            double angle = level.random.nextFloat() * Math.PI;
            BlockPos target = new BlockPos((int) (pos.getX() + Math.cos(angle) * distance), pos.getY(), (int) (pos.getZ() + Math.sin(angle) * distance));

            // Add portal and initialize search
            portal = new PortalData(target);
            state.add(pos, portal);
            System.out.println("Blank portal created!");
        }

        // Create iterator
        if (!portal.isResolved() && portal.iterator == null) {
            BlockPos target = new BlockPos(portal.x, portal.y, portal.z);
            Config c = Config.getInstance();
            portal.iterator = Utils.getStructureSet(level, ImmersiveGateways.locate("portals"))
                    .map(s -> new Utils.NearestMapStructureIterator(level, s, target, 0, c.maxScanDistanceInChunks, false)).orElse(null);
            System.out.println("Iterator created!");

            if (portal.iterator == null) {
                System.out.println("Iterator not created, giving up...");
                portal.setResolved();
            }
        }

        // If the portal is not yet found, continue search
        if (portal.iterator != null) {
            while (portal.iterator.hasNext()) {
                Utils.SearchResult result = portal.iterator.next(lazy ? Config.getInstance().maxScanningTimePerTickInMS : 0);
                if (result.structure() != null) {
                    if (state.search(result.pos()) != null) {
                        System.out.println("Portal already connected, skipping...");
                        continue;
                    }

                    // Portal found, update its final color and position
                    System.out.println("Portal found!");
                    portal.setColor(getColor(level, result.pos()));
                    portal.setPosition(result.pos());
                    portal.setResolved();
                    state.setDirty();

                    // Also add the other side
                    state.add(result.pos(), new PortalData(pos.getX(), pos.getY(), pos.getZ(), getColor(level, pos), true));
                    break;
                } else if (lazy) {
                    // Time constraint reached, wait until the next tick
                    break;
                }
            }

            // No portals found, give up
            if (portal.iterator != null && !portal.iterator.hasNext()) {
                System.out.println("Portal not found, giving up...");
                portal.setResolved();
            }
        }

        return portal;
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

        private int color;
        private boolean resolved;
        private Utils.NearestMapStructureIterator iterator;

        public PortalData(int x, int y, int z, int color, boolean resolved) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.color = color;
            this.resolved = resolved;
        }

        public PortalData(BlockPos target) {
            this(target.getX(), target.getY(), target.getZ(), 0, false);
        }

        public static PortalData load(CompoundTag nbt) {
            return new PortalData(
                    nbt.getInt("x"),
                    nbt.getInt("y"),
                    nbt.getInt("z"),
                    nbt.getInt("color"),
                    nbt.getBoolean("resolved")
            );
        }

        public CompoundTag save() {
            CompoundTag c = new CompoundTag();
            c.putInt("x", x);
            c.putInt("y", y);
            c.putInt("z", z);
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

        public void setPosition(BlockPos pos) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
    }
}
