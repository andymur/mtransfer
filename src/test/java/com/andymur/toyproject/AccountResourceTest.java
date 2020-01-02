package com.andymur.toyproject;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.TransferOperationResult;
import com.andymur.toyproject.core.util.TransferOperation;
import com.andymur.toyproject.util.RestClientHelper;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccountResourceTest {

    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("mtransfer-test.yml");
    private static final Client CLIENT = new JerseyClientBuilder().build();

    private static RestClientHelper REST_CLIENT_HELPER;

    private static final DropwizardTestSupport<MTransferConfiguration> SUPPORT =
            new DropwizardTestSupport<>(MTransferApplication.class,
                    CONFIG_PATH);

    @BeforeEach
    public void beforeClass() throws Exception {
        SUPPORT.before();
        REST_CLIENT_HELPER = RestClientHelper.of("localhost", SUPPORT.getLocalPort());
    }

    @AfterEach
    public void afterClass() {
        SUPPORT.after();
    }

    @Test
    public void shouldReturnNoContentWhenRequestedAccountDoesNotExist() {
        Response response = REST_CLIENT_HELPER.requestAccountStateResponse(1L);
        assertThat(response.getStatus(), is(HttpStatus.NO_CONTENT_204));
    }

    @Test
    public void shouldAddAccountWhenNotExisted() {
        final AccountState newAccount = new AccountState(1, BigDecimal.TEN);

        REST_CLIENT_HELPER.createAccount(newAccount);
        final AccountState requestedAccount = REST_CLIENT_HELPER.getAccount(1L);
        assertThat(requestedAccount, is(newAccount));
    }

    @Test
    public void shouldAddAccountsAndMakeTransfersCorrectly() {
        REST_CLIENT_HELPER.createAccount(1L, new BigDecimal("100.00"));
        REST_CLIENT_HELPER.createAccount(2L, new BigDecimal("200.00"));
        REST_CLIENT_HELPER.transfer(TransferOperation.of(1L, 2L,  new BigDecimal("50.00")));
        AccountState firstAccountState = REST_CLIENT_HELPER.getAccount(1L);
        AccountState secondAccountState = REST_CLIENT_HELPER.getAccount(2L);

        Assert.assertThat("Fifty units must be withdrawn from the first account",
                firstAccountState, is(new AccountState(1L, new BigDecimal("50.00"))));

        Assert.assertThat("Fifty units must be added to the second account", secondAccountState,
                is(new AccountState(2L, new BigDecimal("250.00"))));
    }

    @Test
    public void shouldShowFailedStatusWhenTryingToTransferMoreMoneyThanAccountHas() {
        REST_CLIENT_HELPER.createAccount(1L, new BigDecimal("100.00"));
        REST_CLIENT_HELPER.createAccount(2L, new BigDecimal("200.00"));

        REST_CLIENT_HELPER.transfer(TransferOperation.of(1L, 2L,  new BigDecimal("150.00")),
                result -> result.getStatus() == TransferOperationResult.Status.FAILED);

        AccountState firstAccountState = REST_CLIENT_HELPER.getAccount(1L);
        AccountState secondAccountState = REST_CLIENT_HELPER.getAccount(2L);

        Assert.assertThat("One hundred and fifty units must not be withdrawn from the first account",
                firstAccountState, is(new AccountState(1L, new BigDecimal("100.00"))));

        Assert.assertThat("One hundred and fifty units must not be added to the second account", secondAccountState,
                is(new AccountState(2L, new BigDecimal("200.00"))));
    }

    @Test
    public void shouldDeleteAccountWhenItDoesExist() {
        final AccountState accountToDelete = new AccountState(1L, BigDecimal.TEN);
        REST_CLIENT_HELPER.createAccount(accountToDelete);
        REST_CLIENT_HELPER.deleteAccount(1L);
    }
}
