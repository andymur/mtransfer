package com.andymur.toyproject.core.persistence.operations;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateAccountOperation implements AccountOperation {

	private String operationId;
	private long accountId;
	private BigDecimal newAmount;

	public UpdateAccountOperation() {
		operationId = UUID.randomUUID().toString();
	}

	public UpdateAccountOperation(final long accountId, final BigDecimal newAmount) {
		this();
		this.accountId = accountId;
		this.newAmount = newAmount;
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
	public BigDecimal getNewAmount() {
		return newAmount;
	}

	@JsonProperty
	public OperationType getType() {
		return OperationType.UPDATE;
	}

	@Override
	public String toString() {
		return "UpdateAccountOperation{" +
				"operationId='" + operationId + '\'' +
				", accountId=" + accountId +
				", newAmount=" + newAmount +
				'}';
	}

	public static UpdateAccountOperation of(final long accountId,
											final BigDecimal newAmount) {
		return new UpdateAccountOperation(accountId, newAmount);
	}
}
