package de.bitsharesmunich.blockpos;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.adapters.TransferAmountComparator;
import de.bitsharesmunich.adapters.TransferDateComparator;
import de.bitsharesmunich.adapters.TransferSendReceiveComparator;
import de.bitsharesmunich.adapters.TransfersTableAdapter;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.database.HistoricalTransferEntry;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.interfaces.ExportTaskListener;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.utils.CsvGeneratorTask;
import de.bitsharesmunich.utils.PdfGeneratorTask;
import de.bitsharesmunich.utils.TinyDB;
import de.codecrafters.tableview.SortableTableView;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;
import de.codecrafters.tableview.toolkit.SortStateViewProviders;

public class TransactionListActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getName();

    /* Permission flag */
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    /* The number of transactions to display in a single batch */
    private final int MAX_TRANSACTION_BATCH = 50;

    @BindView(R.id.tableView)
    SortableTableView transfersView;

    /* Dialog with a exportProgress bar used to display exportProgress while generating a new PDF file */
    private ProgressDialog exportProgress;

    private UserAccount mCurrentAccount;
    private BlockpayDatabase database;

    private ExportDetailsHandler mHandler;

    private boolean isPdfRequested;
    private boolean isCsvRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);
        ButterKnife.bind(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mHandler = new ExportDetailsHandler();

        TinyDB tinyDB = new TinyDB(this);
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
        for (int i = 0; i < accountDetails.size(); i++) {
            if (accountDetails.get(i).isSelected) {
                mCurrentAccount = new UserAccount(accountDetails.get(i).account_id);
                break;
            }
        }

        database = new BlockpayDatabase(this);
        List<HistoricalTransferEntry> tranferEntries = database.getTransactions(mCurrentAccount, 0, Long.MAX_VALUE, BlockpayDatabase.ALL_TRANSACTIONS);
        HistoricalTransferEntry[] transferEntriesArray = tranferEntries.toArray(new HistoricalTransferEntry[tranferEntries.size()]);
        TransfersTableAdapter adapter = new TransfersTableAdapter(this, mCurrentAccount, transferEntriesArray);
        transfersView.setDataAdapter(adapter);
        adapter.notifyDataSetChanged();

        if (transfersView.getColumnComparator(0) == null) {
            updateSortTable();
        }
    }

    /**
     * Updating the sort strategy
     */
    private void updateSortTable() {
        SimpleTableHeaderAdapter simpleTableHeaderAdapter = new SimpleTableHeaderAdapter(this,
                getString(R.string.date),
                getString(R.string.all),
                getString(R.string.to_from),
                getString(R.string.amount));
        simpleTableHeaderAdapter.setPaddingLeft(getResources().getDimensionPixelSize(R.dimen.transactionsheaderpading));
        transfersView.setHeaderAdapter(simpleTableHeaderAdapter);

        transfersView.setHeaderSortStateViewProvider(SortStateViewProviders.darkArrows());
        transfersView.setColumnWeight(0, 20);
        transfersView.setColumnWeight(1, 12);
        transfersView.setColumnWeight(2, 27);
        transfersView.setColumnWeight(3, 22);
        transfersView.setColumnComparator(0, new TransferDateComparator());
        transfersView.setColumnComparator(1, new TransferSendReceiveComparator(mCurrentAccount));
        transfersView.setColumnComparator(3, new TransferAmountComparator());
    }

    @OnClick(R.id.btnExport)
    public void onExportClick(){
        ExportDetailsDialog dialog = ExportDetailsDialog.newInstance(mHandler);
        dialog.show(getSupportFragmentManager(), "export-details-tag");
    }

    /**
     * Listener that will be notified of events related to the CSV generation process
     */
    private ExportTaskListener csvListener = new ExportTaskListener() {
        @Override
        public void onUpdate(float percentage) {
            if (exportProgress != null) {
                int progress = (int) (percentage * 100);
                if(!isPdfRequested){
                    exportProgress.setProgress(progress);
                }
            }
        }

        @Override
        public void onReady(String message) {
            if (exportProgress != null && exportProgress.isShowing() && !isPdfRequested) {
                exportProgress.dismiss();
            }
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(String message) {
            Toast.makeText(getApplicationContext(), getText(R.string.csv_generated_msg_error) + message, Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Listener that will be notified of events related to the PDF generation process
     */
    private ExportTaskListener pdfListener = new ExportTaskListener() {

        @Override
        public void onUpdate(float percentage) {
            if (exportProgress != null) {
                int progress = (int) (percentage * 100);
                exportProgress.setProgress(progress);
            }
        }

        @Override
        public void onReady(String message) {
            if (exportProgress != null && exportProgress.isShowing()) {
                exportProgress.dismiss();
            }
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(String message) {
            Toast.makeText(getApplicationContext(), getText(R.string.pdf_generated_msg_error) + message, Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Method used to actually create the exported data
     * @param createPdf: Boolean variable used to specify whether to produce a PDF file
     * @param createCsv: Boolean variable used to specify whether to produce a CSV file
     * @param start:
     * @param end
     */
    private void processExportRequest(boolean createPdf, boolean createCsv, long start, long end){
        if(!createCsv && !createPdf) return;
        List<HistoricalTransferEntry> transfers = null;
        if(start != 0 && end != 0 && start < end){
            transfers = database.getTransactions(mCurrentAccount, start, end, Integer.MAX_VALUE);
        }else{
            transfers = database.getTransactions(mCurrentAccount, 0, Long.MAX_VALUE, Integer.MAX_VALUE);
        }
        isPdfRequested = createPdf;
        isCsvRequested = createCsv;
        if(isCsvRequested){
            CsvGeneratorTask csvGeneratorTask = new CsvGeneratorTask(this, csvListener);
            csvGeneratorTask.execute(transfers.toArray(new HistoricalTransferEntry[transfers.size()]));
        }
        if(isPdfRequested){
            PdfGeneratorTask pdfGeneratorTask = new PdfGeneratorTask(this, mCurrentAccount, pdfListener);
            pdfGeneratorTask.execute(transfers.toArray(new HistoricalTransferEntry[transfers.size()]));
        }

        if (exportProgress == null) {
            exportProgress = new ProgressDialog(this);
        }
        exportProgress.setMessage(getResources().getString(R.string.progress_export_generation));
        exportProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        exportProgress.setIndeterminate(false);
        exportProgress.setMax(100);
        exportProgress.setProgress(0);
        exportProgress.show();
    }

    /**
     * Custom handler used to receive the specific export details that the used has selected.
     */
    public class ExportDetailsHandler extends Handler {

        public static final String KEY_START_TIME = "key_start_time";
        public static final String KEY_END_TIME = "key_end_time";

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            processExportRequest(msg.arg1 == 1, msg.arg2 == 1, bundle.getLong(KEY_START_TIME), bundle.getLong(KEY_END_TIME));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}