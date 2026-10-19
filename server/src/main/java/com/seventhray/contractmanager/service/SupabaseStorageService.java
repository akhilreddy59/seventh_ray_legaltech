package com.seventhray.contractmanager.service;

import com.seventhray.contractmanager.web.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class SupabaseStorageService {
    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public SupabaseStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:}") String serviceKey,
            @Value("${supabase.storage.bucket:contracts}") String bucket) {
        this.supabaseUrl = supabaseUrl;
        this.serviceKey = serviceKey;
        this.bucket = bucket;
    }

    public void upload(String objectPath, String contentType, byte[] data) {
        HttpRequest request = authenticated(objectUri(objectPath))
                .header("Content-Type", contentType)
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
        send(request, "upload");
    }

    public void delete(String objectPath) {
        String body = "{\"prefixes\":[\"" + objectPath + "\"]}";
        HttpRequest request = authenticated(bucketUri())
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                .build();
        send(request, "delete");
    }

    private HttpRequest.Builder authenticated(URI uri) {
        if (supabaseUrl.isBlank() || serviceKey.isBlank() || bucket.isBlank()) {
            throw new StorageException("Supabase Storage is not configured. Set SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, and SUPABASE_STORAGE_BUCKET.");
        }
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .header("apikey", serviceKey)
                .header("Authorization", "Bearer " + serviceKey);
    }

    private URI objectUri(String objectPath) {
        String encodedPath = Arrays.stream(objectPath.split("/"))
                .map(SupabaseStorageService::encodeSegment)
                .collect(Collectors.joining("/"));
        return URI.create(bucketUri().toString() + "/" + encodedPath);
    }

    private URI bucketUri() {
        return URI.create(supabaseUrl.replaceAll("/+$", "") + "/storage/v1/object/" + encodeSegment(bucket));
    }

    private static String encodeSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void send(HttpRequest request, String operation) {
        try {
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new StorageException("Supabase Storage could not " + operation + " the document (HTTP " + response.statusCode() + ").");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StorageException("Supabase Storage request was interrupted.", ex);
        } catch (IOException ex) {
            throw new StorageException("Could not connect to Supabase Storage.", ex);
        }
    }
}
