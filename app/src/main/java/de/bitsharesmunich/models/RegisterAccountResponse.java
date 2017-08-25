package de.bitsharesmunich.models;

/**
 * Created by afnan on 6/16/16.
 */
public class RegisterAccountResponse {
    public Account account;
    public Error error;

    public class Error {
        public String[] base;
    }
}
