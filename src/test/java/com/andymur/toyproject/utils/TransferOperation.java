package com.andymur.toyproject.utils;

import java.math.BigDecimal;

public class TransferOperation {
	private final long sourceAccountId;
	private final long destinationAccountId;
	private final BigDecimal amountToTransfer;

	private TransferOperation(final long sourceAccountId,
							  final long destinationAccountId,
							  final BigDecimal amountToTransfer) {
		this.sourceAccountId = sourceAccountId;
		this.destinationAccountId = destinationAccountId;
		this.amountToTransfer = amountToTransfer;
	}

	public long getSourceAccountId() {
		return sourceAccountId;
	}

	public long getDestinationAccountId() {
		return destinationAccountId;
	}

	public BigDecimal getAmountToTransfer() {
		return amountToTransfer;
	}

	public static TransferOperation of(final long sourceAccountId,
									   final long destinationAccountId,
									   final BigDecimal amountToTransfer) {
		return new TransferOperation(sourceAccountId, destinationAccountId, amountToTransfer);
	}

	public static TransferOperation generate(final Pair<Integer, Integer> fromToAccountIds,
											 final Pair<Integer, Integer> fromToTransferSize) {
		long sourceAccountId = generateAccountId(fromToAccountIds);
		long destinationAccountId = generateAccountId(fromToAccountIds);
		BigDecimal amountToTransfer = generateAmount(fromToTransferSize);
		return of(sourceAccountId, destinationAccountId, amountToTransfer);
	}

	private static long generateAccountId(Pair<Integer, Integer> fromToAccountIds) {
		return Generator.generateLong(fromToAccountIds);
	}

	private static BigDecimal generateAmount(Pair<Integer, Integer> fromToTransferSize) {
		return BigDecimal.valueOf(Generator.generateLong(fromToTransferSize));
	}

	@Override
	public String toString() {
		return "TransferOperation{" +
				"sourceAccountId=" + sourceAccountId +
				", destinationAccountId=" + destinationAccountId +
				", amountToTransfer=" + amountToTransfer +
				'}';
	}
}
