package com.andymur.toyproject.core;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.andymur.toyproject.core.persistence.PersistenceService;
import com.andymur.toyproject.core.persistence.operations.AddAccountOperation;
import com.andymur.toyproject.core.persistence.operations.DeleteAccountOperation;
import com.andymur.toyproject.core.persistence.operations.UpdateAccountOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.andymur.toyproject.core.TransferOperationResult.failed;
import static com.andymur.toyproject.core.TransferOperationResult.success;

public class  AccountServiceImpl implements AccountService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountServiceImpl.class);

    private static final String OK = "OK";

    private final Map<Long, AccountState> accounts = new HashMap<>();

    private final PersistenceService persistenceService;

    public AccountServiceImpl(final PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public synchronized AccountState get(final long id) {
        return accounts.get(id);
    }

    @Override
    public synchronized AccountState put(final AccountState accountState) {
        synchronized (accountState) {
            accounts.put(accountState.getId(), accountState);
            persistenceService.addOperation(AddAccountOperation.of(accountState.getId(), accountState.getAmount()));
            return accountState;
        }
    }

    @Override
    public synchronized Optional<AccountState> delete(final long id) {
        Optional<AccountState> result = Optional.ofNullable(accounts.remove(id));
        persistenceService.addOperation(DeleteAccountOperation.of(id));
        return result;
    }

    @Override
    public synchronized TransferOperationResult transfer(final long sourceAccountId,
                         final long destinationAccountId,
                         final BigDecimal amountToTransfer) {
        try {
            if (sourceAccountId < destinationAccountId) {
                lockAndTransfer(sourceAccountId, destinationAccountId, amountToTransfer);
            } else {
                lockAndTransfer(destinationAccountId, sourceAccountId, amountToTransfer.negate());
            }
            return success(OK);
        } catch (Exception e) {
            return failed(e.getMessage());
        }
    }

    private void lockAndTransfer(final long lowerAccountId,
                          final long upperAccountId,
                          final BigDecimal amountToTransfer) {
        synchronized (accounts.get(lowerAccountId)) {
           synchronized (accounts.get(upperAccountId)) {
                withdraw(lowerAccountId, amountToTransfer);
                deposit(upperAccountId, amountToTransfer);
            }
        }
    }

    private void withdraw(final long accountId,
                          final BigDecimal amountToWithdraw) {
        amountToAdd(accountId, amountToWithdraw.negate());
    }

    private void deposit(final long accountId,
                         final BigDecimal amountToAdd) {
        amountToAdd(accountId, amountToAdd);
    }

    private void amountToAdd(final long accountId,
                             final BigDecimal amountToAdd) {
        final BigDecimal oldAmount = accounts.get(accountId).getAmount();
        final BigDecimal newAmount = oldAmount.add(amountToAdd);

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            LOGGER.warn("Greater amount cannot be withdrawn. accountId = {}, current amount = {}, amount to withdraw = {}",
                    accountId, oldAmount,  amountToAdd);

            throw new IllegalStateException("Amount to withdraw is greater than account has.");
        }
        accounts.put(accountId, new AccountState(accountId, newAmount));
        persistenceService.addOperation(UpdateAccountOperation.of(accountId, newAmount));
    }
}
