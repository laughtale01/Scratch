package edu.minecraft.collaboration.util;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for secure file operations to prevent path traversal attacks
 */
public final class FileSecurityUtils {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    /**
     * Private constructor to prevent instantiation
     */
    private FileSecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Allowed base directories for file operations
    private static final String[] ALLOWED_BASE_DIRS = {
        "metrics",
        "offline_data",
        "config",
        "logs"
    };

    /**
     * Validate and sanitize a file path to prevent path traversal attacks
     * @param basePath The base directory path
     * @param fileName The file name to validate
     * @return A safe File object or null if validation fails
     */
    public static File getSafeFile(String basePath, String fileName) {
        if (basePath == null || fileName == null) {
            LOGGER.error("Base path or file name is null");
            return null;
        }

        // Check if base path is in allowed list
        boolean isAllowed = false;
        for (String allowedDir : ALLOWED_BASE_DIRS) {
            if (basePath.equals(allowedDir) || basePath.startsWith(allowedDir + File.separator)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            LOGGER.error("Base path '{}' is not in allowed directories", basePath);
            return null;
        }

        // Sanitize file name - remove any path traversal attempts
        String sanitizedFileName = sanitizeFileName(fileName);
        if (sanitizedFileName == null) {
            return null;
        }

        try {
            // Create base directory File object
            File baseDir = new File(basePath);

            // Get canonical path of base directory
            String baseDirCanonical = baseDir.getCanonicalPath();

            // Create the target file
            File targetFile = new File(baseDir, sanitizedFileName);

            // Get canonical path of target file
            String targetCanonical = targetFile.getCanonicalPath();

            // Ensure target file is within base directory
            if (!targetCanonical.startsWith(baseDirCanonical + File.separator)) {
                LOGGER.error("Path traversal attempt detected: {} not under {}", targetCanonical, baseDirCanonical);
                return null;
            }

            return targetFile;

        } catch (IOException e) {
            LOGGER.error("Error validating file path", e);
            return null;
        }
    }

    /**
     * Sanitize a file name to remove dangerous characters and path traversal attempts
     * @param fileName The file name to sanitize
     * @return The sanitized file name or null if invalid
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            LOGGER.error("File name is null or empty");
            return null;
        }

        // Remove any path separators
        String sanitized = fileName.replace("..", "")
                                 .replace("/", "")
                                 .replace("\\", "")
                                 .replace(":", "")
                                 .replace("*", "")
                                 .replace("?", "")
                                 .replace("\"", "")
                                 .replace("<", "")
                                 .replace(">", "")
                                 .replace("|", "")
                                 .replace("\0", "");

        // Check if anything remains
        if (sanitized.isEmpty()) {
            LOGGER.error("File name contains only invalid characters");
            return null;
        }

        // Limit file name length
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }

        return sanitized;
    }

    /**
     * Create a safe directory if it doesn't exist
     * @param dirPath The directory path to create
     * @return true if directory exists or was created successfully
     */
    public static boolean ensureSafeDirectory(String dirPath) {
        if (dirPath == null) {
            return false;
        }

        // Check if directory is in allowed list
        boolean isAllowed = false;
        for (String allowedDir : ALLOWED_BASE_DIRS) {
            if (dirPath.equals(allowedDir) || dirPath.startsWith(allowedDir + File.separator)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            LOGGER.error("Directory path '{}' is not in allowed directories", dirPath);
            return false;
        }

        File dir = new File(dirPath);
        if (!dir.exists()) {
            return dir.mkdirs();
        }

        return dir.isDirectory();
    }

    /**
     * Validate that a path is within the game's data directory
     * @param path The path to validate
     * @return true if the path is safe
     */
    public static boolean isPathSafe(Path path) {
        if (path == null) {
            return false;
        }

        try {
            // Get the canonical path
            Path canonicalPath = path.toAbsolutePath().normalize();

            // Get the current working directory
            Path workingDir = Paths.get("").toAbsolutePath();

            // Ensure the path is within the working directory
            return canonicalPath.startsWith(workingDir);

        } catch (Exception e) {
            LOGGER.error("Error validating path safety", e);
            return false;
        }
    }
}
