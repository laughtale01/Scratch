package edu.minecraft.collaboration.util;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages cleanup of system resources to prevent leaks
 */
public final class ResourceCleanupManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static ResourceCleanupManager instance;
    
    // Resource tracking
    private final Map<String, AutoCloseable> managedResources = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> managedExecutors = new ConcurrentHashMap<>();
    private final Set<Runnable> shutdownHooks = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    
    // Cleanup configuration
    private static final long EXECUTOR_SHUTDOWN_TIMEOUT_MS = 10000; // 10 seconds
    private static final long RESOURCE_CLEANUP_TIMEOUT_MS = 5000; // 5 seconds
    
    private ResourceCleanupManager() {
        // Register JVM shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::performShutdown, "Resource-Cleanup-Shutdown"));
        LOGGER.info("Resource cleanup manager initialized");
    }
    
    public static synchronized ResourceCleanupManager getInstance() {
        if (instance == null) {
            instance = new ResourceCleanupManager();
        }
        return instance;
    }
    
    /**
     * Register a resource for automatic cleanup
     * @param name Unique name for the resource
     * @param resource The resource to manage
     */
    public void registerResource(String name, AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        
        AutoCloseable existing = managedResources.put(name, resource);
        if (existing != null && existing != resource) {
            LOGGER.warn("Replacing existing resource '{}', closing old resource", name);
            closeQuietly(existing);
        }
        
        LOGGER.debug("Registered resource for cleanup: {}", name);
    }
    
    /**
     * Register an executor service for automatic shutdown
     * @param name Unique name for the executor
     * @param executor The executor to manage
     */
    public void registerExecutor(String name, ExecutorService executor) {
        if (executor == null) {
            return;
        }
        
        ExecutorService existing = managedExecutors.put(name, executor);
        if (existing != null && existing != executor) {
            LOGGER.warn("Replacing existing executor '{}', shutting down old executor", name);
            shutdownExecutor(name, existing);
        }
        
        LOGGER.debug("Registered executor for cleanup: {}", name);
    }
    
    /**
     * Register a shutdown hook
     * @param hook The hook to run during shutdown
     */
    public void registerShutdownHook(Runnable hook) {
        shutdownHooks.add(hook);
    }
    
    /**
     * Unregister and close a resource
     * @param name The resource name
     * @return true if the resource was found and closed
     */
    public boolean unregisterAndCloseResource(String name) {
        AutoCloseable resource = managedResources.remove(name);
        if (resource != null) {
            closeQuietly(resource);
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
            shutdownExecutor(name, executor);
            return true;
        }
        return false;
    }
    
    /**
     * Perform cleanup of all resources
     */
    public void cleanup() {
        if (shutdownInProgress.compareAndSet(false, true)) {
            performShutdown();
            shutdownInProgress.set(false);
        }
    }
    
    /**
     * Perform shutdown of all managed resources
     */
    private void performShutdown() {
        LOGGER.info("Starting resource cleanup...");
        long startTime = System.currentTimeMillis();
        
        // Run shutdown hooks first
        runShutdownHooks();
        
        // Shutdown executors
        shutdownAllExecutors();
        
        // Close resources
        closeAllResources();
        
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("Resource cleanup completed in {} ms", duration);
    }
    
    /**
     * Run all registered shutdown hooks
     */
    private void runShutdownHooks() {
        if (shutdownHooks.isEmpty()) {
            return;
        }
        
        LOGGER.info("Running {} shutdown hooks", shutdownHooks.size());
        List<Future<?>> hookFutures = new ArrayList<>();
        ExecutorService hookExecutor = Executors.newCachedThreadPool();
        
        try {
            for (Runnable hook : shutdownHooks) {
                hookFutures.add(hookExecutor.submit(() -> {
                    try {
                        hook.run();
                    } catch (Exception e) {
                        LOGGER.error("Error running shutdown hook", e);
                    }
                }));
            }
            
            // Wait for hooks to complete
            for (Future<?> future : hookFutures) {
                try {
                    future.get(RESOURCE_CLEANUP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    LOGGER.warn("Shutdown hook timed out");
                    future.cancel(true);
                } catch (Exception e) {
                    LOGGER.error("Error waiting for shutdown hook", e);
                }
            }
        } finally {
            hookExecutor.shutdownNow();
        }
    }
    
    /**
     * Shutdown all managed executors
     */
    private void shutdownAllExecutors() {
        if (managedExecutors.isEmpty()) {
            return;
        }
        
        LOGGER.info("Shutting down {} executors", managedExecutors.size());
        
        // Shutdown all executors in parallel
        managedExecutors.entrySet().parallelStream().forEach(entry -> {
            shutdownExecutor(entry.getKey(), entry.getValue());
        });
        
        managedExecutors.clear();
    }
    
    /**
     * Shutdown a single executor
     */
    private void shutdownExecutor(String name, ExecutorService executor) {
        LOGGER.debug("Shutting down executor: {}", name);
        
        try {
            executor.shutdown();
            
            if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Executor '{}' did not terminate gracefully, forcing shutdown", name);
                List<Runnable> pending = executor.shutdownNow();
                
                if (!pending.isEmpty()) {
                    LOGGER.warn("Executor '{}' had {} pending tasks", name, pending.size());
                }
                
                if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_MS / 2, TimeUnit.MILLISECONDS)) {
                    LOGGER.error("Executor '{}' did not terminate after forced shutdown", name);
                }
            } else {
                LOGGER.debug("Executor '{}' shutdown successfully", name);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while shutting down executor '{}'", name, e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Close all managed resources
     */
    private void closeAllResources() {
        if (managedResources.isEmpty()) {
            return;
        }
        
        LOGGER.info("Closing {} resources", managedResources.size());
        
        // Close all resources in parallel
        managedResources.entrySet().parallelStream().forEach(entry -> {
            String name = entry.getKey();
            AutoCloseable resource = entry.getValue();
            
            LOGGER.debug("Closing resource: {}", name);
            closeQuietly(resource);
        });
        
        managedResources.clear();
    }
    
    /**
     * Close a resource quietly
     */
    private void closeQuietly(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        
        try {
            resource.close();
        } catch (Exception e) {
            LOGGER.error("Error closing resource", e);
        }
    }
    
    /**
     * Create a managed executor service
     * @param name The executor name
     * @param corePoolSize The core pool size
     * @return A managed executor service
     */
    public ExecutorService createManagedExecutor(String name, int corePoolSize) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ManagedThreadFactory(name)
        );
        
        executor.allowCoreThreadTimeOut(true);
        registerExecutor(name, executor);
        
        return executor;
    }
    
    /**
     * Create a managed scheduled executor service
     * @param name The executor name
     * @param corePoolSize The core pool size
     * @return A managed scheduled executor service
     */
    public ScheduledExecutorService createManagedScheduledExecutor(String name, int corePoolSize) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
            corePoolSize,
            new ManagedThreadFactory(name)
        );
        
        executor.setRemoveOnCancelPolicy(true);
        registerExecutor(name, executor);
        
        return executor;
    }
    
    /**
     * Thread factory for managed executors
     */
    private static class ManagedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final ThreadGroup threadGroup;
        private int threadNumber = 1;
        
        ManagedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
            // SecurityManager is deprecated in Java 17+, use current thread's group
            this.threadGroup = Thread.currentThread().getThreadGroup();
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(threadGroup, r, namePrefix + "-" + threadNumber++, 0);
            
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            
            // Set uncaught exception handler
            t.setUncaughtExceptionHandler((thread, ex) -> 
                LOGGER.error("Uncaught exception in thread {}", thread.getName(), ex)
            );
            
            return t;
        }
    }
    
    /**
     * Get statistics about managed resources
     */
    public ResourceStatistics getStatistics() {
        return new ResourceStatistics(
            managedResources.size(),
            managedExecutors.size(),
            shutdownHooks.size()
        );
    }
    
    /**
     * Resource statistics
     */
    public static class ResourceStatistics {
        private final int managedResources;
        private final int managedExecutors;
        private final int shutdownHooks;
        
        public ResourceStatistics(int managedResources, int managedExecutors, int shutdownHooks) {
            this.managedResources = managedResources;
            this.managedExecutors = managedExecutors;
            this.shutdownHooks = shutdownHooks;
        }
        
        public int getManagedResources() {
            return managedResources;
        }
        
        public int getManagedExecutors() {
            return managedExecutors;
        }
        
        public int getShutdownHooks() {
            return shutdownHooks;
        }
        
        @Override
        public String toString() {
            return String.format("ResourceStats{resources=%d, executors=%d, hooks=%d}",
                managedResources, managedExecutors, shutdownHooks);
        }
    }
}