package com.bank.account;

import com.bank.account.dto.TransactionEvent;
import com.bank.account.dto.TransferDTO;
import com.bank.account.kafkaClients.AccountServiceProducer;
import com.bank.account.model.Account;
import com.bank.account.dto.AccountDTO;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    @Inject
    AccountServiceProducer accountServiceProducer;

    @GET
    public List<Account> getAll() {
        return Account.listAll();
    }

    @POST
    @Transactional
    public Account create(AccountDTO dto) {
        Account account = new Account();
        account.accountNumber = dto.accountNumber;
        account.accountTitle = dto.accountTitle;
        account.iban = dto.iban;
        account.balance = dto.balance;
        account.currency = dto.currency;
        account.persist();
        return account;
    }

    @GET
    @Path("/{accNo}")
    public Account getByAccountNumber(@PathParam("accNo") String accNo) {
        System.out.println("request for---------------: "+accNo);
        return Account.find("accountNumber", accNo).firstResult();
    }

    // ---- Payment Transfer Endpoint ----
    @PUT
    @Path("/{accNo}/transfer")
    @Transactional
    public Response transfer(@PathParam("accNo") String fromAccount, TransferDTO dto) throws Exception {
        System.out.println("Body: " + dto);

        Account acc = Account.find("accountNumber", fromAccount).firstResult();
        if (acc == null) throw new NotFoundException("Incorrect Sender Account (not found)");

        if (acc.balance < dto.amount)
            throw new BadRequestException("Insufficient funds");

        // Tentative deduction
        acc.balance -= dto.amount;
        acc.persist();

        // Emit Kafka event
        TransactionEvent event = new TransactionEvent();
        event.eventType = "TransactionInitiated";
        event.transactionId = UUID.randomUUID().toString();
        event.fromAccount = fromAccount;
        event.toAccount = dto.toAccount;
        event.amount = dto.amount;
        event.currency = dto.currency;
        event.status = "PENDING";
        event.timestamp = Instant.now().toString();

        accountServiceProducer.sendEvent(event);

        return Response.ok(event).build();
    }
}
