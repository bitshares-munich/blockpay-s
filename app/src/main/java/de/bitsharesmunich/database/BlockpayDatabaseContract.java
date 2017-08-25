package de.bitsharesmunich.database;

import android.provider.BaseColumns;

/**
 * Database contract class. Here we define table and column names as constants
 * grouped in their own public static classes.
 *
 * Created by nelson on 1/5/17.
 */

public class BlockpayDatabaseContract {

    public static class BaseTable implements BaseColumns {
        public static final String COLUMN_CREATION_DATE = "creation_date";
    }

    public static class Assets implements BaseColumns {
        public static final String TABLE_NAME = "assets";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_SYMBOL = "symbol";
        public static final String COLUMN_PRECISION = "precision";
        public static final String COLUMN_ISSUER = "issuer";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_MAX_SUPPLY = "max_supply";
        public static final String COLUMN_MARKET_FEE_PERCENT = "market_fee_percent";
        public static final String COLUMN_MAX_MARKET_FEE = "max_market_fee";
        public static final String COLUMN_ISSUER_PERMISSIONS = "issuer_permissions";
        public static final String COLUMN_FLAGS = "flags";
        public static final String COLUMN_ASSET_TYPE = "asset_type";
        public static final String COLUMN_BITASSET_ID = "bitasset_id";
        public static final String COLUMN_ASSET_HOLDERS_COUNT = "holders_count";
    }

    public static class Balances implements BaseColumns {
        public static final String TABLE_NAME = "balances";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_ASSET_ID = "asset_id";
        public static final String COLUMN_ASSET_AMOUNT = "asset_amount";
        public static final String COLUMN_LAST_UPDATE = "last_update";
    }

    public static class BridgeRates implements BaseColumns {
        public static final String TABLE_NAME = "bridge_rates";
        public static final String COLUMN_INPUT_ASSET = "input_coin_type";
        public static final String COLUMN_OUTPUT_ASSET = "output_coin_type";
        public static final String COLUMN_FEE_RATE = "fee_rate";
    }

    public static class UserAccounts extends BaseTable {
        public static final String TABLE_NAME = "user_accounts";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_NAME = "name";
    }

    public static class Transfers extends BaseTable {
        public static final String TABLE_NAME = "transfers";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_FEE_AMOUNT = "fee_amount";
        public static final String COLUMN_FEE_ASSET_ID = "fee_asset_id";
        public static final String COLUMN_FROM = "source";
        public static final String COLUMN_TO = "destination";
        public static final String COLUMN_TRANSFER_AMOUNT = "transfer_amount";
        public static final String COLUMN_TRANSFER_ASSET_ID = "transfer_asset_id";
        public static final String COLUMN_MEMO_MESSAGE = "memo";
        public static final String COLUMN_MEMO_FROM = "memo_from_key";
        public static final String COLUMN_MEMO_TO = "memo_to_key";
        public static final String COLUMN_BLOCK_NUM = "block_num";
        public static final String COLUMN_EQUIVALENT_VALUE_ASSET_ID = "equivalent_value_asset_id";
        public static final String COLUMN_EQUIVALENT_VALUE = "equivalent_value";
    }

    public static class CryptoCurrencies {
        public static final String TABLE_NAME = "crypto_currencies";
    }
}
