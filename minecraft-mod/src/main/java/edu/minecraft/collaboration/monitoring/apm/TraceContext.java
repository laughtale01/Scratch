package edu.minecraft.collaboration.monitoring.apm;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * Trace context interface for distributed tracing
 */
public interface TraceContext extends AutoCloseable {

    /**
     * Add an attribute to the trace span
     */
    void addAttribute(String key, String value);

    /**
     * Add an attribute to the trace span
     */
    void addAttribute(String key, long value);

    /**
     * Add an attribute to the trace span
     */
    void addAttribute(String key, double value);

    /**
     * Add an attribute to the trace span
     */
    void addAttribute(String key, boolean value);

    /**
     * Set the status of the trace span
     */
    void setStatus(StatusCode status, String description);

    /**
     * Add an event to the trace span
     */
    void addEvent(String name);

    /**
     * Add an event with attributes to the trace span
     */
    void addEvent(String name, String attributeKey, String attributeValue);

    /**
     * Set an error on the trace span
     */
    void setError(Throwable throwable);

    /**
     * Get the trace ID
     */
    String getTraceId();

    /**
     * Get the span ID
     */
    String getSpanId();

    /**
     * Close the trace context and finish the span
     */
    @Override
    void close();
}

/**
 * OpenTelemetry implementation of TraceContext
 */
class OpenTelemetryTraceContext implements TraceContext {

    private final Span span;
    private final Scope scope;
    private final Context context;

    public OpenTelemetryTraceContext(Span span, Scope scope) {
        this.span = span;
        this.scope = scope;
        this.context = Context.current();
    }

    @Override
    public void addAttribute(String key, String value) {
        span.setAttribute(key, value);
    }

    @Override
    public void addAttribute(String key, long value) {
        span.setAttribute(key, value);
    }

    @Override
    public void addAttribute(String key, double value) {
        span.setAttribute(key, value);
    }

    @Override
    public void addAttribute(String key, boolean value) {
        span.setAttribute(key, value);
    }

    @Override
    public void setStatus(StatusCode status, String description) {
        span.setStatus(status, description);
    }

    @Override
    public void addEvent(String name) {
        span.addEvent(name);
    }

    @Override
    public void addEvent(String name, String attributeKey, String attributeValue) {
        span.addEvent(name, io.opentelemetry.api.common.Attributes.of(
            io.opentelemetry.api.common.AttributeKey.stringKey(attributeKey), attributeValue));
    }

    @Override
    public void setError(Throwable throwable) {
        span.setStatus(StatusCode.ERROR, throwable.getMessage());
        span.recordException(throwable);
    }

    @Override
    public String getTraceId() {
        return span.getSpanContext().getTraceId();
    }

    @Override
    public String getSpanId() {
        return span.getSpanContext().getSpanId();
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void close() {
        try {
            scope.close();
        } finally {
            span.end();
        }
    }
}

/**
 * No-op implementation for when tracing is disabled
 */
class NoOpTraceContext implements TraceContext {

    @Override
    public void addAttribute(String key, String value) {
        // No-op
    }

    @Override
    public void addAttribute(String key, long value) {
        // No-op
    }

    @Override
    public void addAttribute(String key, double value) {
        // No-op
    }

    @Override
    public void addAttribute(String key, boolean value) {
        // No-op
    }

    @Override
    public void setStatus(StatusCode status, String description) {
        // No-op
    }

    @Override
    public void addEvent(String name) {
        // No-op
    }

    @Override
    public void addEvent(String name, String attributeKey, String attributeValue) {
        // No-op
    }

    @Override
    public void setError(Throwable throwable) {
        // No-op
    }

    @Override
    public String getTraceId() {
        return "no-trace";
    }

    @Override
    public String getSpanId() {
        return "no-span";
    }

    @Override
    public void close() {
        // No-op
    }
}
