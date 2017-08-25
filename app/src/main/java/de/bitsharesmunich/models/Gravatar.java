package de.bitsharesmunich.models;

import android.graphics.Bitmap;
import android.view.View;

import com.google.gson.internal.LinkedTreeMap;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.bitsharesmunich.interfaces.GravatarDelegate;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.ServiceGenerator;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by afnan on 8/22/16.
 */
public class Gravatar
{
    public static String companyName; // name -> displayName
    public static String address; // aboutMe -> Address:
    public static String url; // urls -> first value

    private static String email;
    private static String logoThumbnailUrl;
    private static Bitmap companyLogo; // thumbnailUrl
    private static Gravatar instance;
    private static GravatarDelegate myDelegate;
    private static Date lastUpdatedTime;

    Gravatar(){
        companyName = address = url = logoThumbnailUrl = "";
        companyLogo = null;
        myDelegate = null;
    }

    public static Gravatar getInstance(GravatarDelegate _myDelegate)
    {
        if (instance == null)
        {
            instance = new Gravatar();
        }
        myDelegate = _myDelegate;
        return instance;
    }

    public static Gravatar getInstance()
    {
        if (instance == null)
        {
            instance = new Gravatar();
        }
        return instance;
    }

    public void fetch(String _email)
    {
        if ( _email == null || _email.isEmpty() )
        {
            if(myDelegate!=null) {
                myDelegate.failureUpdateProfile();
                myDelegate.failureUpdateLogo();
            }
            return;
        }

        if ( (email != null) && !email.isEmpty() && (lastUpdatedTime != null) && _email.equals(email) )
        {
            Date curTime = Calendar.getInstance().getTime();
            long mills = curTime.getTime() - lastUpdatedTime.getTime();
            long Mins = mills % (1000*60*60);

            if ( Mins <= 5 )
            {
                if(myDelegate!=null)
                myDelegate.updateProfile(instance);

                if ( companyLogo != null )
                {
                    if(myDelegate!=null)
                    myDelegate.updateCompanyLogo(companyLogo);
                }
                else if ( logoThumbnailUrl != null && !logoThumbnailUrl.isEmpty() )
                {
                    getGravatarImageFromServer(logoThumbnailUrl);
                }

                return;
            }
        }

        email = _email;
        String md5= Helper.md5(email);
        ServiceGenerator sg = new ServiceGenerator("https://en.gravatar.com");
        IWebService service = sg.getService(IWebService.class);
        final Call<Object> postingService = service.getGravatarProfile(md5);
        postingService.enqueue(new Callback<Object>()
        {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response)
            {
                if (response.isSuccessful())
                {
                    if ( response.body() instanceof LinkedTreeMap)
                    {
                        LinkedTreeMap map = (LinkedTreeMap) response.body();

                        if ( map.containsKey("entry") )
                        {
                            Object entry = map.get("entry");

                            if(((ArrayList) entry).size()>0)
                            if ( entry instanceof ArrayList )
                            {

                                if ( (((ArrayList<LinkedTreeMap>) entry).get(0)).containsKey("displayName") )
                                {
                                    companyName = (((ArrayList<LinkedTreeMap>) entry).get(0)).get("displayName").toString();
                                }

                                if ( (((ArrayList<LinkedTreeMap>) entry).get(0)).containsKey("thumbnailUrl") )
                                {
                                    logoThumbnailUrl = (((ArrayList<LinkedTreeMap>) entry).get(0)).get("thumbnailUrl").toString();
                                    getGravatarImageFromServer(logoThumbnailUrl);
                                }

                                if ( (((ArrayList<LinkedTreeMap>) entry).get(0)).containsKey("urls") )
                                {
                                    Object urls = (((ArrayList<LinkedTreeMap>) entry).get(0)).get("urls");

                                    if(((ArrayList) urls).size()>0) {
                                        if (urls instanceof ArrayList) {
                                            Object urlsObject = ((ArrayList) urls).get(0);

                                            if (urlsObject instanceof LinkedTreeMap) {
                                                if (((LinkedTreeMap) urlsObject).containsKey("value")) {
                                                    url = ((LinkedTreeMap) urlsObject).get("value").toString();
                                                }
                                            }
                                        }
                                    }
                                }

                                if ( (((ArrayList<LinkedTreeMap>) entry).get(0)).containsKey("aboutMe") )
                                {
                                    String[] aboutMe = (((ArrayList<LinkedTreeMap>) entry).get(0)).get("aboutMe").toString().split("\n");

                                    for(String item:aboutMe)
                                    {
                                        if ( item.startsWith("Address:") )
                                        {
                                            address = item.substring(8);
                                        }
                                    }
                                }
                            }

                            lastUpdatedTime = Calendar.getInstance().getTime();
                            if(myDelegate!=null)
                            myDelegate.updateProfile(instance);
                            return;
                        }
                    }
                }
                if(myDelegate!=null)
                myDelegate.failureUpdateProfile();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t)
            {
                if(myDelegate!=null)
                myDelegate.failureUpdateProfile();
            }
        });
    }

    private void getGravatarImageFromServer(String imageUri)
    {

        ImageLoader.getInstance().loadImage(imageUri+"?s=200", new SimpleImageLoadingListener()
        {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage)
            {
                companyLogo = loadedImage;
                if(myDelegate!=null)
                myDelegate.updateCompanyLogo(companyLogo);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason)
            {
                if(myDelegate!=null)
                myDelegate.failureUpdateLogo();
            }
        });
    }

    public static Bitmap gravatarLogo(){
        return companyLogo;
    }

    public static void reset() {
        instance = null;
        getInstance();
    }
}
