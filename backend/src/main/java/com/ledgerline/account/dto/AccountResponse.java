package com.ledgerline.account.dto;

import com.ledgerline.account.entity.AccountType;
import com.ledgerline.account.entity.Status;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID publicId,
        String ownerName,
        AccountType type,
        String currency,
        long balancePaise,
        Status status,
        Instant createdAt) {
}
