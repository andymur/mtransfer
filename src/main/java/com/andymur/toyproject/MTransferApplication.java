package com.andymur.toyproject;


import java.sql.Connection;

import com.andymur.toyproject.core.AccountService;
import com.andymur.toyproject.db.AccountDao;
import com.andymur.toyproject.db.AccountDaoImpl;
import com.andymur.toyproject.resources.AccountResource;
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

        final AccountDao accountDao = new AccountDaoImpl(createJdbi(configuration, environment));
        final AccountService accountService = new AccountService(accountDao);

        final AccountResource resource = new AccountResource(accountService);
        environment.jersey().register(resource);
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
}
