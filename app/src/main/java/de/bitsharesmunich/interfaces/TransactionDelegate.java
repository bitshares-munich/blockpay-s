package de.bitsharesmunich.interfaces;

import java.util.List;

import de.bitsharesmunich.models.TransactionDetails;

/**
 * Created by afnan on 8/19/16.
 */
public interface TransactionDelegate {
     void transactionsLoadStatusMessage(String message);
     void transactionsLoadFailureMessage(String reason);
     void transactionsLoadCompleteList(List<TransactionDetails> transactionDetails, int newTransactionsLoaded);
     void transactionsNotLoad();
}
