package de.bitsharesmunich.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AssetsPropertyReader {
    private final String TAG = this.getClass().getName();
    private Context context;
    private Properties properties;

    public AssetsPropertyReader(Context context) {
        this.context = context;
        /**
         * Constructs a new Properties object.
         */
        properties = new Properties();
    }

    public Properties getProperties(String FileName) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(FileName);
            properties.load(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "IOException while opening property asset. Msg: "+e.getMessage());
        }
        return properties;

    }
}