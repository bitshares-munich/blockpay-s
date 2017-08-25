package de.bitsharesmunich.interfaces;

import android.os.Bundle;

/**
 * Interface that must be implemented by any party interested in being notified about database
 * update events.
 *
 * Created by nelson on 1/5/17.
 */
public interface DatabaseListener {

    /**
     * Code used to indicate that the asset list has been updated.
     */
    int ASSETS_UPDATED = 0x01;

    /**
     * Key used to transmit a notification about the number of assets already stored in the database
     */
    String KEY_ASSET_UPDATE_COUNT = "asset_update_count";

    /**
     * Method to be fired whenever a database update event has been detected.
     * @param code: The code informing the listener about what specific event that triggered this method
     * @param data: A bundle of extra data in case it is needed.
     */
    void onDatabaseUpdated(int code, Bundle data);
}
