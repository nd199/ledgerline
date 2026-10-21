package com.ledgerline.account;

import com.ledgerline.AbstractContainerTest;
import com.ledgerline.account.dto.AccountCreateRequest;
import com.ledgerline.account.dto.AccountResponse;
import com.ledgerline.account.entity.AccountType;
import com.ledgerline.account.entity.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountControllerTests extends AbstractContainerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createsAccountAndReadsItBack() {
        ResponseEntity<AccountResponse> created = restTemplate
                .postForEntity("/api/v1/accounts",
                        new AccountCreateRequest("Ravi", AccountType.CUSTOMER),
                        AccountResponse.class);

        //Verify The Response
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        AccountResponse body = created.getBody();
        assertThat(body).isNotNull();
        assertThat(body.publicId()).isNotNull();
        assertThat(body.ownerName()).isEqualTo("Ravi");
        assertThat(body.type()).isEqualTo(AccountType.CUSTOMER);
        assertThat(body.currency()).isEqualTo("INR");
        assertThat(body.balancePaise()).isZero();
        assertThat(body.status()).isEqualTo(Status.ACTIVE);
        assertThat(body.createdAt()).isNotNull();
        assertThat(created.getHeaders().getLocation())
                .isNotNull()
                .hasPath("/api/v1/accounts/" + body.publicId());

        AccountResponse fetched = restTemplate.getForObject(
                "/api/v1/accounts/" + body.publicId(), AccountResponse.class
        );

        assertThat(fetched).isEqualTo(body);
    }
    @Test
    void returnsNotFoundForUnknownAccount() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("/api/v1/accounts/" + UUID.randomUUID(), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("ACCOUNT_NOT_FOUND").doesNotContain("Trace");
    }

    @Test
    void rejectsForABlankOwnerName() {
        ResponseEntity<String> response = restTemplate
                .postForEntity("/api/v1/accounts",
                        new AccountCreateRequest("", AccountType.CUSTOMER),
                        String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("INVALID_REQUEST").doesNotContain("Trace");
    }

    @Test
    void rejectsMalformedPublicId() {
        ResponseEntity<String> response = restTemplate
                .getForEntity("/api/v1/accounts/not-a-uuid", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("INVALID_REQUEST");
    }


}
