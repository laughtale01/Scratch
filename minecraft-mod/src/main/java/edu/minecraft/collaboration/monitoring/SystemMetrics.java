package edu.minecraft.collaboration.monitoring;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Collects system-level metrics (CPU, memory, threads)
 */
public class SystemMetrics {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystemMetrics.class);
    
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final Runtime runtime;
    
    // Cached values
    private double cpuUsage = 0;
    private long lastCpuTime = 0;
    private long lastSystemTime = 0;
    
    public SystemMetrics() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.runtime = Runtime.getRuntime();
    }
    
    /**
     * Update system metrics
     */
    public void update() {
        updateCpuUsage();
    }
    
    /**
     * Collect current system metrics
     */
    public SystemSnapshot collect() {
        SystemSnapshot snapshot = new SystemSnapshot();
        
        // CPU metrics
        snapshot.cpuUsage = cpuUsage;
        snapshot.processCpuUsage = osBean.getProcessCpuLoad() * 100;
        snapshot.systemCpuUsage = osBean.getCpuLoad() * 100;
        snapshot.availableProcessors = runtime.availableProcessors();
        
        // Memory metrics
        snapshot.heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        snapshot.heapMax = memoryBean.getHeapMemoryUsage().getMax();
        snapshot.heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
        snapshot.nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        snapshot.totalMemory = runtime.totalMemory();
        snapshot.freeMemory = runtime.freeMemory();
        snapshot.maxMemory = runtime.maxMemory();
        
        // Thread metrics
        snapshot.threadCount = threadBean.getThreadCount();
        snapshot.peakThreadCount = threadBean.getPeakThreadCount();
        snapshot.daemonThreadCount = threadBean.getDaemonThreadCount();
        
        // System info
        snapshot.osName = System.getProperty("os.name");
        snapshot.osVersion = System.getProperty("os.version");
        snapshot.javaVersion = System.getProperty("java.version");
        
        return snapshot;
    }
    
    /**
     * Update CPU usage calculation
     */
    private void updateCpuUsage() {
        long currentCpuTime = getCurrentCpuTime();
        long currentSystemTime = System.nanoTime();
        
        if (lastSystemTime > 0) {
            long cpuTimeDiff = currentCpuTime - lastCpuTime;
            long systemTimeDiff = currentSystemTime - lastSystemTime;
            
            if (systemTimeDiff > 0) {
                cpuUsage = (cpuTimeDiff * 100.0) / systemTimeDiff / runtime.availableProcessors();
                cpuUsage = Math.min(100.0, Math.max(0.0, cpuUsage));
            }
        }
        
        lastCpuTime = currentCpuTime;
        lastSystemTime = currentSystemTime;
    }
    
    /**
     * Get current CPU time for all threads
     */
    private long getCurrentCpuTime() {
        long cpuTime = 0;
        for (long id : threadBean.getAllThreadIds()) {
            long threadCpuTime = threadBean.getThreadCpuTime(id);
            if (threadCpuTime > 0) {
                cpuTime += threadCpuTime;
            }
        }
        return cpuTime;
    }
    
    /**
     * System metrics snapshot
     */
    public static class SystemSnapshot {
        // CPU metrics
        public double cpuUsage;
        public double processCpuUsage;
        public double systemCpuUsage;
        public int availableProcessors;
        
        // Memory metrics (bytes)
        public long heapUsed;
        public long heapMax;
        public long heapCommitted;
        public long nonHeapUsed;
        public long totalMemory;
        public long freeMemory;
        public long maxMemory;
        
        // Thread metrics
        public int threadCount;
        public int peakThreadCount;
        public int daemonThreadCount;
        
        // System info
        public String osName;
        public String osVersion;
        public String javaVersion;
        
        /**
         * Get memory usage percentage
         */
        public double getMemoryUsagePercent() {
            return (heapUsed * 100.0) / heapMax;
        }
        
        /**
         * Get available memory in MB
         */
        public long getAvailableMemoryMB() {
            return (maxMemory - totalMemory + freeMemory) / (1024 * 1024);
        }
        
        /**
         * Get used memory in MB
         */
        public long getUsedMemoryMB() {
            return (totalMemory - freeMemory) / (1024 * 1024);
        }
    }
}