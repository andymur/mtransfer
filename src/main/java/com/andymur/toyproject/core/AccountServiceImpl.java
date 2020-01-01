package com.andymur.toyproject.core;

import com.andymur.toyproject.core.persistence.PersistenceService;
import com.andymur.toyproject.core.persistence.operations.AddAccountOperation;
import com.andymur.toyproject.core.persistence.operations.DeleteAccountOperation;
import com.andymur.toyproject.core.persistence.operations.UpdateAccountOperation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class  AccountServiceImpl implements AccountService {

    private final Map<Long, AccountState> accounts = new ConcurrentHashMap<>();

    private final PersistenceService persistenceService;

    public AccountServiceImpl(final PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public AccountState get(final long id) {
        //TODO: consider raising an exception instead of returning default value
        final AccountState account = accounts.getOrDefault(id, AccountState.DEFAULT);
        synchronized (account) {
            return account;
        }
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
        synchronized (get(accountId)) {
            amountToAdd(accountId, amountToWithdraw.negate());
        }
    }

    @Override
    public void deposit(final long accountId,
                        final BigDecimal amountToAdd) {
        synchronized (get(accountId)) {
            amountToAdd(accountId, amountToAdd);
        }
    }

    @Override
    public void amountToAdd(final long accountId,
                            final BigDecimal amountToAdd) {
        final BigDecimal oldAmount = get(accountId).getAmount();
        final BigDecimal newAmount = oldAmount.add(amountToAdd);

        accounts.put(accountId, new AccountState(accountId, newAmount));
        persistenceService.addOperation(UpdateAccountOperation.of(accountId, newAmount));
    }

    private void lockAndTransfer(final long lowerAccountId,
                          final long upperAccountId,
                          final BigDecimal amountToTransfer) {
        synchronized (get(lowerAccountId)) {
           synchronized (get(upperAccountId)) {
                withdraw(lowerAccountId, amountToTransfer);
                deposit(upperAccountId, amountToTransfer);
            }
        }
    }
}
