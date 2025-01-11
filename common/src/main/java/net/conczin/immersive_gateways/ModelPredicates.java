package net.conczin.immersive_gateways;

import net.conczin.immersive_gateways.item.GatewayCharge;
import net.conczin.immersive_gateways.mixin.client.ItemPropertiesAccessor;

public class ModelPredicates {
    static void setup() {
        ItemPropertiesAccessor.register(Items.GATEWAY_CHARGE.get(), ImmersiveGateways.locate("linked"),
                (stack, world, entity, i) -> GatewayCharge.isLinked(stack) ? 1 : 0
        );
    }
}
