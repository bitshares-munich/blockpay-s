package de.bitsharesmunich.utils;

import de.bitsharesmunich.graphenej.Asset;

/**
 * Created by nelson on 12/28/16.
 */
public class Constants {

    /**
     * URL of CapFeed service
     */
    public static final String CAPFEED_URL = "http://capfeed.com";

    /**
     * URL of the coin marketcap service
     */
    public static final String COINMARKETCAP_URL = "https://api.coinmarketcap.com";

    /**
     * Base URL used to communicate with the cryptofresh API.
     */
    public static final String CRYPTOFRESH_URL = "http://cryptofresh.com";

    /**
     * Base URL used to communicate with the Blocktrades API.
     */
    public static final String BLOCKTRADES_URL = "https://blocktrades.us";

    /**
     * Key used to store a flag value in the Bundle passed with the Intent that will open
     * up the TabActivity. This activity is the main entry point for this app, and since it
     * is an instance of LockableActivity, it will look for this flag in order to decide
     * wheter or not to display the pin dialog at the onStart life-cycle method.
     */
    public static final String KEY_ASK_FOR_PIN = "ask_for_pin";


    /**
     * Key used to determine whether or not to display the back arrow in the settings activity
     */
    public static final String KEY_DISPLAY_BACK = "key_display_back";

    /**
     * The minimum required length for a PIN number
     */
    public static final int MIN_PIN_LENGTH = 6;

    /**
     * Key used to store a preference value used to keep track of the last time the
     * asset holders data was updated.
     */
    public static final String KEY_LAST_ASSETS_HOLDERS_UPDATE = "key_last_asset_data_update";

    /**
     * Number of hours between 2 updates on the asset holders data.
     */
    public static final long ASSETS_HOLDERS_UPDATE_INTERVAL = 24;

    /**
     * Time in days between 2 updates on the assets data.
     */
    public static final long ASSETS_UPDATE_INTERVAL = 7;

    /**
     * The minimum number of existing assets
     */
    public static final long MIN_ASSET_COUNT = 1000;

    /**
     * Default currency symbol
     */
    public static final String DEFAULT_CURRENCY = "EUR";

    /**
     * Default country
     */
    public static final String DEFAULT_COUNTRY = "DE";

    /**
     * Key used to store the selected input fiat currency
     */
    public static final String KEY_CURRENCY = "key_currency";

    /**
     * Key used to store the selected language
     */
    public static final String KEY_LANGUAGE = "key_language";

    /**
     * Key used to store the selected country code
     */
    public static final String KEY_COUNTRY = "key_country";

    /**
     * Key for a boolean variable used to specify whether the database has already been loaded or not.
     * The database should only be empty once, right after the app installation.
     */
    public static final String KEY_DATABASE_LOADED = "database_loaded";

    /**
     * Key used to store a preference value used to keep track of the last time the bridge
     * fee rates were updated.
     */
    public static final String KEY_LAST_BRIDGE_RATES_UPDATE = "key_last_bridge_rates_update";

    /**
     * Key used to store a preference value used to keep track of the last time the assets in
     * database were updated.
     */
    public static final String KEY_LAST_ASSET_LIST_UPDATE = "key_last_assets_update";

    /**
     * Key used to store a preference that specifies the user-given N amount of loyalty
     * reward tokens to issue for every M amount of smartcoin received.
     */
    public static final String KEY_LOYALTY_REWARD_AMOUNT = "key_loyalty_reward_amount";

    /**
     * Key used to store a preference that specifies the amount M of smartcoins that must be
     * received in order to grant an amount of N loyalty reward tokens.
     */
    public static final String KEY_LOYALTY_SMARTCOIN_AMOUNT = "key_loyalty_payment_amount";

    /**
     * Key used to store a preference that specifies the UIA selected as a reward token
     */
    public static final String KEY_REWARD_ASSET_ID = "key_reward_asset_id";

    /**
     * Number of hours between two bridge fee rates updates.
     */
    public static final long BRIDGE_RATES_UPDATE_INTERVAL = 12;

    /**
     * Bitshares Munich fee value
     */
    public static final double BITSHARES_MUNICH_FEE = 0.005;

    /**
     * The tolerance in the
     */
    public static final double INPUT_TOLERANCE = 0.01;

    /**
     * The basic fee for a non-life-time member, in basic units of the core currency (BTS)
     */
    public static final int FIXED_TRANSFER_FEE =  264000;

    /**
     * Key used to store the expiration time of a blocktrades session token
     */
    public static final String KEY_BLOCKTRADES_SESSION_EXPIRATION = "key_blocktrades_session_expiration";

    /**
     * Key used to store the session token from a blocktrades session
     */
    public static final String KEY_BLOCKTRADES_SESSION_TOKEN = "key_blocktrades_session_token";

    /**
     * Extra network fee. This is currently being setup as a hardcoded value, but it should in fact be
     * calculated from the transaction size.
     *
     * This value was taken from the following calculation
     *
     * 1.47 * 100000 * txLen / 1024
     *
     * Where txLen was verified to be 194 bytes, and the rest was setup from
     * information taken from <a href="http://cryptofresh.com/fees>here</a>
     */
    public static final int EXTRA_NETWORK_FEE = 27849;

    /**
     * Basic fee for the creation of a limit order.
     */
    public static final int LIMIT_ORDER_CREATE_BASIC_FEE = 14700;

    /**
     * Fee charged for the creation of a limit order to lifetime members
     */
    public static final int LIMIT_ORDER_CREATE_LIFETIME_FEE = 2940;

    /**
     * Core asset id
     */
    public static final String CORE_ASSET_ID = "1.3.0";

    /**
     * Keys used to store the small extra amount that we'll be requesting to the bridge each time.
     */
    public static final String KEY_EXTRA_BTS = "key_extra_bts";
    public static final String KEY_EXTRA_BITUSD = "key_extra_bitusd";
    public static final String KEY_EXTRA_BITEUR = "key_extra_biteur";
    public static final String KEY_EXTRA_BITCNY = "key_extra_bitcny";
    public static final String KEY_EXTRA_BITBTC = "key_extra_bitbtc";

    /**
     * Key used to store the timestamp of the last update of the exchange rates between accepted smartcoin
     * output options
     */
    public static final String KEY_LAST_EXTRA_BRIDGE_AMOUNT_UPDATE = "key_last_extra_bridge_amount_update";

    /**
     * The exchange rate period in milliseconds.
     */
    public static final int EXCHANGE_RATES_UPDATE_PERIOD = 1000 * 60 * 60 * 24;

    /**
     * Smartcoin options for output
     */
    public static Asset bitUSD = new Asset("1.3.121");
    public static Asset BTS = new Asset("1.3.0");
    public static Asset bitEUR = new Asset("1.3.120");
    public static Asset bitCNY = new Asset("1.3.113");
    public static Asset bitBTC = new Asset("1.3.103");

    /**
     * Default bucket size in seconds.
     */
    public static final long DEFAULT_BUCKET_SIZE = 3600;

    /**
     * The current app's package
     */
    public static final String PACKAGE = "de.bitsharesmunich";

    /**
     * Alias used to store our encryption keys in the keystore
     */
    public static final String KEYSTORE_ALIAS = PACKAGE + ".keystore_alias";
}
