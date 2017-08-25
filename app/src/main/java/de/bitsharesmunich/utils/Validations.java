package de.bitsharesmunich.utils;

import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by qasim on 4/7/16.
 */
public class Validations {

    public static void  isValidateAccountName(EditText editText)
    {
        InputFilter filter= new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    String checkMe = String.valueOf(source.charAt(i));

                    Pattern pattern = Pattern.compile("[A-Za-z][\\w\\-]*");
                    Matcher matcher = pattern.matcher(checkMe);
                    boolean valid = matcher.matches();
                    if(!valid){
                        Log.d("", "invalid");
                        if(i>1) {
                            return String.valueOf(source.charAt(i - 1));
                        }
                    }
                }
                return null;
            }
        };
        editText.setFilters(new InputFilter[]{filter});
    }
}
