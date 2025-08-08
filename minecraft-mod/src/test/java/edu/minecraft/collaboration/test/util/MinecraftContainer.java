package edu.minecraft.collaboration.test.util;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Custom TestContainer for Minecraft Forge server
 */
public class MinecraftContainer extends GenericContainer<MinecraftContainer> {
    
    private static final int MINECRAFT_PORT = 25565;
    private static final int WEBSOCKET_PORT = 14711;
    private static final String DEFAULT_IMAGE = "minecraft-collaboration-test:latest";
    
    public MinecraftContainer() {
        this(DockerImageName.parse(DEFAULT_IMAGE));
    }
    
    public MinecraftContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }
    
    public MinecraftContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        
        // Expose ports
        this.addExposedPorts(MINECRAFT_PORT, WEBSOCKET_PORT);
        
        // Set wait strategy - wait for WebSocket health check
        this.waitingFor(
            Wait.forHttp("/health")
                .forPort(WEBSOCKET_PORT)
                .withStartupTimeout(Duration.ofMinutes(3))
        );
        
        // Set memory limits
        this.withEnv("JAVA_OPTS", "-Xmx2G -Xms1G");
        
        // Create command to ensure server starts properly
        this.withCommand("/minecraft/start.sh");
    }
    
    /**
     * Add a mod to the server
     * @param modPath Path to the mod JAR file
     * @return this container instance
     */
    public MinecraftContainer withMod(Path modPath) {
        String containerPath = "/minecraft/mods/" + modPath.getFileName().toString();
        this.withCopyFileToContainer(
            org.testcontainers.utility.MountableFile.forHostPath(modPath),
            containerPath
        );
        return this;
    }
    
    /**
     * Get the Minecraft server address
     * @return Server address in format "host:port"
     */
    public String getServerAddress() {
        return getHost() + ":" + getMappedPort(MINECRAFT_PORT);
    }
    
    /**
     * Get the WebSocket server address
     * @return WebSocket address in format "ws://host:port"
     */
    public String getWebSocketAddress() {
        return "ws://" + getHost() + ":" + getMappedPort(WEBSOCKET_PORT);
    }
    
    /**
     * Get the mapped Minecraft server port
     * @return Mapped port number
     */
    public Integer getMinecraftPort() {
        return getMappedPort(MINECRAFT_PORT);
    }
    
    /**
     * Get the mapped WebSocket port
     * @return Mapped port number
     */
    public Integer getWebSocketPort() {
        return getMappedPort(WEBSOCKET_PORT);
    }
}