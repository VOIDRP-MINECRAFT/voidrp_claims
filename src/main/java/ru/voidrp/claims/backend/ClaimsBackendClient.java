package ru.voidrp.claims.backend;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import ru.voidrp.claims.backend.ClaimDtos.ClaimActionResponse;
import ru.voidrp.claims.backend.ClaimDtos.ClaimCreateRequest;
import ru.voidrp.claims.backend.ClaimDtos.ClaimListResponse;
import ru.voidrp.claims.backend.ClaimDtos.ClaimTrustRequest;
import ru.voidrp.claims.backend.ClaimDtos.ClaimUpgradeRequest;
import ru.voidrp.claims.config.ClaimsConfig;

public final class ClaimsBackendClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final ClaimsConfig config;
    private final Gson gson;
    private final HttpClient http;

    public ClaimsBackendClient(ClaimsConfig config) {
        this.config = config;
        this.gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .disableHtmlEscaping()
                .create();
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    public CompletableFuture<ClaimListResponse> listAsync() {
        return sendAsync(request("/api/v1/claims/list").GET().build(), ClaimListResponse.class);
    }

    public CompletableFuture<ClaimActionResponse> createAsync(ClaimCreateRequest req) {
        return sendAsync(jsonPost("/api/v1/claims/create", req), ClaimActionResponse.class);
    }

    public CompletableFuture<ClaimActionResponse> addCubeAsync(String claimId, int cx, int cy, int cz) {
        return sendAsync(
                jsonPost("/api/v1/claims/" + claimId + "/upgrade",
                        new ClaimUpgradeRequest(java.util.List.of(cx, cy, cz))),
                ClaimActionResponse.class);
    }

    public CompletableFuture<ClaimActionResponse> fillAsync(String claimId) {
        return sendAsync(jsonPost("/api/v1/claims/" + claimId + "/fill", new java.util.HashMap<>()),
                ClaimActionResponse.class);
    }

    public CompletableFuture<ClaimActionResponse> deleteAsync(String claimId) {
        return sendAsync(request("/api/v1/claims/" + claimId).DELETE().build(), ClaimActionResponse.class);
    }

    public CompletableFuture<ClaimActionResponse> trustAsync(String claimId, String nick, String action) {
        return sendAsync(jsonPost("/api/v1/claims/" + claimId + "/trust", new ClaimTrustRequest(nick, action)),
                ClaimActionResponse.class);
    }

    // ── internals ────────────────────────────────────────────────────────
    private HttpRequest.Builder request(String path) {
        URI uri = config.backendBaseUrl().resolve(path);
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("X-Game-Auth-Secret", config.gameAuthSecret());
        if (!config.serverSlug().isBlank()) {
            b.header("X-Server-Slug", config.serverSlug());
        }
        return b;
    }

    private HttpRequest jsonPost(String path, Object body) {
        return request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();
    }

    private <T> CompletableFuture<T> sendAsync(HttpRequest req, Class<T> type) {
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return gson.fromJson(resp.body(), type);
                    }
                    throw new RuntimeException("http_" + resp.statusCode() + ": " + resp.body());
                });
    }
}
