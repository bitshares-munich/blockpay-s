package de.bitsharesmunich.utils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.gson.Gson;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import de.bitsharesmunich.blockpos.R;

/**
 * Created by qasim on 4/7/16.
 */
public class Helper {
    public static final String TAG = "Helper";

    /**
     * Value used to represent an unselected index
     */
    public static final int DEFAULT_UNSELECTED_VALUE = 9999;

    public static final ArrayList<String> languages = new ArrayList<>();
    public static Map<String, String> countriesISOMap = new HashMap<String, String>();

    public static void setLanguages() {
        languages.clear();
        languages.add("sq");
        languages.add("ar");
        languages.add("hy");
        languages.add("bn");
        languages.add("bs");
        languages.add("bg");
        languages.add("ca");
        languages.add("zh");
        languages.add("zh-rTW");
        languages.add("hr");
        languages.add("cs");
        languages.add("da");
        languages.add("nl");
        languages.add("en");
        languages.add("et");
        languages.add("fa");
        languages.add("fi");
        languages.add("fr");
        languages.add("de");
        languages.add("el");
        languages.add("he");
        languages.add("hi");
        languages.add("hu");
        languages.add("id");
        languages.add("it");
        languages.add("ja");
        languages.add("ko");
        languages.add("lv");
        languages.add("lt");
        languages.add("ms");
        languages.add("no");
        languages.add("pl");
        languages.add("pt");
        languages.add("ro");
        languages.add("ru");
        languages.add("sr");
        languages.add("sk");
        languages.add("sl");
        languages.add("es");
        languages.add("sv");
        languages.add("th");
        languages.add("tr");
        languages.add("uk");
        languages.add("vi");
    }

    public static final Currency[] SUPPORTED_CURRENCIES = new Currency[]{
            Currency.getInstance(new Locale("en", "US")),
            Currency.getInstance(new Locale("en", "DE")),
            Currency.getInstance(new Locale("en", "CN"))
    };

    public static ArrayList<String> getLanguages() {
        setLanguages();
        return languages;
    }

    public static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void storeStringSharePref(Context context, String key, String value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static int fetchIntSharePref(Context context, String key){
        return fetchIntSharePref(context, key, 9999);
    }

    public static int fetchIntSharePref(Context context, String key, int defaultValue) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(key, defaultValue);
    }


    public static void storeIntSharePref(Context context, String key, int value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static String fetchStringSharePref(Context context, String key, String defautValue){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, defautValue);
    }

    public static String fetchStringSharePref(Context context, String key) {
        return fetchStringSharePref(context, key, "");
    }

    public static void storeObjectSharePref(Context context, String key, Object object) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(object);
        prefsEditor.putString(key, json);
        prefsEditor.commit();

    }

    public static String fetchObjectSharePref(Context context, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = preferences.edit();
        return preferences.getString(key, "");
    }

    public static void storeBoolianSharePref(Context context, String key, Boolean value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static Boolean fetchBoolianSharePref(Context context, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(key, false);
    }

    public static void storeLongSharePref(Context context, String key, long value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    public static long fetchLongSharePref(Context context, String key, long defaultValue) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getLong(key, defaultValue);
    }

    public static long fetchLongSharePref(Context context, String key) {
        return fetchLongSharePref(context, key, -1);
    }

    public static void storeDoubleSharedPref(Context context, String key, double value){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(key, Double.doubleToRawLongBits(value)).apply();
    }

    public static double fetchDoubleSharedPref(Context context, String key, double defaultValue){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        double value;
        try{
            value = Double.longBitsToDouble(preferences.getLong(key, Double.doubleToLongBits(defaultValue)));
        }catch(ClassCastException e){
            String stringValue = preferences.getString(key, "0.0");
            value = Double.parseDouble(stringValue);
        }
        return value;
    }

    public static boolean containKeySharePref(Context context, String key) {
        boolean isContainer = false;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.contains(key)) {
            isContainer = true;
        }
        return isContainer;

    }

    public static String saveToInternalStorage(Context context, Bitmap bitmapImage) {
        ContextWrapper cw = new ContextWrapper(context);
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, "gravatar.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return directory.getAbsolutePath();
    }

    public static Bitmap loadImageFromStorage(Context context) {
        Bitmap bitmap = null;
        try {
            ContextWrapper cw = new ContextWrapper(context);
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File f = new File(directory, "gravatar.jpg");
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;

    }

    public static void deleteImage(Context context) {
        try {
            ContextWrapper cw = new ContextWrapper(context);
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            File file = new File(directory, "gravatar.jpg");
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {

        }


    }


    public static void initImageLoader(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        config.writeDebugLogs(); // Remove for release app

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());


    }

    /*
    public static ArrayList<String> getCountriesArray() {
        Locale[] locales = Locale.getAvailableLocales();
        ArrayList<String> countries = new ArrayList<String>();
        for (Locale locale : locales) {
            try {
                Currency currency = Currency.getInstance(locale);
                String country = locale.getDisplayCountry() + " (" + currency.getCurrencyCode() + ")";
                if (country.trim().length() > 0 && !countries.contains(country) && !country.trim().equals("World")) {
                    countries.add(country);
                }
            }
            catch (Exception e)
            {

            }
        }
        Collections.sort(countries);
        setCountriesISOMap();
        return countries;
    }
    */

    public static String getCountryCode(String countryName){
        if(countriesISOMap.size() == 0){
            setCountriesISOMap();
        }
        return countriesISOMap.get(countryName);
    }

    public static void setCountriesISOMap() {
        String[] isoCountryCodes = Locale.getISOCountries();
        for (int i = 0; i < isoCountryCodes.length; i++) {
            Locale locale = new Locale("", isoCountryCodes[i]);
            countriesISOMap.put(locale.getDisplayCountry(), isoCountryCodes[i]);
        }
    }

    public static String[] getFiatCurrency(String country) {
        Helper.setCountriesISOMap();
        Locale countryLocale = new Locale("", countriesISOMap.get(country));
        Currency currency = Currency.getInstance(countryLocale);
        boolean isSelectedCurrencySupported = false;
        for(int i = 0; i < SUPPORTED_CURRENCIES.length; i++){
            if(SUPPORTED_CURRENCIES[i].equals(currency)){
                isSelectedCurrencySupported = true;
                break;
            }
        }
        String[] arr = null;
        if(isSelectedCurrencySupported) {
            arr = new String[]{currency.getCurrencyCode(), currency.getSymbol()};
        }else {
            arr = new String[]{SUPPORTED_CURRENCIES[0].getCurrencyCode(), SUPPORTED_CURRENCIES[0].getSymbol()};
        }
        return arr;
    }


    public static String getFiatCurrency(Context context)
    {
        String countryCode = Helper.fetchStringSharePref(context, context.getString(R.string.pref_country));
        if (countryCode == null || countryCode.isEmpty()) {
            return "EUR";
        }

        //String countryCode = Helper.fetchStringSharePref(context, context.getString(R.string.pref_country));
        String spinnertext = Helper.getSpinnertextCountry(countryCode);
        return getFiatCurrency(spinnertext)[0];
    }


    public static void setLocale(String lang, String country, Resources res) {
        Locale myLocale;
        if (lang.equalsIgnoreCase("zh-rTW")) {
            myLocale = Locale.TRADITIONAL_CHINESE;
        } else if (lang.equalsIgnoreCase("zh-rCN") || lang.equalsIgnoreCase("zh")) {
            myLocale = Locale.SIMPLIFIED_CHINESE;
        }
        else if (lang.equalsIgnoreCase("pt-rBR") || lang.equalsIgnoreCase("pt")) {
            myLocale = new Locale("pt","BR");
        }
        else if (lang.equalsIgnoreCase("pt-rPT")) {
            myLocale = new Locale("pt","PT");
        } else {
            myLocale = new Locale(lang, country);
        }
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        Log.d(TAG,"Setting locale to: "+myLocale);
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    /**
     * Formats a number according to a specific locale and with a maximum and minimum amount of decimal places.
     * @param locale: The locale that should be used.
     * @param number: The number to represent.
     * @param minFraction: The minimum number of decimal places.
     * @param maxFraction: The maximum number of decimal places.
     * @return: The formatted string representing the number.
     */
    public static String setLocaleNumberFormat(Locale locale, Number number, int minFraction, int maxFraction) {
        NumberFormat formatter = NumberFormat.getInstance(locale);
        formatter.setRoundingMode(RoundingMode.CEILING);
        formatter.setMaximumFractionDigits(minFraction);
        formatter.setMinimumFractionDigits(maxFraction);
        return formatter.format(number);
    }

    /**
     * Same as setLocaleNumberFormat, but specifying only the locale and number parameters, assuming zero
     * decimal places.
     * @param locale: The locale that should be used.
     * @param number: The number to represent.
     * @return: The formatted string representing the number.
     */
    public static String setLocaleNumberFormat(Locale locale, Number number){
        NumberFormat formatter = NumberFormat.getInstance(locale);
        formatter.setMaximumFractionDigits(2);
        String localeFormattedNumber = formatter.format(number);
        return localeFormattedNumber;
    }

    /**
     * Formatting method used to display currencies. This method should decide whether to put the
     * currency symbol as a prefix or sufix depending on the locale parameter.
     *
     * @param locale: The locale that should be used.
     * @param number: The number to represent.
     * @param currencySymbol: The currency symbol to use.
     * @return: The formatted string representing the currency amount.
     */
    public static String setLocaleNumberFormat(Locale locale, Number number, String currencySymbol){
        String formatted = setLocaleNumberFormat(locale, number, 2, 2);
        boolean isPrefix = isPrefix(locale);
        if(isPrefix)
            return String.format("%s %s", currencySymbol, formatted);
        else
            return String.format("%s %s", formatted, currencySymbol);
    }


    public static boolean isPrefix(Locale locale) {
        boolean isPrefix = true;
        int index = 0;
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
        if (numberFormat instanceof DecimalFormat) { // determine if symbol is prefix or suffix
            String localizedPattern = ((DecimalFormat) numberFormat).toLocalizedPattern();

            // here's how we tell where the symbol goes.
            index = localizedPattern.indexOf('\u00A4');  // currency sign

            if (index > 0) {
                isPrefix = false;
            } else {
                isPrefix = true;
            }
        }
        return isPrefix;
    }

    public static char getDecimalSeparator(Locale locale) {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(locale);
        return decimalFormatSymbols.getDecimalSeparator();
    }

    public static Bitmap highlightImage(float radiusBlurMaskFilter, Bitmap src) {
        // create new bitmap, which will be painted and becomes result image
        Bitmap bmOut = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        // setup canvas for painting
        Canvas canvas = new Canvas(bmOut);
        // setup default color
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        // create a blur paint for capturing alpha
        Paint ptBlur = new Paint();
        ptBlur.setMaskFilter(new BlurMaskFilter(radiusBlurMaskFilter, BlurMaskFilter.Blur.NORMAL));
        int[] offsetXY = new int[2];
        // capture alpha into a bitmap
        Bitmap bmAlpha = src.extractAlpha(ptBlur, offsetXY);
        // create a color paint
        Paint ptAlphaColor = new Paint();
        ptAlphaColor.setColor(Color.GRAY);
        // paint color for captured alpha region (bitmap)
        canvas.drawBitmap(bmAlpha, offsetXY[0], offsetXY[1], ptAlphaColor);
        // free memory
        bmAlpha.recycle();

        // paint the image source
        canvas.drawBitmap(src, 0, 0, null);

        // return out final image
        return bmOut;
    }

    public static String getCoinImagePath(String coinType) {
        if (coinType.toLowerCase().contains("trade")) return "blocktrade_icon";
        if (coinType.toLowerCase().contains("doge")) return "doge";
        if (coinType.toLowerCase().contains("btc") && !coinType.toLowerCase().contains("bit") && !coinType.toLowerCase().contains("open"))
            return "btc";
        if (coinType.toLowerCase().contains("bts")) return "bts";
        if (coinType.toLowerCase().contains("dash")) return "dash";
        if (coinType.toLowerCase().contains("eth")) return "eth";
        if (coinType.toLowerCase().contains("ltc")) return "ltc";
        if (coinType.toLowerCase().contains("nbt")) return "nbt";
        if (coinType.toLowerCase().contains("ppc")) return "ppc";
        if (coinType.toLowerCase().contains("nsr")) return "nbt";
        if ((coinType.toLowerCase().contains("steem") || coinType.toLowerCase().contains("sbd")) && !coinType.toLowerCase().contains("open"))
            return "steem";
        if (coinType.toLowerCase().contains("open")) return "open";
        if (coinType.toLowerCase().contains("steem")) return "steem";
        return "bts";
    }

    public static String padString(String str) {
        if (str == null || str.isEmpty()) {
            return "0";
        } else if (str.equals(".")) {
            return "0.";
        } else {
            return str;
        }
    }

    public static String convertDateToGMT(Date date, Context context) {

        if (Helper.containKeySharePref(context, context.getString(R.string.date_time_zone))) {

            String dtz = Helper.fetchStringSharePref(context, context.getString(R.string.date_time_zone));
            TimeZone tz = TimeZone.getTimeZone(dtz);

            SimpleDateFormat destFormat = new SimpleDateFormat("dd MMM");
            destFormat.setTimeZone(tz);
            String result = destFormat.format(date);
            return result;

        } else {
            SimpleDateFormat destFormat = new SimpleDateFormat("dd MMM");
            String result = destFormat.format(date);
            return result;
        }
    }


    public static String convertTimeToGMT(Date date, Context context) {

        if (Helper.containKeySharePref(context, context.getString(R.string.date_time_zone))) {

            String dtz = Helper.fetchStringSharePref(context, context.getString(R.string.date_time_zone));
            TimeZone tz = TimeZone.getTimeZone(dtz);

            SimpleDateFormat destFormat = new SimpleDateFormat("HH:mm:ss");
            destFormat.setTimeZone(tz);
            String result = destFormat.format(date);
            return result;

        } else {
            SimpleDateFormat destFormat = new SimpleDateFormat("HH:mm:ss");
            String result = destFormat.format(date);
            return result;
        }
    }

    public static String convertTimeZoneToGMT(Date date, Context context) {

        if (Helper.containKeySharePref(context, context.getString(R.string.date_time_zone))) {
            String dtz = Helper.fetchStringSharePref(context, context.getString(R.string.date_time_zone));
            TimeZone tz = TimeZone.getTimeZone(dtz);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(tz);
            return calendar.getTimeZone().getDisplayName(false, TimeZone.SHORT);

        } else {
            return "UTC";
        }
    }

    public static String convertTimeZoneToRegion(Date date, Context context) {

        if (Helper.containKeySharePref(context, context.getString(R.string.date_time_zone))) {
            String dtz = Helper.fetchStringSharePref(context, context.getString(R.string.date_time_zone));
            TimeZone tz = TimeZone.getTimeZone(dtz);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(tz);
            String region = calendar.getTimeZone().getID();
            String[] arr = region.split("/");
            for ( String ss : arr) {
                if (ss.equals("Europe")){
                    region = "CET";
                }
            }
            return region;

        } else {
            return "UTC";
        }
    }

    public static int convertDOubleToInt(Double value) {
        String valueString = Double.toString(value);

        for (int i = 0; i < valueString.length(); i++) {
            if (valueString.charAt(i) == '.') {
                valueString = valueString.substring(0, i);
                break;
            }
        }

        int valueInteger = Integer.parseInt(valueString);

        return valueInteger;
    }

    /*
    public static String getFadeCurrency(Context context) {
        String fiat;

        if (Helper.containKeySharePref(context, context.getResources().getString(R.string.pref_fait_currency))) {
            fiat = Helper.fetchObjectSharePref(context, context.getResources().getString(R.string.pref_fait_currency));

            if (fiat.isEmpty()) {
                fiat = "EUR";
            }
        } else {
            fiat = "EUR";
        }

        return fiat;

    }
    */

    public static String getFiatSymbol(Context context) {
        Currency c  = Currency.getInstance(getFiatCurrency(context));
        return c.getSymbol();
    }

    public static ArrayList<Country> getCountriesArray() {
        String[] locales = Locale.getISOCountries();
        ArrayList<Country> countries = new ArrayList<>();
        for (String countryCode : locales) {
            Locale locale = new Locale("", countryCode);
            countries.add(new Country(locale.getCountry(), locale.getDisplayName()));
        }
        Collections.sort(countries);
        setCountriesISOMap();
        return countries;
    }

    public static ArrayList<String> getCountryCodes(){
        String[] locales = Locale.getISOCountries();
        ArrayList<String> countries = new ArrayList<>();
        for (String countryCode : locales) {
            Locale locale = new Locale("", countryCode);
            countries.add(locale.getCountry());
        }
        Collections.sort(countries);
        setCountriesISOMap();
        return countries;
    }

    public static String getSpinnertextCountry(String countryCode) {

        Locale locale = new Locale("", countryCode);
        try {
            Currency currency = Currency.getInstance(locale);
            return locale.getDisplayCountry() + " (" + currency.getCurrencyCode() + ")";
        } catch (Exception e) {

        }
        return "";
    }
    public static String convertDateToGMTWithYear(Date date, Context context) {

        if (Helper.containKeySharePref(context, context.getString(R.string.date_time_zone))) {

            String dtz = Helper.fetchStringSharePref(context, context.getString(R.string.date_time_zone));
            TimeZone tz = TimeZone.getTimeZone(dtz);

            SimpleDateFormat destFormat = new SimpleDateFormat("dd MMM yy");
            destFormat.setTimeZone(tz);
            String result = destFormat.format(date);
            return result;

        } else {
            SimpleDateFormat destFormat = new SimpleDateFormat("dd MMM yyy");
            String result = destFormat.format(date);
            return result;
        }
    }
    public static String getFadeCurrencySymbol(Context context)
    {
        String currrencyCode = getFiatCurrency(context);
        return Currency.getInstance(currrencyCode).getSymbol(Locale.ENGLISH);
    }

    /**
     * Retrieves a NumberFormat instance for a specific currency depending on some parameters
     * @param context: Application context
     * @param currency: The desired currency
     * @param locale: The locale
     * @param minDecimals: The minimum number of decimal places to use
     * @return: A NumberFormat instance
     */
    public static NumberFormat currencyFormat(Context context, Currency currency, Locale locale, int minDecimals){
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        currencyFormatter.setCurrency(currency);
        currencyFormatter.setMinimumFractionDigits(minDecimals);

        //The default JDK handles situations well when the currency is the default currency for the locale
        if (currency.equals(Currency.getInstance(locale))) {
            return currencyFormatter;
        }

        //otherwise we need to "fix things up" when displaying a non-native currency
        if (currencyFormatter instanceof DecimalFormat) {
            DecimalFormat decimalFormat = (DecimalFormat) currencyFormatter;
            String correctedI18NSymbol = getCorrectedInternationalCurrencySymbol(context, currency);
            if (correctedI18NSymbol != null) {
                DecimalFormatSymbols dfs = decimalFormat.getDecimalFormatSymbols(); //this returns a clone of DFS
                dfs.setInternationalCurrencySymbol(correctedI18NSymbol);
                dfs.setCurrencySymbol(correctedI18NSymbol);
                decimalFormat.setDecimalFormatSymbols(dfs);
            }
        }
        return currencyFormatter;
    }

    /**
     * Retrieves a NumberFormat instance with a default value of 0 minimum decimal places.
     * @param context: Application context
     * @param currency: The desired currency
     * @param displayLocale: The locale
     * @return: A NumberFormat instance
     */
    public static NumberFormat currencyFormat(Context context, Currency currency, Locale displayLocale) {
        return currencyFormat(context, currency, displayLocale, 0);
    }

    private static String getCorrectedInternationalCurrencySymbol(Context context, Currency currency) {
        AssetsPropertyReader assetsReader = new AssetsPropertyReader(context);
        Properties properties = assetsReader.getProperties("correctedI18nCurrencySymbols.properties");
        if(properties.containsKey(currency.getCurrencyCode())){
            return properties.getProperty(currency.getCurrencyCode());
        }else{
            return currency.getCurrencyCode();
        }
    }

    public static boolean isRTL(Locale locale, String symbol) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);


        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(symbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        String formattedtext = currencyFormat.format(100.0);

        if (formattedtext.startsWith(symbol)) {
            return false;
        } else {
            return true;
        }

        /*
        final int directionality = Character.getDirectionality(String.format(locale,"%s",locale.getDisplayLanguage(locale)).charAt(0));

        if ( (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT) ||
                (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) )
        {
            return true;
        }
        else
        {
            return false;
        }
        */
    }
    public static String getFadeCurrency(Context context) {
//        Boolean isFade = Helper.containKeySharePref(context, context.getString(R.string.pref_fade_currency));
//        if (isFade) {
//            String currency[] = Helper.fetchStringSharePref(context, context.getString(R.string.pref_fade_currency)).split(" ");
//            return currency[currency.length - 1].replace("(", "").replace(")", "");
//        } else {
            return "EUR";
        //}
    }

}
