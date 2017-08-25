package de.bitsharesmunich.models;

import java.util.ArrayList;

public class AccountDetails
{
    public String status;
    public String brain_key;
    public String address;
    public String account_id;
    public String pub_key;
    public String wif_key;
    public String msg;
    public String pinCode;
    public int posBackupAsset;
    public ArrayList<AccountAssets> AccountAssets;
    public Boolean isSelected;
    public Boolean isLifeTime=false;
    public String account_name;
}
