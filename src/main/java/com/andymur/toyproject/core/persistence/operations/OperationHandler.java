package com.andymur.toyproject.core.persistence.operations;

import java.util.concurrent.TimeUnit;

import com.andymur.toyproject.db.AccountRepository;

public class OperationHandler {

	private final AccountRepository accountRepository;

	public OperationHandler(final AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public void handle(final AccountOperation accountOperation) {
		//TODO: check jdbi exception
		switch (accountOperation.getType()) {
			case ADD: {
				AddAccountOperation addOperation = ((AddAccountOperation) accountOperation);
				accountRepository.add(addOperation.getAccountId(), addOperation.getAmount());
			}
			break;
			case DELETE:
				accountRepository.delete(((DeleteAccountOperation) accountOperation).getAccountId());
				break;
			case UPDATE: {
				UpdateAccountOperation updateOperation = (UpdateAccountOperation) accountOperation;
				accountRepository.update(updateOperation.getAccountId(), updateOperation.getNewAmount());
			}
			break;
			default:
				throw new IllegalStateException("Unsupported operation: " + accountOperation);
		}
	}
}