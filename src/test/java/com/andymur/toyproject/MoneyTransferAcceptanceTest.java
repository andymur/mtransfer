package com.andymur.toyproject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.utils.Pair;
import com.andymur.toyproject.utils.TransferOperation;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.andymur.toyproject.utils.Generator.generateInt;
import static org.hamcrest.CoreMatchers.is;

//TODO: document me
//TODO: add logging
public class MoneyTransferAcceptanceTest {

	private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("mtransfer-test.yml");
	private static final BigDecimal ONE_MILLION = new BigDecimal("1000000.00");

	private static final Pair<Integer, Integer> FROM_TO_ACCOUNT_NUMBER = Pair.of(10, 25);
	private static final Pair<Integer, Integer> FROM_TO_TRANSFER_SIZE = Pair.of(10, 20);
	private static final Pair<Integer, Integer> FROM_TO_OPERATIONS = Pair.of(500, 1000);

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
	public void shouldHaveCorrectAccountDetailsAfterMoneyTransferring() {
		final int accountsNumber = generateInt(FROM_TO_ACCOUNT_NUMBER);
		System.out.println("ACCOUNT NUMBERS: " + accountsNumber);
		final List<TransferOperation> transferOperations = generateTransferOperations(accountsNumber, FROM_TO_OPERATIONS, FROM_TO_TRANSFER_SIZE);
		System.out.println(transferOperations);

		final List<AccountState> accountsToCreate = accountsToCreate(accountsNumber);
		System.out.println("ACCOUNTS TO CREATE: " + accountsToCreate);

		//creating accounts
		//TODO: add multi threading
		accountsToCreate.forEach(MoneyTransferAcceptanceTest::createAccount);

		//making all the transfers
		//TODO: add multi threading
		makeAllTransfersInOneThread(transferOperations);

		final Map<Long, BigDecimal> nettedOperations = nettedOperations(accountsNumber, transferOperations);
		final List<AccountState> accountFinalStates = applyNettedOperations(accountsToCreate, nettedOperations);

		System.out.println(accountFinalStates);

		//check all transfers made in a correct way
		for (final AccountState accountFinalState: accountFinalStates) {
			final AccountState actualAccountState = getAccount(accountFinalState.getId());
			Assert.assertThat("Actual and expected account states must be equal to each other",
					actualAccountState, is(accountFinalState));
		}
	}

	//TODO: extract transferRequestResponse like methods, see AccountResourceTest

	/*************************************************/

	private static AccountState getAccount(long accountId) {
		Response response = requestAccountStateResponse(CLIENT, accountId);
		Assert.assertThat("Account status request has been successfully done",
				response.getStatus(), is(HttpStatus.OK_200));
		return response.readEntity(AccountState.class);
	}

	private static long createAccount(final AccountState accountState) {
		Response response = putAccountRequestResponse(CLIENT, accountState);
		Assert.assertThat("Account creation request has been successfully done",
				response.getStatus(), is(HttpStatus.OK_200));
		return accountState.getId();
	}

	private static void makeAllTransfersInOneThread(List<TransferOperation> transferOperations) {
		transferOperations.forEach(MoneyTransferAcceptanceTest::transfer);
	}

	private static void transfer(final TransferOperation transferOperation) {
		Response response = transferRequestResponse(CLIENT, transferOperation.getSourceAccountId(),
				transferOperation.getDestinationAccountId(),
				transferOperation.getAmountToTransfer());
		Assert.assertThat("Money transfer request has been successfully done",
				response.getStatus(), is(HttpStatus.NO_CONTENT_204));
	}

	/*************************************************/

	private static Response transferRequestResponse(Client client,
													long sourceAccountId,
													long destinationAccountId,
													BigDecimal amountToTransfer) {
		return client.target(
				String.format("http://localhost:%d/account/%d/%d/%s", SUPPORT.getLocalPort(), sourceAccountId, destinationAccountId, amountToTransfer)
		).request()
				.post(Entity.entity(Void.class, MediaType.APPLICATION_JSON_TYPE));
	}

	private static Response requestAccountStateResponse(Client client, long id) {
		return client.target(
				String.format("http://localhost:%d/account/%d", SUPPORT.getLocalPort(), id))
				.request()
				.get();
	}

	private static Response putAccountRequestResponse(Client client, AccountState account) {
		return client.target(
				String.format("http://localhost:%d/account/", SUPPORT.getLocalPort()))
				.request()
				.put(Entity.entity(account, MediaType.APPLICATION_JSON));
	}

	/*************************************************/

	private List<AccountState> accountsToCreate(int accountsNumber) {
		return IntStream.range(1, accountsNumber + 1)
				.mapToObj(id -> new AccountState(id, ONE_MILLION))
				.collect(Collectors.toList());
	}

	private Map<Long, BigDecimal> nettedOperations(final int accountNumber,
												   final List<TransferOperation> transferOperations) {
		final Map<Long, BigDecimal> result = new HashMap<>(accountNumber);

		for (long accountId = 1; accountId <= accountNumber; accountId++) {
			final long currentAccountId = accountId;

			BigDecimal outSum = transferOperations.stream()
					.filter(operation -> operation.getSourceAccountId() == currentAccountId)
					.map(TransferOperation::getAmountToTransfer).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

			BigDecimal inSum = transferOperations.stream()
					.filter(operation -> operation.getDestinationAccountId() == currentAccountId)
					.map(TransferOperation::getAmountToTransfer).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

			result.put(accountId, inSum.subtract(outSum));
		}

		return result;
	}

	private List<AccountState> applyNettedOperations(final List<AccountState> accountStates,
									   final Map<Long, BigDecimal> nettedOperations) {
		final List<AccountState> result = new ArrayList<>();

		for (final AccountState accountState : accountStates) {
			BigDecimal nettedAmountToApply = nettedOperations.getOrDefault(accountState.getId(), BigDecimal.ZERO);
			final AccountState accountFinalState = accountState.copyOf();
			accountFinalState.addAmount(nettedAmountToApply);
			result.add(accountFinalState);
		}

		return result;
	}


	private List<TransferOperation> generateTransferOperations(final int accountsNumber,
															   final Pair<Integer, Integer> fromToOperations,
															   final Pair<Integer, Integer> fromToTransferSize) {

		final int numberOfOperations = generateInt(fromToOperations);
		final Pair<Integer, Integer> fromToAccountIds = Pair.of(1, accountsNumber);

		final List<TransferOperation> result = new ArrayList<>(numberOfOperations);

		for (int i = 0; i < numberOfOperations; i++) {
			result.add(TransferOperation.generate(fromToAccountIds, fromToTransferSize));
		}

		return result;
	}
}
