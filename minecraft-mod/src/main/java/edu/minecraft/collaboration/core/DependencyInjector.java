package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.security.RateLimiter;
import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.monitoring.MetricsCollector;
import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.localization.LanguageManager;
import edu.minecraft.collaboration.config.ConfigurationManager;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Centralized dependency injector for managing service lifecycle and dependencies.
 * Replaces singleton patterns with proper dependency injection.
 * 
 * Features:
 * - Thread-safe service registration and retrieval
 * - Lazy initialization support
 * - Proper lifecycle management
 * - Circular dependency detection
 * - Service disposal and cleanup
 */
public final class DependencyInjector {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    // Thread-safe singleton instance with double-checked locking
    private static volatile DependencyInjector instance;
    private static final Object LOCK = new Object();
    
    // Service registry
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();
    private final Map<Class<?>, Supplier<?>> serviceFactories = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock registryLock = new ReentrantReadWriteLock();
    
    // Circular dependency detection
    private final ThreadLocal<Set<Class<?>>> initializationStack = ThreadLocal.withInitial(java.util.HashSet::new);
    
    // Lifecycle state
    private volatile boolean isShutdown = false;
    
    private DependencyInjector() {
        registerCoreServices();
        LOGGER.info("DependencyInjector initialized with core services");
    }
    
    /**
     * Get the singleton instance using thread-safe double-checked locking
     */
    public static DependencyInjector getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DependencyInjector();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register core services with their factories
     */
    private void registerCoreServices() {
        // Register ConfigurationManager first as other services may depend on it
        registerService(ConfigurationManager.class, () -> new ConfigurationManager());
        
        // Register service factories for lazy initialization (with configuration dependency)
        registerService(RateLimiter.class, () -> {
            ConfigurationManager config = getService(ConfigurationManager.class);
            return new RateLimiter(config);
        });
        registerService(AuthenticationManager.class, () -> new AuthenticationManager());
        registerService(MetricsCollector.class, () -> new MetricsCollector());
        registerService(CollaborationManager.class, () -> new CollaborationManager());
        registerService(LanguageManager.class, () -> new LanguageManager());
        
        LOGGER.info("Registered {} core service factories", serviceFactories.size());
    }
    
    /**
     * Register a service with a factory for lazy initialization
     */
    public <T> void registerService(Class<T> serviceClass, Supplier<T> factory) {
        if (isShutdown) {
            throw new IllegalStateException("DependencyInjector is shutdown");
        }
        
        registryLock.writeLock().lock();
        try {
            serviceFactories.put(serviceClass, factory);
            LOGGER.debug("Registered service factory for: {}", serviceClass.getSimpleName());
        } finally {
            registryLock.writeLock().unlock();
        }
    }
    
    /**
     * Register a pre-created service instance
     */
    public <T> void registerInstance(Class<T> serviceClass, T instance) {
        if (isShutdown) {
            throw new IllegalStateException("DependencyInjector is shutdown");
        }
        
        if (instance == null) {
            throw new IllegalArgumentException("Service instance cannot be null");
        }
        
        registryLock.writeLock().lock();
        try {
            services.put(serviceClass, instance);
            LOGGER.debug("Registered service instance for: {}", serviceClass.getSimpleName());
        } finally {
            registryLock.writeLock().unlock();
        }
    }
    
    /**
     * Get a service instance, creating it if necessary
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        if (isShutdown) {
            throw new IllegalStateException("DependencyInjector is shutdown");
        }
        
        // Fast path: check if already initialized
        registryLock.readLock().lock();
        try {
            Object service = services.get(serviceClass);
            if (service != null) {
                return (T) service;
            }
        } finally {
            registryLock.readLock().unlock();
        }
        
        // Slow path: create the service
        return createService(serviceClass);
    }
    
    /**
     * Create a service instance with circular dependency detection
     */
    @SuppressWarnings("unchecked")
    private <T> T createService(Class<T> serviceClass) {
        // Check for circular dependency
        Set<Class<?>> stack = initializationStack.get();
        if (stack.contains(serviceClass)) {
            throw new IllegalStateException("Circular dependency detected for service: " + serviceClass.getSimpleName());
        }
        
        registryLock.writeLock().lock();
        try {
            // Double-check pattern
            Object service = services.get(serviceClass);
            if (service != null) {
                return (T) service;
            }
            
            // Mark as being initialized
            stack.add(serviceClass);
            
            try {
                // Get factory
                Supplier<?> factory = serviceFactories.get(serviceClass);
                if (factory == null) {
                    throw new IllegalArgumentException("No factory registered for service: " + serviceClass.getSimpleName());
                }
                
                // Create instance
                T instance = (T) factory.get();
                if (instance == null) {
                    throw new IllegalStateException("Factory returned null for service: " + serviceClass.getSimpleName());
                }
                
                // Register instance
                services.put(serviceClass, instance);
                LOGGER.info("Created and registered service instance: {}", serviceClass.getSimpleName());
                
                return instance;
                
            } finally {
                // Remove from initialization stack
                stack.remove(serviceClass);
            }
            
        } finally {
            registryLock.writeLock().unlock();
        }
    }
    
    /**
     * Check if a service is registered
     */
    public boolean isServiceRegistered(Class<?> serviceClass) {
        registryLock.readLock().lock();
        try {
            return services.containsKey(serviceClass) || serviceFactories.containsKey(serviceClass);
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Check if a service instance exists (has been created)
     */
    public boolean isServiceInitialized(Class<?> serviceClass) {
        registryLock.readLock().lock();
        try {
            return services.containsKey(serviceClass);
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Get statistics about registered services
     */
    public ServiceStatistics getStatistics() {
        registryLock.readLock().lock();
        try {
            return new ServiceStatistics(
                serviceFactories.size(),
                services.size(),
                isShutdown
            );
        } finally {
            registryLock.readLock().unlock();
        }
    }
    
    /**
     * Shutdown the dependency injector and cleanup all services
     */
    public void shutdown() {
        if (isShutdown) {
            return;
        }
        
        LOGGER.info("Shutting down DependencyInjector...");
        
        registryLock.writeLock().lock();
        try {
            isShutdown = true;
            
            // Shutdown services that implement disposable interface
            for (Map.Entry<Class<?>, Object> entry : services.entrySet()) {
                Object service = entry.getValue();
                String serviceName = entry.getKey().getSimpleName();
                
                try {
                    // Check for known cleanup methods
                    if (service instanceof RateLimiter) {
                        ((RateLimiter) service).shutdown();
                    } else if (service instanceof MetricsCollector) {
                        ((MetricsCollector) service).shutdown();
                    }
                    // Add more cleanup methods as needed
                    
                    LOGGER.debug("Cleaned up service: {}", serviceName);
                    
                } catch (Exception e) {
                    LOGGER.error("Error shutting down service: {}", serviceName, e);
                }
            }
            
            // Clear all registrations
            services.clear();
            serviceFactories.clear();
            
            LOGGER.info("DependencyInjector shutdown complete");
            
        } finally {
            registryLock.writeLock().unlock();
        }
    }
    
    /**
     * Clear all services (for testing purposes)
     */
    public void clearForTesting() {
        registryLock.writeLock().lock();
        try {
            services.clear();
            serviceFactories.clear();
            isShutdown = false;
            registerCoreServices();
            LOGGER.debug("Cleared all services for testing");
        } finally {
            registryLock.writeLock().unlock();
        }
    }
    
    /**
     * Statistics about the service registry
     */
    public static class ServiceStatistics {
        private final int registeredFactories;
        private final int initializedServices;
        private final boolean isShutdown;
        
        public ServiceStatistics(int registeredFactories, int initializedServices, boolean isShutdown) {
            this.registeredFactories = registeredFactories;
            this.initializedServices = initializedServices;
            this.isShutdown = isShutdown;
        }
        
        public int getRegisteredFactories() {
            return registeredFactories;
        }
        
        public int getInitializedServices() {
            return initializedServices;
        }
        
        public boolean isShutdown() {
            return isShutdown;
        }
        
        @Override
        public String toString() {
            return String.format("ServiceStatistics{registeredFactories=%d, initializedServices=%d, isShutdown=%s}",
                registeredFactories, initializedServices, isShutdown);
        }
    }
}