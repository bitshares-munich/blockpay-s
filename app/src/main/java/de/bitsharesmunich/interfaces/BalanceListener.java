package de.bitsharesmunich.interfaces;

import de.bitsharesmunich.graphenej.models.AccountBalanceUpdate;

/**
 * Interface to be implemented by any party that is interested in being notified of balance updates.
 * Created by nelson on 1/12/17.
 */
public interface BalanceListener {

    /**
     * Method called whenever there has been a change in balance.
     * @param balanceObject: The updated balance object.
     */
    public void onBalanceUpdate(AccountBalanceUpdate balanceObject);
}
