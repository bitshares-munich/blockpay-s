package de.bitsharesmunich.models;

/**
 * Model class used to represent each entry of the list of cryptocurrencies returned by the capfeed API.
 *
 * @see <a href="http://capfeed.com/api">CapFeed API</a>
 *
 * Created by nelson on 1/9/17.
 */
public class CapfeedCryptoCurrency {

    /* Position in terms of market cap */
    public int position;

    /* Symbol and name of the coin */
    public String symbol;
    public String name;

    /* POSIX timestamt */
    public long time;

    /* USD and BTC price of this coin */
    public String usdPrice;
    public String btcPrice;

    /* Trading volume in the last 24 hours*/
    public String usdVolume;

    /* Market cap in US$ */
    public String mktcap;

    /* Currently available supply for this coin */
    public String supply;

    /* Percentage of change in the last 24 hours */
    public String change24;
}
