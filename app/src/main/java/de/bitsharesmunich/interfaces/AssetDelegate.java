package de.bitsharesmunich.interfaces;

import java.util.ArrayList;
import java.util.List;

import de.bitsharesmunich.models.TransactionDetails;

/**
 * Created by Syed Muhammad Muzzammil on 5/19/16.
 */
public interface AssetDelegate {
    //void isUpdate(ArrayList<String> id, ArrayList<String> sym, ArrayList<String> pre, ArrayList<String> am);
    //void TransactionUpdate(List<TransactionDetails> transactionDetails, int nos);
    void getLifetime(String s, int id);
//    void loadAll();
    //void rawTransactionsLoaded(long numberOfTransactions);
    //void allTransactionsLoaded(long numberOfTransactions);
    //void transactionTimeLoaded(long numberOfTransactions, long totalNumberOfTransactions);
    void transactionsLoadComplete(List<TransactionDetails> transactionDetails);
    void transactionsLoadMessageStatus(String message);
    void transactionsLoadFailure(String reason);
   // void transactionsLoadComplete(List<TransactionDetails> transactionDetails, int newTransactionsLoaded);

}
