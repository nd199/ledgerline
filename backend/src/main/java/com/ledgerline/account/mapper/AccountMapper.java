package com.ledgerline.account.mapper;

import com.ledgerline.account.dto.AccountResponse;
import com.ledgerline.account.entity.Account;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class AccountMapper implements Function<Account, AccountResponse> {

    @Override
    public AccountResponse apply(Account account) {
        return new AccountResponse(
                account.getPublicId(),
                account.getOwnerName(),
                account.getType(),
                account.getCurrency(),
                account.getBalancePaise(),
                account.getStatus(),
                account.getCreatedAt());
    }
}
