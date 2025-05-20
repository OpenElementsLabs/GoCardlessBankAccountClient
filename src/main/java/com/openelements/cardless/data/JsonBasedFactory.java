package com.openelements.cardless.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

public class JsonBasedFactory {

    @NonNull
    public static AccessToken createAccessToken(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String access = jsonObject.get("access").getAsString();
        final int access_expires = jsonObject.get("access_expires").getAsInt();
        return new AccessToken(access, access_expires);
    }

    public static AccessAndRefreshToken createAccessAndRefreshToken(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String access = jsonObject.get("access").getAsString();
        final long accessExpires = jsonObject.get("access_expires").getAsLong();
        final String refresh = jsonObject.get("refresh").getAsString();
        final long refreshExpires = jsonObject.get("refresh_expires").getAsLong();
        return new AccessAndRefreshToken(access, accessExpires, refresh, refreshExpires);
    }

    public static Institution createInstitution(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String id = jsonObject.get("id").getAsString();
        final String name = jsonObject.get("name").getAsString();
        final String bic = jsonObject.get("bic").getAsString();
        return new Institution(id, name, bic);
    }

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

    public static JsonObject createRequisitionRequestBody(@NonNull final String institutionId) {
        Objects.requireNonNull(institutionId, "institutionId must not be null");
        final JsonObject body = new JsonObject();
        body.addProperty("redirect", "http://www.yourwebpage.com");
        body.addProperty("institution_id", institutionId);
        return body;
    }

    public static JsonObject createUpdateAccessTokenBody(@NonNull final String refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");
        final JsonObject body = new JsonObject();
        body.addProperty("refresh", refreshToken);
        return body;
    }

    public static JsonObject createReceiveAccessToken(@NonNull final String secretId, @NonNull final String secretKey) {
        Objects.requireNonNull(secretId, "secretId must not be null");
        Objects.requireNonNull(secretKey, "secretKey must not be null");
        final JsonObject body = new JsonObject();
        body.addProperty("secret_id", secretId);
        body.addProperty("secret_key", secretKey);
        return body;
    }

    private static String getAsStringOrNull(@NonNull final JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        } else {
            return element.getAsString();
        }
    }

    private static BookedTransaction createBookedTransaction(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String transactionId = jsonObject.get("transactionId").getAsString();
        final String debtorName = getAsStringOrNull(jsonObject.get("debtorName"));
        final DebtorAccount debtorAccount;
        if (jsonObject.has("debtorAccount")) {
            debtorAccount = createDebtorAccount(jsonObject.get("debtorAccount"));
        } else {
            debtorAccount = null;
        }
        final TransactionAmount transactionAmount = createTransactionAmount(jsonObject.get("transactionAmount"));
        final LocalDate bookingDate = LocalDate.parse(jsonObject.get("bookingDate").getAsString());
        final LocalDate valueDate = LocalDate.parse(jsonObject.get("valueDate").getAsString());
        final String remittanceInformationUnstructured = jsonObject.get("remittanceInformationUnstructured")
                .getAsString();
        return new BookedTransaction(transactionId, debtorName, debtorAccount, transactionAmount, bookingDate,
                valueDate, remittanceInformationUnstructured);
    }

    private static PendingTransaction createPendingTransaction(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final TransactionAmount transactionAmount = createTransactionAmount(jsonObject.get("transactionAmount"));
        final LocalDate valueDate = LocalDate.parse(jsonObject.get("valueDate").getAsString());
        final String remittanceInformationUnstructured = jsonObject.get("remittanceInformationUnstructured")
                .getAsString();
        return new PendingTransaction(transactionAmount, valueDate, remittanceInformationUnstructured);
    }

    private static DebtorAccount createDebtorAccount(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        return new DebtorAccount();
    }

    private static TransactionAmount createTransactionAmount(@NonNull final JsonElement json) {
        Objects.requireNonNull(json, "json must not be null");
        final JsonObject jsonObject = json.getAsJsonObject();
        final String currency = jsonObject.get("currency").getAsString();
        final BigDecimal amount = new BigDecimal(jsonObject.get("amount").getAsString());
        return new TransactionAmount(currency, amount);
    }

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
}
