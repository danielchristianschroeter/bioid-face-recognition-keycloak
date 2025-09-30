import java.util.Base64;
import java.security.MessageDigest;

/**
 * Test JWT generation logic without external dependencies to debug the authentication issue.
 */
public class TestJwtGeneration {
    
    public static void main(String[] args) {
        // Test configuration from environment variables
        String clientId = System.getenv("BWS_CLIENT_ID");
        String key = System.getenv("BWS_KEY");
        
        if (clientId == null || key == null) {
            System.err.println("Please set BWS_CLIENT_ID and BWS_KEY environment variables");
            System.exit(1);
        }
        
        System.out.println("Testing BioID BWS JWT Generation Logic:");
        System.out.println("Client ID: " + maskSensitive(clientId));
        System.out.println("Key length: " + key.length() + " characters");
        
        try {
            // Test the exact same logic as in BioIdJwtTokenProvider
            testKeyProcessing(clientId, key);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testKeyProcessing(String clientId, String secretKey) throws Exception {
        System.out.println("\n=== Key Processing Test ===");
        
        // Step 1: Base64 decode the key (same as BioIdJwtTokenProvider)
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretKey);
            System.out.println("✓ Successfully decoded base64 BioID key, length: " + keyBytes.length + " bytes");
        } catch (IllegalArgumentException e) {
            System.out.println("✗ Failed to decode BioID key as base64");
            throw new IllegalArgumentException("BioID key must be base64 encoded as provided by BWS Portal", e);
        }
        
        // Step 2: Check if key extension is needed
        System.out.println("✓ Using BioID key directly, length: " + keyBytes.length + " bytes");
        
        if (keyBytes.length < 64) {
            System.out.println("⚠ BioID key is " + keyBytes.length + " bytes, extending to 64 bytes for HMAC-SHA512 compatibility");
            MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
            keyBytes = sha512.digest(keyBytes);
            System.out.println("✓ Extended key to " + keyBytes.length + " bytes");
        } else {
            System.out.println("✓ Key is already " + keyBytes.length + " bytes - perfect for HMAC-SHA512");
        }
        
        // Step 3: Show what would be in the JWT
        System.out.println("\n=== JWT Claims Test ===");
        System.out.println("Subject (sub): " + clientId);
        System.out.println("Issuer (iss): " + clientId);
        System.out.println("Audience (aud): BWS");
        System.out.println("Algorithm: HS512");
        
        // Step 4: Verify Client ID format
        System.out.println("\n=== Client ID Validation ===");
        if (clientId.length() == 24 && clientId.matches("[a-f0-9]+")) {
            System.out.println("✓ Client ID format looks correct (24 hex characters)");
        } else {
            System.out.println("⚠ Client ID format may be unusual:");
            System.out.println("  Length: " + clientId.length() + " (expected: 24)");
            System.out.println("  Format: " + (clientId.matches("[a-f0-9]+") ? "hex" : "non-hex"));
        }
        
        // Step 5: Key fingerprint for debugging
        System.out.println("\n=== Key Fingerprint ===");
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] fingerprint = md5.digest(keyBytes);
        System.out.print("Key fingerprint (MD5): ");
        for (byte b : fingerprint) {
            System.out.printf("%02x", b);
        }
        System.out.println();
        
        System.out.println("\n=== Recommendations ===");
        System.out.println("1. Verify Client ID and Key are from the same BWS Portal entry");
        System.out.println("2. Check that the BWS Portal shows this Client ID as active");
        System.out.println("3. Ensure no extra whitespace in Client ID or Key");
        System.out.println("4. Try regenerating the key in BWS Portal if issue persists");
    }
    
    private static String maskSensitive(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }
}