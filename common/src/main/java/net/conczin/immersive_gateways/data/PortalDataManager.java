package net.conczin.immersive_gateways.data;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class PortalDataManager {
    public static PortalDataLookup getState(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PortalDataLookup::load, PortalDataLookup::new, "immersive_gateways");
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

        public void search(int x, int y, int z) {

        }

        public void add(int x, int y, int z) {

        }
    }

    public static class PortalData {
        public final int x;
        public final int y;
        public final int z;
        public final int w;
        public final int h;
        public final Direction.Axis axis;

        public PortalData(int x, int y, int z, int w, int h, Direction.Axis axis) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.axis = axis;
        }

        public static PortalData load(CompoundTag nbt) {
            return new PortalData(
                    nbt.getInt("x"),
                    nbt.getInt("y"),
                    nbt.getInt("z"),
                    nbt.getInt("w"),
                    nbt.getInt("h"),
                    Direction.Axis.byName(nbt.getString("axis"))
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
