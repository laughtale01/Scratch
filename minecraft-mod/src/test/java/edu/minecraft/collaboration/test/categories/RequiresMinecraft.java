package edu.minecraft.collaboration.test.categories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

/**
 * Marks tests that require a full Minecraft environment to run.
 * These tests will be skipped if the minecraft.test.skip property is set.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag("requires-minecraft")
@DisabledIfSystemProperty(named = "minecraft.test.skip", matches = "true")
public @interface RequiresMinecraft {
}