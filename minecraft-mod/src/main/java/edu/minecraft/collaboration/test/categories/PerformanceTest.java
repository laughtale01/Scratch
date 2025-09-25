package edu.minecraft.collaboration.test.categories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for performance tests
 * Used to categorize and potentially exclude performance tests from regular test runs
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PerformanceTest {
    /**
     * Optional description of what this performance test measures
     */
    String value() default "";

    /**
     * Expected maximum execution time in milliseconds
     */
    long maxExecutionTime() default Long.MAX_VALUE;

    /**
     * Whether this test should be run in CI/CD pipeline
     */
    boolean runInCI() default false;
}
