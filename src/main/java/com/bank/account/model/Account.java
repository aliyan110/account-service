package com.bank.account.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Account extends PanacheEntity {
    public String accountNumber;
    public String iban;
    public String accountTitle;
    public double balance;
    public String currency;
}
