package edu.minecraft.collaboration.monitoring.apm;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.core.ResourceManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application Performance Monitoring (APM) Manager
 * Provides comprehensive monitoring, metrics collection, and distributed tracing
 */
public class APMManager implements AutoCloseable {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final CompositeMeterRegistry meterRegistry;
    private final PrometheusMeterRegistry prometheusMeterRegistry;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;
    private final ScheduledExecutorService scheduler;
    private final ResourceManager resourceManager;
    
    // Performance metrics
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gaugeValues = new ConcurrentHashMap<>();
    
    // System health metrics
    private volatile double systemCpuUsage = 0.0;
    private volatile long systemMemoryUsage = 0L;
    private volatile int activeConnections = 0;
    private volatile double averageResponseTime = 0.0;
    
    private volatile boolean enabled = true;
    
    public APMManager() {
        this.resourceManager = ResourceManager.getInstance();
        
        // Initialize Micrometer registry
        this.meterRegistry = new CompositeMeterRegistry();
        this.prometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        this.meterRegistry.add(prometheusMeterRegistry);
        
        // Initialize OpenTelemetry for distributed tracing
        this.openTelemetry = initializeOpenTelemetry();
        this.tracer = openTelemetry.getTracer("minecraft-collaboration-mod");
        
        // Initialize scheduler
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "APM-Manager-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        resourceManager.registerExecutor("APMManager", scheduler);
        
        // Initialize built-in metrics
        initializeBuiltInMetrics();
        
        // Start background monitoring
        startBackgroundMonitoring();
        
        LOGGER.info("APM Manager initialized with Prometheus and OpenTelemetry");
    }
    
    private OpenTelemetry initializeOpenTelemetry() {
        // Configure Jaeger exporter
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
            .setEndpoint("http://localhost:14268") // Jaeger collector endpoint
            .build();
        
        // Configure tracer provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
            .build();
        
        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build();
    }
    
    private void initializeBuiltInMetrics() {
        // System performance counters
        registerCounter("system.requests.total", "Total number of requests processed");
        registerCounter("system.errors.total", "Total number of errors occurred");
        registerCounter("websocket.connections.total", "Total WebSocket connections");
        registerCounter("websocket.messages.sent", "Total WebSocket messages sent");
        registerCounter("websocket.messages.received", "Total WebSocket messages received");
        
        // Business metrics
        registerCounter("collaboration.invitations.sent", "Total collaboration invitations sent");
        registerCounter("collaboration.invitations.accepted", "Total invitations accepted");
        registerCounter("commands.executed", "Total commands executed");
        registerCounter("blocks.placed", "Total blocks placed");
        
        // System gauges
        registerGauge("system.cpu.usage", "System CPU usage percentage", () -> systemCpuUsage);
        registerGauge("system.memory.usage", "System memory usage in bytes", () -> (double) systemMemoryUsage);
        registerGauge("websocket.connections.active", "Active WebSocket connections", () -> (double) activeConnections);
        registerGauge("system.response.time.average", "Average response time in milliseconds", () -> averageResponseTime);
        
        // Performance timers
        registerTimer("websocket.message.processing", "WebSocket message processing time");
        registerTimer("command.execution", "Command execution time");
        registerTimer("collaboration.invitation.processing", "Invitation processing time");
        registerTimer("block.operation", "Block operation time");
    }
    
    /**
     * Start a new trace span
     */
    public TraceContext startTrace(String operationName) {
        return startTrace(operationName, null);
    }
    
    public TraceContext startTrace(String operationName, TraceContext parentContext) {
        if (!enabled) {
            return new NoOpTraceContext();
        }
        
        SpanBuilder spanBuilder = tracer.spanBuilder(operationName);
        
        if (parentContext != null && parentContext instanceof OpenTelemetryTraceContext) {
            OpenTelemetryTraceContext otContext = (OpenTelemetryTraceContext) parentContext;
            spanBuilder.setParent(otContext.getContext());
        }
        
        Span span = spanBuilder.startSpan();
        Scope scope = span.makeCurrent();
        
        return new OpenTelemetryTraceContext(span, scope);
    }
    
    /**
     * Record a timing measurement
     */
    public void recordTiming(String metricName, Duration duration) {
        if (!enabled) return;
        
        Timer timer = timers.get(metricName);
        if (timer != null) {
            timer.record(duration);
        } else {
            LOGGER.warn("Timer {} not found, registering new timer", metricName);
            registerTimer(metricName, "Auto-registered timer");
            recordTiming(metricName, duration);
        }
    }
    
    /**
     * Increment a counter
     */
    public void incrementCounter(String metricName) {
        incrementCounter(metricName, 1);
    }
    
    public void incrementCounter(String metricName, double amount) {
        if (!enabled) return;
        
        Counter counter = counters.get(metricName);
        if (counter != null) {
            counter.increment(amount);
        } else {
            LOGGER.warn("Counter {} not found, registering new counter", metricName);
            registerCounter(metricName, "Auto-registered counter");
            incrementCounter(metricName, amount);
        }
    }
    
    /**
     * Set a gauge value
     */
    public void setGauge(String metricName, double value) {
        if (!enabled) return;
        
        AtomicLong gaugeValue = gaugeValues.get(metricName);
        if (gaugeValue != null) {
            gaugeValue.set((long) value);
        } else {
            LOGGER.warn("Gauge {} not found", metricName);
        }
    }
    
    /**
     * Record a custom metric with tags
     */
    public void recordCustomMetric(String metricName, double value, String... tags) {
        if (!enabled) return;
        
        Tags tagSet = Tags.of(tags);
        Gauge.builder(metricName)
            .tags(tagSet)
            .register(meterRegistry, () -> value);
    }
    
    /**
     * Time a code block execution
     */
    public <T> T timeExecution(String metricName, java.util.function.Supplier<T> operation) {
        if (!enabled) {
            return operation.get();
        }
        
        Timer timer = timers.get(metricName);
        if (timer == null) {
            return operation.get();
        }
        
        return timer.recordCallable(operation::get);
    }
    
    /**
     * Time a code block with tracing
     */
    public <T> T timeAndTrace(String operationName, java.util.function.Supplier<T> operation) {
        if (!enabled) {
            return operation.get();
        }
        
        try (TraceContext context = startTrace(operationName)) {
            long startTime = System.nanoTime();
            try {
                T result = operation.get();
                context.addAttribute("result", "success");
                return result;
            } catch (Exception e) {
                context.addAttribute("result", "error");
                context.addAttribute("error.message", e.getMessage());
                throw e;
            } finally {
                long duration = System.nanoTime() - startTime;
                recordTiming(operationName.toLowerCase().replace(" ", "."), Duration.ofNanos(duration));
            }
        }
    }
    
    /**
     * Register a new counter
     */
    public void registerCounter(String name, String description) {
        Counter counter = Counter.builder(name)
            .description(description)
            .register(meterRegistry);
        counters.put(name, counter);
    }
    
    /**
     * Register a new timer
     */
    public void registerTimer(String name, String description) {
        Timer timer = Timer.builder(name)
            .description(description)
            .register(meterRegistry);
        timers.put(name, timer);
    }
    
    /**
     * Register a new gauge
     */
    public void registerGauge(String name, String description, java.util.function.Supplier<Double> valueSupplier) {
        AtomicLong gaugeValue = new AtomicLong(0);
        gaugeValues.put(name, gaugeValue);
        
        Gauge gauge = Gauge.builder(name)
            .description(description)
            .register(meterRegistry, valueSupplier);
        gauges.put(name, gauge);
    }
    
    private void startBackgroundMonitoring() {
        // Update system metrics every 10 seconds
        scheduler.scheduleAtFixedRate(this::updateSystemMetrics, 0, 10, TimeUnit.SECONDS);
        
        // Generate periodic health reports every minute
        scheduler.scheduleAtFixedRate(this::generateHealthReport, 60, 60, TimeUnit.SECONDS);
        
        // Cleanup old trace data every hour
        scheduler.scheduleAtFixedRate(this::cleanupTraceData, 1, 1, TimeUnit.HOURS);
    }
    
    private void updateSystemMetrics() {
        if (!enabled) return;
        
        try {
            // Update CPU usage
            systemCpuUsage = getSystemCpuUsage();
            
            // Update memory usage
            Runtime runtime = Runtime.getRuntime();
            systemMemoryUsage = runtime.totalMemory() - runtime.freeMemory();
            
            // Update gauge values
            setGauge("system.cpu.usage", systemCpuUsage);
            setGauge("system.memory.usage", systemMemoryUsage);
            setGauge("websocket.connections.active", activeConnections);
            
        } catch (Exception e) {
            LOGGER.error("Error updating system metrics", e);
        }
    }
    
    private double getSystemCpuUsage() {
        // Simplified CPU usage calculation
        // In a real implementation, this would use system APIs
        return Math.random() * 100; // Placeholder
    }
    
    private void generateHealthReport() {
        if (!enabled) return;
        
        try {
            HealthReport report = HealthReport.builder()
                .cpuUsage(systemCpuUsage)
                .memoryUsage(systemMemoryUsage)
                .activeConnections(activeConnections)
                .averageResponseTime(averageResponseTime)
                .requestCount(getCounterValue("system.requests.total"))
                .errorCount(getCounterValue("system.errors.total"))
                .build();
            
            LOGGER.info("Health Report: {}", report);
            
            // Check for alerts
            checkHealthAlerts(report);
            
        } catch (Exception e) {
            LOGGER.error("Error generating health report", e);
        }
    }
    
    private void checkHealthAlerts(HealthReport report) {
        // Check CPU usage
        if (report.getCpuUsage() > 80.0) {
            LOGGER.warn("HIGH CPU USAGE ALERT: {}%", report.getCpuUsage());
        }
        
        // Check memory usage
        long maxMemory = Runtime.getRuntime().maxMemory();
        double memoryUsagePercent = (double) report.getMemoryUsage() / maxMemory * 100;
        if (memoryUsagePercent > 80.0) {
            LOGGER.warn("HIGH MEMORY USAGE ALERT: {}%", memoryUsagePercent);
        }
        
        // Check error rate
        double totalRequests = report.getRequestCount();
        if (totalRequests > 0) {
            double errorRate = report.getErrorCount() / totalRequests * 100;
            if (errorRate > 5.0) {
                LOGGER.warn("HIGH ERROR RATE ALERT: {}%", errorRate);
            }
        }
        
        // Check response time
        if (report.getAverageResponseTime() > 1000.0) {
            LOGGER.warn("HIGH RESPONSE TIME ALERT: {}ms", report.getAverageResponseTime());
        }
    }
    
    private void cleanupTraceData() {
        // In a real implementation, this would clean up old trace data
        LOGGER.debug("Cleaning up old trace data");
    }
    
    private double getCounterValue(String counterName) {
        Counter counter = counters.get(counterName);
        return counter != null ? counter.count() : 0.0;
    }
    
    /**
     * Update active connections count
     */
    public void updateActiveConnections(int count) {
        this.activeConnections = count;
    }
    
    /**
     * Update average response time
     */
    public void updateAverageResponseTime(double responseTime) {
        this.averageResponseTime = responseTime;
    }
    
    /**
     * Get Prometheus metrics as string
     */
    public String getPrometheusMetrics() {
        return prometheusMeterRegistry.scrape();
    }
    
    /**
     * Get meter registry for custom metric registration
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
    
    /**
     * Get OpenTelemetry instance for custom tracing
     */
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }
    
    /**
     * Enable or disable APM
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("APM Manager {}", enabled ? "enabled" : "disabled");
    }
    
    @Override
    public void close() {
        LOGGER.info("Shutting down APM Manager");
        enabled = false;
        
        // Clean up resources
        timers.clear();
        counters.clear();
        gauges.clear();
        gaugeValues.clear();
        
        // Shutdown scheduler
        resourceManager.unregisterAndShutdownExecutor("APMManager");
        
        // Close registries
        try {
            meterRegistry.close();
        } catch (Exception e) {
            LOGGER.error("Error closing meter registry", e);
        }
    }
}