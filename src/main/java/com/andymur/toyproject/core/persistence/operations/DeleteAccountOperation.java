package com.andymur.toyproject.core.persistence.operations;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteAccountOperation implements AccountOperation {

	private String operationId;
	private long accountId;

	public DeleteAccountOperation() {
		operationId = UUID.randomUUID().toString();
	}

	public DeleteAccountOperation(final long accountId) {
		this();
		this.accountId = accountId;
	}

	@Override
	@JsonProperty
	public long getAccountId() {
		return accountId;
	}

	@Override
	@JsonProperty
	public String getOperationId() {
		return operationId;
	}

	@JsonProperty
	public OperationType getType() {
		return OperationType.DELETE;
	}

	@Override
	public String toString() {
		return "DeleteAccountOperation{" +
				"operationId='" + operationId + '\'' +
				", accountId=" + accountId +
				'}';
	}

	public static DeleteAccountOperation of(final long id) {
		return new DeleteAccountOperation(id);
	}
}
