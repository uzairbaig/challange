package com.db.awmd.challenge.service;

import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.FundsTransferException;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FundsTransferService {

    @Autowired
    private AccountsService accountsService;

    @Setter //should be autowired too - using setter to set service from tests
    private NotificationService notificationService;

    //should be declared @transactional so transfer happen in single transaction otherwise rollback in any unexpected situation
    public void transferFunds(final String accountFromId, final String accountToId, final BigDecimal amountToTransfer) {

        validateTransferAmount(amountToTransfer);

        final Account accountFrom = getAccount(accountFromId);
        final Account accountTo = getAccount(accountToId);
        final BigDecimal accountFromBalance = accountFrom.getBalance();

        validateIfFundsAreSufficientForTransfer(accountFrom, amountToTransfer);

        accountFrom.setBalance(accountFromBalance.subtract(amountToTransfer));
        accountTo.setBalance(accountTo.getBalance().add(amountToTransfer));
        notificationService.notifyAboutTransfer(accountFrom, format("Amount of %1$,.2f has been payed to account # %s from your account", amountToTransfer.doubleValue(), accountTo.getAccountId()));
        notificationService.notifyAboutTransfer(accountTo, format("Amount of %1$,.2f has been received into your account from account # %s", amountToTransfer.doubleValue(), accountFrom.getAccountId()));

    }

    private void validateTransferAmount(final BigDecimal amountToTransfer) {
        if (amountToTransfer.compareTo(ZERO) == -1) {
            throw new FundsTransferException("Amount to transfer should always be in positive");
        }
    }

    private void validateIfFundsAreSufficientForTransfer(final Account accountFrom,
                                                         final BigDecimal amountToBeDeducted) {
        if (accountFrom.getBalance().subtract(amountToBeDeducted).compareTo(ZERO) == -1) {
            throw new FundsTransferException(format("Balance is not sufficient in account # %s for this transfer", accountFrom.getAccountId()));
        }
    }

    private Account getAccount(final String accountFromId) {
        return accountsService.getAccount(accountFromId);
    }


}
