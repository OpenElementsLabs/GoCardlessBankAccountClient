package com.openelements.cardless.data;

import java.time.ZonedDateTime;

public record Account(String id,
                      ZonedDateTime created,
                      ZonedDateTime lastAccessed,
                      String iban,
                      String bban,
                      String status,
                      String institutionId,
                      String owner_name,
                      String name) {
}
