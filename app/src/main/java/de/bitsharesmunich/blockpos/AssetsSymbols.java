package de.bitsharesmunich.blockpos;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;

import de.bitsharesmunich.models.CCAssets;
import de.bitsharesmunich.models.Smartcoin;
import de.bitsharesmunich.models.TransactionDetails;
import de.bitsharesmunich.models.Uia;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.ServiceGenerator;
import de.bitsharesmunich.utils.TinyDB;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by afnan on 7/19/16.
 */
public class AssetsSymbols {
    private String TAG = this.getClass().getName();
    TinyDB tinyDB;
    Context context;
    public AssetsSymbols(Context aContext){
        context = aContext;
        tinyDB = new TinyDB(aContext);
    }


    public void getAssetsFromServer() {
        ServiceGenerator sg = new ServiceGenerator(""/*context.getString(R.string.node_server_url)*/);
        IWebService service = sg.getService(IWebService.class);
        final Call<CCAssets> postingService = service.getAssets();
        postingService.enqueue(new Callback<CCAssets>() {

            @Override
            public void onResponse(Call<CCAssets> call, Response<CCAssets> response) {
                if (response.isSuccessful()) {
                    CCAssets ccAssets;
                    ccAssets = response.body();
                    saveAssetsSymbols(ccAssets);
                }
            }

            @Override
            public void onFailure(Call<CCAssets> call, Throwable t) {
            }
        });
    }

    void saveAssetsSymbols(CCAssets ccAssets){

        ArrayList<String> smartcoinsList = tinyDB.getListString(context.getString(R.string.pref_desired_smart_coin_list));
        for(Smartcoin smartcoins : ccAssets.smartcoins){
            smartcoinsList.add(smartcoins.symbol);
        }

        HashSet<String> hashSet1 = new HashSet<>();
        hashSet1.addAll(smartcoinsList);
        smartcoinsList.clear();
        smartcoinsList.addAll(hashSet1);

        ArrayList<String> uiaList = tinyDB.getListString(context.getString(R.string.pref_uia_coin_assets));
        for(Uia uia : ccAssets.uia){
            uiaList.add(uia.symbol);
        }

        HashSet<String> hashSet2 = new HashSet<>();
        hashSet2.addAll(uiaList);
        uiaList.clear();
        uiaList.addAll(hashSet2);

        tinyDB.putListString(context.getString(R.string.pref_desired_smart_coin_list),smartcoinsList);
        tinyDB.putListString(context.getString(R.string.pref_uia_coin_assets),uiaList);

    }

    public Boolean isUiaSymbol(String sym){
        ArrayList<String> uiaList = tinyDB.getListString(context.getString(R.string.pref_uia_coin_assets));
        return uiaList.contains(sym);
    }

    public Boolean isSmartCoinSymbol(String sym){
        sym = sym.replace("bit","");
        ArrayList<String> smartcoinsList = tinyDB.getListString(context.getString(R.string.pref_desired_smart_coin_list));
        return smartcoinsList.contains(sym);
    }
    public ArrayList<String> updatedList(ArrayList<String> sym){
        ArrayList<String> symbols = new ArrayList<>();
        for(String s : sym){
                symbols.add(updateString(s));
        }
        return symbols;
    }
    public ArrayList<TransactionDetails> updatedTransactionDetails(ArrayList<TransactionDetails> td){
        ArrayList<TransactionDetails> symbols = new ArrayList<>();
        for(TransactionDetails s : td){
            if(!s.assetSymbol.contains("bit")) {
                s.assetSymbol = updateString(s.assetSymbol);
            }
            symbols.add(s);
        }
        return symbols;
    }
    public String updateString(String string){
        if(string.equals("BTS")){
            return string;
        }else if(isSmartCoinSymbol(string)){
                return "bit"+string;
        }else if(isUiaSymbol(string)){
            return string;
        }else return string;
    }
    public void displaySpannable(TextView textView,String bit){
        if(bit.contains("bit")) {
            SpannableString ss1 = new SpannableString(bit);
            ss1.setSpan(new RelativeSizeSpan(0.8f), 0, 3, 0); // set size
            ss1.setSpan(0, 3, bit.length(), 0);
            textView.setText(ss1);
        } else {
            textView.setText(bit);
        }
    }
}
