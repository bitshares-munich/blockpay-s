package de.bitsharesmunich.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.bitsharesmunich.blockpos.R;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.database.HistoricalTransferEntry;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.Util;
import de.bitsharesmunich.graphenej.operations.TransferOperation;
import de.bitsharesmunich.utils.Helper;
import de.codecrafters.tableview.TableDataAdapter;

/**
 * Created by nelson on 4/12/17.
 */
public class TransfersTableAdapter extends TableDataAdapter<HistoricalTransferEntry> {
    private String TAG = this.getClass().getName();

    private BlockpayDatabase database;
    private UserAccount userAccount;

    public TransfersTableAdapter(Context context, UserAccount userAccount, HistoricalTransferEntry[] data) {
        super(context, data);
        this.database = new BlockpayDatabase(context);
        this.userAccount = userAccount;
    }

    @Override
    public View getCellView(int rowIndex, int columnIndex, ViewGroup parentView) {
        View renderedView = null;
        HistoricalTransferEntry transferEntry = getRowData(rowIndex);
        switch (columnIndex) {
            //Datetime
            case 0:
                renderedView = renderDateView(transferEntry);
                break;
            //Image send/receive drawable
            case 1:
                renderedView = renderSendReceive(transferEntry);
                break;
            //Transfer details
            case 2:
                renderedView = renderDetails(transferEntry);
                break;
            //Amount
            case 3:
                renderedView = renderAmount(transferEntry);
                break;
        }
        return renderedView;
    }

    private View renderDateView(HistoricalTransferEntry historicalTransfer) {
        LayoutInflater layoutInflater = getLayoutInflater();
        View v = layoutInflater.inflate(R.layout.tx_date, null);
        TextView dateTextView = (TextView) v.findViewById(R.id.transactiondate);
        TextView timeTextView = (TextView) v.findViewById(R.id.transactiontime);
        TextView timeZoneTextView = (TextView) v.findViewById(R.id.transactionttimezone);

        if (historicalTransfer.getTimestamp() > 0) {

            Date date = new Date(historicalTransfer.getTimestamp() * 1000);

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm");
            SimpleDateFormat timeZone = new SimpleDateFormat("zzz");

            TimeZone tz = TimeZone.getTimeZone(timeZone.format(date));
            String formattedTimeZone = tz.getDisplayName(false, TimeZone.SHORT);

            // It was requested that we omit the last part of the time zone information
            // turning GMT-02:00 into GMT-2 for instance.
            // The following code does just that.
            Pattern pattern = Pattern.compile("(GMT[+-])(\\d)(\\d):(\\d\\d)");
            Matcher m = pattern.matcher(formattedTimeZone);
            if (m.matches()) {
                if (m.group(4).equals("00")) {
                    formattedTimeZone = m.group(1) + m.group(3);
                }
            }

            dateTextView.setText(dateFormat.format(date));
            timeTextView.setText(timeFormat.format(date));
            timeZoneTextView.setText(formattedTimeZone);
        }
        return v;
    }

    private View renderSendReceive(HistoricalTransferEntry historicalTransfer) {
        TransferOperation operation = historicalTransfer.getHistoricalTransfer().getOperation();
        LayoutInflater layoutInflater = getLayoutInflater();
        View v = layoutInflater.inflate(R.layout.tx_list_direction, null);
        ImageView imgView = (ImageView) v.findViewById(R.id.iv);
        if (operation.getFrom().getObjectId().equals(userAccount.getObjectId())) {
            imgView.setImageResource(R.drawable.send);
        } else {
            imgView.setImageResource(R.drawable.receive);
        }
        return v;
    }

    private View renderDetails(HistoricalTransferEntry historicalTransfer) {
        TransferOperation operation = historicalTransfer.getHistoricalTransfer().getOperation();
        LayoutInflater me = getLayoutInflater();
        View v = me.inflate(R.layout.tx_list_details, null);

        String toMessage = getContext().getText(R.string.to_capital) + ": " + operation.getTo().getName();
        TextView toUser = (TextView) v.findViewById(R.id.destination_account);
        toUser.setText(toMessage);
        Log.d(TAG,"toMessage: " + toMessage);

        String fromMessage = getContext().getText(R.string.from_capital) + ": " + operation.getFrom().getName();
        TextView fromUser = (TextView) v.findViewById(R.id.origin_account);
        fromUser.setText(fromMessage);
        Log.d(TAG,"fromMessage: " + fromMessage);

        Log.d(TAG,"memo: " + operation.getMemo().getPlaintextMessage());
        if (!operation.getMemo().getPlaintextMessage().equals("")) {
            TextView memoTextView = (TextView) v.findViewById(R.id.memo);
            memoTextView.setText(operation.getMemo().getPlaintextMessage());
        }
        return v;
    }

    private View renderAmount(HistoricalTransferEntry historicalTransfer) {
        TransferOperation operation = historicalTransfer.getHistoricalTransfer().getOperation();
        LayoutInflater me = getLayoutInflater();
        View root = me.inflate(R.layout.tx_list_amount, null);
        TextView transferAmountTextView = (TextView) root.findViewById(R.id.asset_amount);
        AssetAmount transferAmount = operation.getAssetAmount();

        TextView fiatAmountTextView = (TextView) root.findViewById(R.id.fiat_amount);
        AssetAmount smartcoinAmount = historicalTransfer.getEquivalentValue();

//        String language = Helper.fetchStringSharePref(getContext(), getContext().getString(R.string.pref_language), Constants.DEFAULT_LANGUAGE_CODE);
//        Locale locale = new Locale(language);
        Locale locale = getResources().getConfiguration().locale;
        String symbol = "";
        if (transferAmount.getAsset() != null) {
            symbol = transferAmount.getAsset().getSymbol();
        }
        int redColor = ContextCompat.getColor(getContext(), R.color.send_amount);
        int greenColor = ContextCompat.getColor(getContext(), R.color.receive_amount);
        int lightRed = ContextCompat.getColor(getContext(), R.color.send_amount_light);
        int lightGreen = ContextCompat.getColor(getContext(), R.color.receive_amount_light);

        if (operation.getFrom().getObjectId().equals(userAccount.getObjectId())) {
            // User sent this transfer
            transferAmountTextView.setTextColor(redColor);
            fiatAmountTextView.setTextColor(lightRed);
            String amount = Helper.setLocaleNumberFormat(locale, Util.fromBase(transferAmount));
            transferAmountTextView.setText(String.format("- %s %s", amount, symbol));
        } else {
            // User received this transfer
            transferAmountTextView.setTextColor(greenColor);
            fiatAmountTextView.setTextColor(lightGreen);
            String amount = Helper.setLocaleNumberFormat(locale, Util.fromBase(transferAmount));
            transferAmountTextView.setText(String.format("+ %s %s", amount, symbol));
        }

        if (smartcoinAmount != null) {
            Asset smartcoin = database.fillAssetDetails(smartcoinAmount.getAsset());
            String fiatSymbol = smartcoin.getSymbol();
            String eqValue = String.format("~ %s %.2f", fiatSymbol, Util.fromBase(smartcoinAmount));
            fiatAmountTextView.setText(eqValue);
        } else {
            Log.w(TAG, String.format("Fiat amount is null for transfer: %d %s", transferAmount.getAmount().longValue(), transferAmount.getAsset().getSymbol()));
        }
        return root;
    }

}