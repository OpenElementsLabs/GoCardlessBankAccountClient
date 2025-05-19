package com.openelements.cardless;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NonNull;

public class CardlessClient {

    private final String secretId;

    private final String secretKey;

    private final HttpClient httpClient;

    private AtomicReference<AccessAndRefreshToken> accessAndRefreshTokenRef = new AtomicReference<>();

    public CardlessClient(@NonNull final String secretId, @NonNull final String secretKey)
            throws IOException, InterruptedException {
        this.secretId = Objects.requireNonNull(secretId, "secretId must not be null");
        this.secretKey = Objects.requireNonNull(secretKey, "secretKey must not be null");
        httpClient = HttpClient.newBuilder().build();
        receiveAccessToken();
    }

    private static ErrorMessage createFromJson(@NonNull String json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonElement jsonElement = JsonParser.parseString(json);
        if (jsonElement.isJsonObject()) {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            final String summary = jsonObject.get("summary").getAsString();
            final String detail = jsonObject.get("detail").getAsString();
            final int statusCode = jsonObject.get("status_code").getAsInt();
            return new ErrorMessage(summary, detail, statusCode);
        }
        throw new IllegalArgumentException("Invalid JSON format: " + json);
    }

    private HttpRequest createGetRequest(String url) throws IOException, InterruptedException {
        return createGetRequest(url, true);
    }

    private HttpRequest createGetRequest(String url, boolean checkAccessToken)
            throws IOException, InterruptedException {
        Objects.requireNonNull(url, "url must not be null");
        if (checkAccessToken) {
            checkAccessToken();
        }
        final AccessAndRefreshToken accessAndRefreshToken = accessAndRefreshTokenRef.get();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("accept", "application/json");
        if (accessAndRefreshToken != null) {
            builder = builder.header("Authorization", "Bearer " + accessAndRefreshTokenRef.get().access());
        }
        return builder.GET()
                .build();
    }

    private HttpRequest createPostRequest(@NonNull final String url, @NonNull JsonElement body,
            boolean checkAccessToken) throws IOException, InterruptedException {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(body, "body must not be null");
        if (checkAccessToken) {
            checkAccessToken();
        }
        final AccessAndRefreshToken accessAndRefreshToken = accessAndRefreshTokenRef.get();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("accept", "application/json");
        if (accessAndRefreshToken != null) {
            builder = builder.header("Authorization", "Bearer " + accessAndRefreshTokenRef.get().access());
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }

    private JsonElement handleGetRequest(String url) throws IOException, InterruptedException {
        final HttpRequest request = createGetRequest(url);
        final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            try {
                final ErrorMessage errorMessage = createFromJson(response.body());
                throw new ApiCallException(errorMessage);
            } catch (Exception e) {
                throw new IOException("Error in HTTP call", e);
            }
        }
        final String json = response.body();
        try {
            return JsonParser.parseString(json);
        } catch (Exception e) {
            throw new IOException("Error parsing JSON response: " + json, e);
        }
    }

    private JsonElement handlePostRequest(String url, JsonElement body) throws IOException, InterruptedException {
        final HttpRequest request = createPostRequest(url, body, false);
        final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            try {
                final ErrorMessage errorMessage = createFromJson(response.body());
                throw new ApiCallException(errorMessage);
            } catch (Exception e) {
                throw new IOException("Error in HTTP call", e);
            }
        }
        final String json = response.body();
        try {
            return JsonParser.parseString(json);
        } catch (Exception e) {
            throw new IOException("Error parsing JSON response: " + json, e);
        }
    }

    private void receiveAccessToken() throws IOException, InterruptedException {
        final JsonObject body = new JsonObject();
        body.addProperty("secret_id", secretId);
        body.addProperty("secret_key", secretKey);
        final JsonElement response = handlePostRequest("https://bankaccountdata.gocardless.com/api/v2/token/new/",
                body);
        AccessAndRefreshToken token = JsonBasedFactory.createAccessAndRefreshToken(response);
        accessAndRefreshTokenRef.set(token);
    }

    private AccessToken updateAccessToken() throws IOException, InterruptedException {
        final JsonObject body = new JsonObject();
        body.addProperty("refresh", accessAndRefreshTokenRef.get().refresh());
        final JsonElement jsonElement = handlePostRequest(
                "https://bankaccountdata.gocardless.com/api/v2/token/refresh/",
                body);
        return JsonBasedFactory.createAccessToken(jsonElement);
    }

    private synchronized void checkAccessToken() throws IOException, InterruptedException {
        if (accessAndRefreshTokenRef.get() == null) {
            receiveAccessToken();
        }
        final AccessAndRefreshToken currentAccessToken = accessAndRefreshTokenRef.get();

        if (currentAccessToken.willExpireShortly()) {
            AccessToken newAccessToken = updateAccessToken();
            accessAndRefreshTokenRef.set(
                    new AccessAndRefreshToken(newAccessToken.access(), newAccessToken.access_expires(),
                            currentAccessToken.refresh(), currentAccessToken.refreshExpires()));
        }
    }

    public void getRequisitions(int limit, int offset) throws IOException, InterruptedException {
        checkAccessToken();
        final JsonElement jsonElement = handleGetRequest(
                "https://bankaccountdata.gocardless.com/api/v2/requisitions/?limit=" + limit + "&offset=" + offset);
    }

    public List<Institution> getInstitutions(String country) throws IOException, InterruptedException {
        checkAccessToken();
        final JsonElement jsonElement = handleGetRequest(
                "https://bankaccountdata.gocardless.com/api/v2/institutions/?country=" + country);
        return jsonElement.getAsJsonArray().asList().stream()
                .map(JsonBasedFactory::createInstitution)
                .toList();
    }

    public Requisition createRequisition(Institution institution) throws IOException, InterruptedException {
        return createRequisition(institution.id());
    }

    public Requisition createRequisition(String institutionId) throws IOException, InterruptedException {
        final JsonObject body = new JsonObject();
        body.addProperty("redirect", "http://www.yourwebpage.com");
        body.addProperty("institution_id", institutionId);
        final JsonElement jsonElement = handlePostRequest("https://bankaccountdata.gocardless.com/api/v2/requisitions/",
                body);
        return JsonBasedFactory.createRequisition(jsonElement);
    }
}
