package de.bitsharesmunich.adapters;

import com.google.common.primitives.UnsignedLong;

import java.util.Comparator;

import de.bitsharesmunich.database.HistoricalTransferEntry;

/**
 * Created by nelson on 12/14/16.
 */

public class TransferAmountComparator implements Comparator<HistoricalTransferEntry> {

    @Override
    public int compare(HistoricalTransferEntry lhs, HistoricalTransferEntry rhs) {
        UnsignedLong lhsAmount = lhs.getHistoricalTransfer().getOperation().getAssetAmount().getAmount();
        UnsignedLong rhsAmount = rhs.getHistoricalTransfer().getOperation().getAssetAmount().getAmount();
        return lhsAmount.compareTo(rhsAmount);
    }
}
