package de.bitsharesmunich.models;

/**
 * Model used to represent the JSON-formatted response obtained from the
 * <a href="https://coinmarketcap.com/">Coinmarketcap</a> data.
 *
 * The API responds with an array of items. So this object models each of
 * the items in that array.
 */
public class CoinMarketCapItemResponse {
    public String id;
    public String name;
    public String symbol;
    public String rank;
    public String market_cap_usd;
}
