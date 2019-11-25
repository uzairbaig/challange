package com.db.awmd.challenge.service;

import static java.lang.String.format;
import static java.math.BigDecimal.valueOf;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.math.BigDecimal;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.FundsTransferException;
import com.db.awmd.challenge.repository.AccountsRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FundsTransferServiceTest {

    @Rule
    public ExpectedException expectedException = none();

    @Autowired //to create accounts
    private AccountsRepository accountsRepository;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private FundsTransferService fundsTransferService;

    @Before
    public void setup() {
        this.fundsTransferService.setNotificationService(notificationService);
    }

    @Test
    public void shouldTransferFundsSuccessFully() {

        final String accountFromId = randomUUID().toString();
        final String accountToId = randomUUID().toString();

        final Account accountFrom = createAccount(accountFromId, valueOf(1000));
        final Account accountTo = createAccount(accountToId, valueOf(500));

        final BigDecimal amountToTransfer = valueOf(750);
        fundsTransferService.transferFunds(accountFrom.getAccountId(), accountTo.getAccountId(), amountToTransfer);

        assertThat(accountsRepository.getAccount(accountFromId).getBalance(), is(valueOf(250)));
        assertThat(accountsRepository.getAccount(accountToId).getBalance(), is(valueOf(1250)));

        verify(notificationService).notifyAboutTransfer(accountFrom, format("Amount of %1$,.2f has been payed to account # %s from your account", amountToTransfer.doubleValue(), accountTo.getAccountId()));
        verify(notificationService).notifyAboutTransfer(accountTo, format("Amount of %1$,.2f has been received into your account from account # %s", amountToTransfer.doubleValue(), accountFrom.getAccountId()));

    }

    @Test
    public void shouldThrowFundsTransferExceptionWhenInSufficientBalance() {
        final String accountFromId = randomUUID().toString();
        final String accountToId = randomUUID().toString();

        final Account accountFrom = createAccount(accountFromId, valueOf(200));
        final Account accountTo = createAccount(accountToId, valueOf(500));

        final BigDecimal amountToTransfer = valueOf(750);

        expectedException.expect(FundsTransferException.class);
        expectedException.expectMessage(format("Balance is not sufficient in account # %s for this transfer", accountFrom.getAccountId()));

        fundsTransferService.transferFunds(accountFrom.getAccountId(), accountTo.getAccountId(), amountToTransfer);

        verifyZeroInteractions(notificationService);
    }

    @Test
    public void shouldThrowFundsTransferExceptionWhenAmountIsInNegative() {

        final String accountFromId = randomUUID().toString();
        final String accountToId = randomUUID().toString();

        final Account accountFrom = createAccount(accountFromId, valueOf(200));
        final Account accountTo = createAccount(accountToId, valueOf(500));

        final BigDecimal amountToTransfer = valueOf(-50);

        expectedException.expect(FundsTransferException.class);
        expectedException.expectMessage("Amount to transfer should always be in positive");

        fundsTransferService.transferFunds(accountFrom.getAccountId(), accountTo.getAccountId(), amountToTransfer);

        verifyZeroInteractions(notificationService);
    }

    private Account createAccount(final String accountId, final BigDecimal balance) {
        final Account account = new Account(accountId, balance);
        accountsRepository.createAccount(account);
        return account;
    }


}
