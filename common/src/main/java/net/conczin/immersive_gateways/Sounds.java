package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.cobalt.Registration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public interface Sounds {
    Supplier<SoundEvent> ASSEMBLE = register("assemble");
    Supplier<SoundEvent> DISASSEMBLE = register("disassemble");
    Supplier<SoundEvent> GATEWAY = register("gateway");

    static void bootstrap() {
        // nop
    }

    static Supplier<SoundEvent> register(String name) {
        ResourceLocation id = ImmersiveGateways.locate(name);
        return Registration.register(BuiltInRegistries.SOUND_EVENT, id, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
