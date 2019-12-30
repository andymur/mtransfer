package com.andymur.toyproject.core.persistence;

import java.util.List;
import java.util.Optional;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.operations.AccountOperation;

public interface PersistenceService {
	void addOperation(AccountOperation operation);
	boolean operationIsDone(String operationId);
	List<AccountState> list();
	Optional<AccountState> find(long id);
}
