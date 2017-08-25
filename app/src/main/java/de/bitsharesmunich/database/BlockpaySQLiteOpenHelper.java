package de.bitsharesmunich.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by nelson on 1/5/17.
 */
public class BlockpaySQLiteOpenHelper extends SQLiteOpenHelper {
    private final String TAG = this.getClass().getName();

    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "blockpay.db";

    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String TYPE_REAL = " REAL";

    private final String SQL_CREATE_ASSETS_TABLE = "CREATE TABLE IF NOT EXISTS " + BlockpayDatabaseContract.Assets.TABLE_NAME + " (" +
            BlockpayDatabaseContract.Assets.COLUMN_ID + " TEXT PRIMARY KEY, " +
            BlockpayDatabaseContract.Assets.COLUMN_SYMBOL + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_PRECISION + TYPE_INTEGER + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_ISSUER + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_DESCRIPTION + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_MAX_SUPPLY + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_MARKET_FEE_PERCENT + TYPE_REAL + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_MAX_MARKET_FEE + TYPE_REAL + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_ISSUER_PERMISSIONS + TYPE_INTEGER + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_FLAGS + TYPE_INTEGER + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_ASSET_TYPE + TYPE_INTEGER + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_BITASSET_ID + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Assets.COLUMN_ASSET_HOLDERS_COUNT + TYPE_INTEGER + " )";

    private final String SQL_CREATE_BALANCES_TABLE = "CREATE TABLE IF NOT EXISTS " + BlockpayDatabaseContract.Balances.TABLE_NAME + " (" +
            BlockpayDatabaseContract.Balances.COLUMN_USER_ID + TYPE_TEXT + " NOT NULL, " +
            BlockpayDatabaseContract.Balances.COLUMN_ASSET_ID + TYPE_TEXT + " NOT NULL, " +
            BlockpayDatabaseContract.Balances.COLUMN_ASSET_AMOUNT + TYPE_INTEGER + " NOT NULL, " +
            BlockpayDatabaseContract.Balances.COLUMN_LAST_UPDATE + TYPE_INTEGER + ", "+
            "PRIMARY KEY (" +
            BlockpayDatabaseContract.Balances.COLUMN_ASSET_ID + ", " +
            BlockpayDatabaseContract.Balances.COLUMN_USER_ID + "))";

    private final String SQL_CREATE_BRIDGE_RATES_TABLE = "CREATE TABLE IF NOT EXISTS " + BlockpayDatabaseContract.BridgeRates.TABLE_NAME + " (" +
            BlockpayDatabaseContract.BridgeRates.COLUMN_INPUT_ASSET + TYPE_TEXT + " NOT NULL, " +
            BlockpayDatabaseContract.BridgeRates.COLUMN_OUTPUT_ASSET + TYPE_TEXT + " NOT NULL, " +
            BlockpayDatabaseContract.BridgeRates.COLUMN_FEE_RATE + TYPE_REAL + " NOT NULL, " +
            "PRIMARY KEY (" +
            BlockpayDatabaseContract.BridgeRates.COLUMN_INPUT_ASSET + ", " +
            BlockpayDatabaseContract.BridgeRates.COLUMN_OUTPUT_ASSET + "))";

    private static final String SQL_CREATE_USER_ACCOUNTS_TABLE = "CREATE TABLE IF NOT EXISTS " + BlockpayDatabaseContract.UserAccounts.TABLE_NAME + "(" +
            BlockpayDatabaseContract.UserAccounts.COLUMN_ID + " TEXT PRIMARY KEY, " +
            BlockpayDatabaseContract.UserAccounts.COLUMN_NAME + TYPE_TEXT + ")";

    private static final String SQL_CREATE_TRANSFERS_TABLE = "CREATE TABLE IF NOT EXISTS " + BlockpayDatabaseContract.Transfers.TABLE_NAME + " (" +
            BlockpayDatabaseContract.Transfers.COLUMN_ID + " TEXT PRIMARY KEY, " +
            BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP + TYPE_INTEGER + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_FEE_AMOUNT + TYPE_INTEGER + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_FEE_ASSET_ID + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_FROM + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_TO + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_AMOUNT + TYPE_INTEGER + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_ASSET_ID + TYPE_TEXT + " DEFAULT '', " +
            BlockpayDatabaseContract.Transfers.COLUMN_MEMO_MESSAGE + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_MEMO_FROM + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_MEMO_TO + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_BLOCK_NUM + TYPE_INTEGER + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE_ASSET_ID + TYPE_TEXT + ", " +
            BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE + TYPE_INTEGER + ", " +
            "FOREIGN KEY (" + BlockpayDatabaseContract.Transfers.COLUMN_FEE_ASSET_ID + ") REFERENCES " +
            BlockpayDatabaseContract.Assets.TABLE_NAME + "(" + BlockpayDatabaseContract.Assets.COLUMN_ID + "), " +
            "FOREIGN KEY (" + BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_ASSET_ID + ") REFERENCES " +
            BlockpayDatabaseContract.Assets.TABLE_NAME + "(" + BlockpayDatabaseContract.Assets.COLUMN_ID + "), " +
            "FOREIGN KEY (" + BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE_ASSET_ID + ") REFERENCES " +
            BlockpayDatabaseContract.Assets.TABLE_NAME + "(" + BlockpayDatabaseContract.Assets.COLUMN_ID + "))";

    public BlockpaySQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        Log.d(TAG, SQL_CREATE_ASSETS_TABLE);
        Log.d(TAG, SQL_CREATE_BALANCES_TABLE);
        Log.d(TAG, SQL_CREATE_BRIDGE_RATES_TABLE);
        Log.d(TAG, SQL_CREATE_TRANSFERS_TABLE);

        try{
            db.execSQL(SQL_CREATE_USER_ACCOUNTS_TABLE);
        }catch(SQLiteException e){
            Log.w(TAG,"SQLiteException. Msg: "+e.getMessage());
        }
        try{
            db.execSQL(SQL_CREATE_ASSETS_TABLE);
        }catch(SQLiteException e){
            Log.w(TAG, "SQLiteException. Msg: "+e.getMessage());
        }
        try{
            db.execSQL(SQL_CREATE_TRANSFERS_TABLE);
        }catch(SQLiteException e){
            Log.w(TAG, "SQLiteException. Msg: "+e.getMessage());
        }
        try{
            db.execSQL(SQL_CREATE_BALANCES_TABLE);
        }catch(SQLiteException e){
            Log.w(TAG, "SQLiteException. Msg: "+e.getMessage());
        }
        try{
            db.execSQL(SQL_CREATE_BRIDGE_RATES_TABLE);
        }catch(SQLiteException e){
            Log.w(TAG, "SQLiteException. Msg: "+e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade");
        onCreate(db);
    }
}
