package de.bitsharesmunich.models;

/**
 * Class used to help in the deserialization procedure of the response to the blocktrades
 * 'trading-pairs' API call.
 *
 * This call will return an array of JSON-formatted objects that will be translated into
 * an instance of this class.
 *
 * Created by nelson on 1/17/17.
 */
public class BlocktradesTradingPair {
    private String inputCoinType;
    private String outputCoinType;
    private String rateFee;

    public String getInputCoinType() {
        return inputCoinType;
    }

    public void setInputCoinType(String inputCoinType) {
        this.inputCoinType = inputCoinType;
    }

    public String getOutputCoinType() {
        return outputCoinType;
    }

    public void setOutputCoinType(String outputCoinType) {
        this.outputCoinType = outputCoinType;
    }

    public double getRateFee() {
        return Double.valueOf(this.rateFee);
    }

    public void setRateFee(double rateFee) {
        this.rateFee = String.format("%.2f", rateFee);
    }
}
