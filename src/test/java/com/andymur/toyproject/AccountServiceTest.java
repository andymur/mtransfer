package com.andymur.toyproject;

import com.andymur.toyproject.core.AccountService;
import com.andymur.toyproject.core.AccountServiceImpl;
import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.PersistenceService;
import com.andymur.toyproject.core.persistence.operations.AccountOperation;
import com.andymur.toyproject.core.utils.Pair;
import com.andymur.toyproject.core.utils.TransferOperation;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.andymur.toyproject.core.utils.Generator.generateInt;
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

    private static final BigDecimal TEN_MILLIONS = new BigDecimal("100000000.00");
    private static final Pair<Integer, Integer> FROM_TO_ACCOUNT_NUMBER = Pair.of(200, 1000);
    private static final Pair<Integer, Integer> FROM_TO_TRANSFER_SIZE = Pair.of(10, 20);
    private static final Pair<Integer, Integer> FROM_TO_OPERATIONS = Pair.of(1000, 5000);

    @BeforeEach
    public void setUp() {
        accountService = new AccountServiceImpl(createPersistenceServiceMock());
    }

    @AfterEach
    public void tearDown() {
    }

    @RepeatedTest(10)
    public void shouldHaveCorrectAccountDetailsAfterMoneyTransferring() throws InterruptedException {
        final int accountsNumber = generateInt(FROM_TO_ACCOUNT_NUMBER);
        LOGGER.info("Number of accounts: {}", accountsNumber);

        List<AccountState> accountsToCreate = prepareAccountsToCreate(accountsNumber);
        createAllAccounts(CREATION_EXECUTOR_SERVICE, accountsToCreate, accountService);
        checkAllAccountsAreCreated(accountsNumber);

        final List<TransferOperation> transferOperations
                = generateTransferOperations(accountsNumber, FROM_TO_OPERATIONS, FROM_TO_TRANSFER_SIZE);

        LOGGER.info("Number of transfer operations: {}", transferOperations.size());
        LOGGER.debug("Generated transfer operations: {}", stringifyTransferOperations(transferOperations));

        makeAllTransfers(TRANSFERRING_EXECUTOR_SERVICE, transferOperations, accountService);

        List<AccountState> accountsExpectedFinalState = calculateAccountsFinalState(accountsToCreate, transferOperations);
        List<AccountState> accountsActualFinalState = accountsActualFinalState(accountsNumber, accountService);

        checkAllMoneyTransferredCorrectly(accountsExpectedFinalState, accountsActualFinalState);
    }

    private List<AccountState> prepareAccountsToCreate(final int accountsNumber) {
        return IntStream.range(1, accountsNumber + 1)
                .mapToObj(id -> new AccountState(id, TEN_MILLIONS))
                .collect(Collectors.toList());
    }

    private void checkAllAccountsAreCreated(final int accountsNumber) {
        for (int i = 1; i <= accountsNumber; i++) {
            Assert.assertThat("", accountService.get(i), is(new AccountState(i, TEN_MILLIONS)));
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

    //TODO: move to one place with money transfer acceptance test

    /******/

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

    private static void createAllAccounts(final ExecutorService executorService,
                                          final List<AccountState> accountsToCreate,
                                          final AccountService accountService) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(accountsToCreate.size());

        for (final AccountState account : accountsToCreate) {
            executorService.submit(() -> {
                try {
                    accountService.put(account);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30L, TimeUnit.SECONDS);
    }

    private static void makeAllTransfers(final ExecutorService executorService,
                                         final List<TransferOperation> transferOperations,
                                         final AccountService accountService) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(transferOperations.size());

        for (final TransferOperation transferOperation : transferOperations) {
            executorService.submit(() -> {
                try {
                    accountService.transfer(transferOperation.getSourceAccountId(),
                            transferOperation.getDestinationAccountId(),
                            transferOperation.getAmountToTransfer());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30L, TimeUnit.SECONDS);
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

    private String stringifyTransferOperations(final List<TransferOperation> transferOperations) {
        final List<String> resultList = new ArrayList<>(transferOperations.size());
        for (final TransferOperation operation : transferOperations) {
            resultList.add(String.format("(%d, %d, %s)",
                    operation.getSourceAccountId(), operation.getDestinationAccountId(), operation.getAmountToTransfer()));
        }
        return String.join(", ", resultList);
    }

    private List<AccountState> calculateAccountsFinalState(final List<AccountState> initialAccountsState,
                                                           final List<TransferOperation> transferOperations) {
        final Map<Long, BigDecimal> nettedOperations = calculateNettedOperations(initialAccountsState.size(), transferOperations);
        return applyNettedOperations(initialAccountsState, nettedOperations);
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

    /******/

    private PersistenceService createPersistenceServiceMock() {
        return new PersistenceService() {
            @Override
            public String addOperation(AccountOperation operation) {
                //no op
                return UUID.randomUUID().toString();
            }

            @Override
            public AccountOperation.Status getOperationStatus(String operationId) {
                return AccountOperation.Status.DONE;
            }

            @Override
            public List<AccountState> list() {
                return Collections.emptyList();
            }

            @Override
            public Optional<AccountState> find(long id) {
                return Optional.empty();
            }

            @Override
            public void run() {
                // no op
            }
        };
    }
}
