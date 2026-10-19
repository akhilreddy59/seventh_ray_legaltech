package com.seventhray.contractmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Service
public class SupabaseAuthService {
    private final String supabaseUrl;
    private final String publicKey;
    private final ObjectMapper objectMapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public SupabaseAuthService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.anon-key:}") String publicKey,
            ObjectMapper objectMapper) {
        this.supabaseUrl = supabaseUrl;
        this.publicKey = publicKey;
        this.objectMapper = objectMapper;
    }

    public UUID resolveUserId(String accessToken) {
        if (supabaseUrl.isBlank() || publicKey.isBlank()) {
            throw new AuthProviderUnavailableException("Supabase Auth is not configured.");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(supabaseUrl.replaceAll("/+$", "") + "/auth/v1/user"))
                    .timeout(Duration.ofSeconds(8))
                    .header("apikey", publicKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new InvalidAccessTokenException();
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AuthProviderUnavailableException("Supabase Auth could not verify the session.");
            }
            JsonNode id = objectMapper.readTree(response.body()).get("id");
            if (id == null || !id.isTextual()) throw new AuthProviderUnavailableException("Supabase Auth returned an invalid user.");
            return UUID.fromString(id.asText());
        } catch (InvalidAccessTokenException | AuthProviderUnavailableException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AuthProviderUnavailableException("Supabase Auth request was interrupted.", ex);
        } catch (IOException | IllegalArgumentException ex) {
            throw new AuthProviderUnavailableException("Could not verify the Supabase session.", ex);
        }
    }

    public static class InvalidAccessTokenException extends RuntimeException {}

    public static class AuthProviderUnavailableException extends RuntimeException {
        public AuthProviderUnavailableException(String message) { super(message); }
        public AuthProviderUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
