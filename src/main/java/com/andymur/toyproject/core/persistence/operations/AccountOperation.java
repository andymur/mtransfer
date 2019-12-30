package com.andymur.toyproject.core.persistence.operations;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface AccountOperation {

	@JsonProperty
	OperationType getType();

	@JsonProperty
	String getOperationId();

	@JsonProperty
	long getAccountId();

	enum OperationType {
		ADD, UPDATE, DELETE
	}
}
