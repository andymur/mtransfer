package com.andymur.toyproject.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.util.Pair;
import com.andymur.toyproject.core.util.TransferOperation;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.andymur.toyproject.core.util.Generator.generateInt;
import static org.hamcrest.CoreMatchers.is;

public class AcceptanceTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceTestHelper.class);

    public static List<AccountState> prepareAccountsToCreate(final int accountsNumber,
                                                             BigDecimal accountInitialValue) {
        return IntStream.range(1, accountsNumber + 1)
                .mapToObj(id -> new AccountState(id, accountInitialValue))
                .collect(Collectors.toList());
    }

    public static void createAllAccounts(final ExecutorService executorService,
                                         final List<AccountState> accountsToCreate,
                                         final Consumer<AccountState> accountCreator,
                                         final long timeoutInSeconds) throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(accountsToCreate.size());

        for (final AccountState account : accountsToCreate) {
            executorService.submit(() -> {
                try {
                    accountCreator.accept(account);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(timeoutInSeconds, TimeUnit.SECONDS);
    }

    public static void makeAllTransfers(final ExecutorService executorService,
                                        final List<TransferOperation> transferOperations,
                                        final Consumer<TransferOperation> operationMaker,
                                        final long timeoutInSeconds) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(transferOperations.size());

        for (final TransferOperation transferOperation : transferOperations) {
            executorService.submit(() -> {
                try {
                    operationMaker.accept(transferOperation);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(timeoutInSeconds, TimeUnit.SECONDS);
    }

    public static List<TransferOperation> generateTransferOperations(final int accountsNumber,
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

    public static List<AccountState> calculateAccountsFinalState(final List<AccountState> initialAccountsState,
                                                                 final List<TransferOperation> transferOperations) {
        final Map<Long, BigDecimal> nettedOperations = calculateNettedOperations(initialAccountsState.size(), transferOperations);
        return applyNettedOperations(initialAccountsState, nettedOperations);
    }

    public static void checkAllMoneyTransferredCorrectly(final List<AccountState> accountsExpectedFinalState,
                                                         final List<AccountState> accountsActualFinalState) {
        checkAllMoneyTransferredCorrectly(accountsExpectedFinalState, accountsActualFinalState, false);
    }

    public static void checkAllMoneyTransferredCorrectly(final List<AccountState> accountsExpectedFinalState,
                                                         final List<AccountState> accountsActualFinalState,
                                                         final boolean raiseOnlyWarning) {
        int idx = 0;
        for (final AccountState accountFinalState : accountsExpectedFinalState) {
            final AccountState actualAccountState = accountsActualFinalState.get(idx++);

            Assert.assertThat("Actual and expected accounts' ids must be equal to each other",
                    actualAccountState.getId(), is(accountFinalState.getId()));

            if (raiseOnlyWarning && !accountFinalState.getAmount().equals(actualAccountState.getAmount())) {
                LOGGER.warn("Actual and expected accounts' amounts must be equal to each other, actual = {}, expected = {}",
                        actualAccountState.getAmount(), accountFinalState.getAmount());
            } else {
                Assert.assertThat("Actual and expected accounts' amounts must be equal to each other",
                        actualAccountState.getAmount(), is(accountFinalState.getAmount()));
            }
        }
    }

    public static String stringifyTransferOperations(final List<TransferOperation> transferOperations) {
        final List<String> resultList = new ArrayList<>(transferOperations.size());
        for (final TransferOperation operation : transferOperations) {
            resultList.add(String.format("(%d, %d, %s)",
                    operation.getSourceAccountId(), operation.getDestinationAccountId(), operation.getAmountToTransfer()));
        }
        return String.join(", ", resultList);
    }

    private static Map<Long, BigDecimal> calculateNettedOperations(final int accountNumber,
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

    private static List<AccountState> applyNettedOperations(final List<AccountState> accountStates,
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
}
