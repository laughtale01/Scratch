package edu.minecraft.collaboration.integration;

import edu.minecraft.collaboration.test.DockerTestUtils;
import edu.minecraft.collaboration.test.util.MinecraftContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestContainers integration setup and verification
 * These tests require Docker to be installed and running
 */
@Testcontainers
@EnabledIf("edu.minecraft.collaboration.test.DockerTestUtils#isDockerAvailable")
public class TestContainersSetup {

    @Container
    private static final MinecraftContainer minecraft = createContainer();

    private static MinecraftContainer createContainer() {
        if (!DockerTestUtils.isDockerAvailable()) {
            return null;
        }
        return new MinecraftContainer().withMod(getModPath());
    }

    private static Path getModPath() {
        // Get the built mod JAR path
        return Paths.get("build/libs/minecraft-collaboration-mod-1.0.0-all.jar");
    }

    @BeforeEach
    void checkDocker() {
        DockerTestUtils.assumeDockerAvailable();
    }

    @BeforeAll
    static void setup() {
        if (!DockerTestUtils.isDockerAvailable()) {
            return; // Skip setup if Docker is not available
        }
        // Ensure the container is started
        assertTrue(minecraft.isRunning(), "Minecraft container should be running");
    }
    
    @Test
    void testMinecraftServerIsAccessible() {
        String serverAddress = minecraft.getServerAddress();
        assertNotNull(serverAddress);
        assertTrue(serverAddress.contains(":"));
        
        System.out.println("Minecraft server running at: " + serverAddress);
    }
    
    @Test
    void testWebSocketServerIsAccessible() {
        String wsAddress = minecraft.getWebSocketAddress();
        assertNotNull(wsAddress);
        assertTrue(wsAddress.startsWith("ws://"));
        
        System.out.println("WebSocket server running at: " + wsAddress);
    }
    
    @Test
    void testHealthCheckEndpoint() throws Exception {
        String healthUrl = "http://" + minecraft.getHost() + ":" + 
                          minecraft.getWebSocketPort() + "/health";
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(healthUrl))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "Health check should return 200");
        assertTrue(response.body().contains("status"), 
            "Health response should contain status");
    }
    
    @Test
    void testPortMapping() {
        Integer minecraftPort = minecraft.getMinecraftPort();
        Integer wsPort = minecraft.getWebSocketPort();
        
        assertNotNull(minecraftPort);
        assertNotNull(wsPort);
        assertNotEquals(minecraftPort, wsPort);
        
        assertTrue(minecraftPort > 0 && minecraftPort < 65536);
        assertTrue(wsPort > 0 && wsPort < 65536);
    }
}