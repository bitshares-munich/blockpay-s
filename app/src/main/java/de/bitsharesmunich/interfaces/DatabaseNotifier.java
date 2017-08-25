package de.bitsharesmunich.interfaces;

/**
 * Interface implemented by any entity in charge of a BlockpayDatabase instance that
 * might do some operations that other parties, namely the DatabaseListeners might
 * be interested in.
 *
 * Created by nelson on 1/5/17.
 */
public interface DatabaseNotifier {

    /**
     * Method used to add a new database listener.
     * @param listener
     */
    public void addListener(DatabaseListener listener);

    /**
     * Method used to remove an existing database listener.
     * @param listener
     */
    public void removeListener(DatabaseListener listener);
}
