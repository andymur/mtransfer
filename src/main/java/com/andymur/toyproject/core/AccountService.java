package com.andymur.toyproject.core;

import java.math.BigDecimal;
import java.util.Optional;
//TODO: document me
public interface AccountService {

    void transfer(long sourceAccountId, long destinationAccountId, BigDecimal amountToTransfer);

    AccountState get(long id);

    AccountState put(AccountState accountState);

    Optional<AccountState> delete(long id);
}
