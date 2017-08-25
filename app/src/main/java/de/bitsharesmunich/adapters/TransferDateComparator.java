package de.bitsharesmunich.adapters;

import java.util.Comparator;

import de.bitsharesmunich.database.HistoricalTransferEntry;

/**
 * Created by nelson on 4/12/17.
 */
public class TransferDateComparator implements Comparator<HistoricalTransferEntry> {

    @Override
    public int compare(HistoricalTransferEntry lhs, HistoricalTransferEntry rhs) {
        return (int) (lhs.getTimestamp() - rhs.getTimestamp());
    }
}