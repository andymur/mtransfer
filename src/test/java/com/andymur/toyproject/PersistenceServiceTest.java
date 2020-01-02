package com.andymur.toyproject;

import com.andymur.toyproject.core.AccountState;
import com.andymur.toyproject.core.persistence.PersistenceService;
import com.andymur.toyproject.core.persistence.PersistenceServiceImpl;
import com.andymur.toyproject.core.persistence.operations.AccountOperation.Status;
import com.andymur.toyproject.core.persistence.operations.AddAccountOperation;
import com.andymur.toyproject.core.persistence.operations.DeleteAccountOperation;
import com.andymur.toyproject.core.persistence.operations.OperationHandler;
import com.andymur.toyproject.core.persistence.operations.UpdateAccountOperation;
import com.andymur.toyproject.db.AccountRepository;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.is;

public class PersistenceServiceTest {

    private static int NUMBER_OF_THREADS = 5;

    private static String DB_URL = "jdbc:hsqldb:mem:mtransfer";
    private static String DB_USERNAME = "sa";
    private static String DB_PASSWORD = "sa";

    private static BigDecimal PI_AMOUNT = new BigDecimal("3.14");
    private static BigDecimal E_AMOUNT = new BigDecimal("2.71");

    private PersistenceService persistenceService;

    @BeforeEach
    public void beforeClass() throws Exception {
        setUpDatabase();
        final AccountRepository accountRepository = new AccountRepository(getJdbi());
        final OperationHandler operationHandler = new OperationHandler(accountRepository);
        persistenceService = new PersistenceServiceImpl(accountRepository, operationHandler);

        Executors.newFixedThreadPool(NUMBER_OF_THREADS,
                new ThreadFactoryBuilder().setNameFormat("persistence-service").build())
                .submit(persistenceService);
    }

    @AfterEach
    public void afterClass() throws DatabaseException, SQLException {
        tearDownDatabase();
    }

    @Test
    public void shouldPersistNewAccountWhenItDoesNotExist() {
        String addOperationId = persistenceService.addOperation(AddAccountOperation.of(1L, PI_AMOUNT));

        while (persistenceService.getOperationStatus(addOperationId) == Status.IN_PROGRESS) {
        }

        Status addOperationStatus = persistenceService.getOperationStatus(addOperationId);
        Assert.assertThat("Adding account operation has failed",
                addOperationStatus, is(Status.DONE));

        AccountState accountState = persistenceService.find(1L).orElse(AccountState.DEFAULT);

        Assert.assertThat("Account details are incorrect",
                accountState, is(new AccountState(1L, PI_AMOUNT)));
    }

    @Test
    public void shouldUpdateAccountWhenItDoesExist() {
        String addOperationId = persistenceService.addOperation(AddAccountOperation.of(1L, PI_AMOUNT));

        while (persistenceService.getOperationStatus(addOperationId) == Status.IN_PROGRESS) {
        }

        Status addOperationStatus = persistenceService.getOperationStatus(addOperationId);
        Assert.assertThat("Adding account operation has failed",
                addOperationStatus, is(Status.DONE));

        AccountState accountState = persistenceService.find(1L).orElse(AccountState.DEFAULT);
        Assert.assertThat("Account details are incorrect",
                accountState, is(new AccountState(1L, PI_AMOUNT)));

        String updateOperationId = persistenceService.addOperation(UpdateAccountOperation.of(1L, E_AMOUNT));

        while (persistenceService.getOperationStatus(updateOperationId) == Status.IN_PROGRESS) {
        }

        Status updateOperationStatus = persistenceService.getOperationStatus(updateOperationId);

        Assert.assertThat("Updating account operations has failed",
                updateOperationStatus, is(Status.DONE));

        AccountState updatedAccountState = persistenceService.find(1L).orElse(AccountState.DEFAULT);
        Assert.assertThat("Account details are incorrect",
                updatedAccountState, is(new AccountState(1L, E_AMOUNT)));
    }

    @Test
    public void shouldDeleteAccountWhenItDoesExist() {
        String addOperationId = persistenceService.addOperation(AddAccountOperation.of(1L, PI_AMOUNT));

        while (persistenceService.getOperationStatus(addOperationId) == Status.IN_PROGRESS) {
        }

        Status addOperationStatus = persistenceService.getOperationStatus(addOperationId);
        Assert.assertThat("Adding account operation has failed",
                addOperationStatus, is(Status.DONE));

        AccountState accountState = persistenceService.find(1L).orElse(AccountState.DEFAULT);
        Assert.assertThat("Account details are incorrect",
                accountState, is(new AccountState(1L, PI_AMOUNT)));

        String deleteOperationId = persistenceService.addOperation(DeleteAccountOperation.of(1L));

        while (persistenceService.getOperationStatus(deleteOperationId) == Status.IN_PROGRESS) {
        }

        Status deleteOperationStatus = persistenceService.getOperationStatus(deleteOperationId);
        Assert.assertThat("Deleting account operation has failed",
                deleteOperationStatus, is(Status.DONE));

        Optional<AccountState> deletedAccountState = persistenceService.find(1L);
        Assert.assertThat("Account should not exist anymore", deletedAccountState, is(Optional.empty()));
    }

    @Test
    public void shouldReturnCorrectListOfAccountDetailsWhenThereAre() {
        String firstAddOperationId = persistenceService.addOperation(AddAccountOperation.of(1L, PI_AMOUNT));

        while (persistenceService.getOperationStatus(firstAddOperationId) == Status.IN_PROGRESS) {
        }

        Status addOperationStatus = persistenceService.getOperationStatus(firstAddOperationId);
        Assert.assertThat("Adding first account operation has failed",
                addOperationStatus, is(Status.DONE));

        String secondAddOperationId = persistenceService.addOperation(AddAccountOperation.of(2L, E_AMOUNT));

        while (persistenceService.getOperationStatus(secondAddOperationId) == Status.IN_PROGRESS) {
        }

        addOperationStatus = persistenceService.getOperationStatus(secondAddOperationId);
        Assert.assertThat("Adding second account operation has failed",
                addOperationStatus, is(Status.DONE));

        List<AccountState> accounts = persistenceService.list();

        Assert.assertThat("Number of accounts is incorrect", accounts.size(), is(2));

        Assert.assertThat("First account's details are incorrect",
                accounts.get(0), is(new AccountState(1L, PI_AMOUNT)));

        Assert.assertThat("Second account's details are incorrect",
                accounts.get(1), is(new AccountState(2L, E_AMOUNT)));
    }

    private Jdbi getJdbi() {
        Jdbi jdbi = Jdbi.create(DB_URL, DB_USERNAME, DB_PASSWORD);
        jdbi.installPlugin(new SqlObjectPlugin());
        return jdbi;
    }

    private void setUpDatabase() throws SQLException, LiquibaseException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

        Liquibase liquibase = new Liquibase("test-migrations.xml",
                new ClassLoaderResourceAccessor(), database);
        liquibase.update("");
    }

    private void tearDownDatabase() throws SQLException, DatabaseException {
        Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

        Liquibase liquibase = new Liquibase("test-migrations.xml",
                new ClassLoaderResourceAccessor(), database);

        liquibase.dropAll();
    }
}
