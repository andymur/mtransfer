package com.andymur.toyproject.db;

import com.andymur.toyproject.core.AccountState;
import org.jdbi.v3.sqlobject.SqlOperation;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountDao {

    @SqlQuery("SELECT id, amount FROM accounts ORDER BY id")
    @RegisterBeanMapper(AccountState.class)
    List<AccountState> list();

    @SqlQuery("SELECT id, amount FROM accounts where id = :id")
    @RegisterBeanMapper(AccountState.class)
    Optional<AccountState> find(@Bind("id") long id);

    @SqlUpdate("INSERT INTO accounts (id, amount) values (:id, :amount)")
    void addAccount(@Bind("id") long id, @Bind("amount") BigDecimal amount);

    @SqlUpdate("DELETE FROM accounts where id = :id")
    void deleteAccount(@Bind("id") long id);

    @SqlUpdate("UPDATE accounts SET amount = :newAmount where id = :id)")
    void updateAccount(@Bind("id") long id, @Bind("newAmount") BigDecimal newAmount);
}
