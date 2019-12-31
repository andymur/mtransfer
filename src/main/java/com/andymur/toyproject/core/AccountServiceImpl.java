package com.andymur.toyproject.core;

import com.andymur.toyproject.core.persistence.PersistenceService;
import com.andymur.toyproject.core.persistence.PersistenceServiceImpl;
import com.andymur.toyproject.core.persistence.operations.UpdateAccountOperation;
import com.andymur.toyproject.db.AccountRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class  AccountServiceImpl implements AccountService {

    private final Map<Long, AccountState> accounts = new ConcurrentHashMap<>();
    private final PersistenceService persistenceService;

    public AccountServiceImpl(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public AccountState get(final long id) {
        //TODO: consider raising an exception instead of returning default value
        return accounts.getOrDefault(id, AccountState.DEFAULT);
    }

    @Override
    public AccountState put(final AccountState accountState) {
        //TODO: use persistence
        accounts.put(accountState.getId(), accountState);
        return accountState;
    }

    @Override
    public Optional<AccountState> delete(final long id) {
        //TODO: use persistence
        return Optional.ofNullable(accounts.remove(id));
    }

    @Override
    public List<AccountState> list() {
        return new ArrayList<>(accounts.values());
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

    @Override
    public void withdraw(final long accountId,
                         final BigDecimal amountToWithdraw) {
        amountToAdd(accountId, amountToWithdraw.negate());
    }

    @Override
    public void deposit(final long accountId,
                        final BigDecimal amountToAdd) {
        amountToAdd(accountId, amountToAdd);
    }

    @Override
    public void amountToAdd(final long accountId,
                            final BigDecimal amountToAdd) {
        //TODO: use persistence
        get(accountId).addAmount(amountToAdd);
    }

    private void lockAndTransfer(final long lowerAccountId,
                          final long upperAccountId,
                          final BigDecimal amountToTransfer) {
        //TODO: consider using ReadWriteLock
        synchronized (get(lowerAccountId)) {
            synchronized (get(upperAccountId)) {
                withdraw(lowerAccountId, amountToTransfer);
                deposit(upperAccountId, amountToTransfer);
            }
        }
    }
}
