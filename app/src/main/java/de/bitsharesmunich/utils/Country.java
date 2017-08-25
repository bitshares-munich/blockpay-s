package de.bitsharesmunich.utils;

/**
 * Created by nelson on 2/3/17.
 */

public class Country implements Comparable<Country> {
    private String countryName;
    private String countryCode;

    public Country(String code, String name){
        this.countryCode = code;
        this.countryName = name;
    }


    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override
    public int compareTo(Country another) {
        return this.countryName.compareTo(another.getCountryName());
    }
}
