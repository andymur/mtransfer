package com.andymur.toyproject.db;

import com.andymur.toyproject.core.AccountState;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.List;

public interface AccountDao {
    //TODO: make it from here
    /*@SqlQuery("SELECT * FROM accounts ORDER BY id")
    @RegisterBeanMapper(AccountState.class)*/
    List<AccountState> listAccounts();
}
