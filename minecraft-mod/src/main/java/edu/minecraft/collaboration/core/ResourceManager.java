package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Centralized resource manager for handling all system resources
 * including ExecutorServices, AutoCloseable resources, and shutdown hooks.
 *
 * This class ensures proper resource cleanup and prevents resource leaks
 * by managing all resources through a single point of control.
 */
public final class ResourceManager implements AutoCloseable {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    // Singleton instance
    private static volatile ResourceManager instance;
    private static final ReentrantLock INSTANCE_LOCK = new ReentrantLock();

    // Configuration
    private static final long EXECUTOR_SHUTDOWN_TIMEOUT_MS = 15000; // 15 seconds
    private static final long RESOURCE_CLEANUP_TIMEOUT_MS = 10000; // 10 seconds

    // Resource tracking
    private final Map<String, AutoCloseable> managedResources = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> managedExecutors = new ConcurrentHashMap<>();
    private final Set<Runnable> shutdownHooks = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final AtomicBoolean shutdownComplete = new AtomicBoolean(false);

    // Shutdown hook thread
    private Thread shutdownHookThread;

    private ResourceManager() {
        // Register JVM shutdown hook
        registerJvmShutdownHook();
        LOGGER.info("ResourceManager initialized - centralized resource management enabled");
    }

    /**
     * Get the singleton instance of ResourceManager
     */
    public static ResourceManager getInstance() {
        if (instance == null) {
            INSTANCE_LOCK.lock();
            try {
                if (instance == null) {
                    instance = new ResourceManager();
                }
            } finally {
                INSTANCE_LOCK.unlock();
            }
        }
        return instance;
    }

    /**
     * Register a resource for automatic cleanup
     * @param name Unique name for the resource
     * @param resource The resource to manage
     * @throws IllegalStateException if shutdown is in progress
     */
    public void registerResource(String name, AutoCloseable resource) {
        if (shutdownInProgress.get()) {
            throw new IllegalStateException("Cannot register resources during shutdown");
        }

        if (resource == null) {
            LOGGER.warn("Attempted to register null resource: {}", name);
            return;
        }

        AutoCloseable existing = managedResources.put(name, resource);
        if (existing != null && existing != resource) {
            LOGGER.warn("Replacing existing resource '{}', closing old resource", name);
            closeResourceQuietly(existing, name + " (replaced)");
        }

        LOGGER.debug("Registered resource for cleanup: {}", name);
    }

    /**
     * Register an executor service for automatic shutdown
     * @param name Unique name for the executor
     * @param executor The executor to manage
     * @throws IllegalStateException if shutdown is in progress
     */
    public void registerExecutor(String name, ExecutorService executor) {
        if (shutdownInProgress.get()) {
            throw new IllegalStateException("Cannot register executors during shutdown");
        }

        if (executor == null) {
            LOGGER.warn("Attempted to register null executor: {}", name);
            return;
        }

        ExecutorService existing = managedExecutors.put(name, executor);
        if (existing != null && existing != executor) {
            LOGGER.warn("Replacing existing executor '{}', shutting down old executor", name);
            shutdownExecutorQuietly(existing, name + " (replaced)");
        }

        LOGGER.debug("Registered executor for cleanup: {}", name);
    }

    /**
     * Register a shutdown hook to run during cleanup
     * @param hook The hook to run during shutdown
     * @throws IllegalStateException if shutdown is in progress
     */
    public void registerShutdownHook(Runnable hook) {
        if (shutdownInProgress.get()) {
            throw new IllegalStateException("Cannot register shutdown hooks during shutdown");
        }

        if (hook != null) {
            shutdownHooks.add(hook);
            LOGGER.debug("Registered shutdown hook");
        }
    }

    /**
     * Unregister and close a resource
     * @param name The resource name
     * @return true if the resource was found and closed
     */
    public boolean unregisterAndCloseResource(String name) {
        AutoCloseable resource = managedResources.remove(name);
        if (resource != null) {
            closeResourceQuietly(resource, name);
            LOGGER.debug("Unregistered and closed resource: {}", name);
            return true;
        }
        return false;
    }

    /**
     * Unregister and shutdown an executor
     * @param name The executor name
     * @return true if the executor was found and shutdown
     */
    public boolean unregisterAndShutdownExecutor(String name) {
        ExecutorService executor = managedExecutors.remove(name);
        if (executor != null) {
            shutdownExecutorQuietly(executor, name);
            LOGGER.debug("Unregistered and shutdown executor: {}", name);
            return true;
        }
        return false;
    }

    /**
     * Perform immediate shutdown of all resources
     * This method is idempotent and can be called multiple times safely
     */
    @Override
    public void close() {
        shutdown();
    }

    /**
     * Perform shutdown of all managed resources
     */
    public void shutdown() {
        if (shutdownComplete.get()) {
            LOGGER.debug("ResourceManager already shutdown, skipping");
            return;
        }

        if (!shutdownInProgress.compareAndSet(false, true)) {
            LOGGER.info("ResourceManager shutdown already in progress, waiting for completion");
            waitForShutdownCompletion();
            return;
        }

        LOGGER.info("Starting ResourceManager shutdown...");
        long startTime = System.currentTimeMillis();

        try {
            // Run shutdown hooks first
            runShutdownHooks();

            // Shutdown executors (most critical)
            shutdownAllExecutors();

            // Close remaining resources
            closeAllResources();

            shutdownComplete.set(true);
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("ResourceManager shutdown completed successfully in {} ms", duration);

        } catch (Exception e) {
            LOGGER.error("Error during ResourceManager shutdown", e);
            shutdownComplete.set(true);
        }
    }

    /**
     * Wait for shutdown completion if it's in progress
     */
    private void waitForShutdownCompletion() {
        long startWait = System.currentTimeMillis();
        while (!shutdownComplete.get() && (System.currentTimeMillis() - startWait) < RESOURCE_CLEANUP_TIMEOUT_MS) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Register JVM shutdown hook
     */
    private void registerJvmShutdownHook() {
        shutdownHookThread = new Thread(this::shutdown, "ResourceManager-ShutdownHook");
        shutdownHookThread.setDaemon(false);

        try {
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
            LOGGER.debug("JVM shutdown hook registered");
        } catch (IllegalStateException e) {
            LOGGER.warn("Could not register JVM shutdown hook - JVM may be shutting down");
        }
    }

    /**
     * Run all registered shutdown hooks
     */
    private void runShutdownHooks() {
        if (shutdownHooks.isEmpty()) {
            return;
        }

        LOGGER.info("Running {} shutdown hooks", shutdownHooks.size());

        for (Runnable hook : shutdownHooks) {
            try {
                hook.run();
            } catch (Exception e) {
                LOGGER.error("Error running shutdown hook", e);
            }
        }

        shutdownHooks.clear();
    }

    /**
     * Shutdown all managed executors
     */
    private void shutdownAllExecutors() {
        if (managedExecutors.isEmpty()) {
            return;
        }

        LOGGER.info("Shutting down {} executors", managedExecutors.size());

        // Shutdown all executors gracefully first
        managedExecutors.forEach((name, executor) -> {
            LOGGER.debug("Initiating graceful shutdown for executor: {}", name);
            executor.shutdown();
        });

        // Wait for graceful termination with timeout
        long shutdownStart = System.currentTimeMillis();
        long gracefulTimeout = EXECUTOR_SHUTDOWN_TIMEOUT_MS / 2;

        managedExecutors.entrySet().removeIf(entry -> {
            String name = entry.getKey();
            ExecutorService executor = entry.getValue();

            try {
                if (executor.awaitTermination(gracefulTimeout, TimeUnit.MILLISECONDS)) {
                    LOGGER.debug("Executor '{}' terminated gracefully", name);
                    return true;
                } else {
                    LOGGER.warn("Executor '{}' did not terminate gracefully, will force shutdown", name);
                    return false;
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for executor '{}' termination", name);
                Thread.currentThread().interrupt();
                return false;
            }
        });

        // Force shutdown remaining executors
        if (!managedExecutors.isEmpty()) {
            LOGGER.warn("Force shutting down {} remaining executors", managedExecutors.size());

            managedExecutors.forEach((name, executor) -> {
                try {
                    executor.shutdownNow();

                    long remainingTimeout = Math.max(1000,
                        EXECUTOR_SHUTDOWN_TIMEOUT_MS - (System.currentTimeMillis() - shutdownStart));

                    if (!executor.awaitTermination(remainingTimeout, TimeUnit.MILLISECONDS)) {
                        LOGGER.error("Executor '{}' did not terminate after forced shutdown", name);
                    } else {
                        LOGGER.debug("Executor '{}' terminated after forced shutdown", name);
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted while force shutting down executor '{}'", name);
                    Thread.currentThread().interrupt();
                }
            });
        }

        managedExecutors.clear();
    }

    /**
     * Close all managed resources
     */
    private void closeAllResources() {
        if (managedResources.isEmpty()) {
            return;
        }

        LOGGER.info("Closing {} resources", managedResources.size());

        managedResources.forEach((name, resource) -> {
            closeResourceQuietly(resource, name);
        });

        managedResources.clear();
    }

    /**
     * Close a resource quietly without throwing exceptions
     */
    private void closeResourceQuietly(AutoCloseable resource, String name) {
        if (resource == null) {
            return;
        }

        try {
            LOGGER.debug("Closing resource: {}", name);
            resource.close();
        } catch (Exception e) {
            LOGGER.error("Error closing resource '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Shutdown an executor quietly without throwing exceptions
     */
    private void shutdownExecutorQuietly(ExecutorService executor, String name) {
        if (executor == null) {
            return;
        }

        try {
            LOGGER.debug("Shutting down executor: {}", name);
            executor.shutdown();

            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Executor '{}' did not terminate gracefully, forcing shutdown", name);
                executor.shutdownNow();

                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.error("Executor '{}' did not terminate after forced shutdown", name);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while shutting down executor '{}'", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get resource management statistics
     */
    public ResourceStatistics getStatistics() {
        return new ResourceStatistics(
            managedResources.size(),
            managedExecutors.size(),
            shutdownHooks.size(),
            shutdownInProgress.get(),
            shutdownComplete.get()
        );
    }

    /**
     * Check if ResourceManager is shutdown
     */
    public boolean isShutdown() {
        return shutdownComplete.get();
    }

    /**
     * Resource management statistics
     */
    public static class ResourceStatistics {
        private final int managedResources;
        private final int managedExecutors;
        private final int shutdownHooks;
        private final boolean shutdownInProgress;
        private final boolean shutdownComplete;

        public ResourceStatistics(int managedResources, int managedExecutors, int shutdownHooks,
                                 boolean shutdownInProgress, boolean shutdownComplete) {
            this.managedResources = managedResources;
            this.managedExecutors = managedExecutors;
            this.shutdownHooks = shutdownHooks;
            this.shutdownInProgress = shutdownInProgress;
            this.shutdownComplete = shutdownComplete;
        }

        public int getManagedResources() { return managedResources; }
        public int getManagedExecutors() { return managedExecutors; }
        public int getShutdownHooks() { return shutdownHooks; }
        public boolean isShutdownInProgress() { return shutdownInProgress; }
        public boolean isShutdownComplete() { return shutdownComplete; }

        @Override
        public String toString() {
            return String.format("ResourceStats{resources=%d, executors=%d, hooks=%d, "
                               + "shutdownInProgress=%b, shutdownComplete=%b}",
                               managedResources, managedExecutors, shutdownHooks,
                               shutdownInProgress, shutdownComplete);
        }
    }
}
