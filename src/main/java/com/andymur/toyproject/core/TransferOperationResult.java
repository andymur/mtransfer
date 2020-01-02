package com.andymur.toyproject.core;

import static com.andymur.toyproject.core.TransferOperationResult.Status.FAILED;
import static com.andymur.toyproject.core.TransferOperationResult.Status.SUCCESS;

public class TransferOperationResult {

	private String message;
	private Status status;

	public TransferOperationResult() {
	}

	private TransferOperationResult(final String message,
									final Status status) {
		this.message = message;
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public Status getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "TransferOperationResult{" +
				"message='" + message + '\'' +
				", status=" + status +
				'}';
	}

	public static TransferOperationResult success(final String operationId) {
		return new TransferOperationResult(operationId, SUCCESS);
	}

	public static TransferOperationResult failed(final String operationId) {
		return new TransferOperationResult(operationId, FAILED);
	}

	public enum Status {
		SUCCESS, FAILED
	}
}
