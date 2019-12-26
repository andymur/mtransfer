package com.andymur.toyproject;

import com.andymur.toyproject.core.AccountState;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.JerseyClientBuilder;
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

public class AccountAcceptanceTest {

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
        Response response = requestAccount(CLIENT, 1);
        assertThat(response.getStatus(), is(HttpStatus.OK_200));
        AccountState requestedAccount = response.readEntity(AccountState.class);
        assertThat(requestedAccount, is(AccountState.DEFAULT));
    }

    @Test
    public void shouldAddAccountWhenNotExisted() {
        final AccountState newAccount = new AccountState(1, BigDecimal.TEN);

        Response putAccountMethodResponse = putAccount(CLIENT, newAccount);

        assertThat(putAccountMethodResponse.getStatus(), is(HttpStatus.OK_200));
        AccountState createdAccount = putAccountMethodResponse.readEntity(AccountState.class);
        assertThat(createdAccount, is(newAccount));

        Response getAccountMethodResponse = requestAccount(CLIENT, 1);

        assertThat(getAccountMethodResponse.getStatus(), is(HttpStatus.OK_200));
        AccountState requestedAccount = getAccountMethodResponse.readEntity(AccountState.class);
        assertThat(requestedAccount, is(newAccount));
    }

    @Test
    public void shouldDeleteAccountWhenItDoesExist() {
        final AccountState accountToDelete = new AccountState(1, BigDecimal.TEN);
        putAccount(CLIENT, accountToDelete);

        Response deletedAccountResponse = deleteAccount(CLIENT, 1);
        assertThat(deletedAccountResponse.getStatus(), is(HttpStatus.OK_200));
    }

    private Response deleteAccount(Client client, long id) {
        return client.target(
                String.format("http://localhost:%d/account/%d", SUPPORT.getLocalPort(), id)
        ).request()
        .delete();
    }

    private Response requestAccount(Client client, long id) {
        return client.target(
                String.format("http://localhost:%d/account/%d", SUPPORT.getLocalPort(), id))
                .request()
                .get();
    }

    private Response putAccount(Client client, AccountState account) {
        return client.target(
                String.format("http://localhost:%d/account/", SUPPORT.getLocalPort()))
                .request()
                .put(Entity.entity(account, MediaType.APPLICATION_JSON));
    }

}
