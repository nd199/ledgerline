package com.ledgerline.account.service;

import com.ledgerline.common.exception.ResourceNotFound;
import com.ledgerline.account.dto.AccountCreateRequest;
import com.ledgerline.account.dto.AccountResponse;
import com.ledgerline.account.entity.Account;
import com.ledgerline.account.entity.Status;
import com.ledgerline.account.mapper.AccountMapper;
import com.ledgerline.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final String CURRENCY_INR = "INR";

    private final AccountRepository accountRepository;

    private final AccountMapper accountMapper;

    @Override
    public AccountResponse createAccount(AccountCreateRequest createRequest) {
        Account account = Account.builder()
                .publicId(UUID.randomUUID())
                .ownerName(createRequest.ownerName())
                .type(createRequest.type())
                .currency(CURRENCY_INR)
                .balancePaise(0L)
                .status(Status.ACTIVE)
                .build();

        return accountMapper.apply(accountRepository.save(account));
    }

    @Override
    public AccountResponse getAccountByPublicId(UUID publicId) {
        Account account = accountRepository.getAccountByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFound(
                        String.format("Account with publicId %s not found", publicId)));

        return accountMapper.apply(account);
    }
}
