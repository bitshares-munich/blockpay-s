package de.bitsharesmunich.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;

import com.google.common.primitives.UnsignedLong;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.AssetOptions;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.models.AccountProperties;
import de.bitsharesmunich.graphenej.models.BlockHeader;
import de.bitsharesmunich.graphenej.models.HistoricalTransfer;
import de.bitsharesmunich.graphenej.objects.Memo;
import de.bitsharesmunich.graphenej.operations.TransferOperation;
import de.bitsharesmunich.interfaces.DatabaseListener;
import de.bitsharesmunich.models.BlocktradesTradingPair;

/**
 * Database wrapper class, providing access to the underlying database.
 * 
 * Created by nelson on 1/5/17.
 */

public class BlockpayDatabase {
    private String TAG = this.getClass().getName();
    /**
     * Constant used to specify an unlimited amount of transactions for the
     * second argument of the getTransactions method.
     */
    public static final int ALL_TRANSACTIONS = -1;

    /**
     * The default number of transactions to load in the transaction list
     */
    public static final int DEFAULT_TRANSACTION_BATCH_SIZE = 5;

    private BlockpaySQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;

    public BlockpayDatabase(Context context){
        dbHelper = new BlockpaySQLiteOpenHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    public void close(){
        db.close();
    }

    // ################## Assets ##################

    /**
     * Stores a list of assets
     * @param assets: Assets to store
     * @param listener: An optional parameter of a class implementing the DatabaseListener interface
     */
    public int putAssets(List<Asset> assets, DatabaseListener listener){
        int count = 0;
        Bundle updateBundle = new Bundle();
        ContentValues contentValues = null;
        for(Asset asset : assets){
            contentValues = new ContentValues();
            contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ID, asset.getObjectId());
            contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_PRECISION, asset.getPrecision());
            contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_SYMBOL, asset.getSymbol());
            contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ISSUER, asset.getIssuer());
            contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ASSET_TYPE, Asset.AssetType.UIA.ordinal());
            contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ASSET_HOLDERS_COUNT, -1);

            AssetOptions options = asset.getAssetOptions();
            if(options != null){
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_MAX_SUPPLY, options.getMaxSupply().toString());
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_MARKET_FEE_PERCENT, options.getMarketFeePercent());
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_MAX_MARKET_FEE, options.getMaxMarketFee().toString());
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ISSUER_PERMISSIONS, options.getIssuerPermissions());
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_FLAGS, options.getFlags());
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_DESCRIPTION, options.getDescription());
            }
            if(asset.getBitassetId() != null){
                Log.d(TAG, String.format("Got bitasset id for %s, id: %s bitAssetId: %s", asset.getSymbol(), asset.getObjectId(), asset.getBitassetId()));
                // Just for smartcoins and prediction market tokens
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_BITASSET_ID, asset.getBitassetId());
                // We can't yet tell if this is a smartcoin or a prediction market token, so we assume the first
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ASSET_TYPE, Asset.AssetType.SMART_COIN.ordinal());
            }else{
                contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_BITASSET_ID, "");
            }

            try {
                db.insertOrThrow(BlockpayDatabaseContract.Assets.TABLE_NAME, null, contentValues);
                count++;
                if(listener != null){
                    updateBundle.putInt(DatabaseListener.KEY_ASSET_UPDATE_COUNT, count);
                    listener.onDatabaseUpdated(DatabaseListener.ASSETS_UPDATED, updateBundle);
                }
            } catch(SQLException e){
                // No need to log this exception, as it is expected that some
                Log.e(TAG,"SQLException. Msg: "+e.getMessage()+". Asset: "+asset.getObjectId());
            }
        }
        return count;
    }

    /**
     * Updates the asset type
     * @param asset: The asset to update
     * @param type: The new value of the type field.
     */
    public void updateAssetType(Asset asset, Asset.AssetType type){
        ContentValues contentValues = new ContentValues();
        contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ASSET_TYPE, type.ordinal());
        String where = BlockpayDatabaseContract.Assets.COLUMN_ID + "=?";
        String[] whereArgs = { asset.getObjectId() };
        db.update(BlockpayDatabaseContract.Assets.TABLE_NAME, contentValues, where, whereArgs);
    }

    /**
     * Updates the 'holders_count' column of the Assets table, which stores the number of
     * users currently holding a given asset.
     *
     * @param asset: The asset to update
     * @param holdersCount: The number of accounts holding this asset.
     */
    public void updateAssetHolders(Asset asset, long holdersCount){
        ContentValues contentValues = new ContentValues();
        contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ASSET_HOLDERS_COUNT, holdersCount);
        String where = BlockpayDatabaseContract.Assets.COLUMN_ID + "=?";
        String[] whereArgs = { asset.getObjectId() };
        db.update(BlockpayDatabaseContract.Assets.TABLE_NAME, contentValues, where, whereArgs);
    }

    /**
     * Retrieves a list of assets filtered by the number of users holding them.
     * @param minHolderCount: The minimum number of holders that an asset must have
     *                      in order to be included in the list.
     * @param maxHolderCount: The maximum amount of holders an asset must have in order to be
     *                      included in the list
     * @return: A filtered list of assets.
     */
    public List<Asset> getAssets(long minHolderCount, long maxHolderCount){
        String selection = BlockpayDatabaseContract.Assets.COLUMN_ASSET_HOLDERS_COUNT + " > ? AND " +
                BlockpayDatabaseContract.Assets.COLUMN_ASSET_HOLDERS_COUNT + "< ?";
        String[] selectionArgs = new String[]{ String.format("%d", minHolderCount), String.format("%d", maxHolderCount) };
        return this.getAssets(selection, selectionArgs);
    }

    /**
     * Retrieves a HashMap instance that will map an asset id to an instance of the Asset class.
     *
     * @return: A HashMap connecting asset ids to Asset object instances.
     */
    public HashMap<String, Asset> getAssetMap() {
        HashMap<String, Asset> assetMap = new HashMap<>();
        String[] columns = {
                BlockpayDatabaseContract.Assets.COLUMN_ID,
                BlockpayDatabaseContract.Assets.COLUMN_SYMBOL,
                BlockpayDatabaseContract.Assets.COLUMN_PRECISION
        };
        Cursor cursor = db.query(true, BlockpayDatabaseContract.Assets.TABLE_NAME, columns, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(0);
                String symbol = cursor.getString(1);
                int precision = cursor.getInt(2);
                assetMap.put(cursor.getString(0), new Asset(id, symbol, precision));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return assetMap;
    }

    /**
     * Returns a list of all assets in the database.
     * @return: List of all assets
     */
    public List<Asset> getAssets(){
        return this.getAssets(null, null);
    }

    /**
     * Retrieves a list of known witness-fed assets.
     * @return: List of known assets
     */
    public List<Asset> getWitnessFedAssets(){
        String selection = BlockpayDatabaseContract.Assets.COLUMN_BITASSET_ID + " != ?";
        String[] selectionArgs = new String[] {""};
        return this.getAssets(selection, selectionArgs);
    }

    /**
     * Retrieves a list of assets, filtered by their asset type.
     * @param type: The type of the assets to be included in the resuling list.
     * @return: List of assets.
     */
    public List<Asset> getAssets(Asset.AssetType type){
        String selection = BlockpayDatabaseContract.Assets.COLUMN_ASSET_TYPE + "=?";
        String[] selectionArgs = new String[]{ String.format("%s", type.ordinal()) };
        return this.getAssets(selection, selectionArgs);
    }

    /**
     * Private method used to perform a basic query in the assets table. This method should
     * be called by other variants so that they don't have to repeat the basic query code, and
     * can instead just focus on building a 'selection' and 'selectionArgs' parameters.
     *
     * @param selection: The query 'selection' argument.
     * @param selectionArgs: The query 'selectionArgs' argument.
     * @return: The list of assets that is the result of the query.
     */
    private List<Asset> getAssets(String selection, String[] selectionArgs){
        String table = BlockpayDatabaseContract.Assets.TABLE_NAME;
        ArrayList<Asset> assetList = new ArrayList<>();
        Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);
        if(cursor.moveToFirst()){
            do {
                Asset asset = buildAsset(cursor);
                assetList.add(asset);
            }while(cursor.moveToNext());
        }
        cursor.close();
        return assetList;
    }

    /**
     * Returns a given Asset instance, given only its bit asset data id
     * @param bitAssetId: Bit asset data id field.
     * @return: Instance of Asset class with all possible fields.
     */
    public Asset getAssetByProperty(String bitAssetId){
        return this.getAssetByProperty(BlockpayDatabaseContract.Assets.COLUMN_BITASSET_ID, bitAssetId);
    }

    /**
     * Returns a given Asset instance, given only its symbol
     * @param symbol: The symbol used by the desired Asset
     * @return
     */
    public Asset getAssetBySymbol(String symbol){
        return this.getAssetByProperty(BlockpayDatabaseContract.Assets.COLUMN_SYMBOL, symbol);
    }

    /**
     * Generic method that receives a specific column and value pair and returns any asset that
     * matches that paired condition.
     * @param column: The column to use
     * @param value: The desired value of the previously specified column.
     * @return: An Asset instance, if there is one, null otherwise.
     */
    private Asset getAssetByProperty(String column, String value){
        String table = BlockpayDatabaseContract.Assets.TABLE_NAME;
        String selection = column + "=?";
        String[] selectionArgs = new String[]{ value };
        Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);
        Asset asset = null;
        if(cursor.moveToFirst()){
            asset = buildAsset(cursor);
        }
        cursor.close();
        return asset;
    }

    /**
     * Given an incomplete instance of the Asset object, performs a query and fills the asset
     * reference with 'precision', 'symbol' and 'description' data.
     *
     * The incomplete object passed as argument must have at least its object if set.
     * @param asset: Incomplete asset instance.
     * @return: Complete asset instance.
     */
    public Asset fillAssetDetails(Asset asset){
        Asset completeAsset = null;
        String table = BlockpayDatabaseContract.Assets.TABLE_NAME;
        String selection = BlockpayDatabaseContract.Assets.COLUMN_ID + "=?";
        String[] selectionArgs = new String[]{ asset.getObjectId() };
        Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);
        if(cursor.moveToFirst()){
            completeAsset = buildAsset(cursor);
        }
        cursor.close();
        return completeAsset;
    }

    /**
     * Internal method used to take the data from the cursor and create a representative Asset instance.
     * This method will not modify the cursor state neither close it.
     * @param cursor: The cursor from where to read the data.
     * @return: An Asset instance.
     */
    private Asset buildAsset(Cursor cursor){
        String id = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_ID));
        String symbol = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_SYMBOL));
        int precision = cursor.getInt(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_PRECISION));
        String issuer = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_ISSUER));
        Asset asset = new Asset(id, symbol, precision, issuer);
        AssetOptions options = new AssetOptions();
        options.setMaxSupply(UnsignedLong.valueOf(cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_MAX_SUPPLY))));
        options.setMarketFeePercent(cursor.getFloat(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_MARKET_FEE_PERCENT)));
        options.setFlags(cursor.getInt(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_FLAGS)));
        options.setIssuerPermissions(cursor.getInt(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_ISSUER_PERMISSIONS)));
        options.setDescription(cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_DESCRIPTION)));
        asset.setAssetOptions(options);

        // The bitasset id field is only present in smartcoins and prediction markets
        String bitAssetId = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_BITASSET_ID));
        if(bitAssetId != null){
            asset.setBitassetDataId(bitAssetId);
        }
        // Selecting the proper asset type
        int ordinal = cursor.getInt(cursor.getColumnIndex(BlockpayDatabaseContract.Assets.COLUMN_ASSET_TYPE));
        Asset.AssetType assetType = Asset.AssetType.values()[ordinal];
        asset.setAssetType(assetType);
        return asset;
    }

    /**
     * Resets all the recorded data about the amount of people holding each asset. This might
     * be required from time to time in order to keep the database up to date.
     */
    public void resetAssetHolders(){
        ContentValues contentValues = new ContentValues();
        contentValues.put(BlockpayDatabaseContract.Assets.COLUMN_ASSET_HOLDERS_COUNT, -1);
        String table = BlockpayDatabaseContract.Assets.TABLE_NAME;
        String where = BlockpayDatabaseContract.Assets.COLUMN_ASSET_HOLDERS_COUNT + "=?";
        String[] whereArgs = new String[]{ String.format("%d", -1) };
        db.update(table, contentValues, where, whereArgs);
    }

    // ################## Balances ##################

    /**
     * Updates the existing balances for a single user, or inserts them in case the database doesn't already have
     * entries for a specific asset and the user account.
     * @param userAccount: User account whose balances are being stored.
     * @param balances: List of balances
     */
    public void putBalances(UserAccount userAccount, List<AssetAmount> balances){
        for(AssetAmount assetAmount : balances){
            ContentValues contentValues = new ContentValues();
            contentValues.put(BlockpayDatabaseContract.Balances.COLUMN_USER_ID, userAccount.getObjectId());
            contentValues.put(BlockpayDatabaseContract.Balances.COLUMN_ASSET_ID, assetAmount.getAsset().getObjectId());
            contentValues.put(BlockpayDatabaseContract.Balances.COLUMN_ASSET_AMOUNT, assetAmount.getAmount().longValue());
            contentValues.put(BlockpayDatabaseContract.Balances.COLUMN_LAST_UPDATE, System.currentTimeMillis() / 1000);

            String table = BlockpayDatabaseContract.Balances.TABLE_NAME;
            String where = BlockpayDatabaseContract.Balances.COLUMN_USER_ID + "=? AND " +
                    BlockpayDatabaseContract.Balances.COLUMN_ASSET_ID + "=?";
            String[] whereArgs = new String[]{ userAccount.getObjectId(), assetAmount.getAsset().getObjectId()};

            int updated = db.update(table, contentValues, where, whereArgs);
            if(updated == 0){
                db.insert(table, null, contentValues);
            }
        }
    }

    /**
     * Same as putBalances, but requiring only one balance instead of a list of balances.
     * @param userAccount: The user account whose balance we want to update.
     * @param balance: The balance to update.
     */
    public void putBalance(UserAccount userAccount, AssetAmount balance){
        ArrayList<AssetAmount> balanceList = new ArrayList<>();
        balanceList.add(balance);
        this.putBalances(userAccount, balanceList);
    }

    /**
     * Returns an instance of the AssetAmount class, representing the current balance of a given asset
     * for a specific user.
     * @param userAccount: The user account from which we want to query the balance.
     * @param asset: The specific asset we are interested in.
     * @return: Instance of the AssetAmount class representing the current recorded balance of a given asset.
     */
    public AssetAmount getBalance(UserAccount userAccount, Asset asset){
        String table = BlockpayDatabaseContract.Balances.TABLE_NAME;
        String[] columns = new String[] {
                BlockpayDatabaseContract.Balances.COLUMN_USER_ID,
                BlockpayDatabaseContract.Balances.COLUMN_ASSET_ID,
                BlockpayDatabaseContract.Balances.COLUMN_ASSET_AMOUNT
        };
        String where = BlockpayDatabaseContract.Balances.COLUMN_USER_ID + "=?" +
                " AND " + BlockpayDatabaseContract.Balances.COLUMN_ASSET_ID + "=?";
        String[] whereArgs = new String[]{ userAccount.getObjectId(), asset.getObjectId()};
        AssetAmount assetAmount = null;
        Cursor cursor = db.query(table, columns, where, whereArgs, null, null, null, null);
        Asset queriedAsset = this.fillAssetDetails(asset);
        if(cursor.moveToFirst()){
            assetAmount = new AssetAmount(UnsignedLong.valueOf(cursor.getLong(2)), queriedAsset);
        }else{
            assetAmount = new AssetAmount(UnsignedLong.valueOf(0), queriedAsset);
        }
        cursor.close();
        return assetAmount;
    }

    /**
     * Similar to getBalance, but will return all balances of a user, instead of just one.
     * @param userAccount: The user account from which we want to query the balance.
     * @return: A list of balances, one for every asset.
     */
    public List<AssetAmount> getAllBalances(UserAccount userAccount){
        String table = BlockpayDatabaseContract.Balances.TABLE_NAME;
        String[] columns = new String[] {
                BlockpayDatabaseContract.Balances.COLUMN_USER_ID,
                BlockpayDatabaseContract.Balances.COLUMN_ASSET_ID,
                BlockpayDatabaseContract.Balances.COLUMN_ASSET_AMOUNT
        };
        String where = BlockpayDatabaseContract.Balances.COLUMN_USER_ID + "=?";
        String[] whereArgs = new String[]{ userAccount.getObjectId() };
        Cursor cursor = db.query(table, columns, where, whereArgs, null, null, null, null);

        AssetAmount assetAmount = null;
        ArrayList<AssetAmount> balances = new ArrayList<>();
        if(cursor.moveToFirst()){
            do{
                Asset queriedAsset = this.fillAssetDetails(new Asset(cursor.getString(1)));
                assetAmount = new AssetAmount(UnsignedLong.valueOf(cursor.getLong(2)), queriedAsset);
                balances.add(assetAmount);
            }while(cursor.moveToNext());
        }
        cursor.close();
        return balances;
    }

    // ################## Bridge fees ##################

    /**
     * Creates or updates existing entry in the 'bridge_rates' table, used to keep track of the specific
     * fee rates charged bt the bridge for each cryptocurrency pair.
     * @param tradingPairs
     */
    public void putBridgeFees(List<BlocktradesTradingPair> tradingPairs){
        ContentValues contentValues;
        if(tradingPairs == null ){
            return;
        }
        for(BlocktradesTradingPair tradingPair : tradingPairs){
            contentValues = new ContentValues();
            contentValues.put(BlockpayDatabaseContract.BridgeRates.COLUMN_INPUT_ASSET, tradingPair.getInputCoinType().toUpperCase());
            contentValues.put(BlockpayDatabaseContract.BridgeRates.COLUMN_OUTPUT_ASSET, tradingPair.getOutputCoinType().toUpperCase());
            contentValues.put(BlockpayDatabaseContract.BridgeRates.COLUMN_FEE_RATE, tradingPair.getRateFee());

            String table = BlockpayDatabaseContract.BridgeRates.TABLE_NAME;
            String where = BlockpayDatabaseContract.BridgeRates.COLUMN_INPUT_ASSET + "=? AND " +
                    BlockpayDatabaseContract.BridgeRates.COLUMN_OUTPUT_ASSET + "=?";
            String[] whereArgs = new String[]{ tradingPair.getInputCoinType(), tradingPair.getOutputCoinType() };
            int updated = db.update(table, contentValues, where, whereArgs);
            if(updated == 0){
                db.insert(table, null, contentValues);
            }
        }
    }

    /**
     * Given an input-output pair, this method queries the database in order to retrieve the
     * percentage of the fee charged by the blocktrades bridge for this specific currency pair.
     *
     * @param inputCoin: Input coin symbol
     * @param outputCoin: Ouput coin symbol.
     * @return: The corresponding percentage fee, or -1.0 if no fee has been registered
     * for that specific currency pair.
     */
    public double getBridgeFee(String inputCoin, String outputCoin){
        String table = BlockpayDatabaseContract.BridgeRates.TABLE_NAME;
        String where = BlockpayDatabaseContract.BridgeRates.COLUMN_INPUT_ASSET + "=? AND " +
                BlockpayDatabaseContract.BridgeRates.COLUMN_OUTPUT_ASSET + "=?";
        String[] whereArgs = new String[]{ inputCoin.toUpperCase(), outputCoin.toUpperCase() };
        Cursor cursor = db.query(table, null, where, whereArgs, null, null, null, null);
        double feeRate = -1.0;
        if(cursor.moveToFirst()){
            feeRate = cursor.getDouble(cursor.getColumnIndex(BlockpayDatabaseContract.BridgeRates.COLUMN_FEE_RATE));
        }
        cursor.close();
        return feeRate;
    }

    /**
     * Checks to see if the bridge has a specific output coin.
     * @param outputCoin: Ouput coin symbol.
     * @return: True if the bridge currently supports that specific output, false otherwise.
     */
    public boolean bridgeSupportsOuput(String outputCoin){
        String table = BlockpayDatabaseContract.BridgeRates.TABLE_NAME;
        String where = BlockpayDatabaseContract.BridgeRates.COLUMN_OUTPUT_ASSET + "=? OR " +
                BlockpayDatabaseContract.BridgeRates.COLUMN_OUTPUT_ASSET + "=?";
        String[] whereArgs = new String[]{ outputCoin.toUpperCase(), "BIT"+outputCoin.toUpperCase() };
        Cursor cursor = db.query(table, null, where, whereArgs, null, null, null, null);
        boolean result = false;
        if(cursor.moveToFirst()){
            result = true;
        }
        cursor.close();
        return result;
    }

    // ################## User accounts ##################

    /**
     * Inserts a list of entries in the user_accounts table
     *
     * @param accountProperties: List of account properties.
     * @return: The number of accounts inserted
     */
    public int putUserAccounts(List<AccountProperties> accountProperties) {
        ContentValues contentValues = new ContentValues();
        int count = 0;
        for (AccountProperties properties : accountProperties) {
            contentValues.put(BlockpayDatabaseContract.UserAccounts.COLUMN_ID, properties.id);
            contentValues.put(BlockpayDatabaseContract.UserAccounts.COLUMN_NAME, properties.name);

            try {
                db.insertOrThrow(BlockpayDatabaseContract.UserAccounts.TABLE_NAME, null, contentValues);
                count++;
            } catch (SQLException e) {

            }
        }
        return count;
    }

    /**
     * Making a query to fetch all unknown account names. That would be missing entries in the
     * user_accounts table.
     *
     * @return: List of all accounts with missing names.
     */
    public List<UserAccount> getMissingAccountNames() {
        String sql = "SELECT DISTINCT %s FROM %s WHERE %s NOT IN (SELECT %s FROM %s)";

        String firstReplacedSql = String.format(sql,
                BlockpayDatabaseContract.Transfers.COLUMN_TO,
                BlockpayDatabaseContract.Transfers.TABLE_NAME,
                BlockpayDatabaseContract.Transfers.COLUMN_TO,
                BlockpayDatabaseContract.UserAccounts.COLUMN_ID,
                BlockpayDatabaseContract.UserAccounts.TABLE_NAME);

        String secondReplacedSql = String.format(sql,
                BlockpayDatabaseContract.Transfers.COLUMN_FROM,
                BlockpayDatabaseContract.Transfers.TABLE_NAME,
                BlockpayDatabaseContract.Transfers.COLUMN_FROM,
                BlockpayDatabaseContract.UserAccounts.COLUMN_ID,
                BlockpayDatabaseContract.UserAccounts.TABLE_NAME);


        String[] firstSelectionArgs = {
                BlockpayDatabaseContract.Transfers.COLUMN_FROM,
                BlockpayDatabaseContract.Transfers.COLUMN_FROM
        };

        String[] secondSelectionArgs = {
                BlockpayDatabaseContract.Transfers.COLUMN_TO,
                BlockpayDatabaseContract.Transfers.COLUMN_TO
        };

        Cursor firstCursor = db.rawQuery(firstReplacedSql, null);
        Cursor secondCursor = db.rawQuery(secondReplacedSql, null);

        ArrayList<UserAccount> accounts = new ArrayList<>();
        if (firstCursor.moveToFirst()) {
            do {
                accounts.add(new UserAccount(firstCursor.getString(0)));
            } while (firstCursor.moveToNext());
        }

        if (secondCursor.moveToFirst()) {
            do {
                accounts.add(new UserAccount(secondCursor.getString(0)));
            } while (secondCursor.moveToNext());
        }

        firstCursor.close();
        secondCursor.close();
        return accounts;
    }

    /**
     * Given an incomplete instance of the UserAccount object, this method performs a query and
     * fills in the missing details.
     * <p>
     * The incomplete object passed as argument must have at least its object if set.
     *
     * @param account: The incomplete UserAccount instance
     * @return: The same UserAccount instance, but with all the fields with valid data.
     */
    public UserAccount fillUserDetails(UserAccount account) {
        String table = BlockpayDatabaseContract.UserAccounts.TABLE_NAME;
        String selection = BlockpayDatabaseContract.UserAccounts.COLUMN_ID + "=?";
        String[] selectionArgs = new String[]{account.getObjectId()};
        Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null, null);
        if (cursor.moveToFirst()) {
            String accountName = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.UserAccounts.COLUMN_NAME));
            account.setName(accountName);
        }
        cursor.close();
        return account;
    }

    /**
     * @return: A HashMap connecting account ids to account names.
     */
    public HashMap<String, String> getUserMap() {
        HashMap<String, String> userMap = new HashMap<>();
        String[] columns = {BlockpayDatabaseContract.UserAccounts.COLUMN_ID, BlockpayDatabaseContract.UserAccounts.COLUMN_NAME};
        Cursor cursor = db.query(true, BlockpayDatabaseContract.UserAccounts.TABLE_NAME, columns, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                userMap.put(cursor.getString(0), cursor.getString(1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return userMap;
    }

    // ################## Transactions ##################

    /**
     * Stores a list of historical transfer transactions as obtained from
     * the full node into the database
     *
     * @param transactions: List of historical transfer transactions.
     */
    public int putTransactions(List<HistoricalTransferEntry> transactions) {
        int count = 0;
        ContentValues contentValues;
        for (int i = 0; i < transactions.size(); i++) {
            contentValues = new ContentValues();
            HistoricalTransferEntry transferEntry = transactions.get(i);
            HistoricalTransfer historicalTransfer = transferEntry.getHistoricalTransfer();
            TransferOperation operation = historicalTransfer.getOperation();
            if (operation == null) continue;

            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_ID, historicalTransfer.getId());
            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP, transferEntry.getTimestamp());
            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_FEE_AMOUNT, operation.getFee().getAmount().longValue());
            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_FEE_ASSET_ID, operation.getFee().getAsset().getObjectId());
            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_FROM, operation.getFrom().getObjectId());
            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_TO, operation.getTo().getObjectId());
            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_AMOUNT, operation.getAssetAmount().getAmount().longValue());
            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_ASSET_ID, operation.getAssetAmount().getAsset().getObjectId());
            contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_BLOCK_NUM, historicalTransfer.getBlockNum());

            if (transferEntry.getEquivalentValue() != null) {
                AssetAmount assetAmount = transferEntry.getEquivalentValue();
                contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE_ASSET_ID, assetAmount.getAsset().getObjectId());
                contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE, assetAmount.getAmount().longValue());
            }

            Memo memo = operation.getMemo();
            if (!memo.getPlaintextMessage().equals("")) {
                contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_MEMO_FROM, memo.getSource().toString());
                contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_MEMO_TO, memo.getSource().toString());
                contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_MEMO_MESSAGE, memo.getPlaintextMessage());
            }
            try {
                db.insertOrThrow(BlockpayDatabaseContract.Transfers.TABLE_NAME, null, contentValues);
                count++;
            } catch (SQLException e) {
                //Ignoring exception, usually throwed becase the UNIQUE constraint failed.
            }
        }
        Log.d(TAG, String.format("Inserted %d transactions in database", count));
        return count;
    }

    /**
     * Updates the equivalent value for a specific historical transfer entry
     * @param transfer: The historical transfer whose equivalent value we want to update, with the correct amount.
     * @return
     */
    public int updateEquivalentValue(HistoricalTransferEntry transfer) {
        String table = BlockpayDatabaseContract.Transfers.TABLE_NAME;
        String whereClause = BlockpayDatabaseContract.Transfers.COLUMN_ID + "=?";
        String[] whereArgs = new String[]{transfer.getHistoricalTransfer().getId()};

        ContentValues contentValues = new ContentValues();
        Log.d(TAG, String.format("Updating eq value. asset id: %s, amount: %d", transfer.getEquivalentValue().getAsset().getObjectId(), transfer.getEquivalentValue().getAmount().longValue()));
        contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE, transfer.getEquivalentValue().getAmount().longValue());
        contentValues.put(BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE_ASSET_ID, transfer.getEquivalentValue().getAsset().getObjectId());

        int updated = db.update(table, contentValues, whereClause, whereArgs);
        return updated;
    }

    /**
     * Retrieves the list of historical transfers.
     *
     * @param userAccount: The user account whose transactions we're interested in.
     * @param max:         The maximum number of transactions to fetch, if the value is <= 0, then the
     *                     query will put no limits on the number of returned values.
     * @return: The list of historical transfer transactions.
     */
    public List<HistoricalTransferEntry> getTransactions(UserAccount userAccount, long start, long end, int max) {
        HashMap<String, String> userMap = this.getUserMap();
        HashMap<String, Asset> assetMap = this.getAssetMap();

        String tableName = BlockpayDatabaseContract.Transfers.TABLE_NAME;
        String orderBy = BlockpayDatabaseContract.Transfers.COLUMN_BLOCK_NUM + " DESC";
        String selection = "( " + BlockpayDatabaseContract.Transfers.COLUMN_FROM + " = ? OR " + BlockpayDatabaseContract.Transfers.COLUMN_TO + " = ? ) AND ( " +
                BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP + " > ? AND "+BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP + " < ?)";
        String[] selectionArgs = {
                userAccount.getObjectId(),
                userAccount.getObjectId(),
                String.format("%d", start),
                String.format("%d", end)
        };
        String limit = max > 0 ? String.format("%d", max) : null;
        Cursor cursor = db.query(tableName, null, selection, selectionArgs, null, null, orderBy, limit);
        ArrayList<HistoricalTransferEntry> transfers = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                HistoricalTransferEntry transferEntry = new HistoricalTransferEntry();
                HistoricalTransfer historicalTransfer = new HistoricalTransfer();

                // Getting origin and destination user account ids
                String fromId = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_FROM));
                String toId = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_TO));

                // Skipping transfer if we are missing users information
//                if (userMap.get(fromId) == null || userMap.get(toId) == null) {
//                    cursor.moveToNext();
//                    continue;
//                }

                // Skipping transfer if we are missing timestamp information
//                long t = cursor.getLong(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP));
//                if (t == 0) {
//                    cursor.moveToNext();
//                    continue;
//                }

                // Building UserAccount instances
                UserAccount from = new UserAccount(fromId, userMap.get(fromId));
                UserAccount to = new UserAccount(toId, userMap.get(toId));

                String transferAssetId = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_ASSET_ID));
                String feeAssetId = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_FEE_ASSET_ID));

                // Transfer and fee assets
                Asset transferAsset = assetMap.get(transferAssetId);
                Asset feeAsset = assetMap.get(feeAssetId);

                // Skipping transfer if we are missing transfer and fee asset information
//                if (transferAsset == null || feeAsset == null) {
//                    cursor.moveToNext();
//                    continue;
//                }

                // Transfer and fee amounts
                AssetAmount transferAmount = new AssetAmount(UnsignedLong.valueOf(cursor.getLong(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_AMOUNT))), transferAsset);
                AssetAmount feeAmount = new AssetAmount(UnsignedLong.valueOf(cursor.getLong(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_FEE_AMOUNT))), feeAsset);

                // Building a TransferOperation
                TransferOperation transferOperation = new TransferOperation(from, to, transferAmount, feeAmount);

                // Building memo data
                String memoMessage = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_MEMO_MESSAGE));
                Memo memo = new Memo();
                memo.setPlaintextMessage(memoMessage);
                transferOperation.setMemo(memo);

                // Adding other historical transfer data
                historicalTransfer.setId(cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_ID)));
                historicalTransfer.setOperation(transferOperation);
                historicalTransfer.setBlockNum(cursor.getInt(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_BLOCK_NUM)));

                // Adding the HistoricalTransfer instance
                transferEntry.setHistoricalTransfer(historicalTransfer);

                // Setting the timestamp
                transferEntry.setTimestamp(cursor.getLong(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP)));

                // Adding equivalent value data
                String id = cursor.getString(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE_ASSET_ID));
                long equivalentValue = cursor.getLong(cursor.getColumnIndex(BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE));
                if (id != null) {
                    String table = BlockpayDatabaseContract.Assets.TABLE_NAME;
                    String[] columns = new String[]{
                            BlockpayDatabaseContract.Assets.COLUMN_SYMBOL,
                            BlockpayDatabaseContract.Assets.COLUMN_PRECISION
                    };
                    String where = BlockpayDatabaseContract.Assets.COLUMN_ID + "=?";
                    String[] whereArgs = new String[]{id};
                    Cursor assetCursor = db.query(true, table, columns, where, whereArgs, null, null, null, null);
                    if (assetCursor.moveToFirst()) {
                        String symbol = assetCursor.getString(0);
                        int precision = assetCursor.getInt(1);
                        AssetAmount eqValueAssetAmount = new AssetAmount(UnsignedLong.valueOf(equivalentValue), new Asset(id, symbol, precision));
                        transferEntry.setEquivalentValue(eqValueAssetAmount);
                    } else {
                        Log.w(TAG, "Got empty cursor while trying to fill asset data");
                    }
                    assetCursor.close();
                } else {
                    //cursor.moveToNext();
                    //continue;
                    Date date = new Date(transferEntry.getTimestamp() * 1000);
                    Log.w(TAG,String.format("Got no eq value for transaction at %s", date.toString()));
                }

                // Adding historical transfer entry to array
                transfers.add(transferEntry);
            } while (cursor.moveToNext());
        } else {
            Log.w(TAG, "No historical transactions");
        }
        cursor.close();
        return transfers;
    }

    /**
     * Returns all missing asset references from the transfers table.
     *
     * @return: List of Asset instances.
     */
    public List<Asset> getMissingAssets() {
        String sql = "SELECT DISTINCT %s from %s where %s not in (select %s from %s)";
        String finalSql = String.format(sql,
                BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_ASSET_ID,
                BlockpayDatabaseContract.Transfers.TABLE_NAME,
                BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_ASSET_ID,
                BlockpayDatabaseContract.Assets.COLUMN_ID,
                BlockpayDatabaseContract.Assets.TABLE_NAME);

        ArrayList<Asset> missingAssets = new ArrayList<>();
        Cursor cursor = db.rawQuery(finalSql, null);
        if (cursor.moveToFirst()) {
            do {
                missingAssets.add(new Asset(cursor.getString(0)));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return missingAssets;
    }

    /**
     * This method is used to obtain the list of transfer operations stored in the database
     * with the date and time information missing.
     *
     * @return: A list of block numbers.
     */
    public LinkedList<Long> getMissingTransferTimes(int limitValue) {
        LinkedList<Long> missingTimes = new LinkedList<>();
        String table = BlockpayDatabaseContract.Transfers.TABLE_NAME;
        String[] columns = {BlockpayDatabaseContract.Transfers.COLUMN_BLOCK_NUM};
        String selection = BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP + "= ?";
        String[] selectionArgs = {"0"};
        String limit = String.format("%d", limitValue);
        String orderBy = BlockpayDatabaseContract.Transfers.COLUMN_BLOCK_NUM + " DESC";
        Cursor cursor = db.query(table, columns, selection, selectionArgs, null, null, orderBy, limit);
        if (cursor.moveToFirst()) {
            do {
                missingTimes.add(new Long(cursor.getLong(0)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        Log.d(TAG, String.format("Got %d missing times", missingTimes.size()));
        return missingTimes;
    }

    /**
     * Sets a missing block time information.
     *
     * @param blockHeader: The block header data of this transaction.
     * @param blockNum:    The block number, which is not included in the block header
     *                     and has to be passed separately.
     * @return: True if there was one transfer entry being updated, false otherwise.
     */
    public boolean setBlockTime(BlockHeader blockHeader, long blockNum) {
        boolean updated = false;
        ContentValues values = new ContentValues();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            Date date = dateFormat.parse(blockHeader.timestamp);
            values.put(BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP, date.getTime() / 1000);

            String table = BlockpayDatabaseContract.Transfers.TABLE_NAME;
            String whereClause = BlockpayDatabaseContract.Transfers.COLUMN_BLOCK_NUM + "=?";
            String[] whereArgs = {String.format("%d", blockNum)};
            int count = db.update(table, values, whereClause, whereArgs);
            if (count > 0) {
                updated = true;
            } else {
                Log.w(TAG, String.format("Failed to update block time. block: %d", blockNum));
            }
        } catch (ParseException e) {
            Log.e(TAG, "ParseException. Msg: " + e.getMessage());
        }
        return updated;
    }

    /**
     * Method used to obtain a list of all historical transfers for which we don't have
     * an equivalent value.
     * <p>
     * Every HistoricalTransferEntry object in the list returned is only
     * partially built, with just enough information to fill the 2 missing equivalent values
     * columns, which are the equivalent value asset id, and asset amount.
     *
     * @return: List of all historical transfers lacking an equivalent value.
     */
    public LinkedList<HistoricalTransferEntry> getMissingEquivalentValues() {
        LinkedList<HistoricalTransferEntry> historicalEntries = new LinkedList<>();
        String table = BlockpayDatabaseContract.Transfers.TABLE_NAME;
        String[] columns = {
                BlockpayDatabaseContract.Transfers.COLUMN_ID,
                BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP,
                BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_ASSET_ID,
                BlockpayDatabaseContract.Transfers.COLUMN_TRANSFER_AMOUNT,
                BlockpayDatabaseContract.Transfers.COLUMN_FROM,
                BlockpayDatabaseContract.Transfers.COLUMN_TO
        };
        String selection = BlockpayDatabaseContract.Transfers.COLUMN_EQUIVALENT_VALUE_ASSET_ID + " is null and " +
                BlockpayDatabaseContract.Transfers.COLUMN_TIMESTAMP + " != 0";
        Log.i(TAG, "Selection: " + selection);
        Cursor cursor = db.query(table, columns, selection, null, null, null, null, null);
        Log.i(TAG, String.format("Got cursor with %d entries", cursor.getCount()));
        if (cursor.moveToFirst()) {
            do {
                String historicalTransferId = cursor.getString(0);
                long timestamp = cursor.getLong(1);
                String assetId = cursor.getString(2);
                long amount = cursor.getLong(3);
                String sourceId = cursor.getString(4);
                String destinationId = cursor.getString(5);

                UserAccount from = new UserAccount(sourceId);
                UserAccount to = new UserAccount(destinationId);
                AssetAmount transferAmount = new AssetAmount(UnsignedLong.valueOf(amount), new Asset(assetId));

                TransferOperation operation = new TransferOperation(from, to, transferAmount);

                HistoricalTransfer transfer = new HistoricalTransfer();
                transfer.setId(historicalTransferId);
                transfer.setOperation(operation);

                HistoricalTransferEntry transferEntry = new HistoricalTransferEntry();
                transferEntry.setHistoricalTransfer(transfer);
                transferEntry.setTimestamp(timestamp);

                historicalEntries.add(transferEntry);
            } while (cursor.moveToNext());
            cursor.close();
            Log.i(TAG, String.format("Got %d transactions with missing equivalent value", historicalEntries.size()));
        }
        return historicalEntries;
    }

    /**
     * Method used to clear the transfers table
     */
    public void clearTransfers() {
        db.execSQL("delete from " + BlockpayDatabaseContract.Transfers.TABLE_NAME);
    }
}
