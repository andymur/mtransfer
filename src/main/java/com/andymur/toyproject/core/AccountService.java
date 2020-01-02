package com.andymur.toyproject.core;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service for account resource
 */
public interface AccountService {

    /**
     * Performs money transfer from one account to another
     * @param sourceAccountId supplier account id
     * @param destinationAccountId acceptor account id
     * @param amountToTransfer money to transfer
     * @return result of the operation (e.g. OK, NOT_OK)
     */
    TransferOperationResult transfer(long sourceAccountId, long destinationAccountId, BigDecimal amountToTransfer);

    /**
     * Fetches particular account by its id
     * @param id account's identifier
     * @return account state
     */
    AccountState get(long id);

    /**
     * Adds or replace (by id) account
     * @param accountState account to add
     * @return added accountState
     */
    AccountState put(AccountState accountState);

    /**
     * Tries to delete an account by its id
     * @param id account's identifier
     * @return deleted account state or empty if account hasn't been found
     */
    Optional<AccountState> delete(long id);
}
