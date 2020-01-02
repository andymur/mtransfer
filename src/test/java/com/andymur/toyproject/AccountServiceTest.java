package com.andymur.toyproject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.andymur.toyproject.core.AccountService;
import com.andymur.toyproject.core.AccountServiceImpl;
import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.PersistenceServiceMock;
import com.andymur.toyproject.core.util.Pair;
import com.andymur.toyproject.core.util.TransferOperation;
import com.andymur.toyproject.util.AcceptanceTestHelper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.andymur.toyproject.core.util.Generator.generateInt;
import static com.andymur.toyproject.util.AcceptanceTestHelper.calculateAccountsFinalState;
import static com.andymur.toyproject.util.AcceptanceTestHelper.checkAllMoneyTransferredCorrectly;
import static com.andymur.toyproject.util.AcceptanceTestHelper.generateTransferOperations;
import static com.andymur.toyproject.util.AcceptanceTestHelper.prepareAccountsToCreate;
import static com.andymur.toyproject.util.AcceptanceTestHelper.stringifyTransferOperations;
import static org.hamcrest.CoreMatchers.is;

public class AccountServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoneyTransferAcceptanceTest.class);

    private static int NUMBER_OF_THREADS = 5;

    private static final ExecutorService CREATION_EXECUTOR_SERVICE = Executors.newFixedThreadPool(NUMBER_OF_THREADS,
            new ThreadFactoryBuilder().setNameFormat("create-account-%d").build());
    ;

    private static final ExecutorService TRANSFERRING_EXECUTOR_SERVICE = Executors.newFixedThreadPool(NUMBER_OF_THREADS,
            new ThreadFactoryBuilder().setNameFormat("transfer-money-%d").build());

    private AccountService accountService;

    private static final BigDecimal ONE_HUNDRED_MILLIONS = new BigDecimal("100000000.00");
    private static final Pair<Integer, Integer> FROM_TO_ACCOUNT_NUMBER = Pair.of(200, 1000);
    private static final Pair<Integer, Integer> FROM_TO_TRANSFER_SIZE = Pair.of(10, 20);
    private static final Pair<Integer, Integer> FROM_TO_OPERATIONS = Pair.of(1000, 5000);

    @BeforeEach
    public void setUp() {
        accountService = new AccountServiceImpl(new PersistenceServiceMock());
    }

    @AfterEach
    public void tearDown() {
    }

    @RepeatedTest(10)
    public void shouldHaveCorrectAccountDetailsAfterMoneyTransferring() throws InterruptedException {
        final int accountsNumber = generateInt(FROM_TO_ACCOUNT_NUMBER);
        LOGGER.info("Number of accounts: {}", accountsNumber);

        List<AccountState> accountsToCreate = prepareAccountsToCreate(accountsNumber, ONE_HUNDRED_MILLIONS);
        createAllAccounts(CREATION_EXECUTOR_SERVICE, accountService, accountsToCreate);
        checkAllAccountsAreCreated(accountsNumber);

        final List<TransferOperation> transferOperations
                = generateTransferOperations(accountsNumber, FROM_TO_OPERATIONS, FROM_TO_TRANSFER_SIZE);

        LOGGER.info("Number of transfer operations: {}", transferOperations.size());
        LOGGER.debug("Generated transfer operations: {}", stringifyTransferOperations(transferOperations));

        makeAllTransfers(TRANSFERRING_EXECUTOR_SERVICE, accountService, transferOperations);

        List<AccountState> accountsExpectedFinalState = calculateAccountsFinalState(accountsToCreate, transferOperations);
        List<AccountState> accountsActualFinalState = accountsActualFinalState(accountsNumber, accountService);

        LOGGER.debug("Accounts expected final state: {}", accountsExpectedFinalState);
        LOGGER.debug("Accounts actual final state: {}", accountsActualFinalState);

        checkAllMoneyTransferredCorrectly(accountsExpectedFinalState, accountsActualFinalState);
    }

    private void checkAllAccountsAreCreated(final int accountsNumber) {
        for (int i = 1; i <= accountsNumber; i++) {
            Assert.assertThat("", accountService.get(i), is(new AccountState(i, ONE_HUNDRED_MILLIONS)));
        }
    }

    private List<AccountState> accountsActualFinalState(final int accountsNumber,
                                                        final AccountService accountService) {
        final List<AccountState> result = new ArrayList<>();
        for (int i = 1; i <= accountsNumber; i++) {
            result.add(accountService.get(i));
        }
        return result;
    }

    private static void createAllAccounts(final ExecutorService executorService,
                                          final AccountService accountService,
                                          final List<AccountState> accountsToCreate) throws InterruptedException {
        AcceptanceTestHelper.createAllAccounts(executorService, accountsToCreate,
                accountService::put, 1L);
    }

    private static void makeAllTransfers(final ExecutorService executorService,
                                         final AccountService accountService,
                                         final List<TransferOperation> transferOperations) throws InterruptedException {
        AcceptanceTestHelper.makeAllTransfers(executorService, transferOperations,
                operation -> accountService.transfer(operation.getSourceAccountId(), operation.getDestinationAccountId(), operation.getAmountToTransfer()),
                3L);
    }

}
