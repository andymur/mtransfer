package com.andymur.toyproject.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class AccountState {

    public static final AccountState DEFAULT = new AccountState(0, BigDecimal.ZERO);

    private long id = 1;
    private BigDecimal amount = BigDecimal.ZERO;

    public AccountState() {
        //for the sake of de-serialization
    }

    public AccountState(final long id,
                        final BigDecimal amount) {
        this.id = id;
        this.amount = amount;
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public BigDecimal getAmount() {
        return amount;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public void addAmount(final BigDecimal amountToAdd) {
        setAmount(amount.add(amountToAdd));
    }

    public AccountState copyOf() {
        return new AccountState(id, amount);
    }

    @Override
    public String toString() {
        return "AccountState{" +
                "id=" + id +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountState that = (AccountState) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
