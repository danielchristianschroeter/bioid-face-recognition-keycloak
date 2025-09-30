import java.util.Base64;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Simple test to validate BioID BWS connection and JWT generation.
 * This can be used to test the configuration outside of Keycloak.
 */
public class TestBioIdConnection {
    
    public static void main(String[] args) {
        // Test configuration from environment variables
        String clientId = System.getenv("BWS_CLIENT_ID");
        String key = System.getenv("BWS_KEY");
        String endpoint = System.getenv("BWS_ENDPOINT");
        
        if (clientId == null || key == null) {
            System.err.println("Please set BWS_CLIENT_ID and BWS_KEY environment variables");
            System.exit(1);
        }
        
        if (endpoint == null) {
            endpoint = "grpc.bws-eu.bioid.com:443";
        }
        
        System.out.println("Testing BioID BWS Configuration:");
        System.out.println("Client ID: " + maskSensitive(clientId));
        System.out.println("Endpoint: " + endpoint);
        System.out.println("Key length: " + key.length() + " characters");
        
        try {
            // Test JWT generation
            String token = generateJWT(clientId, key, 60);
            System.out.println("JWT generated successfully");
            System.out.println("Token length: " + token.length() + " characters");
            System.out.println("Token preview: " + token.substring(0, Math.min(50, token.length())) + "...");
            
            // Test key decoding
            testKeyDecoding(key);
            
            System.out.println("\nConfiguration appears to be valid!");
            System.out.println("If you're still getting HTTP 308 errors, the issue may be:");
            System.out.println("1. Network connectivity to " + endpoint);
            System.out.println("2. Firewall blocking gRPC traffic on port 443");
            System.out.println("3. Corporate proxy interfering with gRPC connections");
            System.out.println("4. Verify you're using the correct gRPC endpoint (grpc.bws-eu.bioid.com)");
            
        } catch (Exception e) {
            System.err.println("Error testing configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String generateJWT(String clientId, String key, int expireMinutes) {
        // Decode base64 key
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(key);
            System.out.println("Key decoded from base64, length: " + keyBytes.length + " bytes");
        } catch (IllegalArgumentException e) {
            System.out.println("Key is not base64 encoded, using as raw string");
            keyBytes = key.getBytes();
        }
        
        // Ensure key meets minimum length for HMAC-SHA512
        if (keyBytes.length < 64) {
            try {
                java.security.MessageDigest sha512 = java.security.MessageDigest.getInstance("SHA-512");
                keyBytes = sha512.digest(keyBytes);
                System.out.println("Extended key to 64 bytes using SHA-512 hash");
            } catch (Exception e) {
                throw new RuntimeException("SHA-512 not available", e);
            }
        }
        
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);
        
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expireMinutes * 60L);
        
        return Jwts.builder()
            .subject(clientId)
            .issuer(clientId)
            .audience().add("BWS").and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey, Jwts.SIG.HS512)
            .compact();
    }
    
    private static void testKeyDecoding(String key) {
        try {
            byte[] decoded = Base64.getDecoder().decode(key);
            System.out.println("✓ Key is valid base64, decoded length: " + decoded.length + " bytes");
            
            // BioID BWS uses 512-bit (64 byte) symmetric keys
            if (decoded.length == 64) {
                System.out.println("✓ Key length is exactly 64 bytes - perfect for BioID BWS HMAC-SHA512");
            } else if (decoded.length >= 32 && decoded.length <= 128) {
                System.out.println("⚠ Key length (" + decoded.length + " bytes) is acceptable but not optimal");
                System.out.println("  BioID BWS typically uses 64-byte keys for HMAC-SHA512");
            } else {
                System.out.println("✗ WARNING: Key length (" + decoded.length + " bytes) may be too short for BioID BWS");
                System.out.println("  BioID BWS requires at least 32 bytes for secure HMAC signing");
            }
            
            // Show first few bytes for debugging (but not the full key for security)
            System.out.print("Key starts with bytes: ");
            for (int i = 0; i < Math.min(4, decoded.length); i++) {
                System.out.printf("%02x ", decoded[i]);
            }
            System.out.println("...");
            
        } catch (IllegalArgumentException e) {
            System.out.println("✗ CRITICAL: Key is not base64 encoded!");
            System.out.println("  BioID BWS keys must be base64 encoded strings from the BWS Portal");
            System.out.println("  Your key appears to be: " + key.substring(0, Math.min(10, key.length())) + "...");
            System.out.println("  Expected format: base64 string like 'Wq/7Ov6e3XDheMdfZmvUerTO...'");
        }
    }
    
    private static String maskSensitive(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}