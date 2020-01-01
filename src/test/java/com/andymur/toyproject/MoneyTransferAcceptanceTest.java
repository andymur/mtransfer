package com.andymur.toyproject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.TransferOperationsAuditLog;
import com.andymur.toyproject.core.utils.Pair;
import com.andymur.toyproject.core.utils.TransferOperation;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.andymur.toyproject.core.utils.Generator.generateInt;
import static org.hamcrest.CoreMatchers.is;

//TODO: document me
public class MoneyTransferAcceptanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoneyTransferAcceptanceTest.class);

    private static int NUMBER_OF_THREADS = 5;

    private static final ExecutorService CREATION_EXECUTOR_SERVICE = Executors.newFixedThreadPool(NUMBER_OF_THREADS,
            new ThreadFactoryBuilder().setNameFormat("create-account-%d").build());
    ;

    private static final ExecutorService TRANSFERRING_EXECUTOR_SERVICE = Executors.newFixedThreadPool(NUMBER_OF_THREADS,
            new ThreadFactoryBuilder().setNameFormat("transfer-money-%d").build());
    ;

    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("mtransfer-test-with-persistence.yml");

    private static final Client CLIENT = new JerseyClientBuilder().build();
    private static final DropwizardTestSupport<MTransferConfiguration> SUPPORT =
            new DropwizardTestSupport<>(MTransferApplication.class,
                    CONFIG_PATH);

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000.00");
    private static final Pair<Integer, Integer> FROM_TO_ACCOUNT_NUMBER = Pair.of(10, 25);
    private static final Pair<Integer, Integer> FROM_TO_TRANSFER_SIZE = Pair.of(10, 20);
    private static final Pair<Integer, Integer> FROM_TO_OPERATIONS = Pair.of(1000, 2000);

    private TransferOperationsAuditLog resourceAuditLog;

    @BeforeEach
    public void beforeClass() throws Exception {
        SUPPORT.before();
        resourceAuditLog = ((MTransferApplication) SUPPORT.getApplication()).getResourceAuditLog();
    }

    @AfterEach
    public void afterClass() {
        SUPPORT.after();
    }


    @Test
    public void shouldHaveCorrectAccountDetailsAfterMoneyTransferring() throws InterruptedException {

        final int accountsNumber = generateInt(FROM_TO_ACCOUNT_NUMBER);
        LOGGER.info("starting the acceptance test, with number of accounts = {}", accountsNumber);

        final List<TransferOperation> transferOperations = generateTransferOperations(accountsNumber, FROM_TO_OPERATIONS, FROM_TO_TRANSFER_SIZE);
        LOGGER.info("Generation of transfer operation process has been done. {}", stringifyTransferOperations(transferOperations));

        final List<AccountState> accountsToCreate = prepareAccountsToCreate(accountsNumber);
        LOGGER.info("Accounts are prepared for creation. {}", accountsToCreate);

        //creating accounts
        createAllAccounts(CREATION_EXECUTOR_SERVICE, accountsToCreate);

        //making all the transfers
        makeAllTransfers(TRANSFERRING_EXECUTOR_SERVICE, transferOperations);

        final List<AccountState> accountsActualFinalState = getAccountsActualState(accountsNumber);
        LOGGER.info("Actual accounts final state. {}", accountsActualFinalState);

        LOGGER.info("Resource log transfer requests. {}", resourceAuditLog.stringifyLog());

        final List<AccountState> accountsExpectedFinalState = calculateAccountsFinalState(accountsToCreate, resourceAuditLog.getLog());
        LOGGER.info("Calculated accounts final state. {}", accountsExpectedFinalState);

        //check all transfers made in a correct way
        checkAllMoneyTransferredCorrectly(accountsExpectedFinalState, accountsActualFinalState);
        checkWarningCauseOfJerseyDuplicateRequests(calculateAccountsFinalState(accountsToCreate, transferOperations),
                accountsActualFinalState);
    }

    //TODO: extract transferRequestResponse like methods, see AccountResourceTest

    /*************************************************/

    private static AccountState getAccount(final long accountId) {
        LOGGER.info("Fetching account in the test; accountId = {}", accountId);
        Response response = requestAccountStateResponse(CLIENT, accountId);
        Assert.assertThat("Account status request has been successfully done",
                response.getStatus(), is(HttpStatus.OK_200));
        AccountState result = response.readEntity(AccountState.class);
        return result;
    }

    private static long createAccount(final AccountState accountState) {
        LOGGER.info("Creating account in the test; account = {}", accountState);
        Response response = putAccountRequestResponse(CLIENT, accountState);
        Assert.assertThat("Account creation request has been successfully done",
                response.getStatus(), is(HttpStatus.OK_200));
        return accountState.getId();
    }

    private static void transfer(final TransferOperation transferOperation) {
        Response response = transferRequestResponse(CLIENT, transferOperation.getSourceAccountId(),
                transferOperation.getDestinationAccountId(),
                transferOperation.getAmountToTransfer());
        String transferResult = response.readEntity(String.class);

        Assert.assertThat("Money transfer request has been successfully done",
                response.getStatus(), is(HttpStatus.NO_CONTENT_204));
        Assert.assertThat("Money transfer request has been successfully done",
                transferResult, is("OK"));
    }

    /*************************************************/

    private String stringifyTransferOperations(final List<TransferOperation> transferOperations) {
        final List<String> resultList = new ArrayList<>(transferOperations.size());
        for (final TransferOperation operation : transferOperations) {
            resultList.add(String.format("(%d, %d, %s)",
                    operation.getSourceAccountId(), operation.getDestinationAccountId(), operation.getAmountToTransfer()));
        }
        return String.join(", ", resultList);
    }

    private static Response transferRequestResponse(final Client client,
                                                    final long sourceAccountId,
                                                    final long destinationAccountId,
                                                    final BigDecimal amountToTransfer) {
        Response result = client.target(
                String.format("http://localhost:%d/account/%d/%d/%s", SUPPORT.getLocalPort(), sourceAccountId, destinationAccountId, amountToTransfer)
        ).request()
                .post(Entity.entity(Void.class, MediaType.APPLICATION_JSON_TYPE));
        return result;
    }

    private static Response requestAccountStateResponse(final Client client,
                                                        final long id) {
        return client.target(
                String.format("http://localhost:%d/account/%d", SUPPORT.getLocalPort(), id))
                .request()
                .get();
    }

    private static Response putAccountRequestResponse(final Client client,
                                                      final AccountState account) {
        return client.target(
                String.format("http://localhost:%d/account/", SUPPORT.getLocalPort()))
                .request()
                .put(Entity.entity(account, MediaType.APPLICATION_JSON));
    }

    /*************************************************/
    private static void createAllAccounts(final ExecutorService executorService,
                                          final List<AccountState> accountsToCreate) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(accountsToCreate.size());

        for (final AccountState account : accountsToCreate) {
            executorService.submit(() -> {
                try {
                    createAccount(account);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    private static void makeAllTransfers(final ExecutorService executorService,
                                         final List<TransferOperation> transferOperations) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(transferOperations.size());
        for (final TransferOperation transferOperation : transferOperations) {
            executorService.submit(() -> {
                try {
                    transfer(transferOperation);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30L, TimeUnit.SECONDS);
    }

    //TODO: add comment
    private void checkWarningCauseOfJerseyDuplicateRequests(final List<AccountState> accountExpectedFinalStates,
                                                            final List<AccountState> accountActualFinalStates) {
        checkAllMoneyTransferredCorrectly(accountExpectedFinalStates, accountActualFinalStates, true);
    }

    private void checkAllMoneyTransferredCorrectly(final List<AccountState> accountsExpectedFinalState,
                                                   final List<AccountState> accountsActualFinalState) {
        checkAllMoneyTransferredCorrectly(accountsExpectedFinalState, accountsActualFinalState, false);
    }

    private void checkAllMoneyTransferredCorrectly(final List<AccountState> accountsExpectedFinalState,
                                                   final List<AccountState> accountsActualFinalState,
                                                   final boolean raiseOnlyWarning) {
        int idx = 0;
        for (final AccountState accountFinalState : accountsExpectedFinalState) {
            final AccountState actualAccountState = accountsActualFinalState.get(idx++);
            if (raiseOnlyWarning && !accountFinalState.equals(actualAccountState)) {
                LOGGER.warn("Actual and expected account states must be equal to each other, actual = {}, expected = {}",
                        actualAccountState, accountFinalState);
            } else {
                Assert.assertThat("Actual and expected account states must be equal to each other",
                        actualAccountState, is(accountFinalState));
            }
        }
    }

    private List<AccountState> getAccountsActualState(final int accountsNumber) {
        final List<AccountState> result = new ArrayList<>(accountsNumber);
        for (int accountId = 1; accountId <= accountsNumber; accountId++) {
            result.add(getAccount(accountId));
        }
        return result;
    }

    private List<AccountState> calculateAccountsFinalState(final List<AccountState> initialAccountsState,
                                                           final List<TransferOperation> transferOperations) {
        final Map<Long, BigDecimal> nettedOperations = calculateNettedOperations(initialAccountsState.size(), transferOperations);
        return applyNettedOperations(initialAccountsState, nettedOperations);
    }

    private List<AccountState> prepareAccountsToCreate(final int accountsNumber) {
        return IntStream.range(1, accountsNumber + 1)
                .mapToObj(id -> new AccountState(id, ONE_MILLION))
                .collect(Collectors.toList());
    }

    private Map<Long, BigDecimal> calculateNettedOperations(final int accountNumber,
                                                            final List<TransferOperation> transferOperations) {
        final Map<Long, BigDecimal> result = new HashMap<>(accountNumber);

        for (long accountId = 1; accountId <= accountNumber; accountId++) {
            final long currentAccountId = accountId;

            BigDecimal outSum = transferOperations.stream()
                    .filter(operation -> operation.getSourceAccountId() == currentAccountId)
                    .map(TransferOperation::getAmountToTransfer).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

            BigDecimal inSum = transferOperations.stream()
                    .filter(operation -> operation.getDestinationAccountId() == currentAccountId)
                    .map(TransferOperation::getAmountToTransfer).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

            result.put(accountId, inSum.subtract(outSum));
        }

        return result;
    }

    private List<AccountState> applyNettedOperations(final List<AccountState> accountStates,
                                                     final Map<Long, BigDecimal> nettedOperations) {
        final List<AccountState> result = new ArrayList<>();

        for (final AccountState accountState : accountStates) {
            BigDecimal nettedAmountToApply = nettedOperations.getOrDefault(accountState.getId(), BigDecimal.ZERO);
            final AccountState accountFinalState = accountState.copyOf();
            accountFinalState.addAmount(nettedAmountToApply);
            result.add(accountFinalState);
        }

        return result;
    }


    private List<TransferOperation> generateTransferOperations(final int accountsNumber,
                                                               final Pair<Integer, Integer> fromToOperations,
                                                               final Pair<Integer, Integer> fromToTransferSize) {

        final int numberOfOperations = generateInt(fromToOperations);
        final Pair<Integer, Integer> fromToAccountIds = Pair.of(1, accountsNumber);

        final List<TransferOperation> result = new ArrayList<>(numberOfOperations);

        for (int i = 0; i < numberOfOperations; i++) {
            result.add(TransferOperation.generate(fromToAccountIds, fromToTransferSize));
        }

        return result;
    }
}
