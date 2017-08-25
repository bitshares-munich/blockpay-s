package de.bitsharesmunich.models;

import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.UserAccount;

/**
 * Created by nelson on 1/18/17.
 */
public class PendingBalanceUpdate {
    public UserAccount owner;
    public AssetAmount balance;

    public PendingBalanceUpdate(UserAccount userAccount, AssetAmount balance){
        this.owner = userAccount;
        this.balance = balance;
    }
}
