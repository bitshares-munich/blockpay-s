package de.bitsharesmunich.models;

import android.content.Context;

import java.util.HashMap;

import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.TinyDB;


/**
 * Created by Syed Muhammad Muzzammil on 5/30/16.
 */
public class MerchantEmail {

    static public void saveMerchantEmail(Context context,String email){
        Helper.storeStringSharePref(context,"merchant_email",email);
    }

    static public String getMerchantEmail(Context context){
        return Helper.fetchStringSharePref(context,"merchant_email");
    }
}
