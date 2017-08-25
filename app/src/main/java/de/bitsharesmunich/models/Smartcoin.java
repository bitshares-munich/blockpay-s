package de.bitsharesmunich.models;

public class Smartcoin {
    public Integer id;
    public String asset_id;
    public String symbol;
    public String precision;
    public String description;

    public Smartcoin(){}

    public Smartcoin(String assetId, String symbol, String precision, String description){
        this.asset_id = assetId;
        this.symbol = symbol;
        this.precision = precision;
        this.description = description;
    }
}