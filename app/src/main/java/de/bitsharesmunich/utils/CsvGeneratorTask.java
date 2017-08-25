package de.bitsharesmunich.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.bitsharesmunich.blockpos.R;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.database.HistoricalTransferEntry;
import de.bitsharesmunich.graphenej.Util;
import de.bitsharesmunich.graphenej.models.HistoricalTransfer;
import de.bitsharesmunich.interfaces.ExportTaskListener;

/**
 * Created by nelson on 4/13/17.
 */

public class CsvGeneratorTask extends AsyncTask<HistoricalTransferEntry, Float, String> {
    private final String TAG = this.getClass().getName();

    private Context mContext;
    private ExportTaskListener mListener;

    public CsvGeneratorTask(Context context, ExportTaskListener listener){
        mContext = context;
        mListener = listener;
    }

    @Override
    protected String doInBackground(HistoricalTransferEntry... historicalTransferEntries) {
        String result = "";
        List<HistoricalTransferEntry> historicalTransfers = new ArrayList<>(Arrays.asList(historicalTransferEntries));
        String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + mContext.getResources().getString(R.string.folder_name);
        File file = new File(extStorage, mContext.getResources().getString(R.string.exported_file_name) + ".csv");
        BlockpayDatabase db = new BlockpayDatabase(mContext);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:MM:ss");
        Date date = null;
        int counter = 0;
        try {
            file.createNewFile();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(file));
            String[] columnNames = mContext.getResources().getStringArray(R.array.csv_columns);
            csvWriter.writeNext(columnNames);
            for(HistoricalTransferEntry historicalTransferEntry : historicalTransfers){
                HistoricalTransfer historicalTransfer = historicalTransferEntry.getHistoricalTransfer();
                calendar.setTimeInMillis(historicalTransferEntry.getTimestamp() * 1000);
                date = calendar.getTime();

                String[] row = {
                        historicalTransfer.getId(),
                        String.format("%d", historicalTransfer.getBlockNum()),
                        db.fillUserDetails(historicalTransfer.getOperation().getFrom()).getName(),
                        db.fillUserDetails(historicalTransfer.getOperation().getTo()).getName(),
                        dateFormat.format(date),
                        timeFormat.format(date),
                        String.format("%.4f", Util.fromBase(historicalTransfer.getOperation().getAssetAmount())),
                        historicalTransfer.getOperation().getAssetAmount().getAsset().getSymbol(),
                        String.format("%.4f", Util.fromBase(historicalTransferEntry.getEquivalentValue())),
                        historicalTransferEntry.getEquivalentValue().getAsset().getSymbol(),
                        historicalTransfer.getOperation().getMemo().getPlaintextMessage()
                };
                csvWriter.writeNext(row);

                /* Updating progress */
                if (counter % 8 == 0) {
                    float ratio = ((float) counter) / ((float) historicalTransfers.size());
                    publishProgress(ratio);
                }
                counter++;
            }
            csvWriter.close();
            result = mContext.getResources().getString(R.string.csv_generated_msg) + file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG,"IOException while creating CSV file. Msg: "+e.getMessage());
            result = "Error while creating a CSV file. Msg: "+e.getMessage();
        } catch (NullPointerException e){
            Log.e(TAG,"NullPointerException while creating a CSV file. Msg: "+e.getMessage());
            result = "Error while creating a CSV file. Msg: "+e.getMessage();
        }
        db.close();
        return result;
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
