package com.andymur.toyproject.db;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.andymur.toyproject.core.AccountState;
import org.jdbi.v3.core.Jdbi;

public class AccountRepository {
	private final Jdbi jdbi;

	public AccountRepository(final Jdbi jdbi) {
		this.jdbi = jdbi;
	}

	public void add(final long id, final BigDecimal bigDecimal) {
		getDao().addAccount(id, bigDecimal);
	}

	public void add(final AccountState accountState) {
		getDao().addAccount(accountState.getId(), accountState.getAmount());
	}

	public void delete(long id) {
		getDao().deleteAccount(id);
	}

	public void update(long id, BigDecimal newAmount) {
		getDao().updateAccount(id, newAmount);
	}

	public List<AccountState> list() {
		return getDao().list();
	}

	public Optional<AccountState> find(final long id) {
		return getDao().find(id);
	}

	private AccountDao getDao() {
		return jdbi.onDemand(AccountDao.class);
	}
}
