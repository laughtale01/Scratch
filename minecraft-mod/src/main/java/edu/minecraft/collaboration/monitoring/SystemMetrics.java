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
        snapshot.setCpuUsage(cpuUsage);
        snapshot.setProcessCpuUsage(osBean.getProcessCpuLoad() * 100);
        snapshot.setSystemCpuUsage(osBean.getCpuLoad() * 100);
        snapshot.setAvailableProcessors(runtime.availableProcessors());
        
        // Memory metrics
        snapshot.setHeapUsed(memoryBean.getHeapMemoryUsage().getUsed());
        snapshot.setHeapMax(memoryBean.getHeapMemoryUsage().getMax());
        snapshot.setHeapCommitted(memoryBean.getHeapMemoryUsage().getCommitted());
        snapshot.setNonHeapUsed(memoryBean.getNonHeapMemoryUsage().getUsed());
        snapshot.setTotalMemory(runtime.totalMemory());
        snapshot.setFreeMemory(runtime.freeMemory());
        snapshot.setMaxMemory(runtime.maxMemory());
        
        // Thread metrics
        snapshot.setThreadCount(threadBean.getThreadCount());
        snapshot.setPeakThreadCount(threadBean.getPeakThreadCount());
        snapshot.setDaemonThreadCount(threadBean.getDaemonThreadCount());
        
        // System info
        snapshot.setOsName(System.getProperty("os.name"));
        snapshot.setOsVersion(System.getProperty("os.version"));
        snapshot.setJavaVersion(System.getProperty("java.version"));
        
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
        private double cpuUsage;
        private double processCpuUsage;
        private double systemCpuUsage;
        private int availableProcessors;
        
        // Memory metrics (bytes)
        private long heapUsed;
        private long heapMax;
        private long heapCommitted;
        private long nonHeapUsed;
        private long totalMemory;
        private long freeMemory;
        private long maxMemory;
        
        // Thread metrics
        private int threadCount;
        private int peakThreadCount;
        private int daemonThreadCount;
        
        // System info
        private String osName;
        private String osVersion;
        private String javaVersion;
        
        // CPU getters/setters
        public double getCpuUsage() {
            return cpuUsage;
        }
        public void setCpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
        }
        public double getProcessCpuUsage() {
            return processCpuUsage;
        }
        public void setProcessCpuUsage(double processCpuUsage) {
            this.processCpuUsage = processCpuUsage;
        }
        public double getSystemCpuUsage() {
            return systemCpuUsage;
        }
        public void setSystemCpuUsage(double systemCpuUsage) {
            this.systemCpuUsage = systemCpuUsage;
        }
        public int getAvailableProcessors() {
            return availableProcessors;
        }
        public void setAvailableProcessors(int availableProcessors) {
            this.availableProcessors = availableProcessors;
        }
        
        // Memory getters/setters
        public long getHeapUsed() {
            return heapUsed;
        }
        public void setHeapUsed(long heapUsed) {
            this.heapUsed = heapUsed;
        }
        public long getHeapMax() {
            return heapMax;
        }
        public void setHeapMax(long heapMax) {
            this.heapMax = heapMax;
        }
        public long getHeapCommitted() {
            return heapCommitted;
        }
        public void setHeapCommitted(long heapCommitted) {
            this.heapCommitted = heapCommitted;
        }
        public long getNonHeapUsed() {
            return nonHeapUsed;
        }
        public void setNonHeapUsed(long nonHeapUsed) {
            this.nonHeapUsed = nonHeapUsed;
        }
        public long getTotalMemory() {
            return totalMemory;
        }
        public void setTotalMemory(long totalMemory) {
            this.totalMemory = totalMemory;
        }
        public long getFreeMemory() {
            return freeMemory;
        }
        public void setFreeMemory(long freeMemory) {
            this.freeMemory = freeMemory;
        }
        public long getMaxMemory() {
            return maxMemory;
        }
        public void setMaxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
        }
        
        // Thread getters/setters
        public int getThreadCount() {
            return threadCount;
        }
        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }
        public int getPeakThreadCount() {
            return peakThreadCount;
        }
        public void setPeakThreadCount(int peakThreadCount) {
            this.peakThreadCount = peakThreadCount;
        }
        public int getDaemonThreadCount() {
            return daemonThreadCount;
        }
        public void setDaemonThreadCount(int daemonThreadCount) {
            this.daemonThreadCount = daemonThreadCount;
        }
        
        // System info getters/setters
        public String getOsName() {
            return osName;
        }
        public void setOsName(String osName) {
            this.osName = osName;
        }
        public String getOsVersion() {
            return osVersion;
        }
        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }
        public String getJavaVersion() {
            return javaVersion;
        }
        public void setJavaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
        }
        
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