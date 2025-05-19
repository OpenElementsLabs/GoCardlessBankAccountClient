package com.openelements.cardless;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
        return new Requisition(id, created, redirect, status, institution_id, agreement, reference, link);
    }
}
