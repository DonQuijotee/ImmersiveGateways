package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.item.GatewayItem;
import net.minecraft.world.item.Item;

public interface Items {
    Item GATEWAY = new GatewayItem(baseProps());

    static Item.Properties baseProps() {
        return new Item.Properties();
    }

    static void registerItems(Common.RegisterHelper<Item> helper) {
        helper.register(Common.locate("gateway"), GATEWAY);
    }
}
