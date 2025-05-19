package com.openelements.cardless;

import java.time.LocalDateTime;

public record AccessAndRefreshToken(String access, long accessExpires, String refresh, long refreshExpires,
                                    LocalDateTime createdAt) {

    public AccessAndRefreshToken(String access, long accessExpires, String refresh, long refreshExpires) {
        this(access, accessExpires, refresh, refreshExpires, LocalDateTime.now());
    }

    public boolean willExpireShortly() {
        return createdAt.plusSeconds(accessExpires).isBefore(LocalDateTime.now().plusSeconds(10));
    }
}
