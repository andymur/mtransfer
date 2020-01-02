package com.andymur.toyproject.core.persistence;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.operations.AccountOperation;

public class PersistenceServiceMock implements PersistenceService {

	@Override
	public String addOperation(final AccountOperation operation) {
		//no op
		return UUID.randomUUID().toString();
	}

	@Override
	public AccountOperation.Status getOperationStatus(final String operationId) {
		return AccountOperation.Status.DONE;
	}

	@Override
	public List<AccountState> list() {
		return Collections.emptyList();
	}

	@Override
	public Optional<AccountState> find(final long id) {
		return Optional.empty();
	}

	@Override
	public void run() {
		// no op
	}
}
