package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.cobalt.Registration;
import net.conczin.immersive_gateways.item.GatewayCharge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public interface Items {
    List<Supplier<Item>> items = new LinkedList<>();

    Supplier<Item> GATEWAY_CHARGE = register("gateway_charge", () -> new GatewayCharge(baseProps().stacksTo(1)));
    Supplier<Item> GATEWAY = register("gateway", () -> new BlockItem(Blocks.GATEWAY.get(), baseProps()));

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

    static List<ItemStack> getSortedItems() {
        return items.stream().map(i -> i.get().getDefaultInstance()).toList();
    }
}
