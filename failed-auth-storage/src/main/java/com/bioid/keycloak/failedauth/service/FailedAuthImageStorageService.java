package com.bioid.keycloak.failedauth.service;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.failedauth.config.FailedAuthConfiguration;
import com.bioid.keycloak.failedauth.entity.*;
import com.bioid.keycloak.failedauth.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import org.hibernate.cfg.Configuration;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for managing failed authentication image storage.
 * 
 * This service handles:
 * - Storing failed authentication attempts with images and metadata
 * - Retrieving and filtering failed attempts
 * - Enrolling selected images to improve templates
 * - Cleanup and retention management
 * - Security and encryption
 * - Audit logging
 */
public class FailedAuthImageStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(FailedAuthImageStorageService.class);
    
    private final FailedAuthConfiguration config;
    private final EncryptionService encryptionService;
    private final ImageProcessingService imageProcessingService;
    private final ObjectMapper objectMapper;
    private static EntityManagerFactory emf;
    
    public FailedAuthImageStorageService(FailedAuthConfiguration config) {
        this.config = config;
        this.encryptionService = new EncryptionService();
        this.imageProcessingService = new ImageProcessingService(config);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Initialize EntityManagerFactory if not already done
        initializeEntityManagerFactory();
        
        logger.info("FailedAuthImageStorageService initialized");
        config.logConfiguration();
    }
    
    /**
     * Store a failed authentication attempt with images and metadata.
     * 
     * @param session Keycloak session
     * @param realm Realm model
     * @param user User model
     * @param images List of base64-encoded images
     * @param failureReason Reason for failure
     * @param verificationScore Verification score
     * @param verificationThreshold Verification threshold
     * @param livenessMode Liveness detection mode
     * @param livenessScore Liveness score
     * @param livenessPassed Whether liveness passed
     * @param challengeDirection Challenge direction (if applicable)
     * @param retryAttempt Current retry attempt number
     * @param maxRetries Maximum retry attempts
     * @param sessionId Session ID
     * @param ipAddress IP address
     * @param userAgent User agent
     * @return Attempt ID (UUID)
     * @throws FailedAuthStorageException if storage fails
     */
    public String storeFailedAttempt(
            KeycloakSession session,
            RealmModel realm,
            UserModel user,
            List<String> images,
            String failureReason,
            Double verificationScore,
            Double verificationThreshold,
            String livenessMode,
            Double livenessScore,
            Boolean livenessPassed,
            String challengeDirection,
            Integer retryAttempt,
            Integer maxRetries,
            String sessionId,
            String ipAddress,
            String userAgent) throws FailedAuthStorageException {
        
        if (!config.isStorageEnabled()) {
            logger.debug("Failed auth storage is disabled");
            return null;
        }
        
        try {
            logger.info("Storing failed authentication attempt for user: {}", user.getUsername());
            
            // Check if user has disabled storage
            FailedAuthUserPreferencesEntity prefs = getUserPreferences(session, user.getId(), realm.getId());
            if (prefs != null && !prefs.getStorageEnabled()) {
                logger.debug("User {} has disabled failed auth storage", user.getUsername());
                return null;
            }
            
            // Check max attempts per user
            long existingCount = countUserAttempts(session, user.getId());
            if (existingCount >= config.getMaxAttemptsPerUser()) {
                logger.warn("User {} has reached max attempts limit: {}", 
                    user.getUsername(), config.getMaxAttemptsPerUser());
                // Delete oldest attempt to make room
                deleteOldestAttempt(session, user.getId());
            }
            
            // Get class ID from user
            Long classId = getClassIdFromUser(user);
            if (classId == null) {
                logger.warn("User {} has no class ID, cannot store failed attempt", user.getUsername());
                return null;
            }
            
            // Create attempt entity
            String attemptId = UUID.randomUUID().toString();
            FailedAuthAttemptEntity attempt = new FailedAuthAttemptEntity(
                attemptId, user.getId(), realm.getId(), user.getUsername(), classId
            );
            
            // Set timestamps
            Instant now = Instant.now();
            attempt.setTimestamp(now);
            attempt.setExpiresAt(now.plus(config.getRetentionDays(), ChronoUnit.DAYS));
            
            // Set failure details
            attempt.setFailureReason(failureReason);
            attempt.setVerificationScore(verificationScore);
            attempt.setVerificationThreshold(verificationThreshold);
            if (verificationScore != null && verificationThreshold != null) {
                attempt.setScoreDifference(verificationScore - verificationThreshold);
            }
            
            // Set liveness information
            attempt.setLivenessMode(livenessMode);
            attempt.setLivenessScore(livenessScore);
            attempt.setLivenessThreshold(config.isRequireLivenessPass() ? 0.5 : null);
            attempt.setLivenessPassed(livenessPassed);
            attempt.setChallengeDirection(challengeDirection);
            
            // Set retry information
            attempt.setRetryAttempt(retryAttempt);
            attempt.setMaxRetries(maxRetries);
            
            // Set session information
            attempt.setSessionId(sessionId);
            attempt.setIpAddress(ipAddress);
            attempt.setUserAgent(userAgent);
            
            // Set image count
            attempt.setImageCount(images.size());
            
            // Process and store images
            List<Double> qualityScores = new ArrayList<>();
            EntityManager em = getEntityManager(session);
            
            for (int i = 0; i < images.size(); i++) {
                try {
                    FailedAuthImageEntity imageEntity = processAndStoreImage(
                        attempt, i, images.get(i)
                    );
                    attempt.addImage(imageEntity);
                    
                    if (imageEntity.getQualityScore() != null) {
                        qualityScores.add(imageEntity.getQualityScore());
                    }
                } catch (Exception e) {
                    logger.error("Failed to process image {}: {}", i, e.getMessage());
                    // Continue with other images
                }
            }
            
            // Calculate average quality score
            if (!qualityScores.isEmpty()) {
                double avgQuality = qualityScores.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                attempt.setAvgQualityScore(avgQuality);
            }
            
            // Set security flags
            attempt.setEncrypted(config.isEncryptImages());
            attempt.setIntegrityVerified(config.isVerifyIntegrity());
            
            // Persist attempt
            em.persist(attempt);
            em.flush();
            
            // Update user preferences statistics
            if (prefs == null) {
                prefs = new FailedAuthUserPreferencesEntity(user.getId(), realm.getId());
                em.persist(prefs);
            }
            prefs.incrementTotalAttempts();
            em.merge(prefs);
            
            logger.info("Successfully stored failed attempt {} for user {}", 
                attemptId, user.getUsername());
            
            // Check if user should be notified
            // Check if user should be notified
            if (prefs.shouldNotify()) {
                logger.info("User {} should be notified about failed attempts", user.getUsername());
                // Note: Notification implementation is handled by FailedAuthNotificationService
                // which should be called by a scheduled job or event listener
                // This is intentionally decoupled to avoid blocking the authentication flow
            }
            
            return attemptId;
            
        } catch (Exception e) {
            logger.error("Failed to store failed authentication attempt", e);
            throw new FailedAuthStorageException("Failed to store attempt", e);
        }
    }
    
    /**
     * Process and store a single image.
     */
    private FailedAuthImageEntity processAndStoreImage(
            FailedAuthAttemptEntity attempt,
            int imageIndex,
            String base64Image) throws Exception {
        
        // Decode base64
        byte[] imageBytes = imageProcessingService.decodeBase64Image(base64Image);
        
        // Validate image
        if (!imageProcessingService.validateImage(imageBytes)) {
            throw new Exception("Invalid image data");
        }
        
        if (!imageProcessingService.validateImageSize(imageBytes)) {
            throw new Exception("Image size exceeds maximum allowed");
        }
        
        // Get image properties
        int[] dimensions = imageProcessingService.getImageDimensions(imageBytes);
        String format = imageProcessingService.detectImageFormat(imageBytes);
        
        // Create thumbnail if enabled
        byte[] thumbnailBytes = null;
        if (config.isIncludeThumbnails()) {
            try {
                thumbnailBytes = imageProcessingService.createThumbnail(imageBytes);
            } catch (Exception e) {
                logger.warn("Failed to create thumbnail for image {}: {}", imageIndex, e.getMessage());
            }
        }
        
        // Encrypt images if enabled
        byte[] encryptedImage = imageBytes;
        byte[] encryptedThumbnail = thumbnailBytes;
        String checksum = null;
        
        if (config.isEncryptImages()) {
            encryptedImage = encryptionService.encrypt(imageBytes);
            if (thumbnailBytes != null) {
                encryptedThumbnail = encryptionService.encrypt(thumbnailBytes);
            }
        }
        
        // Calculate checksum if integrity verification enabled
        if (config.isVerifyIntegrity()) {
            checksum = encryptionService.calculateChecksum(imageBytes);
        }
        
        // Create image entity
        FailedAuthImageEntity imageEntity = new FailedAuthImageEntity(
            attempt, imageIndex, encryptedImage, encryptedImage.length
        );
        
        imageEntity.setThumbnailData(encryptedThumbnail);
        imageEntity.setWidth(dimensions[0]);
        imageEntity.setHeight(dimensions[1]);
        imageEntity.setFormat(format);
        imageEntity.setCaptureTimestamp(Instant.now());
        
        // Note: Face detection results are not available at storage time
        // These will be populated when BWS verification response is available
        // For now, we store the image and mark it as requiring analysis
        imageEntity.setFaceFound(null); // Unknown until analyzed
        imageEntity.setFaceCount(null); // Unknown until analyzed
        imageEntity.setQualityScore(null); // Unknown until analyzed
        imageEntity.setRecommendedForEnrollment(false); // Conservative default
        
        return imageEntity;
    }
    
    /**
     * Initialize the EntityManagerFactory programmatically without persistence.xml.
     * This uses a separate database connection from Keycloak's main database.
     * Configuration is read from environment variables.
     */
    private static synchronized void initializeEntityManagerFactory() {
        if (emf == null || !emf.isOpen()) {
            try {
                logger.info("Initializing EntityManagerFactory for failed-auth-storage");
                
                // Build properties from environment variables
                Map<String, String> properties = new HashMap<>();
                
                // Database connection
                String dbUrl = System.getenv().getOrDefault("FAILED_AUTH_DB_URL", 
                    "jdbc:postgresql://localhost:5433/failed_auth");
                String dbUser = System.getenv().getOrDefault("FAILED_AUTH_DB_USER", "failed_auth");
                String dbPassword = System.getenv().getOrDefault("FAILED_AUTH_DB_PASSWORD", "failed_auth_password");
                
                properties.put("jakarta.persistence.jdbc.url", dbUrl);
                properties.put("jakarta.persistence.jdbc.user", dbUser);
                properties.put("jakarta.persistence.jdbc.password", dbPassword);
                properties.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
                
                // Hibernate settings
                properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
                properties.put("hibernate.hbm2ddl.auto", 
                    System.getenv().getOrDefault("FAILED_AUTH_DB_SCHEMA_UPDATE", "update"));
                properties.put("hibernate.show_sql", 
                    System.getenv().getOrDefault("FAILED_AUTH_DB_SHOW_SQL", "false"));
                properties.put("hibernate.format_sql", "false");
                
                // Connection settings - use Hibernate's built-in connection handling
                // (no external pool needed, compatible with Keycloak/Quarkus runtime)
                properties.put("hibernate.connection.pool_size", 
                    System.getenv().getOrDefault("FAILED_AUTH_DB_POOL_SIZE", "10"));
                
                // Entity classes
                properties.put("hibernate.archive.autodetection", "class");
                
                logger.info("Creating EntityManagerFactory with database URL: {}", dbUrl);
                
                // Create using Hibernate directly to avoid persistence.xml
                org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
                
                // Add entity classes
                configuration.addAnnotatedClass(FailedAuthAttemptEntity.class);
                configuration.addAnnotatedClass(FailedAuthImageEntity.class);
                configuration.addAnnotatedClass(FailedAuthAuditLogEntity.class);
                configuration.addAnnotatedClass(FailedAuthUserPreferencesEntity.class);
                configuration.addAnnotatedClass(FailedAuthRealmConfigEntity.class);
                
                // Set properties
                properties.forEach(configuration::setProperty);
                
                // Build EntityManagerFactory
                emf = configuration.buildSessionFactory().unwrap(EntityManagerFactory.class);
                
                logger.info("EntityManagerFactory initialized successfully");
            } catch (Exception e) {
                logger.error("Failed to initialize EntityManagerFactory", e);
                throw new RuntimeException("Failed to initialize failed auth storage", e);
            }
        }
    }
    
    /**
     * Get EntityManager from our persistence unit.
     * Each call creates a new EntityManager that must be closed by the caller.
     */
    private EntityManager getEntityManager(KeycloakSession session) {
        if (emf == null || !emf.isOpen()) {
            initializeEntityManagerFactory();
        }
        return emf.createEntityManager();
    }
    
    /**
     * Shutdown the EntityManagerFactory.
     * Should be called when the service is no longer needed.
     */
    public static synchronized void shutdown() {
        if (emf != null && emf.isOpen()) {
            logger.info("Shutting down EntityManagerFactory");
            emf.close();
            emf = null;
        }
    }
    
    /**
     * Get user preferences, creating default if not exists.
     */
    private FailedAuthUserPreferencesEntity getUserPreferences(
            KeycloakSession session, String userId, String realmId) {
        EntityManager em = getEntityManager(session);
        return em.find(FailedAuthUserPreferencesEntity.class, userId);
    }
    
    /**
     * Count existing attempts for a user.
     */
    private long countUserAttempts(KeycloakSession session, String userId) {
        EntityManager em = getEntityManager(session);
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(a) FROM FailedAuthAttemptEntity a WHERE a.userId = :userId AND a.enrolled = false",
            Long.class
        );
        query.setParameter("userId", userId);
        return query.getSingleResult();
    }
    
    /**
     * Delete oldest unenrolled attempt for a user.
     */
    private void deleteOldestAttempt(KeycloakSession session, String userId) {
        EntityManager em = getEntityManager(session);
        TypedQuery<FailedAuthAttemptEntity> query = em.createQuery(
            "SELECT a FROM FailedAuthAttemptEntity a WHERE a.userId = :userId AND a.enrolled = false ORDER BY a.timestamp ASC",
            FailedAuthAttemptEntity.class
        );
        query.setParameter("userId", userId);
        query.setMaxResults(1);
        
        List<FailedAuthAttemptEntity> results = query.getResultList();
        if (!results.isEmpty()) {
            FailedAuthAttemptEntity oldest = results.get(0);
            logger.info("Deleting oldest attempt {} to make room for new attempt", oldest.getAttemptId());
            em.remove(oldest);
        }
    }
    
    /**
     * Get class ID from user attributes.
     */
    private Long getClassIdFromUser(UserModel user) {
        String classIdStr = user.getFirstAttribute("classId");
        if (classIdStr != null && !classIdStr.isEmpty()) {
            try {
                return Long.parseLong(classIdStr);
            } catch (NumberFormatException e) {
                logger.warn("Invalid classId format for user {}: {}", user.getUsername(), classIdStr);
            }
        }
        return null;
    }
    
    /**
     * Get failed attempts for a user with pagination and filtering.
     * 
     * @param session Keycloak session
     * @param userId User ID
     * @param enrolledFilter Filter by enrollment status (null = all)
     * @param minQuality Minimum quality score filter
     * @param page Page number (0-based)
     * @param pageSize Page size
     * @return List of attempt entities
     */
    public List<FailedAuthAttemptEntity> getFailedAttempts(
            KeycloakSession session,
            String userId,
            Boolean enrolledFilter,
            Double minQuality,
            int page,
            int pageSize) {
        
        EntityManager em = getEntityManager(session);
        
        StringBuilder queryStr = new StringBuilder(
            "SELECT a FROM FailedAuthAttemptEntity a WHERE a.userId = :userId"
        );
        
        if (enrolledFilter != null) {
            queryStr.append(" AND a.enrolled = :enrolled");
        }
        
        if (minQuality != null) {
            queryStr.append(" AND a.avgQualityScore >= :minQuality");
        }
        
        queryStr.append(" ORDER BY a.timestamp DESC");
        
        TypedQuery<FailedAuthAttemptEntity> query = em.createQuery(
            queryStr.toString(), FailedAuthAttemptEntity.class
        );
        
        query.setParameter("userId", userId);
        if (enrolledFilter != null) {
            query.setParameter("enrolled", enrolledFilter);
        }
        if (minQuality != null) {
            query.setParameter("minQuality", minQuality);
        }
        
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);
        
        return query.getResultList();
    }
    
    /**
     * Get a specific failed attempt by ID.
     * 
     * @param session Keycloak session
     * @param attemptId Attempt ID
     * @param userId User ID (for authorization)
     * @return Attempt entity
     * @throws AttemptNotFoundException if not found
     * @throws UnauthorizedAccessException if user doesn't own attempt
     */
    public FailedAuthAttemptEntity getAttempt(
            KeycloakSession session,
            String attemptId,
            String userId) throws AttemptNotFoundException, UnauthorizedAccessException {
        
        EntityManager em = getEntityManager(session);
        FailedAuthAttemptEntity attempt = em.find(FailedAuthAttemptEntity.class, attemptId);
        
        if (attempt == null) {
            throw new AttemptNotFoundException(attemptId);
        }
        
        if (!attempt.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException(userId, attemptId);
        }
        
        return attempt;
    }
    
    /**
     * Get decrypted image data.
     * 
     * @param session Keycloak session
     * @param attemptId Attempt ID
     * @param imageIndex Image index
     * @param userId User ID (for authorization)
     * @param thumbnail Whether to get thumbnail or full image
     * @return Decrypted image bytes
     * @throws Exception if retrieval fails
     */
    public byte[] getImage(
            KeycloakSession session,
            String attemptId,
            int imageIndex,
            String userId,
            boolean thumbnail) throws Exception {
        
        FailedAuthAttemptEntity attempt = getAttempt(session, attemptId, userId);
        
        FailedAuthImageEntity image = attempt.getImages().stream()
            .filter(img -> img.getImageIndex() == imageIndex)
            .findFirst()
            .orElseThrow(() -> new Exception("Image not found: " + imageIndex));
        
        byte[] encryptedData = thumbnail ? image.getThumbnailData() : image.getImageData();
        
        if (encryptedData == null) {
            throw new Exception("Image data not available");
        }
        
        // Decrypt if encrypted
        if (attempt.getEncrypted()) {
            return encryptionService.decrypt(encryptedData);
        }
        
        return encryptedData;
    }
    
    /**
     * Delete a failed attempt.
     * 
     * @param session Keycloak session
     * @param attemptId Attempt ID
     * @param userId User ID (for authorization)
     * @throws Exception if deletion fails
     */
    public void deleteAttempt(
            KeycloakSession session,
            String attemptId,
            String userId) throws Exception {
        
        FailedAuthAttemptEntity attempt = getAttempt(session, attemptId, userId);
        
        EntityManager em = getEntityManager(session);
        
        // Create audit log
        FailedAuthAuditLogEntity auditLog = new FailedAuthAuditLogEntity(
            attempt, userId, "DELETE", userId
        );
        em.persist(auditLog);
        
        // Update user preferences
        FailedAuthUserPreferencesEntity prefs = getUserPreferences(session, userId, attempt.getRealmId());
        if (prefs != null) {
            prefs.incrementDeletedAttempts();
            em.merge(prefs);
        }
        
        // Delete attempt (cascade will delete images and audit logs)
        em.remove(attempt);
        
        logger.info("Deleted failed attempt {} for user {}", attemptId, userId);
    }
    
    /**
     * Cleanup expired attempts.
     * 
     * @param session Keycloak session
     * @return Number of attempts cleaned up
     */
    public int cleanupExpiredAttempts(KeycloakSession session) {
        if (!config.isAutoCleanupEnabled()) {
            return 0;
        }
        
        EntityManager em = getEntityManager(session);
        
        TypedQuery<FailedAuthAttemptEntity> query = em.createQuery(
            "SELECT a FROM FailedAuthAttemptEntity a WHERE a.expiresAt < :now",
            FailedAuthAttemptEntity.class
        );
        query.setParameter("now", Instant.now());
        query.setMaxResults(config.getCleanupBatchSize());
        
        List<FailedAuthAttemptEntity> expired = query.getResultList();
        
        for (FailedAuthAttemptEntity attempt : expired) {
            em.remove(attempt);
        }
        
        logger.info("Cleaned up {} expired failed authentication attempts", expired.size());
        
        return expired.size();
    }
    
    /**
     * Enroll selected images from a failed attempt to improve template.
     * 
     * @param session Keycloak session
     * @param realm Realm model
     * @param user User model
     * @param attemptId Attempt ID
     * @param imageIndices List of image indices to enroll
     * @param bioIdClient BioID client for enrollment
     * @return Enrollment result with template status
     * @throws EnrollmentException if enrollment fails
     */
    public EnrollmentResult enrollFailedAttempt(
            KeycloakSession session,
            RealmModel realm,
            UserModel user,
            String attemptId,
            List<Integer> imageIndices,
            BioIdClient bioIdClient) throws EnrollmentException {
        
        try {
            logger.info("Enrolling failed attempt {} for user {}", attemptId, user.getUsername());
            
            // Get attempt
            FailedAuthAttemptEntity attempt = getAttempt(session, attemptId, user.getId());
            
            if (attempt.getEnrolled()) {
                throw new EnrollmentException("Attempt already enrolled", "ALREADY_ENROLLED");
            }
            
            // Validate image indices
            if (imageIndices.isEmpty()) {
                throw new EnrollmentException("No images selected", "NO_IMAGES_SELECTED");
            }
            
            if (imageIndices.size() > config.getEnrollMaxImagesPerRequest()) {
                throw new EnrollmentException(
                    "Too many images selected (max: " + config.getEnrollMaxImagesPerRequest() + ")",
                    "TOO_MANY_IMAGES"
                );
            }
            
            // Get and decrypt selected images
            List<byte[]> imagesToEnroll = new ArrayList<>();
            List<FailedAuthImageEntity> selectedImages = new ArrayList<>();
            
            for (Integer index : imageIndices) {
                FailedAuthImageEntity imageEntity = attempt.getImages().stream()
                    .filter(img -> img.getImageIndex().equals(index))
                    .findFirst()
                    .orElseThrow(() -> new EnrollmentException(
                        "Image not found: " + index, "IMAGE_NOT_FOUND"
                    ));
                
                // Verify quality if enabled
                if (config.isEnrollVerifyBeforeEnroll()) {
                    if (imageEntity.getQualityScore() == null || 
                        imageEntity.getQualityScore() < config.getMinEnrollQualityScore()) {
                        throw new EnrollmentException(
                            "Image " + index + " quality too low for enrollment",
                            "QUALITY_TOO_LOW"
                        );
                    }
                }
                
                // Decrypt image
                byte[] imageData = imageEntity.getImageData();
                if (attempt.getEncrypted()) {
                    imageData = encryptionService.decrypt(imageData);
                }
                
                imagesToEnroll.add(imageData);
                selectedImages.add(imageEntity);
            }
            
            // Convert to base64 for BWS API
            List<String> base64Images = imagesToEnroll.stream()
                .map(bytes -> Base64.getEncoder().encodeToString(bytes))
                .collect(Collectors.toList());
            
            // Call BWS Enroll API
            logger.info("Calling BWS Enroll API with {} images for classId {}", 
                base64Images.size(), attempt.getClassId());
            
            // Perform actual BWS enrollment
            BioIdClient.EnrollmentResult enrollmentResult;
            try {
                enrollmentResult = bioIdClient.enrollFaceWithMultipleImages(
                    attempt.getClassId(), 
                    base64Images
                );
            } catch (Exception e) {
                logger.error("BWS enrollment failed for classId {}: {}", 
                    attempt.getClassId(), e.getMessage(), e);
                throw new EnrollmentException(
                    "BWS enrollment failed: " + e.getMessage(), 
                    "BWS_ENROLLMENT_FAILED"
                );
            }
            
            if (!enrollmentResult.isAvailable()) {
                throw new EnrollmentException(
                    "Template not available after enrollment", 
                    "TEMPLATE_NOT_AVAILABLE"
                );
            }
            
            int enrolledImages = enrollmentResult.getEnrolledImages();
            int newFeatureVectors = enrollmentResult.getFeatureVectors();
            
            // Update attempt entity
            EntityManager em = getEntityManager(session);
            attempt.setEnrolled(true);
            attempt.setEnrolledAt(Instant.now());
            attempt.setEnrolledBy(user.getId());
            attempt.setEnrolledImageIndices(objectMapper.writeValueAsString(imageIndices));
            
            // Create enrollment result JSON
            Map<String, Object> enrollmentResultMap = new HashMap<>();
            enrollmentResultMap.put("success", true);
            enrollmentResultMap.put("enrolledImages", enrolledImages);
            enrollmentResultMap.put("newFeatureVectors", newFeatureVectors);
            enrollmentResultMap.put("timestamp", Instant.now().toString());
            attempt.setEnrollmentResult(objectMapper.writeValueAsString(enrollmentResultMap));
            
            em.merge(attempt);
            
            // Create audit log
            FailedAuthAuditLogEntity auditLog = new FailedAuthAuditLogEntity(
                attempt, user.getId(), "ENROLL", user.getId()
            );
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("imageIndices", imageIndices);
            auditDetails.put("enrolledImages", enrolledImages);
            auditLog.setDetails(objectMapper.writeValueAsString(auditDetails));
            em.persist(auditLog);
            
            // Update user preferences
            FailedAuthUserPreferencesEntity prefs = getUserPreferences(session, user.getId(), realm.getId());
            if (prefs != null) {
                prefs.incrementEnrolledAttempts();
                em.merge(prefs);
            }
            
            logger.info("Successfully enrolled {} images from attempt {} for user {}", 
                enrolledImages, attemptId, user.getUsername());
            
            // Return result
            EnrollmentResult result = new EnrollmentResult();
            result.setSuccess(true);
            result.setAttemptId(attemptId);
            result.setEnrolledImages(enrolledImages);
            result.setNewFeatureVectors(newFeatureVectors);
            result.setTimestamp(Instant.now());
            
            return result;
            
        } catch (EnrollmentException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to enroll failed attempt", e);
            throw new EnrollmentException("Enrollment failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get statistics for a user's failed attempts.
     * 
     * @param session Keycloak session
     * @param userId User ID
     * @return Statistics object
     */
    public FailedAttemptStatistics getStatistics(KeycloakSession session, String userId) {
        EntityManager em = getEntityManager(session);
        
        // Get total count
        TypedQuery<Long> totalQuery = em.createQuery(
            "SELECT COUNT(a) FROM FailedAuthAttemptEntity a WHERE a.userId = :userId",
            Long.class
        );
        totalQuery.setParameter("userId", userId);
        long totalCount = totalQuery.getSingleResult();
        
        // Get enrolled count
        TypedQuery<Long> enrolledQuery = em.createQuery(
            "SELECT COUNT(a) FROM FailedAuthAttemptEntity a WHERE a.userId = :userId AND a.enrolled = true",
            Long.class
        );
        enrolledQuery.setParameter("userId", userId);
        long enrolledCount = enrolledQuery.getSingleResult();
        
        // Get average quality
        TypedQuery<Double> avgQualityQuery = em.createQuery(
            "SELECT AVG(a.avgQualityScore) FROM FailedAuthAttemptEntity a WHERE a.userId = :userId AND a.avgQualityScore IS NOT NULL",
            Double.class
        );
        avgQualityQuery.setParameter("userId", userId);
        Double avgQuality = avgQualityQuery.getSingleResult();
        
        // Get recommended count
        TypedQuery<Long> recommendedQuery = em.createQuery(
            "SELECT COUNT(a) FROM FailedAuthAttemptEntity a WHERE a.userId = :userId AND a.enrolled = false AND a.avgQualityScore >= :minQuality",
            Long.class
        );
        recommendedQuery.setParameter("userId", userId);
        recommendedQuery.setParameter("minQuality", config.getMinEnrollQualityScore());
        long recommendedCount = recommendedQuery.getSingleResult();
        
        FailedAttemptStatistics stats = new FailedAttemptStatistics();
        stats.setTotalCount((int) totalCount);
        stats.setEnrolledCount((int) enrolledCount);
        stats.setUnenrolledCount((int) (totalCount - enrolledCount));
        stats.setRecommendedCount((int) recommendedCount);
        stats.setAvgQualityScore(avgQuality != null ? avgQuality : 0.0);
        
        return stats;
    }
    
    /**
     * Inner class for enrollment result.
     */
    public static class EnrollmentResult {
        private boolean success;
        private String attemptId;
        private int enrolledImages;
        private int newFeatureVectors;
        private Instant timestamp;
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getAttemptId() { return attemptId; }
        public void setAttemptId(String attemptId) { this.attemptId = attemptId; }
        
        public int getEnrolledImages() { return enrolledImages; }
        public void setEnrolledImages(int enrolledImages) { this.enrolledImages = enrolledImages; }
        
        public int getNewFeatureVectors() { return newFeatureVectors; }
        public void setNewFeatureVectors(int newFeatureVectors) { this.newFeatureVectors = newFeatureVectors; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Inner class for statistics.
     */
    public static class FailedAttemptStatistics {
        private int totalCount;
        private int enrolledCount;
        private int unenrolledCount;
        private int recommendedCount;
        private double avgQualityScore;
        
        // Getters and setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        
        public int getEnrolledCount() { return enrolledCount; }
        public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }
        
        public int getUnenrolledCount() { return unenrolledCount; }
        public void setUnenrolledCount(int unenrolledCount) { this.unenrolledCount = unenrolledCount; }
        
        public int getRecommendedCount() { return recommendedCount; }
        public void setRecommendedCount(int recommendedCount) { this.recommendedCount = recommendedCount; }
        
        public double getAvgQualityScore() { return avgQualityScore; }
        public void setAvgQualityScore(double avgQualityScore) { this.avgQualityScore = avgQualityScore; }
    }
}
