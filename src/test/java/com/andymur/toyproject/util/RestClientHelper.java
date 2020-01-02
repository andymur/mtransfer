package com.andymur.toyproject.util;

import com.andymur.toyproject.MoneyTransferAcceptanceTest;
import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.TransferOperationResult;
import com.andymur.toyproject.core.util.TransferOperation;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static javax.ws.rs.RuntimeType.CLIENT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RestClientHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientHelper.class);
    private static final String BASE_URL_PATTERN = "http://%s:%d/account/";

    private final Client restClient = new JerseyClientBuilder().build();

    private final String host;
    private final int port;

    private RestClientHelper(final String host,
                             final int port) {
        this.host = host;
        this.port = port;
    }

    public static RestClientHelper of(final String host,
                                      final int port) {
        return new RestClientHelper(host, port);
    }

    public AccountState getAccount(final long accountId) {
        LOGGER.info("Fetching account in the test; accountId = {}", accountId);
        Response response = requestAccountStateResponse(accountId);
        assertThat("Account status request has been failed",
                response.getStatus(), is(HttpStatus.OK_200));
        AccountState result = response.readEntity(AccountState.class);
        return result;
    }

    public void createAccount(final long accountId,
                              final BigDecimal amount) {
        createAccount(new AccountState(accountId, amount));
    }

    public void createAccount(final AccountState accountState) {
        LOGGER.info("Creating account in the test; account = {}", accountState);
        Response response = putAccountRequestResponse(accountState);
        assertThat("Account creation request has failed",
                response.getStatus(), is(HttpStatus.OK_200));
    }

    public void transfer(final TransferOperation transferOperation) {
        transfer(transferOperation, result -> result.getStatus() == TransferOperationResult.Status.SUCCESS);
    }

    public void transfer(final TransferOperation transferOperation,
                         final Predicate<TransferOperationResult> checkTransferResult) {

        Response response = transferRequestResponse(transferOperation.getSourceAccountId(),
                transferOperation.getDestinationAccountId(),
                transferOperation.getAmountToTransfer());

        TransferOperationResult transferResult = response.readEntity(TransferOperationResult.class);

        assertThat("Money transfer request has failed",
                response.getStatus(), is(HttpStatus.OK_200));

        Assert.assertTrue("Money transfer operation result check failed",
                checkTransferResult.test(transferResult));
    }

    public void deleteAccount(final long accountId) {
        Response deletedAccountResponse = deleteAccountRequestResponse(accountId);
        assertThat(deletedAccountResponse.getStatus(), is(HttpStatus.OK_200));
    }

    private Response transferRequestResponse(final long sourceAccountId,
                                             final long destinationAccountId,
                                             final BigDecimal amountToTransfer) {
        Response result = restClient.target(
                String.format(BASE_URL_PATTERN + "%d/%d/%s", host, port,
                        sourceAccountId, destinationAccountId, amountToTransfer)
        ).request()
                .post(Entity.entity(Void.class, MediaType.APPLICATION_JSON_TYPE));

        return result;
    }

    private Response putAccountRequestResponse(final AccountState account) {
        return restClient.target(
                String.format(BASE_URL_PATTERN, host, port))
                .request()
                .put(Entity.entity(account, MediaType.APPLICATION_JSON));
    }

    public Response requestAccountStateResponse(final long id) {
        return restClient.target(
                String.format(BASE_URL_PATTERN + "%d", host, port, id))
                .request()
                .get();
    }

    private Response deleteAccountRequestResponse(final long id) {
        return restClient.target(
                String.format(BASE_URL_PATTERN + "%d", host, port, id)
        ).request()
                .delete();
    }
}
