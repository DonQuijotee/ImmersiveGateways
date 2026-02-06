package net.conczin.immersive_gateways;

import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

public class IrisCompat {
    private static BooleanSupplier shaderCheck;

    static {
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");

            Method getInstance = irisApiClass.getMethod("getInstance");
            Method isShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");

            shaderCheck = () -> {
                try {
                    Object instance = getInstance.invoke(null);
                    return (boolean) isShaderPackInUse.invoke(instance);
                } catch (Exception e) {
                    return false;
                }
            };
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            shaderCheck = () -> false;
        }
    }

    public static boolean isShaderPackInUse() {
        return shaderCheck.getAsBoolean();
    }
}