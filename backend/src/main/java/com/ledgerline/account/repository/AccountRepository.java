package com.ledgerline.account.repository;

import com.ledgerline.account.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> getAccountByPublicId(UUID publicId);

    Page<Account> getAccountByOwnerName(String ownerName, Pageable pageable);
}
