package com.bioid.keycloak.admin.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for BWS Management API (REST).
 * 
 * The BWS Management API is available at https://bwsportal.bioid.com/api/
 * and requires JWT Bearer authentication.
 * 
 * @see <a href="https://developer.bioid.com/bws/management">BWS Management API Documentation</a>
 */
public class BWSManagementClient implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(BWSManagementClient.class);
  
  private static final String DEFAULT_BASE_URL = "https://bwsportal.bioid.com/api";
  
  private final String baseUrl;
  private final String jwtToken;
  private final HttpClient httpClient;

  /**
   * Create a new BWS Management API client.
   * 
   * @param jwtToken JWT token for authentication (use jwt tool or BWS Portal to generate)
   */
  public BWSManagementClient(String jwtToken) {
    this(DEFAULT_BASE_URL, jwtToken);
  }

  /**
   * Create a new BWS Management API client with custom base URL.
   * 
   * @param baseUrl base URL of the Management API
   * @param jwtToken JWT token for authentication
   */
  public BWSManagementClient(String baseUrl, String jwtToken) {
    this.baseUrl = baseUrl;
    this.jwtToken = jwtToken;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }

  /**
   * Get the number of classes (enrolled templates) for a specific client.
   * 
   * @param clientId the BWS client ID
   * @return number of classes
   * @throws Exception if API call fails
   */
  public int getClassCount(String clientId) throws Exception {
    String url = baseUrl + "/client/classcount/" + clientId;
    
    logger.debug("Calling BWS Management API: GET {}", url);
    
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(30))
        .header("Accept", "application/json")
        .header("Authorization", "Bearer " + jwtToken)
        .GET()
        .build();
    
    HttpResponse<String> response = httpClient.send(request, 
        HttpResponse.BodyHandlers.ofString());
    
    if (response.statusCode() != 200) {
      String errorMsg = String.format("BWS Management API returned status %d: %s", 
          response.statusCode(), response.body());
      logger.error(errorMsg);
      throw new Exception(errorMsg);
    }
    
    // Response should be just a number
    String responseBody = response.body().trim();
    try {
      int count = Integer.parseInt(responseBody);
      logger.debug("BWS class count for client {}: {}", clientId, count);
      return count;
    } catch (NumberFormatException e) {
      throw new Exception("Invalid response from BWS Management API: " + responseBody);
    }
  }

  @Override
  public void close() {
    // HttpClient doesn't need explicit closing in Java 11+
  }
}
