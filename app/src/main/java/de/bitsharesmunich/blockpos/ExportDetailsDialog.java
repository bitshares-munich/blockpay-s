package de.bitsharesmunich.blockpos;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.bitsharesmunich.widgets.DatePickerFragment;

/**
 * Created by nelson on 4/13/17.
 */

public class ExportDetailsDialog extends DialogFragment {
    private final String TAG = this.getClass().getName();

    private TransactionListActivity.ExportDetailsHandler mHandler;
    private CheckBox selectAllDates;
    private CheckBox pdfCheckbox;
    private CheckBox csvCheckbox;
    private Button startDateButton;
    private Button endDateButton;
    private TextView startDateText;
    private TextView endDateText;
    private Button okButton;

    private boolean isCsv;
    private boolean isPdf;

    private DatePickerHandler mDatePickerHandler;

    /**
     * DatePicker message handler.
     */
    public class DatePickerHandler extends Handler {
        public static final int START_TIME = 0;
        public static final int END_TIME = 1;
        public static final String KEY_TIMESTAMP = "key_timestamp";
        private long startTime;
        private long endTime;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            long timestamp = (long) bundle.get(KEY_TIMESTAMP);
            Log.d(TAG,"timestamp: "+timestamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
            Date date;
            switch(msg.arg1){
                case START_TIME:
                    startTime = timestamp;
                    date = new Date(startTime);
                    startDateText.setText(dateFormat.format(date));
                    break;
                case END_TIME:
                    endTime = timestamp;
                    date = new Date(endTime);
                    endDateText.setText(dateFormat.format(date));
                    break;
            }
        }

        public long getTime(int which){
            Log.d(TAG,"getTime. which: "+which);
            switch(which){
                case START_TIME:
                    return startTime;
                case END_TIME:
                    return endTime;
            }
            return 0;
        }
    }

    public static ExportDetailsDialog newInstance(TransactionListActivity.ExportDetailsHandler handler){
        ExportDetailsDialog f = new ExportDetailsDialog();
        f.setHandler(handler);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(R.style.BlockpayDialog, android.R.style.Theme_Material_Light_Dialog);
        mDatePickerHandler = new DatePickerHandler();
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        getDialog().setTitle(R.string.export_dialog_title);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.export_details_dialog, container, false);
        selectAllDates = (CheckBox) view.findViewById(R.id.select_all_checkbox);
        selectAllDates.setOnCheckedChangeListener(mSelectAllDatesListener);

        csvCheckbox = (CheckBox) view.findViewById(R.id.format_csv);
        pdfCheckbox = (CheckBox) view.findViewById(R.id.format_pdf);

        csvCheckbox.setOnCheckedChangeListener(mFormatCheckboxListener);
        pdfCheckbox.setOnCheckedChangeListener(mFormatCheckboxListener);

        startDateButton = (Button) view.findViewById(R.id.startDate);
        endDateButton = (Button) view.findViewById(R.id.endDate);
        startDateButton.setOnClickListener(mStartDateClickListener);
        endDateButton.setOnClickListener(mStartDateClickListener);

        okButton = (Button) view.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Message message = mHandler.obtainMessage();
                message.arg1 = isPdf ? 1 : 0;
                message.arg2 = isCsv ? 1 : 0;
                Bundle bundle = new Bundle();
                bundle.putLong(TransactionListActivity.ExportDetailsHandler.KEY_START_TIME, mDatePickerHandler.getTime(DatePickerHandler.START_TIME) / 1000);
                bundle.putLong(TransactionListActivity.ExportDetailsHandler.KEY_END_TIME, mDatePickerHandler.getTime(DatePickerHandler.END_TIME) / 1000);
                message.setData(bundle);
                mHandler.sendMessage(message);
                dismiss();
            }
        });

        startDateText = (TextView) view.findViewById(R.id.start_date_text_view);
        endDateText = (TextView) view.findViewById(R.id.end_date_text_view);
        return view;
    }

    public TransactionListActivity.ExportDetailsHandler getHandler() {
        return mHandler;
    }

    public void setHandler(TransactionListActivity.ExportDetailsHandler mHandler) {
        this.mHandler = mHandler;
    }

    private View.OnClickListener mStartDateClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            int which = -1;
            if(v.getId() == R.id.startDate){
                which = DatePickerHandler.START_TIME;
            }else if(v.getId() == R.id.endDate){
                which = DatePickerHandler.END_TIME;
            }
            DialogFragment datePickerFragment = DatePickerFragment.newInstance(which, mDatePickerHandler);
            datePickerFragment.show(getActivity().getSupportFragmentManager(), "date-picker");
        }
    };

    /**
     * Change listener that will be notified of check/uncheck events and will disable/enable
     * the date selection buttons accordingly
     */
    private CompoundButton.OnCheckedChangeListener mSelectAllDatesListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                startDateButton.setEnabled(false);
                endDateButton.setEnabled(false);
            }else{
                startDateButton.setEnabled(true);
                endDateButton.setEnabled(true);
            }
        }
    };

    /**
     * Change listener that will be notified of check/uncheck events and will store the current status
     * in private fields.
     */
    private CompoundButton.OnCheckedChangeListener mFormatCheckboxListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(buttonView.getId() == R.id.format_csv){
                isCsv = isChecked;
            }else if(buttonView.getId() == R.id.format_pdf){
                isPdf = isChecked;
            }
        }
    };
}
