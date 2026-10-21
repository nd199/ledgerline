package com.ledgerline.account.dto;

import com.ledgerline.account.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(

        @NotBlank(message = "Owner name must not be blank")
        @Size(max = 50, message = "Owner name must not exceed 50 characters")
        String ownerName,

        @NotNull(message = "Type must not be null")
        AccountType type) {
}
