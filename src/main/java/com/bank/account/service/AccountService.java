package com.bank.account.service;

import com.bank.account.dto.TransactionEvent;
import com.bank.account.kafkaClients.AccountServiceProducer;
import com.bank.account.model.Account;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class AccountService {

    @Inject
    AccountServiceProducer producer;

    @Transactional
    public void process(TransactionEvent event) throws ExecutionException, InterruptedException {
        Account sender = Account.find("accountNumber", event.fromAccount).firstResult();
        System.out.println("Sender: " + sender);
        if (sender != null) {
            sender.balance += event.getAmount();
            sender.persist();
        }
        // Send rolled_back event
        event.eventType = "TransactionRolledBack";
        event.status = "ROLLED_BACK";
        event.timestamp = Instant.now().toString();
        producer.sendEvent(event);
    }
}
