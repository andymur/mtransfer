package com.andymur.toyproject.core;

import com.andymur.toyproject.core.utils.TransferOperation;

import java.util.ArrayList;
import java.util.List;

public class TransferOperationsAuditLog {
    private final List<TransferOperation> operations = new ArrayList<>();

    public synchronized void addOperation(TransferOperation transferOperation) {
        operations.add(transferOperation);
    }

    public synchronized List<TransferOperation> getLog() {
        return operations;
    }

    public synchronized String stringifyLog() {
        final List<String> result = new ArrayList<>(operations.size());
        for (final TransferOperation operation: getLog()) {
            result.add(String.format("(%d, %d, %s)",
                    operation.getSourceAccountId(), operation.getDestinationAccountId(), operation.getAmountToTransfer()));
        }
        return String.join(", ", result);
    }
}
