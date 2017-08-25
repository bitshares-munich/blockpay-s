package de.bitsharesmunich.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.List;

import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Country;

/**
 * Created by nelson on 2/3/17.
 */

public class CountryAdapter implements SpinnerAdapter {
    private final String TAG = this.getClass().getName();
    private Context mContext;
    private List<Country> mCountryList;

    public CountryAdapter(Context context, List<Country> countryList){
        this.mContext = context;
        this.mCountryList = countryList;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        }
        ((TextView) convertView).setText(mCountryList.get(position).getCountryName());
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
        return mCountryList.size();
    }

    @Override
    public Object getItem(int position) {
        return mCountryList.get(position);
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
        textView.setText(mCountryList.get(position).getCountryName());
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

    /**
     * Returns the index of a given country
     * @param countryCode
     * @return
     */
    public int getIndexOf(String countryCode) {
        if(countryCode.equals("")) {
            countryCode = Constants.DEFAULT_COUNTRY;
        }
        int i = 0;
        for(Country country : mCountryList){
            if(country.getCountryCode().equals(countryCode)){
                break;
            }
            i++;
        }
        return i % mCountryList.size();
    }
}
