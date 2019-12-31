package com.andymur.toyproject.resources;

import com.andymur.toyproject.core.AccountService;
import com.andymur.toyproject.core.AccountServiceImpl;
import com.andymur.toyproject.core.AccountState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountState.class);
    private final AccountService accountService;

    public AccountResource(AccountService accountService) {
        this.accountService = accountService;
    }

    @GET
    @Path("{id}")
    public AccountState read(@PathParam("id") final long id) {
        LOGGER.info("read; id={}", id);
        return accountService.get(id);
    }

    @GET
    public List<AccountState> list() {
        LOGGER.info("list; ");
        return accountService.list();
    }

    @PUT
    public AccountState put(final AccountState accountState) {
        LOGGER.info("put; account={}", accountState);
        return accountService.put(accountState);
    }

    @POST
    @Path("{sourceAccountId}/{destinationAccountId}/{amount}")
    public void transfer(@PathParam("sourceAccountId") final long sourceAccountId,
                         @PathParam("destinationAccountId") final long destinationAccountId,
                         @PathParam("amount") final BigDecimal amount) {
        accountService.transfer(sourceAccountId, destinationAccountId, amount);
    }

    @DELETE
    @Path("{id}")
    public AccountState delete(@PathParam("id") final long id) {
        LOGGER.info("delete; id={}", id);
        Optional<AccountState> deletedAccount = accountService.delete(id);
        return deletedAccount.orElse(AccountState.DEFAULT);
    }
}
