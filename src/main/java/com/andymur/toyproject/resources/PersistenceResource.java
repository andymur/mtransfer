package com.andymur.toyproject.resources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.PersistenceServiceImpl;
import com.andymur.toyproject.core.persistence.operations.AccountOperation;
import com.andymur.toyproject.core.persistence.operations.AddAccountOperation;
import com.andymur.toyproject.core.persistence.operations.DeleteAccountOperation;
import com.andymur.toyproject.core.persistence.operations.UpdateAccountOperation;
import com.andymur.toyproject.db.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/persistence")
@Produces(MediaType.APPLICATION_JSON)
public class PersistenceResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceResource.class);

	private final PersistenceServiceImpl persistenceService;
	private final AccountRepository accountRepository;

	public PersistenceResource(final PersistenceServiceImpl persistenceService,
							   final AccountRepository accountRepository) {
		this.persistenceService = persistenceService;
		this.accountRepository = accountRepository;
	}

	@POST
	@Path("/add")
	public AccountOperation addOperation(final AccountState accountState) {
		LOGGER.info("addOperation;");
		return addOperation(createOperation(AccountOperation.OperationType.ADD, accountState));
	}

	@POST
	@Path("/delete")
	public AccountOperation deleteOperation(final AccountState accountState) {
		LOGGER.info("deleteOperation;");
		return addOperation(createOperation(AccountOperation.OperationType.DELETE, accountState));
	}

	@POST
	@Path("/update")
	public AccountOperation updateOperation(final AccountState accountState) {
		LOGGER.info("updateOperation;");
		return addOperation(createOperation(AccountOperation.OperationType.UPDATE, accountState));
	}

	@GET
	@Path("/list")
	public List<AccountState> list() {
		LOGGER.info("list;");
		return accountRepository.list();
	}

	@GET
	@Path("find/{id}")
	public Optional<AccountState> find(@PathParam("id") long id) {
		LOGGER.info("find; id = {}", id);
		return accountRepository.find(id);
	}

	@GET
	@Path("/check/{operationId}")
	public Boolean checkOperation(@PathParam("operationId") String operationId) {
		LOGGER.info("operation check; operation id = {}", operationId);
		return persistenceService.operationIsDone(operationId);
	}

	private AccountOperation addOperation(AccountOperation operation) {
		persistenceService.addOperation(operation);
		return operation;
	}

	private AccountOperation createOperation(AccountOperation.OperationType operationType,
											 AccountState accountState) {
		switch (operationType) {
			case ADD:
				return AddAccountOperation.of(accountState.getId(), accountState.getAmount());
			case UPDATE:
				return UpdateAccountOperation.of(accountState.getId(), accountState.getAmount());
			case DELETE:
				return DeleteAccountOperation.of(accountState.getId());
		}
		throw new IllegalStateException("Unsupported operation: " + operationType);
	}
}
