package com.andymur.toyproject;

import com.andymur.toyproject.core.AccountState;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

//TODO: document me
public class AccountResourceTest {
    //TODO: test transfer, no sufficient funds, same account, lower & greater, to/from non existed account
    //TODO: Add persistence to AccountService (mock it here)
    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("mtransfer-test.yml");
    private static final Client CLIENT = new JerseyClientBuilder().build();
    private static final DropwizardTestSupport<MTransferConfiguration> SUPPORT =
            new DropwizardTestSupport<>(MTransferApplication.class,
                    CONFIG_PATH);

    @BeforeEach
    public void beforeClass() throws Exception {
        SUPPORT.before();
    }

    @AfterEach
    public void afterClass() {
        SUPPORT.after();
    }

    @Test
    public void shouldReturnDefaultAccountWhenRequestedAccountDoesNotExist() {
        Response response = requestAccountStateResponse(CLIENT, 1);
        assertThat(response.getStatus(), is(HttpStatus.OK_200));
        AccountState requestedAccount = response.readEntity(AccountState.class);
        assertThat(requestedAccount, is(AccountState.DEFAULT));
    }

    @Test
    public void shouldAddAccountWhenNotExisted() {
        final AccountState newAccount = new AccountState(1, BigDecimal.TEN);

        Response putAccountMethodResponse = putAccountRequestResponse(CLIENT, newAccount);

        assertThat(putAccountMethodResponse.getStatus(), is(HttpStatus.OK_200));
        AccountState createdAccount = putAccountMethodResponse.readEntity(AccountState.class);
        assertThat(createdAccount, is(newAccount));

        Response getAccountMethodResponse = requestAccountStateResponse(CLIENT, 1);

        assertThat(getAccountMethodResponse.getStatus(), is(HttpStatus.OK_200));
        AccountState requestedAccount = getAccountMethodResponse.readEntity(AccountState.class);
        assertThat(requestedAccount, is(newAccount));
    }

    @Test
    public void shouldAddAccountsAndMakeTransfersCorrectly() {
        createAccount(1L, new BigDecimal("100.00"));
        createAccount(2L, new BigDecimal("200.00"));
        transfer(1L, 2L,  new BigDecimal("50.00"));
        AccountState firstAccountState = getAccount(1L);
        AccountState secondAccountState = getAccount(2L);

        Assert.assertThat("Fifty units must be withdrawn from the first account",
                firstAccountState, is(new AccountState(1L, new BigDecimal("50.00"))));

        Assert.assertThat("Fifty units must be added to the second account", secondAccountState,
                is(new AccountState(2L, new BigDecimal("250.00"))));
    }

    @Test
    public void shouldDeleteAccountWhenItDoesExist() {
        final AccountState accountToDelete = new AccountState(1, BigDecimal.TEN);
        putAccountRequestResponse(CLIENT, accountToDelete);

        Response deletedAccountResponse = deleteAccountRequestResponse(CLIENT, 1);
        assertThat(deletedAccountResponse.getStatus(), is(HttpStatus.OK_200));
    }

    private static long createAccount(long id) {
        return createAccount(id, new BigDecimal("0.00"));
    }

    private static long createAccount(long id, BigDecimal initialAmount) {
        Response response = putAccountRequestResponse(CLIENT, new AccountState(id, initialAmount));
        Assert.assertThat("Account creation request has been successfully done",
                response.getStatus(), is(HttpStatus.OK_200));
        return id;
    }

    private static void transfer(long sourceAccountId,
                                 long destinationAccountId,
                                 BigDecimal amountToTransfer) {
        Response response = transferRequestResponse(CLIENT, sourceAccountId, destinationAccountId, amountToTransfer);
        Assert.assertThat("Money transfer request has been successfully done",
                response.getStatus(), is(HttpStatus.NO_CONTENT_204));
    }

    private static AccountState getAccount(long accountId) {
        Response response = requestAccountStateResponse(CLIENT, accountId);
        Assert.assertThat("Account status request has been successfully done",
                response.getStatus(), is(HttpStatus.OK_200));
        return response.readEntity(AccountState.class);
    }

    private static Response transferRequestResponse(Client client,
                                                    long sourceAccountId,
                                                    long destinationAccountId,
                                                    BigDecimal amountToTransfer) {
        return client.target(
                String.format("http://localhost:%d/account/%d/%d/%s", SUPPORT.getLocalPort(), sourceAccountId, destinationAccountId, amountToTransfer)
        ).request()
        .post(Entity.entity(Void.class, MediaType.APPLICATION_JSON_TYPE));
    }

    private static Response deleteAccountRequestResponse(Client client, long id) {
        return client.target(
                String.format("http://localhost:%d/account/%d", SUPPORT.getLocalPort(), id)
        ).request()
        .delete();
    }

    private static Response requestAccountStateResponse(Client client, long id) {
        return client.target(
                String.format("http://localhost:%d/account/%d", SUPPORT.getLocalPort(), id))
                .request()
                .get();
    }

    private static Response putAccountRequestResponse(Client client, AccountState account) {
        return client.target(
                String.format("http://localhost:%d/account/", SUPPORT.getLocalPort()))
                .request()
                .put(Entity.entity(account, MediaType.APPLICATION_JSON));
    }

}
