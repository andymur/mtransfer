package com.andymur.toyproject.resources;

import com.andymur.toyproject.core.AccountService;
import com.andymur.toyproject.core.AccountState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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
    public AccountState read(@PathParam("id") long id) {
        LOGGER.info("read; id={}", id);
        return accountService.get(id);
    }

    @PUT
    public AccountState write(AccountState accountState) {
        LOGGER.info("write; account={}", accountState);
        return accountService.put(accountState);
    }

    @DELETE
    @Path("{id}")
    public AccountState delete(@PathParam("id") long id) {
        LOGGER.info("delete; id={}", id);
        Optional<AccountState> deletedAccount = accountService.delete(id);
        return deletedAccount.orElse(AccountState.DEFAULT);
    }
}
