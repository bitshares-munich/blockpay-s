package de.bitsharesmunich.models;

import android.content.Context;

import java.util.HashMap;

import de.bitsharesmunich.utils.TinyDB;


/**
 * Created by developer on 8/3/16.
 */
public class EquivalentFiatStorage
{
    //HashMap<String,String> equivalentHm = new HashMap<>();
    //HashMap<String,HashMap<String,String>> equivalentHmFiat = new HashMap<>();
    TinyDB tinyDB;

    public EquivalentFiatStorage(Context _context)
    {
        tinyDB = new TinyDB(_context);
    }

    public void saveEqHM(String fiat,HashMap<String,String> eq)
    {
        String hmName = "equivalentHmFiat" + fiat;
        HashMap<String,String> equivalentHm = (HashMap<String, String>) tinyDB.getHashmap(hmName);

        for ( String key:eq.keySet() )
        {
            equivalentHm.put(key,eq.get(key));
        }

        tinyDB.putHashmapObject(hmName,equivalentHm);
    }

    public HashMap<String,String> getEqHM(String fiat)
    {
        String hmName = "equivalentHmFiat" + fiat;
        return (HashMap<String, String>) tinyDB.getHashmap(hmName);
    }
}
