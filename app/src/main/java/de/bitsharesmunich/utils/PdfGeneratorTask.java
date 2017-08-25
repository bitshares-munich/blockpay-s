package de.bitsharesmunich.utils;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.bitsharesmunich.blockpos.PdfTable;
import de.bitsharesmunich.blockpos.R;
import de.bitsharesmunich.database.HistoricalTransferEntry;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.interfaces.ExportTaskListener;

/**
 * AsyncTask subclass used to move the PDF generation procedure to a background thread
 * and inform the UI of the progress.
 *
 * Created by nelson on 12/28/16.
 */
public class PdfGeneratorTask extends AsyncTask<HistoricalTransferEntry, Float, String> {
    private Context mContext;
    private UserAccount userAccount;
    private ExportTaskListener mListener;

    public PdfGeneratorTask(Context context, UserAccount user, ExportTaskListener listener){
        this.mContext = context;
        this.userAccount = user;
        this.mListener = listener;
    }

    @Override
    protected String doInBackground(HistoricalTransferEntry... historicalTransferEntries) {
        List<HistoricalTransferEntry> historicalTransfers = new ArrayList<>(Arrays.asList(historicalTransferEntries));
        PdfTable myTable = new PdfTable(mContext, mContext.getResources().getString(R.string.exported_file_name), mListener);
        return myTable.createTable(mContext, historicalTransfers, userAccount);
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        mListener.onUpdate(values[0]);
    }

    @Override
    protected void onPostExecute(String message) {
        mListener.onReady(message);
    }
}
