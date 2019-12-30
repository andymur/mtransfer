package com.andymur.toyproject.db;

import com.andymur.toyproject.core.AccountState;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

public class AccountDaoImpl implements AccountDao {

    private final Jdbi jdbi;

    public AccountDaoImpl(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<AccountState> listAccounts() {
        return jdbi.withHandle(handle ->
                handle.createQuery("select id, amount from public.accounts order by id")
                        .map((rs, ctx) -> new AccountState(rs.getLong(0), rs.getBigDecimal(1)))
                        .list());
    }
}
