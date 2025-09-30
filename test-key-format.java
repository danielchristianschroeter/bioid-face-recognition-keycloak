import java.util.Base64;

/**
 * Simple test to validate BioID BWS key format without external dependencies.
 */
public class TestKeyFormat {
    
    public static void main(String[] args) {
        // Test configuration from environment variables
        String clientId = System.getenv("BWS_CLIENT_ID");
        String key = System.getenv("BWS_KEY");
        
        if (clientId == null || key == null) {
            System.err.println("Please set BWS_CLIENT_ID and BWS_KEY environment variables");
            System.exit(1);
        }
        
        System.out.println("Testing BioID BWS Key Format:");
        System.out.println("Client ID: " + maskSensitive(clientId));
        System.out.println("Key length: " + key.length() + " characters");
        
        testKeyDecoding(key);
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
                System.out.println("⚠ Key length (" + decoded.length + " bytes) is shorter than optimal");
                System.out.println("  Will be extended to 64 bytes using SHA-512 hash for HMAC-SHA512 compatibility");
                
                // Test the extension
                try {
                    java.security.MessageDigest sha512 = java.security.MessageDigest.getInstance("SHA-512");
                    byte[] extended = sha512.digest(decoded);
                    System.out.println("✓ Extended key length: " + extended.length + " bytes");
                } catch (Exception e) {
                    System.out.println("✗ Failed to extend key: " + e.getMessage());
                }
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