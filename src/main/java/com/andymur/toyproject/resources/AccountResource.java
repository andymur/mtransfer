package com.andymur.toyproject.resources;

import com.andymur.toyproject.core.AccountService;
import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.TransferOperationResult;
import com.andymur.toyproject.core.TransferOperationsAuditLog;
import com.andymur.toyproject.core.util.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.Optional;

import static com.andymur.toyproject.core.TransferOperationResult.Status.SUCCESS;

@Path("/account")
@Produces(MediaType.APPLICATION_JSON)

public class AccountResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountState.class);
    private final AccountService accountService;
    private final TransferOperationsAuditLog transferOperationsAuditLog;

    public AccountResource(final AccountService accountService,
                           final TransferOperationsAuditLog transferOperationsAuditLog) {
        this.accountService = accountService;
        this.transferOperationsAuditLog = transferOperationsAuditLog;
    }

    @GET
    @Path("{id}")
    public AccountState read(@PathParam("id") final long id) {
        LOGGER.info("read; id={}", id);
        return accountService.get(id);
    }

    @PUT
    public AccountState put(final AccountState accountState) {
        LOGGER.info("put; account={}", accountState);
        return accountService.put(accountState);
    }

    @POST
    @Path("{sourceAccountId}/{destinationAccountId}/{amount}")
    public TransferOperationResult transfer(@PathParam("sourceAccountId") final long sourceAccountId,
                                            @PathParam("destinationAccountId") final long destinationAccountId,
                                            @PathParam("amount") final BigDecimal amountToTransfer) {
        LOGGER.info("transfer; fromAccountId = {}, toACcountId = {} amountToTransfer = {}",
                sourceAccountId,
                destinationAccountId,
                amountToTransfer);


        final TransferOperationResult operationResult = accountService.transfer(sourceAccountId, destinationAccountId, amountToTransfer);

        if (operationResult.getStatus() == SUCCESS) {
            transferOperationsAuditLog.addOperation(
                    new TransferOperation(sourceAccountId, destinationAccountId, amountToTransfer)
            );
        } else {
            LOGGER.warn("transfer; operation has failed", operationResult);
        }

        return operationResult;
    }

    @DELETE
    @Path("{id}")
    public AccountState delete(@PathParam("id") final long id) {
        LOGGER.info("delete; id={}", id);
        Optional<AccountState> deletedAccount = accountService.delete(id);
        return deletedAccount.orElse(AccountState.DEFAULT);
    }
}
