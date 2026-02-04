package net.conczin.immersive_gateways.block;

import net.conczin.immersive_gateways.Common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GatewayExecutorController {
    private static ExecutorService executor;

    private GatewayExecutorController() {
    }

    private static ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("Immersive Gateways Searcher");
            return thread;
        });
    }

    private static synchronized ExecutorService ensureExecutor() {
        if (executor == null || executor.isShutdown()) {
            executor = createExecutor();
            Common.LOGGER.debug("Gateway executor initialized");
        }
        return executor;
    }

    public static void submit(Runnable task) {
        ensureExecutor().execute(task);
    }

    public static synchronized void reset() {
        shutdownInternal();
        executor = createExecutor();
        Common.LOGGER.debug("Gateway executor reset");
    }

    public static synchronized void shutdown() {
        shutdownInternal();
    }

    private static void shutdownInternal() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
            Common.LOGGER.debug("Gateway executor shut down");
        }
    }
}

