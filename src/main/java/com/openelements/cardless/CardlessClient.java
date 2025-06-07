package com.openelements.cardless;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.openelements.cardless.data.AccessAndRefreshToken;
import com.openelements.cardless.data.AccessToken;
import com.openelements.cardless.data.Account;
import com.openelements.cardless.data.Balance;
import com.openelements.cardless.data.ErrorMessage;
import com.openelements.cardless.data.Institution;
import com.openelements.cardless.data.JsonBasedFactory;
import com.openelements.cardless.data.Requisition;
import com.openelements.cardless.data.RequisitionsPage;
import com.openelements.cardless.data.Transactions;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CardlessClient {

    private final static Logger log = LoggerFactory.getLogger(CardlessClient.class);

    private final HttpClient httpClient;

    private AtomicReference<AccessAndRefreshToken> accessAndRefreshTokenRef = new AtomicReference<>();

    public CardlessClient(@NonNull final String secretId, @NonNull final String secretKey)
            throws IOException, InterruptedException {
        Objects.requireNonNull(secretId, "secretId must not be null");
        Objects.requireNonNull(secretKey, "secretKey must not be null");
        httpClient = HttpClient.newBuilder().build();
        final JsonObject body = JsonBasedFactory.createReceiveAccessToken(secretId, secretKey);
        final JsonElement response = handlePostRequest("https://bankaccountdata.gocardless.com/api/v2/token/new/",
                body);
        final AccessAndRefreshToken token = JsonBasedFactory.createAccessAndRefreshToken(response);
        accessAndRefreshTokenRef.set(token);
    }

    @NonNull
    private static ErrorMessage createFromJson(@NonNull final String json) {
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

    @NonNull
    private HttpRequest createGetRequest(@NonNull final String url) throws IOException, InterruptedException {
        return createGetRequest(url, true);
    }

    @NonNull
    private HttpRequest createGetRequest(@NonNull final String url, final boolean checkAccessToken)
            throws IOException, InterruptedException {
        Objects.requireNonNull(url, "url must not be null");
        if (checkAccessToken) {
            checkAccessToken();
        }
        final AccessAndRefreshToken accessAndRefreshToken = accessAndRefreshTokenRef.get();
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("accept", "application/json");
        if (accessAndRefreshToken != null) {
            builder.header("Authorization", "Bearer " + accessAndRefreshTokenRef.get().access());
        }
        return builder.GET()
                .build();
    }

    @NonNull
    private HttpRequest createPostRequest(@NonNull final String url, @NonNull JsonElement body,
            boolean checkAccessToken) throws IOException, InterruptedException {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(body, "body must not be null");
        if (checkAccessToken) {
            checkAccessToken();
        }
        final AccessAndRefreshToken accessAndRefreshToken = accessAndRefreshTokenRef.get();
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("accept", "application/json");
        if (accessAndRefreshToken != null) {
            builder.header("Authorization", "Bearer " + accessAndRefreshTokenRef.get().access());
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
    }

    @NonNull
    private JsonElement handleGetRequest(@NonNull final String url) throws IOException, InterruptedException {
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

    @NonNull
    private JsonElement handlePostRequest(@NonNull final String url, @NonNull final JsonElement body)
            throws IOException, InterruptedException {
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

    @NonNull
    private AccessToken updateAccessToken() throws IOException, InterruptedException {
        final JsonObject body = JsonBasedFactory.createUpdateAccessTokenBody(accessAndRefreshTokenRef.get().refresh());
        final JsonElement jsonElement = handlePostRequest(
                "https://bankaccountdata.gocardless.com/api/v2/token/refresh/",
                body);
        return JsonBasedFactory.createAccessToken(jsonElement);
    }

    private synchronized void checkAccessToken() throws IOException, InterruptedException {
        final AccessAndRefreshToken currentAccessToken = accessAndRefreshTokenRef.get();
        if (currentAccessToken.willExpireShortly()) {
            AccessToken newAccessToken = updateAccessToken();
            accessAndRefreshTokenRef.set(
                    new AccessAndRefreshToken(newAccessToken.access(), newAccessToken.access_expires(),
                            currentAccessToken.refresh(), currentAccessToken.refreshExpires()));
        }
    }

    @NonNull
    public RequisitionsPage getRequisitions(final int limit, final int offset)
            throws IOException, InterruptedException {
        log.debug("Fetching requisitions with limit: {}, offset: {}", limit, offset);
        final JsonElement jsonElement = handleGetRequest(
                "https://bankaccountdata.gocardless.com/api/v2/requisitions/?limit=" + limit + "&offset=" + offset);
        log.debug("Received JSON: {}", jsonElement);
        return JsonBasedFactory.createRequisitionsPage(jsonElement);
    }

    @NonNull
    public List<Institution> getInstitutions(@NonNull final String country) throws IOException, InterruptedException {
        Objects.requireNonNull(country, "country must not be null");
        log.debug("Fetching institutions for country: {}", country);
        final JsonElement jsonElement = handleGetRequest(
                "https://bankaccountdata.gocardless.com/api/v2/institutions/?country=" + country);
        log.debug("Received JSON: {}", jsonElement);
        return jsonElement.getAsJsonArray().asList().stream()
                .map(JsonBasedFactory::createInstitution)
                .toList();
    }

    @NonNull
    public Requisition createRequisition(@NonNull final Institution institution)
            throws IOException, InterruptedException {
        return createRequisition(institution.id());
    }

    @NonNull
    public Requisition createRequisition(@NonNull final String institutionId) throws IOException, InterruptedException {
        Objects.requireNonNull(institutionId, "institutionId must not be null");
        log.debug("Creating requisition for institutionId: {}", institutionId);
        final JsonObject body = JsonBasedFactory.createRequisitionRequestBody(institutionId);
        final JsonElement jsonElement = handlePostRequest("https://bankaccountdata.gocardless.com/api/v2/requisitions/",
                body);
        log.debug("Received JSON: {}", jsonElement);
        return JsonBasedFactory.createRequisition(jsonElement);
    }

    @NonNull
    public Transactions getTransactions(@NonNull final String account) throws IOException, InterruptedException {
        Objects.requireNonNull(account, "account must not be null");
        log.debug("Fetching transactions for account: {}", account);
        final JsonElement jsonElement = handleGetRequest(
                "https://bankaccountdata.gocardless.com/api/v2/accounts/" + account + "/transactions/");
        log.debug("Received JSON: {}", jsonElement);
        return JsonBasedFactory.createTransactions(jsonElement);
    }

    @NonNull
    public Account getAccount(@NonNull final String id) throws IOException, InterruptedException {
        Objects.requireNonNull(id, "id must not be null");
        log.debug("Fetching account with id: {}", id);
        final JsonElement jsonElement = handleGetRequest(
                "https://bankaccountdata.gocardless.com/api/v2/accounts/" + id + "/");
        log.debug("Received JSON: {}", jsonElement);
        return JsonBasedFactory.createAccount(jsonElement);
    }

    @NonNull
    public List<Balance> getBalances(@NonNull final String accountId) throws IOException, InterruptedException {
        Objects.requireNonNull(accountId, "accountId must not be null");
        log.debug("Fetching balances for accountId: {}", accountId);
        final JsonElement jsonElement = handleGetRequest(
                "https://bankaccountdata.gocardless.com/api/v2/accounts/" + accountId + "/balances/");
        log.debug("Received JSON: {}", jsonElement);
        return JsonBasedFactory.createBalances(jsonElement);
    }
}
