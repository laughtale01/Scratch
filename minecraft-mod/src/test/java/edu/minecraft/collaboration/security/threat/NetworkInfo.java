package edu.minecraft.collaboration.security.threat;

/**
 * Network information for threat detection tests
 */
public class NetworkInfo {
    private final String ipAddress;
    private final int port;
    private final String protocol;
    private final boolean isSecure;
    private final boolean isInternal;
    
    public NetworkInfo(String ipAddress, int port, String protocol) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.protocol = protocol;
        this.isSecure = protocol != null && protocol.toLowerCase().contains("https");
        this.isInternal = ipAddress != null && ipAddress.startsWith("192.168.");
    }
    
    public NetworkInfo(String ipAddress, String portStr, String protocol, boolean isSecure, boolean isInternal) {
        this.ipAddress = ipAddress;
        this.port = Integer.parseInt(portStr);
        this.protocol = protocol;
        this.isSecure = isSecure;
        this.isInternal = isInternal;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public boolean isSecure() {
        return isSecure;
    }
    
    public boolean isInternal() {
        return isInternal;
    }
}