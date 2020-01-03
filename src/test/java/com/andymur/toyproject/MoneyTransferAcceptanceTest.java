package com.andymur.toyproject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.TransferOperationsAuditLog;
import com.andymur.toyproject.core.util.Pair;
import com.andymur.toyproject.core.util.TransferOperation;
import com.andymur.toyproject.util.AcceptanceTestHelper;
import com.andymur.toyproject.util.RestClientHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.andymur.toyproject.core.util.Generator.generateInt;
import static com.andymur.toyproject.util.AcceptanceTestHelper.calculateAccountsFinalState;
import static com.andymur.toyproject.util.AcceptanceTestHelper.checkAllMoneyTransferredCorrectly;
import static com.andymur.toyproject.util.AcceptanceTestHelper.generateTransferOperations;
import static com.andymur.toyproject.util.AcceptanceTestHelper.prepareAccountsToCreate;
import static com.andymur.toyproject.util.AcceptanceTestHelper.stringifyTransferOperations;

/**
 * Very similar to AccountServiceTest test (in term of test steps and idea)
 *
 * The difference is that test does it in the whole system without much mocks.
 *
 * (Only persistence is mocked, we can/should definitely get rid of this also,
 * Persistence tested in PersistenceServiceTest).
 *
 */
public class MoneyTransferAcceptanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoneyTransferAcceptanceTest.class);

    private static int NUMBER_OF_THREADS = 5;

    private static final ExecutorService CREATION_EXECUTOR_SERVICE = Executors.newFixedThreadPool(NUMBER_OF_THREADS,
            new ThreadFactoryBuilder().setNameFormat("create-account-%d").build());
    ;

    private static final ExecutorService TRANSFERRING_EXECUTOR_SERVICE = Executors.newFixedThreadPool(NUMBER_OF_THREADS,
            new ThreadFactoryBuilder().setNameFormat("transfer-money-%d").build());
    ;

    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("mtransfer-test.yml");

    private static final DropwizardTestSupport<MTransferConfiguration> SUPPORT =
            new DropwizardTestSupport<>(MTransferApplication.class,
                    CONFIG_PATH);

    private static RestClientHelper REST_CLIENT_HELPER;

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000.00");
    private static final Pair<Integer, Integer> FROM_TO_ACCOUNT_NUMBER = Pair.of(10, 25);
    private static final Pair<Integer, Integer> FROM_TO_TRANSFER_SIZE = Pair.of(10, 20);
    private static final Pair<Integer, Integer> FROM_TO_OPERATIONS = Pair.of(1000, 2000);

    private TransferOperationsAuditLog resourceAuditLog;

    @BeforeEach
    public void beforeClass() throws Exception {
        SUPPORT.before();
        resourceAuditLog = ((MTransferApplication) SUPPORT.getApplication()).getResourceAuditLog();
        REST_CLIENT_HELPER = RestClientHelper.of("localhost", SUPPORT.getLocalPort());
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

        final List<AccountState> accountsToCreate = prepareAccountsToCreate(accountsNumber, ONE_MILLION);
        LOGGER.info("Accounts are prepared for creation. {}", accountsToCreate);

        //creating accounts
        createAllAccounts(CREATION_EXECUTOR_SERVICE, REST_CLIENT_HELPER, accountsToCreate);

        //making all the transfers
        makeAllTransfers(TRANSFERRING_EXECUTOR_SERVICE, REST_CLIENT_HELPER, transferOperations);

        final List<AccountState> accountsActualFinalState = getAccountsActualState(REST_CLIENT_HELPER, accountsNumber);
        LOGGER.info("Actual accounts final state. {}", accountsActualFinalState);

        LOGGER.info("Resource log transfer requests. {}", resourceAuditLog.stringifyLog());

        // See checkWarningCauseOfJerseyDuplicateRequests
        final List<AccountState> accountsExpectedFinalState = calculateAccountsFinalState(accountsToCreate, resourceAuditLog.getLog());
        LOGGER.info("Calculated accounts final state. {}", accountsExpectedFinalState);

        //check all transfers made in a correct way
        checkAllMoneyTransferredCorrectly(accountsExpectedFinalState, accountsActualFinalState);
        checkWarningCauseOfJerseyDuplicateRequests(calculateAccountsFinalState(accountsToCreate, transferOperations),
                accountsActualFinalState);
    }

    private static void createAllAccounts(final ExecutorService executorService,
                                          final RestClientHelper restClientHelper,
                                          final List<AccountState> accountsToCreate) throws InterruptedException {
        AcceptanceTestHelper.createAllAccounts(executorService,
                accountsToCreate,
                restClientHelper::createAccount,
                5L);
    }

    private static void makeAllTransfers(final ExecutorService executorService,
                                         final RestClientHelper restClientHelper,
                                         final List<TransferOperation> transferOperations) throws InterruptedException {
        AcceptanceTestHelper.makeAllTransfers(executorService, transferOperations, restClientHelper::transfer, 30L);
    }

    // I've found that sometimes jersey rest client send request twice
    // see https://stackoverflow.com/questions/37956741/jersey-resource-receiving-duplicate-requests-from-jersey-client
    // https://github.com/jersey/jersey/issues/3526
    // for that reason we have audit log (all the operations were actually applied from the test)
    // we log warning here when generated (assumed) number of transfer operation does not equal to really applied ones
    private void checkWarningCauseOfJerseyDuplicateRequests(final List<AccountState> accountExpectedFinalStates,
                                                            final List<AccountState> accountActualFinalStates) {
        checkAllMoneyTransferredCorrectly(accountExpectedFinalStates, accountActualFinalStates, true);
    }

    private List<AccountState> getAccountsActualState(final RestClientHelper restClientHelper,
                                                      final int accountsNumber) {
        final List<AccountState> result = new ArrayList<>(accountsNumber);
        for (int accountId = 1; accountId <= accountsNumber; accountId++) {
            result.add(restClientHelper.getAccount(accountId));
        }
        return result;
    }

}
