package de.bitsharesmunich.widgets;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.GregorianCalendar;

import de.bitsharesmunich.blockpos.ExportDetailsDialog;

/**
 * Created by nelson on 4/13/17.
 */

public class DatePickerFragment extends DialogFragment implements
        DatePickerDialog.OnDateSetListener {
    public final String TAG = this.getClass().getName();
    public static final String KEY_WHICH = "key_which";

    private int which;
    private ExportDetailsDialog.DatePickerHandler mHandler;

    public static DatePickerFragment newInstance(int which, ExportDetailsDialog.DatePickerHandler handler){
        DatePickerFragment f = new DatePickerFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_WHICH, which);
        f.setArguments(bundle);
        f.setHandler(handler);
        return f;
    }

    public void setHandler(ExportDetailsDialog.DatePickerHandler handler){
        mHandler = handler;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        which = getArguments().getInt(KEY_WHICH);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Message msg = Message.obtain();
        msg.arg1 = which;
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(year, month, day);
        Bundle bundle = new Bundle();
        bundle.putLong(ExportDetailsDialog.DatePickerHandler.KEY_TIMESTAMP, calendar.getTime().getTime());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
}