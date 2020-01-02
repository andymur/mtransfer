package com.andymur.toyproject.core.persistence;

import java.util.List;
import java.util.Optional;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.operations.AccountOperation;
import com.andymur.toyproject.core.persistence.operations.AccountOperation.Status;

/**
 * Persists operations into account entities
 */
public interface PersistenceService extends Runnable {

	/**
	 * Add new operation to perform
	 * @param operation to perform (could be update, delete, or add)
	 * @return operation id
	 */
	String addOperation(AccountOperation operation);

	/**
	 * Check operation status
	 * @param operationId which status to check
	 * @return status of the operation (DONE, FAILED, IN PROGRESS)
	 */
	Status getOperationStatus(String operationId);

	/**
	 * Fetches all available accounts
	 * @return all accounts
	 */
	List<AccountState> list();

	/**
	 * Fetches particular account by its key
	 * @param id key of account
	 * @return account record
	 */
	Optional<AccountState> find(long id);
}
