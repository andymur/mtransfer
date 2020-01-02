package com.andymur.toyproject;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

public class MTransferConfiguration extends Configuration {

	@Valid
	@NotNull
	private DataSourceFactory database = new DataSourceFactory();

	@Valid
	@NotNull
	private boolean doRunMigrations;

	@Valid
	private boolean mockPersistence;

	@JsonProperty("database")
	public DataSourceFactory getDataSourceFactory() {
		return database;
	}

	@JsonProperty("database")
	public void setDataSourceFactory(final DataSourceFactory dataSourceFactory) {
		this.database = dataSourceFactory;
	}


	@JsonProperty("runMigrationsOnStart")
	public boolean doRunMigrations() {
		return doRunMigrations;
	}

	@JsonProperty("runMigrationsOnStart")
	public void setDoRunMigrations(final boolean doRunMigrations) {
		this.doRunMigrations = doRunMigrations;
	}

	@JsonProperty("mockPersistence")
	public boolean mockPersistence() {
		return mockPersistence;
	}

	@JsonProperty("mockPersistence")
	public void setMockPersistence(final boolean mockPersistence) {
		this.mockPersistence = mockPersistence;
	}
}
