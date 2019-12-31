package com.andymur.toyproject.core.persistence.operations;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AddAccountOperation implements AccountOperation {

	private String operationId;
	private long accountId;
	private BigDecimal amount;

	public AddAccountOperation() {
		operationId = UUID.randomUUID().toString();
	}

	public AddAccountOperation(final long accountId,
							   final BigDecimal amount) {
		this();
		this.accountId = accountId;
		this.amount = amount;
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
	public BigDecimal getAmount() {
		return amount;
	}

	@JsonIgnore
	public OperationType getType() {
		return OperationType.ADD;
	}

	@Override
	public String toString() {
		return "AddAccountOperation{" +
				"operationId='" + operationId + '\'' +
				", accountId=" + accountId +
				", amount=" + amount +
				'}';
	}

	public static AddAccountOperation of(final long accountId,
										 final BigDecimal amount) {
		return new AddAccountOperation(accountId, amount);
	}
}
