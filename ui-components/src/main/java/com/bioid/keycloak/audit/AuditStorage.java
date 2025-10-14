package com.bioid.keycloak.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage implementation for audit events with configurable retention policies.
 * 
 * <p>This implementation stores audit events in JSON format with daily rotation
 * and automatic cleanup based on retention policies.
 */
public class AuditStorage {

    private static final Logger logger = LoggerFactory.getLogger(AuditStorage.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Path auditDirectory;
    private final AuditConfiguration config;

    public AuditStorage() {
        this.config = new AuditConfiguration();
        
        // Get audit directory from system property or use default
        String auditDirPath = System.getProperty(
            "bioid.audit.directory",
            System.getProperty("jboss.server.data.dir", System.getProperty("java.io.tmpdir")) + "/bioid-audit"
        );
        
        this.auditDirectory = Paths.get(auditDirPath);
        
        // Create audit directory if it doesn't exist
        try {
            Files.createDirectories(auditDirectory);
            logger.info("Audit storage directory: {}", auditDirectory);
        } catch (IOException e) {
            logger.error("Failed to create audit directory: {}", auditDirectory, e);
            throw new RuntimeException("Failed to initialize audit storage", e);
        }
    }

    /**
     * Stores an audit event to persistent storage.
     * 
     * @param auditEvent The audit event to store
     */
    public void storeAuditEvent(AdminAuditEvent auditEvent) {
        try {
            String jsonEvent = objectMapper.writeValueAsString(auditEvent);
            Path dailyLogFile = getDailyLogFile(auditEvent.getTimestamp());
            
            // Append to daily log file
            Files.write(dailyLogFile, 
                       (jsonEvent + System.lineSeparator()).getBytes(),
                       StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            logger.debug("Stored audit event: {}", auditEvent.getActionType());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit event", e);
        } catch (IOException e) {
            logger.error("Failed to write audit event to storage", e);
        }
    }

    /**
     * Retrieves audit events for the specified criteria.
     * 
     * @param realmId The realm ID to filter by
     * @param startTime The start time (inclusive)
     * @param endTime The end time (inclusive)
     * @param actionTypes The action types to filter by (null for all)
     * @param adminUserId The admin user ID to filter by (null for all)
     * @return List of matching audit events
     */
    public List<AdminAuditEvent> retrieveAuditEvents(String realmId, Instant startTime, Instant endTime,
                                                     List<AdminActionType> actionTypes, String adminUserId) {
        
        List<AdminAuditEvent> events = new ArrayList<>();
        
        try {
            // Get all log files in the date range
            List<Path> logFiles = getLogFilesInRange(startTime, endTime);
            
            for (Path logFile : logFiles) {
                if (Files.exists(logFile)) {
                    List<AdminAuditEvent> fileEvents = readEventsFromFile(logFile);
                    
                    // Filter events based on criteria
                    Stream<AdminAuditEvent> filteredStream = fileEvents.stream()
                        .filter(event -> realmId == null || realmId.equals(event.getRealmId()))
                        .filter(event -> event.getTimestamp().isAfter(startTime.minusSeconds(1)))
                        .filter(event -> event.getTimestamp().isBefore(endTime.plusSeconds(1)));
                    
                    if (actionTypes != null && !actionTypes.isEmpty()) {
                        filteredStream = filteredStream.filter(event -> actionTypes.contains(event.getActionType()));
                    }
                    
                    if (adminUserId != null) {
                        filteredStream = filteredStream.filter(event -> adminUserId.equals(event.getAdminUserId()));
                    }
                    
                    events.addAll(filteredStream.collect(Collectors.toList()));
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to retrieve audit events", e);
        }
        
        return events;
    }

    /**
     * Performs cleanup of old audit events based on retention policy.
     * 
     * @param retentionDays Number of days to retain audit events
     * @return Number of files cleaned up
     */
    public int performCleanup(int retentionDays) {
        int cleanedFiles = 0;
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        
        try {
            List<Path> allLogFiles = Files.list(auditDirectory)
                .filter(path -> path.getFileName().toString().startsWith("audit-"))
                .filter(path -> path.getFileName().toString().endsWith(".log"))
                .collect(Collectors.toList());
            
            for (Path logFile : allLogFiles) {
                String fileName = logFile.getFileName().toString();
                String dateStr = fileName.substring(6, 16); // Extract date from "audit-yyyy-MM-dd.log"
                
                try {
                    LocalDate fileDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                    if (fileDate.isBefore(cutoffDate)) {
                        Files.deleteIfExists(logFile);
                        cleanedFiles++;
                        logger.info("Cleaned up old audit log file: {}", fileName);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse date from audit log file: {}", fileName, e);
                }
            }
            
        } catch (IOException e) {
            logger.error("Failed to perform audit cleanup", e);
        }
        
        logger.info("Audit cleanup completed. Cleaned {} files older than {} days", cleanedFiles, retentionDays);
        return cleanedFiles;
    }

    /**
     * Gets storage statistics.
     * 
     * @return Storage statistics
     */
    public AuditStorageStats getStorageStats() {
        AuditStorageStats stats = new AuditStorageStats();
        
        try {
            List<Path> logFiles = Files.list(auditDirectory)
                .filter(path -> path.getFileName().toString().startsWith("audit-"))
                .collect(Collectors.toList());
            
            stats.setTotalFiles(logFiles.size());
            
            long totalSize = 0;
            int totalEvents = 0;
            
            for (Path logFile : logFiles) {
                totalSize += Files.size(logFile);
                totalEvents += countEventsInFile(logFile);
            }
            
            stats.setTotalSizeBytes(totalSize);
            stats.setTotalEvents(totalEvents);
            stats.setStorageDirectory(auditDirectory.toString());
            
        } catch (IOException e) {
            logger.error("Failed to calculate storage stats", e);
        }
        
        return stats;
    }

    // Private helper methods

    private Path getDailyLogFile(Instant timestamp) {
        LocalDate date = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();
        String fileName = "audit-" + date.format(DATE_FORMATTER) + ".log";
        return auditDirectory.resolve(fileName);
    }

    private List<Path> getLogFilesInRange(Instant startTime, Instant endTime) {
        List<Path> logFiles = new ArrayList<>();
        
        LocalDate startDate = startTime.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = endTime.atZone(ZoneId.systemDefault()).toLocalDate();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            String fileName = "audit-" + currentDate.format(DATE_FORMATTER) + ".log";
            Path logFile = auditDirectory.resolve(fileName);
            logFiles.add(logFile);
            currentDate = currentDate.plusDays(1);
        }
        
        return logFiles;
    }

    private List<AdminAuditEvent> readEventsFromFile(Path logFile) {
        List<AdminAuditEvent> events = new ArrayList<>();
        
        try {
            List<String> lines = Files.readAllLines(logFile);
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    try {
                        AdminAuditEvent event = objectMapper.readValue(line, AdminAuditEvent.class);
                        events.add(event);
                    } catch (JsonProcessingException e) {
                        logger.warn("Failed to parse audit event from line: {}", line, e);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read audit events from file: {}", logFile, e);
        }
        
        return events;
    }

    private int countEventsInFile(Path logFile) {
        try {
            return (int) Files.lines(logFile)
                .filter(line -> !line.trim().isEmpty())
                .count();
        } catch (IOException e) {
            logger.error("Failed to count events in file: {}", logFile, e);
            return 0;
        }
    }

    /**
     * Storage statistics data class.
     */
    public static class AuditStorageStats {
        private int totalFiles;
        private long totalSizeBytes;
        private int totalEvents;
        private String storageDirectory;

        // Getters and Setters
        public int getTotalFiles() {
            return totalFiles;
        }

        public void setTotalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
        }

        public long getTotalSizeBytes() {
            return totalSizeBytes;
        }

        public void setTotalSizeBytes(long totalSizeBytes) {
            this.totalSizeBytes = totalSizeBytes;
        }

        public int getTotalEvents() {
            return totalEvents;
        }

        public void setTotalEvents(int totalEvents) {
            this.totalEvents = totalEvents;
        }

        public String getStorageDirectory() {
            return storageDirectory;
        }

        public void setStorageDirectory(String storageDirectory) {
            this.storageDirectory = storageDirectory;
        }

        public double getTotalSizeMB() {
            return totalSizeBytes / (1024.0 * 1024.0);
        }

        @Override
        public String toString() {
            return String.format("AuditStorageStats{files=%d, size=%.2fMB, events=%d, dir='%s'}", 
                               totalFiles, getTotalSizeMB(), totalEvents, storageDirectory);
        }
    }
}