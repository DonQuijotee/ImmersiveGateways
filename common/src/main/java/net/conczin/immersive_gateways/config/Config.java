package net.conczin.immersive_gateways.config;

import net.conczin.immersive_gateways.ImmersiveGateways;

import java.util.Map;

public final class Config extends JsonConfig {
    private static final Config INSTANCE = loadOrCreate(new Config(), Config.class);

    public Config() {
        super(ImmersiveGateways.MOD_ID);
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    @Override
    int getVersion() {
        return 0;
    }

    public int minDistance = 512;
    public int maxDistance = 16384;
    public int maxScanDistanceInChunks = 256;
    public int maxScanningTimePerTickInMS = 2;
    public boolean onlyPlayersCanTeleport = true;

    public Map<String, Integer> colors = Map.ofEntries(
            Map.entry("minecraft:plains", 0x7fb238),
            Map.entry("minecraft:desert", 0xf7e9a3),
            Map.entry("minecraft:forest", 0x599011),
            Map.entry("minecraft:taiga", 0x4d694b),
            Map.entry("minecraft:swamp", 0x6db015),
            Map.entry("minecraft:jungle", 0x337322),
            Map.entry("minecraft:savanna", 0xd87f33),
            Map.entry("minecraft:badlands", 0xba6d2c),
            Map.entry("minecraft:snowy_tundra", 0xa0a0ff),
            Map.entry("minecraft:mountains", 0x4f684e),
            Map.entry("minecraft:beach", 0xf7e9a3),
            Map.entry("minecraft:ocean", 0x4a80ff),
            Map.entry("minecraft:river", 0x5cdbd5),
            Map.entry("minecraft:nether_wastes", 0x8e2020),
            Map.entry("minecraft:the_end", 0x8a8adc),
            Map.entry("minecraft:mushroom_fields", 0x7f3653),
            Map.entry("minecraft:dark_forest", 0x0a1601),
            Map.entry("minecraft:birch_forest", 0x4e6b3b),
            Map.entry("minecraft:snowy_mountains", 0xa0a0ff),
            Map.entry("minecraft:flower_forest", 0xf27fa5),
            Map.entry("minecraft:ice_spikes", 0x8a8adc),
            Map.entry("minecraft:lukewarm_ocean", 0x6699d8),
            Map.entry("minecraft:cold_ocean", 0x5884ba),
            Map.entry("minecraft:deep_ocean", 0x24357d),
            Map.entry("minecraft:eroded_badlands", 0xba6d2c),
            Map.entry("minecraft:wooded_badlands_plateau", 0xba6d2c),
            Map.entry("minecraft:sunflower_plains", 0xfaee4d)
    );
}
