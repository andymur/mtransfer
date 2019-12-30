package com.andymur.toyproject.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountService {
    void transfer(long sourceAccountId, long destinationAccountId, BigDecimal amountToTransfer);
    void withdraw(long accountId, BigDecimal amountToWithdraw);
    void deposit(long accountId, BigDecimal amountToAdd);
    AccountState get(long id);
    List<AccountState> list();
    AccountState put(AccountState accountState);
    Optional<AccountState> delete(long id);
}
