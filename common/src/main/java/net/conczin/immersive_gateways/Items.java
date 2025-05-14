package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.cobalt.Registration;
import net.conczin.immersive_gateways.item.GatewayItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public interface Items {
    List<Supplier<Item>> items = new LinkedList<>();

    Supplier<Item> GATEWAY = register("gateway", () -> new GatewayItem(baseProps()));

    static Supplier<Item> register(String name, Supplier<Item> item) {
        Supplier<Item> register = Registration.register(BuiltInRegistries.ITEM, ImmersiveGateways.locate(name), item);
        items.add(register);
        return register;
    }

    static void bootstrap() {
    }

    static Item.Properties baseProps() {
        return new Item.Properties();
    }
}
