package com.ledgerline.account;

import com.ledgerline.AbstractContainerTest;
import com.ledgerline.account.entity.AccountType;
import com.ledgerline.account.entity.Account;
import com.ledgerline.account.entity.Status;
import com.ledgerline.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AccountRepositoryTests extends AbstractContainerTest {

    @Autowired
    private AccountRepository accountRepository;


    @Test
    void seedSystemFundingAccount() {
        List<Account> ledgerLineFunding = accountRepository
                .findAll()
                .stream()
                .filter(a -> a.getOwnerName().equals("Ledgerline Funding"))
                .filter(a -> a.getType() == AccountType.SYSTEM_FUNDING)
                .toList();
        assertThat(ledgerLineFunding).hasSize(1);
        assertThat(ledgerLineFunding.getFirst().getType()).isEqualTo(AccountType.SYSTEM_FUNDING);
        assertThat(ledgerLineFunding.getFirst().getBalancePaise()).isZero();
        assertThat(ledgerLineFunding.getFirst().getStatus()).isEqualTo(Status.ACTIVE);
    }


    @Test
    void rejectsNegativeBalanceOnCustomerAccount() {
        Account customer = Account.builder()
                .publicId(UUID.randomUUID())
                .ownerName("OverDrawer")
                .type(AccountType.CUSTOMER)
                .currency("INR")
                .balancePaise(-100L)
                .status(Status.ACTIVE)
                .build();

        assertThatThrownBy(() -> accountRepository.saveAndFlush(customer))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsNegativeBalanceOnSystemFundingAccount() {
        Account systemFunding = Account.builder()
                .publicId(UUID.randomUUID())
                .ownerName("System Funding")
                .type(AccountType.SYSTEM_FUNDING)
                .currency("INR")
                .balancePaise(-100L)
                .status(Status.ACTIVE)
                .build();

        assertThatCode(() -> accountRepository.saveAndFlush(systemFunding))
                .doesNotThrowAnyException();
    }


}
