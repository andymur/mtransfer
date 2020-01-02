package com.andymur.toyproject.core;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.andymur.toyproject.core.persistence.PersistenceService;
import com.andymur.toyproject.core.persistence.operations.AddAccountOperation;
import com.andymur.toyproject.core.persistence.operations.DeleteAccountOperation;
import com.andymur.toyproject.core.persistence.operations.UpdateAccountOperation;

public class  AccountServiceImpl implements AccountService {

    private final Map<Long, AccountState> accounts = new ConcurrentHashMap<>();

    private final PersistenceService persistenceService;

    public AccountServiceImpl(final PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public AccountState get(final long id) {
        return accounts.get(id);
    }

    @Override
    public AccountState put(final AccountState accountState) {
        accounts.put(accountState.getId(), accountState);
        persistenceService.addOperation(AddAccountOperation.of(accountState.getId(), accountState.getAmount()));
        return accountState;
    }

    @Override
    public Optional<AccountState> delete(final long id) {
        Optional<AccountState> result = Optional.ofNullable(accounts.remove(id));
        persistenceService.addOperation(DeleteAccountOperation.of(id));
        return result;
    }

    @Override
    public void transfer(final long sourceAccountId,
                         final long destinationAccountId,
                         final BigDecimal amountToTransfer) {
        //TODO: add exceptions handling (e.g. account not found, source has no sufficient funds etc)
        if (sourceAccountId < destinationAccountId) {
            lockAndTransfer(sourceAccountId, destinationAccountId, amountToTransfer);
        } else {
            lockAndTransfer(destinationAccountId, sourceAccountId, amountToTransfer.negate());
        }
    }

    private void lockAndTransfer(final long lowerAccountId,
                          final long upperAccountId,
                          final BigDecimal amountToTransfer) {
        final AccountState lowerAccount = accounts.get(lowerAccountId);
        final AccountState upperAccount = accounts.get(upperAccountId);
        synchronized (lowerAccount) {
           synchronized (upperAccount) {
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

        accounts.put(accountId, new AccountState(accountId, newAmount));
        persistenceService.addOperation(UpdateAccountOperation.of(accountId, newAmount));
    }
}
