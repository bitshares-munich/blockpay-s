package de.bitsharesmunich.interfaces;

import de.bitsharesmunich.blockpos.ExchangeRateResult;

/**
 * Interface to be implemented by any entity that wants to be notified of the result
 * Created by nelson on 1/10/17.
 */
public interface ExchangeRateListener {
    void onExchangeRate(ExchangeRateResult exchangeRate);
}
