package de.bitsharesmunich.interfaces;

import android.graphics.Bitmap;

import java.util.List;

import de.bitsharesmunich.models.Gravatar;
import de.bitsharesmunich.models.TransactionDetails;

/**
 * Created by Syed Muhammad Muzzammil on 5/19/16.
 */
public interface GravatarDelegate
{
    void updateProfile(Gravatar myGravatar);
    void updateCompanyLogo(Bitmap logo);
    void failureUpdateProfile();
    void failureUpdateLogo();
}
