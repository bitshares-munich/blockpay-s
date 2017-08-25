package de.bitsharesmunich.blockpos;

import de.bitsharesmunich.graphenej.Asset;

/**
 * Created by nelson on 1/10/17.
 */
public class ExchangeRateResult{
    public Asset base;
    public Asset quote;
    public double conversionRate;
    public String error;

    public ExchangeRateResult(Asset base, Asset quote, double conversionRate, String error){
        this.base = base;
        this.quote = quote;
        this.conversionRate = conversionRate;
        this.error = error;
    }

    public Asset getBase() {
        return base;
    }

    public void setBase(Asset base) {
        this.base = base;
    }

    public double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public Asset getQuote() {
        return quote;
    }

    public void setQuote(Asset quote) {
        this.quote = quote;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}