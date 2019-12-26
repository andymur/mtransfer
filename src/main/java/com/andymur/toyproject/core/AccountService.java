package com.andymur.toyproject.core;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AccountService {

    private Map<Long, AccountState> accounts = new ConcurrentHashMap<>();

    public AccountService() {
    }

    public AccountState get(long id) {
        return accounts.getOrDefault(id, AccountState.DEFAULT);
    }

    public AccountState put(AccountState accountState) {
        accounts.put(accountState.getId(), accountState);
        return accountState;
    }

    public Optional<AccountState> delete(long id) {
        return Optional.ofNullable(accounts.remove(id));
    }
}
