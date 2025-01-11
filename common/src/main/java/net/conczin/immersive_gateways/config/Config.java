package net.conczin.immersive_gateways.config;

import net.conczin.immersive_gateways.ImmersiveGateways;

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
}
