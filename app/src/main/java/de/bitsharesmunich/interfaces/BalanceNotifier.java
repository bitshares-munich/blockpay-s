package de.bitsharesmunich.interfaces;

/**
 * Interface implemented by any entity that might receive a network notification from
 * change in balance. It should also manage a list of BalanceListeners.
 *
 * Created by nelson on 1/12/17.
 */
public interface BalanceNotifier {

    /**
     * Method to add a new listener.
     * @param listener
     */
    void addBalanceListener(BalanceListener listener);

    /**
     * Removes an existing listener.
     * @param listener
     */
    void removeBalanceListener(BalanceListener listener);
}
