package com.openelements.cardless.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.openelements.cardless.data.AccessAndRefreshToken;
import com.openelements.cardless.data.AccessToken;
import com.openelements.cardless.data.Account;
import com.openelements.cardless.data.Amount;
import com.openelements.cardless.data.Balance;
import com.openelements.cardless.data.BookedTransaction;
import com.openelements.cardless.data.CounterpartyAccount;
import com.openelements.cardless.data.Institution;
import com.openelements.cardless.data.PendingTransaction;
import com.openelements.cardless.data.Requisition;
import com.openelements.cardless.data.RequisitionsPage;
import com.openelements.cardless.data.Transactions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class JsonBasedFactory {

    @NonNull
    public static AccessToken createAccessToken(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String access = jsonObject.get("access").getAsString();
        final int access_expires = jsonObject.get("access_expires").getAsInt();
        return new AccessToken(access, access_expires);
    }

    @NonNull
    public static AccessAndRefreshToken createAccessAndRefreshToken(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String access = jsonObject.get("access").getAsString();
        final long accessExpires = jsonObject.get("access_expires").getAsLong();
        final String refresh = jsonObject.get("refresh").getAsString();
        final long refreshExpires = jsonObject.get("refresh_expires").getAsLong();
        return new AccessAndRefreshToken(access, accessExpires, refresh, refreshExpires);
    }

    @NonNull
    public static Institution createInstitution(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String id = jsonObject.get("id").getAsString();
        final String name = jsonObject.get("name").getAsString();
        final String bic = jsonObject.get("bic").getAsString();
        final String transactionTotalDays = getAsStringOrNull(jsonObject.get("transaction_total_days"));
        final String maxAccessValidForDays = getAsStringOrNull(jsonObject.get("max_access_valid_for_days"));
        final String logo = getAsStringOrNull(jsonObject.get("logo"));
        return new Institution(id, name, bic, transactionTotalDays, maxAccessValidForDays, logo);
    }

    @NonNull
    public static Requisition createRequisition(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String id = jsonObject.get("id").getAsString();
        final String created = jsonObject.get("created").getAsString();
        final String redirect = jsonObject.get("redirect").getAsString();
        final String status = jsonObject.get("status").getAsString();
        final String institution_id = jsonObject.get("institution_id").getAsString();
        final String agreement = jsonObject.get("agreement").getAsString();
        final String reference = jsonObject.get("reference").getAsString();
        final String link = jsonObject.get("link").getAsString();
        final List<String> accounts = new ArrayList<>();
        if (jsonObject.has("accounts")) {
            jsonObject.getAsJsonArray("accounts").forEach(account -> accounts.add(account.getAsString()));
        }
        return new Requisition(id, created, redirect, status, institution_id, agreement, reference, accounts, link);
    }

    @NonNull
    public static RequisitionsPage createRequisitionsPage(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final int count = jsonObject.get("count").getAsInt();
        final String next = getAsStringOrNull(jsonObject.get("next"));
        final String previous = getAsStringOrNull(jsonObject.get("previous"));
        final List<Requisition> requisitions = jsonObject.getAsJsonArray("results").asList()
                .stream()
                .map(JsonBasedFactory::createRequisition)
                .toList();
        return new RequisitionsPage(count, next, previous, requisitions);
    }

    @NonNull
    public static JsonObject createRequisitionRequestBody(@NonNull final String institutionId) {
        Objects.requireNonNull(institutionId, "institutionId must not be null");
        final JsonObject body = new JsonObject();
        body.addProperty("redirect", "http://www.yourwebpage.com");
        body.addProperty("institution_id", institutionId);
        return body;
    }

    @NonNull
    public static JsonObject createUpdateAccessTokenBody(@NonNull final String refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        final JsonObject body = new JsonObject();
        body.addProperty("refresh", refreshToken);
        return body;
    }

    @NonNull
    public static JsonObject createReceiveAccessToken(@NonNull final String secretId, @NonNull final String secretKey) {
        Objects.requireNonNull(secretId, "secretId must not be null");
        Objects.requireNonNull(secretKey, "secretKey must not be null");
        final JsonObject body = new JsonObject();
        body.addProperty("secret_id", secretId);
        body.addProperty("secret_key", secretKey);
        return body;
    }

    @Nullable
    private static String getAsStringOrNull(@NonNull final JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        } else {
            return element.getAsString();
        }
    }

    @NonNull
    private static Optional<String> getAsString(@NonNull final JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(element.getAsString());
        }
    }

    @NonNull
    private static BookedTransaction createBookedTransaction(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String transactionId = jsonObject.get("transactionId").getAsString();
        final Amount transactionAmount = createAmount(jsonObject.get("transactionAmount"));
        final LocalDate bookingDate = getAsString(jsonObject.get("bookingDate"))
                .map(v -> LocalDate.parse(v)).orElse(null);
        final LocalDate valueDate = getAsString(jsonObject.get("valueDate"))
                .map(v -> LocalDate.parse(v)).orElse(null);
        final String message = getAsString(jsonObject.get("remittanceInformationUnstructured"))
                .or(() -> getAsString(jsonObject.get("remittanceInformationStructured")))
                .or(() -> {
                    if (jsonObject.has("remittanceInformationUnstructuredArray")) {
                        return Optional.of(jsonObject.get("remittanceInformationUnstructuredArray").toString());
                    }
                    if (jsonObject.has("remittanceInformationStructuredArray")) {
                        return Optional.of(jsonObject.get("remittanceInformationStructuredArray").toString());
                    }
                    return Optional.empty();
                })
                .orElse(null);

        final String counterpartyName;
        final CounterpartyAccount counterpartyAccount;

        if (transactionAmount.amount().signum() > 0) {
            counterpartyName = getAsStringOrNull(jsonObject.get("debtorName"));
            if (jsonObject.has("debtorAccount")) {
                counterpartyAccount = createCounterpartyAccount(jsonObject.get("debtorAccount"));
            } else {
                counterpartyAccount = new CounterpartyAccount(null);
            }
        } else {
            counterpartyName = getAsStringOrNull(jsonObject.get("creditorName"));
            if (jsonObject.has("creditorAccount")) {
                counterpartyAccount = createCounterpartyAccount(jsonObject.get("creditorAccount"));
            } else {
                counterpartyAccount = new CounterpartyAccount(null);
            }
        }
        final String additionalInformation = getAsStringOrNull(jsonObject.get("additionalInformation"));

        return new BookedTransaction(transactionId, counterpartyName, counterpartyAccount, transactionAmount,
                bookingDate,
                valueDate, message, additionalInformation);
    }

    @NonNull
    private static PendingTransaction createPendingTransaction(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final Amount transactionAmount = createAmount(jsonObject.get("transactionAmount"));
        final LocalDate valueDate = getAsString(jsonObject.get("valueDate"))
                .map(v -> LocalDate.parse(v)).orElse(null);
        final String remittanceInformationUnstructured = getAsStringOrNull(
                jsonObject.get("remittanceInformationUnstructured"));
        final String additionalInformation = getAsStringOrNull(jsonObject.get("additionalInformation"));

        return new PendingTransaction(transactionAmount, valueDate, remittanceInformationUnstructured,
                additionalInformation);
    }

    @NonNull
    private static CounterpartyAccount createCounterpartyAccount(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final String iban;
        if (json.isJsonObject()) {
            final JsonObject jsonObject = json.getAsJsonObject();
            iban = getAsStringOrNull(jsonObject.get("iban"));
        } else {
            iban = null;
        }
        return new CounterpartyAccount(iban);
    }

    @NonNull
    private static Amount createAmount(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String currency = jsonObject.get("currency").getAsString();
        final BigDecimal amount = new BigDecimal(jsonObject.get("amount").getAsString());
        return new Amount(currency, amount);
    }

    @NonNull
    public static Transactions createTransactions(@NonNull final JsonElement jsonElement) {
        Objects.requireNonNull(jsonElement, "jsonElement must not be null");
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        final JsonObject transactions = jsonObject.getAsJsonObject("transactions");
        final List<BookedTransaction> bookedList = transactions.getAsJsonArray("booked").getAsJsonArray().asList()
                .stream()
                .map(json -> createBookedTransaction(json))
                .toList();
        final List<PendingTransaction> pendingList = transactions.getAsJsonArray("pending").getAsJsonArray().asList()
                .stream()
                .map(json -> createPendingTransaction(json))
                .toList();
        return new Transactions(bookedList, pendingList);
    }

    @NonNull
    public static Account createAccount(@NonNull final JsonElement jsonElement) {
        Objects.requireNonNull(jsonElement, "jsonElement must not be null");
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        final String id = jsonObject.get("id").getAsString();
        final ZonedDateTime created = getAsString(jsonObject.get("created"))
                .map(v -> ZonedDateTime.parse(v))
                .orElse(null);
        final ZonedDateTime lastAccessed = getAsString(jsonObject.get("last_accessed"))
                .map(v -> ZonedDateTime.parse(v)).orElse(null);
        final String iban = getAsStringOrNull(jsonObject.get("iban"));
        final String bban = getAsStringOrNull(jsonObject.get("bban"));
        final String status = getAsStringOrNull(jsonObject.get("status"));
        final String institutionId = jsonObject.get("institution_id").getAsString();
        final String ownerName = getAsStringOrNull(jsonObject.get("owner_name"));
        final String name = getAsStringOrNull(jsonObject.get("name"));
        return new Account(id, created, lastAccessed, iban, bban, status, institutionId, ownerName, name);
    }

    @NonNull
    public static List<Balance> createBalances(@NonNull final JsonElement jsonElement) {
        Objects.requireNonNull(jsonElement, "jsonElement must not be null");
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        return jsonObject.getAsJsonArray("balances").asList().stream()
                .map(json -> createBalance(json))
                .toList();
    }

    @NonNull
    private static Balance createBalance(@NonNull final JsonElement jsonElement) {
        Objects.requireNonNull(jsonElement, "jsonElement must not be null");
        final JsonObject jsonObject = jsonElement.getAsJsonObject();
        final Amount balanceAmount = createAmount(jsonObject.getAsJsonObject("balanceAmount"));
        final String balanceType = getAsStringOrNull(jsonObject.get("balanceType"));
        final LocalDate referenceDate = getAsString(jsonObject.get("referenceDate"))
                .map(v -> LocalDate.parse(v)).orElse(null);
        return new Balance(balanceAmount, balanceType, referenceDate);
    }
}
