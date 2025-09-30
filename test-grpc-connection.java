import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;

/**
 * Test gRPC connection to BioID BWS service
 */
public class TestGrpcConnection {
    
    public static void main(String[] args) {
        String endpoint = System.getenv("BWS_ENDPOINT");
        if (endpoint == null) {
            endpoint = "face.bws-eu.bioid.com";
        }
        
        System.out.println("Testing gRPC connection to: " + endpoint);
        
        // Test different connection approaches
        testConnection(endpoint, 443, true, "HTTPS/gRPC-TLS on port 443");
        testConnection(endpoint, 80, false, "HTTP/gRPC on port 80");
        testConnection(endpoint, 8080, false, "HTTP/gRPC on port 8080");
        testConnection(endpoint, 9090, true, "HTTPS/gRPC-TLS on port 9090");
    }
    
    private static void testConnection(String host, int port, boolean useTls, String description) {
        System.out.println("\n--- Testing " + description + " ---");
        
        ManagedChannel channel = null;
        try {
            NettyChannelBuilder builder = NettyChannelBuilder.forTarget(host + ":" + port)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(30, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(4 * 1024 * 1024);
            
            if (useTls) {
                builder.useTransportSecurity();
                System.out.println("Using TLS transport security");
            } else {
                builder.usePlaintext();
                System.out.println("Using plaintext transport");
            }
            
            channel = builder.build();
            
            // Try to connect (this will trigger the actual connection)
            System.out.println("Attempting to connect...");
            
            // Wait for connection to be established or fail
            boolean connected = channel.getState(true) != io.grpc.ConnectivityState.TRANSIENT_FAILURE;
            
            if (connected) {
                System.out.println("✓ Connection established successfully");
                
                // Try to get channel state
                io.grpc.ConnectivityState state = channel.getState(false);
                System.out.println("Channel state: " + state);
                
            } else {
                System.out.println("✗ Connection failed");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Connection failed with error: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("  Cause: " + e.getCause().getMessage());
            }
        } finally {
            if (channel != null) {
                try {
                    channel.shutdown();
                    channel.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}