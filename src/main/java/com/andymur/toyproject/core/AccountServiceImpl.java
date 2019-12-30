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

    public AccountState get(long id) {
        //TODO: consider raising an exception instead of returning default value
        return accounts.getOrDefault(id, AccountState.DEFAULT);
    }

    public AccountState put(AccountState accountState) {
        accounts.put(accountState.getId(), accountState);
        return accountState;
    }

    public Optional<AccountState> delete(long id) {
        return Optional.ofNullable(accounts.remove(id));
    }

    public List<AccountState> list() {
        return new ArrayList<>(accounts.values());
    }

    @Override
    public void transfer(long sourceAccountId, long destinationAccountId, BigDecimal amountToTransfer) {
        //TODO: add exceptions handling (e.g. account not found, source has no sufficient funds etc)
        if (sourceAccountId < destinationAccountId) {
            lockAndTransfer(sourceAccountId, destinationAccountId, amountToTransfer);
        } else {
            lockAndTransfer(destinationAccountId, sourceAccountId, amountToTransfer.negate());
        }
    }

    @Override
    public void withdraw(long accountId, BigDecimal amountToWithdraw) {

    }

    @Override
    public void deposit(long accountId, BigDecimal amountToAdd) {

    }

    private void lockAndTransfer(final long lowerAccountId,
                          final long upperAccountId,
                          final BigDecimal amountToTransfer) {
        //TODO: consider using ReadWriteLock
        synchronized (get(lowerAccountId)) {
            synchronized (get(upperAccountId)) {
                AccountState firstAccount = get(lowerAccountId);
                AccountState secondAccount = get(upperAccountId);

                firstAccount.addAmount(amountToTransfer.negate());
                secondAccount.addAmount(amountToTransfer);
            }
        }
    }
}
