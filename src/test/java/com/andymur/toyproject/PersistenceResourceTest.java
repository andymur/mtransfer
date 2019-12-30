package com.andymur.toyproject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.operations.AccountOperation;
import com.andymur.toyproject.core.persistence.operations.AddAccountOperation;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PersistenceResourceTest {

	private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("mtransfer-test.yml");
	private static final Client CLIENT = new JerseyClientBuilder().build();

	private static final DropwizardTestSupport<MTransferConfiguration> SUPPORT =
			new DropwizardTestSupport<>(MTransferApplication.class,
					CONFIG_PATH);

	@BeforeEach
	public void beforeClass() throws Exception {
		SUPPORT.before();
	}

	@AfterEach
	public void afterClass() {
		SUPPORT.after();
	}

	@Test
	public void testAddOperation() {
		final Response addAccountResponse = addOperationRequestResponse(CLIENT, new AccountState(1L, BigDecimal.TEN));
		assertThat(addAccountResponse.getStatus(), is(HttpStatus.OK_200));
		final AccountOperation accountOperation = addAccountResponse.readEntity(AddAccountOperation.class);
		System.out.println("OPERATION ID: " + accountOperation.getOperationId());

		final Response checkOperationResponse = checkOperationRequestResponse(CLIENT, accountOperation.getOperationId());

		final Boolean operationIsDone = checkOperationResponse.readEntity(Boolean.class);
		System.out.println("OPERATION IS DONE: " + operationIsDone);
	}

	private Response addOperationRequestResponse(Client client, AccountState accountState) {
		return client.target(
				String.format("http://localhost:%d/persistence/add", SUPPORT.getLocalPort()))
				.request()
				.post(Entity.entity(accountState, MediaType.APPLICATION_JSON_TYPE));
	}

	private Response deleteOperationRequestResponse(Client client, AccountState accountState) {
		return client.target(
				String.format("http://localhost:%d/persistence/delete", SUPPORT.getLocalPort()))
				.request()
				.post(Entity.entity(accountState, MediaType.APPLICATION_JSON_TYPE));
	}

	private Response updateOperationRequestResponse(Client client, AccountState accountState) {
		return client.target(
				String.format("http://localhost:%d/persistence/update", SUPPORT.getLocalPort()))
				.request()
				.post(Entity.entity(accountState, MediaType.APPLICATION_JSON_TYPE));
	}

	private Response checkOperationRequestResponse(Client client, String operationId) {
		return client.target(
				String.format("http://localhost:%d/check/%s", SUPPORT.getLocalPort(), operationId))
				.request()
				.get();
	}

	private Response listRequestResponse(Client client) {
		return client.target(
				String.format("http://localhost:%d/persistence/list", SUPPORT.getLocalPort()))
				.request()
				.get();
	}

	private Response findAccountRequestResponse(Client client, long id) {
		return client.target(
				String.format("http://localhost:%d/persistence/find/%d", SUPPORT.getLocalPort(), id))
				.request()
				.get();
	}
}
