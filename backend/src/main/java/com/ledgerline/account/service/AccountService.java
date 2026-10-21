package com.ledgerline.account.service;

import com.ledgerline.account.dto.AccountCreateRequest;
import com.ledgerline.account.dto.AccountResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface AccountService {

    AccountResponse createAccount(AccountCreateRequest createRequest);

    AccountResponse getAccountByPublicId(UUID publicId);
}
