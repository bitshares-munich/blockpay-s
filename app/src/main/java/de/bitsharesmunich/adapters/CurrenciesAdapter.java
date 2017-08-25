package de.bitsharesmunich.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import de.bitsharesmunich.blockpos.R;
import de.bitsharesmunich.error.IllegalResourceState;

/**
 * Created by nelson on 2/7/17.
 */
public class CurrenciesAdapter implements SpinnerAdapter {
    private String TAG = this.getClass().getName();
    private Context mContext;
    private String[] currencies;
    private String[] currencyNames;
    private int[] countryFlags = new int[]{
            R.drawable.us,
            R.drawable.eu,
            R.drawable.cn,
            R.drawable.gb,
            R.drawable.ar,
            R.drawable.ca,
            R.drawable.ch,
            R.drawable.kr,
            R.drawable.jp,
            R.drawable.hk,
            R.drawable.sg,
            R.drawable.au,
            R.drawable.ru,
            R.drawable.se
    };

    public CurrenciesAdapter(Context context){
        mContext = context;
        currencyNames = mContext.getResources().getStringArray(R.array.currencies_names);
        currencies = mContext.getResources().getStringArray(R.array.currencies);
        Log.d(TAG, "currency names: "+currencyNames.length+", currencies: "+currencies.length);
        if(currencyNames.length != currencies.length){
            throw new IllegalResourceState("Currency names array and symbols must have the same length");
        }
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.currency_item, parent, false);
        }
        TextView currencyName = (TextView) convertView.findViewById(R.id.currencyName);
        ImageView countryFlag = (ImageView) convertView.findViewById(R.id.countryFlag);

        String template = "%s (%s)";
        currencyName.setText(String.format(template, currencyNames[position], currencies[position]));

        countryFlag.setImageResource(countryFlags[position]);
        return convertView;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return currencies.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView = (TextView) View.inflate(mContext, android.R.layout.simple_spinner_item, null);
        textView.setText(currencyNames[position]);
        return textView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
