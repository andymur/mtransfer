package com.andymur.toyproject;


import java.sql.Connection;
import java.util.concurrent.Executors;

import com.andymur.toyproject.core.AccountServiceImpl;
import com.andymur.toyproject.core.TransferOperationsAuditLog;
import com.andymur.toyproject.core.persistence.PersistenceService;
import com.andymur.toyproject.core.persistence.PersistenceServiceImpl;
import com.andymur.toyproject.core.persistence.PersistenceServiceMock;
import com.andymur.toyproject.core.persistence.operations.OperationHandler;
import com.andymur.toyproject.db.AccountRepository;
import com.andymur.toyproject.resources.AccountResource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;

public class MTransferApplication extends Application<MTransferConfiguration> {
	private static final int PERSISTENCE_THREAD_NUMBERS = 1;

	private final TransferOperationsAuditLog resourceAuditLog = new TransferOperationsAuditLog();

	public static void main(final String[] args) throws Exception {
		new MTransferApplication().run(args);
	}

	@Override
	public String getName() {
		return "MTransfer-App";
	}

	@Override
	public void initialize(final Bootstrap<MTransferConfiguration> bootstrap) {
		super.initialize(bootstrap);
		bootstrap.addBundle(new MigrationsBundle<MTransferConfiguration>() {
			@Override
			public DataSourceFactory getDataSourceFactory(MTransferConfiguration configuration) {
				return configuration.getDataSourceFactory();
			}
		});
	}

	@Override
	public void run(final MTransferConfiguration configuration,
					final Environment environment) throws Exception {

		if (configuration.doRunMigrations()) {
			runMigrations(configuration, environment);
		}

		final AccountRepository accountRepository = new AccountRepository(createJdbi(configuration, environment));
		final OperationHandler operationHandler = new OperationHandler(accountRepository);


		final PersistenceService persistenceService = configuration.mockPersistence()
				? new PersistenceServiceMock()
				: new PersistenceServiceImpl(accountRepository, operationHandler);

		startPersistenceServiceThread(persistenceService);

		final AccountServiceImpl accountService = new AccountServiceImpl(persistenceService);
		final AccountResource accountResource = new AccountResource(accountService, resourceAuditLog);

		environment.jersey().register(accountResource);
	}

	public TransferOperationsAuditLog getResourceAuditLog() {
		return resourceAuditLog;
	}

	private void runMigrations(final MTransferConfiguration configuration,
							   final Environment environment) throws Exception {
		ManagedDataSource managedDataSource = configuration.getDataSourceFactory().build(environment.metrics(), "migrations");

		try (Connection connection = managedDataSource.getConnection()) {
			Liquibase migrator = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
			migrator.update("");
		}
	}

	private Jdbi createJdbi(final MTransferConfiguration configuration,
							final Environment environment) {
		final JdbiFactory factory = new JdbiFactory();
		return factory.build(environment, configuration.getDataSourceFactory(), "hsqldb");
	}

	private void startPersistenceServiceThread(final PersistenceService persistenceService) {
		Executors.newFixedThreadPool(PERSISTENCE_THREAD_NUMBERS,
				new ThreadFactoryBuilder().setNameFormat("persistence-service").build())
				.submit(persistenceService);
	}
}
