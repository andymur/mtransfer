package com.andymur.toyproject.core.persistence;

import java.util.List;
import java.util.Optional;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.operations.AccountOperation;
import com.andymur.toyproject.core.persistence.operations.AccountOperation.Status;
//TODO: document me
public interface PersistenceService extends Runnable {
	String addOperation(AccountOperation operation);
	Status getOperationStatus(String operationId);
	boolean hasOperationsToComplete();
	List<AccountState> list();
	Optional<AccountState> find(long id);
}
