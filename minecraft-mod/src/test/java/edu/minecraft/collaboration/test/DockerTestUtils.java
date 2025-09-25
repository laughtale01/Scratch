package edu.minecraft.collaboration.test;

import org.junit.jupiter.api.Assumptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Docker-dependent tests
 */
public class DockerTestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerTestUtils.class);
    private static Boolean dockerAvailable = null;

    /**
     * Check if Docker is available and running
     */
    public static boolean isDockerAvailable() {
        if (dockerAvailable == null) {
            try {
                Process process = Runtime.getRuntime().exec("docker version");
                int exitCode = process.waitFor();
                dockerAvailable = (exitCode == 0);

                if (!dockerAvailable) {
                    LOGGER.warn("Docker is not available or not running");
                }
            } catch (Exception e) {
                LOGGER.warn("Cannot check Docker availability: {}", e.getMessage());
                dockerAvailable = false;
            }
        }
        return dockerAvailable;
    }

    /**
     * Skip test if Docker is not available
     */
    public static void assumeDockerAvailable() {
        Assumptions.assumeTrue(isDockerAvailable(),
            "Skipping test - Docker is not available");
    }
}