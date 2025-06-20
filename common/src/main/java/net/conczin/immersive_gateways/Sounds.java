package net.conczin.immersive_gateways;

import net.minecraft.sounds.SoundEvent;

public interface Sounds {
    SoundEvent ASSEMBLE = register("assemble");
    SoundEvent DISASSEMBLE = register("disassemble");
    SoundEvent GATEWAY = register("gateway");

    private static SoundEvent register(String id) {
        return SoundEvent.createVariableRangeEvent(Common.locate(id));
    }

    static void registerSounds(Common.RegisterHelper<SoundEvent> helper) {
        helper.register(ASSEMBLE.getLocation(), ASSEMBLE);
        helper.register(DISASSEMBLE.getLocation(), DISASSEMBLE);
        helper.register(GATEWAY.getLocation(), GATEWAY);
    }
}
