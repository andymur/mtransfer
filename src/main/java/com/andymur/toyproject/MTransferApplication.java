package com.andymur.toyproject;


import java.sql.Connection;
import java.util.concurrent.Executors;

import com.andymur.toyproject.core.AccountServiceImpl;
import com.andymur.toyproject.core.persistence.PersistenceServiceImpl;
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
    //TODO: fix health check warning
    //TODO: check sub configuration
    //TODO: check guice?

    //TODO: Add seq generator
    //TODO: Polish
    //TODO: Integration test with persistence (ideally with random multi threading)
    //TODO: Add documentation and pretty logging in the tests
    //TODO: Add banner
    // https://github.com/dropwizard/dropwizard/tree/master/dropwizard-example
    // https://www.dropwizard.io/en/stable/getting-started.html
    // https://www.dropwizard.io/en/stable/manual/core.html#man-core-commands-configured
    // https://www.dropwizard.io/en/stable/manual/testing.html
    public static void main( String[] args ) throws Exception {
        new MTransferApplication().run(args);
    }

    @Override
    public String getName() {
        return "MTransfer-App";
    }

    @Override
    public void initialize(Bootstrap<MTransferConfiguration> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.addBundle(new MigrationsBundle<MTransferConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(MTransferConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
    }

    @Override
    public void run(MTransferConfiguration configuration,
                    Environment environment) throws Exception {

        if (configuration.doRunMigrations()) {
            runMigrations(configuration, environment);
        }

        final AccountRepository accountRepository = new AccountRepository(createJdbi(configuration, environment));
        final OperationHandler operationHandler = new OperationHandler(accountRepository);

        final PersistenceServiceImpl persistenceService = new PersistenceServiceImpl(accountRepository, operationHandler);
        startPersistenceServiceThread(persistenceService);

        final AccountServiceImpl accountService = new AccountServiceImpl(persistenceService);

        final AccountResource accountResource = new AccountResource(accountService);

        environment.jersey().register(accountResource);
    }

    private void runMigrations(MTransferConfiguration configuration, Environment environment) throws Exception {
        ManagedDataSource managedDataSource = configuration.getDataSourceFactory().build(environment.metrics(), "migrations");

        try (Connection connection = managedDataSource.getConnection()) {
            Liquibase migrator = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        }
    }

    private Jdbi createJdbi(MTransferConfiguration configuration, Environment environment) {
        final JdbiFactory factory = new JdbiFactory();
        return factory.build(environment, configuration.getDataSourceFactory(), "hsqldb");
    }

    private void startPersistenceServiceThread(PersistenceServiceImpl persistenceService) {
        Executors.newFixedThreadPool(1,
                new ThreadFactoryBuilder().setNameFormat("persistence-service").build())
                .submit(persistenceService);
    }
}
