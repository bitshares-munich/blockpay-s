package de.bitsharesmunich.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.bitsharesmunich.blockpos.R;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.graphenej.Asset;

/**
 * Created by nelson on 2/28/17.
 */

public class UserIssuedAssetAdapter extends BaseAdapter implements Filterable {
    private final String TAG = this.getClass().getName();
    private LayoutInflater inflater = null;
    private List<String> originalList;
    private List<String> filteredList;

    public UserIssuedAssetAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        BlockpayDatabase database = new BlockpayDatabase(context);
        List<Asset> assetList = database.getAssets(Asset.AssetType.UIA);
        originalList = new ArrayList<String>();
        filteredList = new ArrayList<String>();

        // Adding BTS to the list of possible reward tokens
        originalList.add("BTS");

        for (Asset asset : assetList) {
            originalList.add(asset.getSymbol());
            filteredList.add(asset.getSymbol());
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (convertView == null) vi = inflater.inflate(R.layout.dropdown_item, null);
        TextView uiaName = (TextView) vi.findViewById(R.id.uia_name);
        uiaName.setText(filteredList.get(position));
        return vi;
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

    @Override
    public Filter getFilter() {
        return new Filter() {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                ArrayList<String> newFilteredList = new ArrayList<String>();

                if (constraint != null && constraint.length() > 2) {
                    for (String asset : originalList) {
                        if (asset.toLowerCase().contains(constraint.toString().toLowerCase())) {
                            newFilteredList.add(asset);
                        }
                    }
                } else {
                    results.values = originalList;
                    results.count = originalList.size();
                }
                results.values = newFilteredList;
                results.count = newFilteredList.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList = (List<String>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}