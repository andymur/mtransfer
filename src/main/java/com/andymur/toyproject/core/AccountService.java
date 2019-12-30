package com.andymur.toyproject.core;

import com.andymur.toyproject.db.AccountRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AccountService {

    private final Map<Long, AccountState> accounts = new ConcurrentHashMap<>();

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

    public List<AccountState> list() {
        return new ArrayList<>(accounts.values());
    }
}
