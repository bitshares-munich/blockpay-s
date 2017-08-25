package de.bitsharesmunich.models;

/**
 * Model used to deserialize the response of the initiate-trade blocktrades API call.
 *
 * Created by mbilal on 4/11/16.
 */
public class InitiateTradeResponse {
    public String inputAddress;
    public String inputMemo;
    public String inputCoinType;
    public String outputAddress;
    public String outputCoinType;
    public String refundAddress;
    public BlocktradesError error;
}
