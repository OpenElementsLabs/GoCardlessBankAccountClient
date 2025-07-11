package com.openelements.cardless.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.openelements.cardless.ApiCallException;
import com.openelements.cardless.CardlessClient;
import com.openelements.cardless.CardlessException;
import com.openelements.cardless.data.AccessAndRefreshToken;
import com.openelements.cardless.data.AccessToken;
import com.openelements.cardless.data.Account;
import com.openelements.cardless.data.Balance;
import com.openelements.cardless.data.ErrorMessage;
import com.openelements.cardless.data.Institution;
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

public class CardlessClientImpl implements CardlessClient {

    private final static Logger log = LoggerFactory.getLogger(CardlessClientImpl.class);

    private final HttpClient httpClient;

    private AtomicReference<AccessAndRefreshToken> accessAndRefreshTokenRef = new AtomicReference<>();

    public CardlessClientImpl(@NonNull final String secretId, @NonNull final String secretKey)
            throws CardlessException {
        Objects.requireNonNull(secretId, "secretId must not be null");
        Objects.requireNonNull(secretKey, "secretKey must not be null");
        try {
            httpClient = HttpClient.newBuilder().build();
            final JsonObject body = JsonBasedFactory.createReceiveAccessToken(secretId, secretKey);
            final JsonElement response = handlePostRequest("https://bankaccountdata.gocardless.com/api/v2/token/new/",
                    body);
            final AccessAndRefreshToken token = JsonBasedFactory.createAccessAndRefreshToken(response);
            accessAndRefreshTokenRef.set(token);
        } catch (Exception e) {
            throw new CardlessException("Error in creating cardless client", e);
        }
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
    private HttpRequest createDeleteRequest(@NonNull final String url) throws IOException, InterruptedException {
        return createDeleteRequest(url, true);
    }

    @NonNull
    private HttpRequest createDeleteRequest(@NonNull final String url, final boolean checkAccessToken)
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
        return builder.DELETE()
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
            throws CardlessException {
        log.debug("Fetching requisitions with limit: {}, offset: {}", limit, offset);
        try {
            final JsonElement jsonElement = handleGetRequest(
                    "https://bankaccountdata.gocardless.com/api/v2/requisitions/?limit=" + limit + "&offset=" + offset);
            log.debug("Received JSON: {}", jsonElement);
            return JsonBasedFactory.createRequisitionsPage(jsonElement);
        } catch (Exception e) {
            throw new CardlessException("Error fetching requisitions for limit " + limit + " and offset " + offset, e);
        }
    }

    @NonNull
    public List<Institution> getInstitutions(@NonNull final String country) throws CardlessException {
        Objects.requireNonNull(country, "country must not be null");
        log.debug("Fetching institutions for country: {}", country);
        try {
            final JsonElement jsonElement = handleGetRequest(
                    "https://bankaccountdata.gocardless.com/api/v2/institutions/?country=" + country);
            log.debug("Received JSON: {}", jsonElement);
            return jsonElement.getAsJsonArray().asList().stream()
                    .map(JsonBasedFactory::createInstitution)
                    .toList();
        } catch (Exception e) {
            throw new CardlessException("Error fetching institutions for country '" + country + "'", e);
        }
    }

    @NonNull
    @Override
    public Requisition createRequisition(@NonNull final String institutionId, @NonNull URI redirect)
            throws CardlessException {
        Objects.requireNonNull(institutionId, "institutionId must not be null");
        Objects.requireNonNull(redirect, "redirect URI must not be null");
        log.debug("Creating requisition for institutionId: '{}' and redirect: '{}'", institutionId, redirect);
        try {
            final JsonObject body = JsonBasedFactory.createRequisitionRequestBody(institutionId, redirect);
            final JsonElement jsonElement = handlePostRequest(
                    "https://bankaccountdata.gocardless.com/api/v2/requisitions/",
                    body);
            log.debug("Received JSON: {}", jsonElement);
            return JsonBasedFactory.createRequisition(jsonElement);
        } catch (Exception e) {
            throw new CardlessException("Error creating requisition for institutionId '" + institutionId + "'", e);
        }
    }

    @Override
    public void deleteRequisition(@NonNull String requisitionId) throws CardlessException {
        Objects.requireNonNull(requisitionId, "requisitionId must not be null");
        log.debug("Deleting requisition with id: {}", requisitionId);
        try {
            final HttpRequest request = createDeleteRequest(
                    "https://bankaccountdata.gocardless.com/api/v2/requisitions/" + requisitionId + "/");
            final HttpResponse<Void> response = httpClient.send(request, BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                throw new CardlessException("Error deleting requisition with id '" + requisitionId + "'");
            }
            log.debug("Requisition with id {} deleted successfully", requisitionId);
        } catch (Exception e) {
            throw new CardlessException("Error deleting requisition with id '" + requisitionId + "'", e);
        }
    }

    @NonNull
    public Transactions getTransactions(@NonNull final String account) throws CardlessException {
        Objects.requireNonNull(account, "account must not be null");
        log.debug("Fetching transactions for account: {}", account);
        try {
            final JsonElement jsonElement = handleGetRequest(
                    "https://bankaccountdata.gocardless.com/api/v2/accounts/" + account + "/transactions/");
            log.debug("Received JSON: {}", jsonElement);
            return JsonBasedFactory.createTransactions(jsonElement);
        } catch (Exception e) {
            throw new CardlessException("Error fetching transactions for account '" + account + "'", e);
        }
    }

    @NonNull
    public Account getAccount(@NonNull final String id) throws CardlessException {
        Objects.requireNonNull(id, "id must not be null");
        log.debug("Fetching account with id: {}", id);
        try {
            final JsonElement jsonElement = handleGetRequest(
                    "https://bankaccountdata.gocardless.com/api/v2/accounts/" + id + "/");
            log.debug("Received JSON: {}", jsonElement);
            return JsonBasedFactory.createAccount(jsonElement);
        } catch (Exception e) {
            throw new CardlessException("Error fetching account with id '" + id + "'", e);
        }
    }

    @NonNull
    public List<Balance> getBalances(@NonNull final String accountId) throws CardlessException {
        Objects.requireNonNull(accountId, "accountId must not be null");
        log.debug("Fetching balances for accountId: {}", accountId);
        try {
            final JsonElement jsonElement = handleGetRequest(
                    "https://bankaccountdata.gocardless.com/api/v2/accounts/" + accountId + "/balances/");
            log.debug("Received JSON: {}", jsonElement);
            return JsonBasedFactory.createBalances(jsonElement);
        } catch (Exception e) {
            throw new CardlessException("Error in fetching balances for accountId '" + accountId + "'", e);
        }
    }

    @Override
    public @NonNull Institution getInstitution(@NonNull String institutionId) throws CardlessException {
        Objects.requireNonNull(institutionId, "institutionId must not be null");
        log.debug("Fetching institution for institutionId: {}", institutionId);
        try {
            final JsonElement jsonElement = handleGetRequest(
                    "https://bankaccountdata.gocardless.com/api/v2/institutions/" + institutionId + "/");
            log.debug("Received JSON: {}", jsonElement);
            return JsonBasedFactory.createInstitution(jsonElement);
        } catch (Exception e) {
            throw new CardlessException("Error fetching institution for institutionId '" + institutionId + "'", e);
        }
    }
}
