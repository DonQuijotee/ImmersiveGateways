package net.conczin.immersive_gateways;

import com.mojang.datafixers.types.Type;
import net.conczin.immersive_gateways.block.GatewayBlockEntity;
import net.conczin.immersive_gateways.cobalt.Registration;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public interface BlockEntityTypes {
    Supplier<BlockEntityType<GatewayBlockEntity>> GATEWAY = register("gateway", () -> Registration.blockEntityTypeBuilder(GatewayBlockEntity::new, Blocks.GATEWAY.get()));

    static <T extends BlockEntity> Supplier<BlockEntityType<T>> register(String name, Supplier<BlockEntityType.Builder<T>> type) {
        Type<?> datafixerType = Util.fetchChoiceType(References.BLOCK_ENTITY, name);
        return Registration.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ImmersiveGateways.locate(name), () -> type.get().build(datafixerType));
    }

    static void bootstrap() {
    }
}
