package com.andymur.toyproject.core.persistence;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.operations.AccountOperation;
import com.andymur.toyproject.core.persistence.operations.AccountOperation.Status;
import com.andymur.toyproject.core.persistence.operations.OperationHandler;
import com.andymur.toyproject.db.AccountRepository;
import org.jdbi.v3.core.JdbiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceServiceImpl implements PersistenceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceServiceImpl.class);
	private final BlockingQueue<AccountOperation> operationsQueue = new LinkedBlockingDeque<>();

	private final Set<String> completedOperations = Collections.synchronizedSet(new HashSet<>());
	private final Set<String> failedOperations = Collections.synchronizedSet(new HashSet<>());

	private final AccountRepository accountRepository;
	private final OperationHandler operationHandler;

	public PersistenceServiceImpl(final AccountRepository accountRepository,
								  final OperationHandler operationHandler) {
		this.accountRepository = accountRepository;
		this.operationHandler = operationHandler;
	}

	@Override
	public String addOperation(AccountOperation operation) {
		operationsQueue.add(operation);
		return operation.getOperationId();
	}

	@Override
	public Status getOperationStatus(final String operationId) {
		if (completedOperations.contains(operationId)) {
			return Status.DONE;
		} else if (failedOperations.contains(operationId)){
			return Status.FAILED;
		} else {
			return Status.IN_PROGRESS;
		}
	}

	@Override
	public boolean hasOperationsToComplete() {
		return !operationsQueue.isEmpty();
	}

	@Override
	public List<AccountState> list() {
		return accountRepository.list();
	}

	@Override
	public Optional<AccountState> find(final long id) {
		return accountRepository.find(id);
	}

	@Override
	public void run() {
		handleQueue();
	}

	private void handleQueue() {
		while (true) {
			try {
				AccountOperation operation = operationsQueue.take();
				LOGGER.info("handle operation; operation = {}", operation);
				try {
					operationHandler.handle(operation);
					completedOperations.add(operation.getOperationId());
				} catch (JdbiException e) {
					LOGGER.info("JDBC related error occurred while handling operation {}" , operation, e);
					failedOperations.add(operation.getOperationId());
				}
			} catch (InterruptedException e) {
				LOGGER.info("Operations handling process has been interrupted", e);
				return;
			}
		}
	}
}
