package de.bitsharesmunich.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

/**
 * Class that provides encryption/decryption support by using the key management framework provided
 * by the KeyStore system.
 *
 * The implemented scheme was taken from <a href="https://medium.com/@ericfu/securely-storing-secrets-in-an-android-application-501f030ae5a3">this</> blog post.
 *
 * @see <a href="https://developer.android.com/training/articles/keystore.html">Android Keystore System</a>
 */

public class CryptoUtils {
    private final String TAG = this.getClass().getName();
    private final String FIXED_IV = "69b522faabab";

    private static final String SHARED_PREFERENCE_NAME = "secret_key";

    // Key used to store the encrypted AES key
    private static final String ENCRYPTED_KEY = "encrypted_key";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String AES_MODE_POST_M = "AES/GCM/NoPadding";
    private static final String AES_MODE_PRE_M = "AES/ECB/PKCS7Padding";
    private static final String RSA_MODE =  "RSA/ECB/PKCS1Padding";

    private Context mContext;
    private KeyStore keyStore;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public CryptoUtils(Context context) {
        this.mContext = context;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            if(!keyStore.containsAlias(Constants.KEYSTORE_ALIAS)){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d(TAG,"Creating key for API level 23 and superior");
                    KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                    keyGenerator.init(new KeyGenParameterSpec.Builder(Constants.KEYSTORE_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(false)
                            .build());
                    keyGenerator.generateKey();
                }else{
                    Log.d(TAG,"Creating key for API level below 23");
                    // Generate a key pair for encryption
                    Calendar start = Calendar.getInstance();
                    Calendar end = Calendar.getInstance();
                    end.add(Calendar.YEAR, 30);

                    KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                            .setAlias(Constants.KEYSTORE_ALIAS)
                            .setSubject(new X500Principal("CN=" + Constants.KEYSTORE_ALIAS))
                            .setSerialNumber(BigInteger.TEN)
                            .setStartDate(start.getTime())
                            .setEndDate(end.getTime())
                            .build();

                    KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
                    kpg.initialize(spec);
                    kpg.generateKeyPair();

                    SharedPreferences pref = context.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
                    String encryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
                    if (encryptedKeyB64 == null) {
                        byte[] key = new byte[16];
                        SecureRandom secureRandom = new SecureRandom();
                        secureRandom.nextBytes(key);

                        byte[] encryptedKey = rsaEncrypt(key);
                        encryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT);
                        SharedPreferences.Editor edit = pref.edit();
                        edit.putString(ENCRYPTED_KEY, encryptedKeyB64);
                        edit.commit();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while accessing the keystore. Msg: " + e.getMessage());
        }
    }

    @SuppressLint("NewApi")
    public String encryptPostM(String input){
        String encryptedBase64Encoded = null;
        try {
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(Constants.KEYSTORE_ALIAS, null)).getSecretKey();
            Cipher c = Cipher.getInstance(AES_MODE_POST_M);
            c.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, FIXED_IV.getBytes()));
            byte[] encodedBytes = c.doFinal(input.getBytes());
            encryptedBase64Encoded = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
        }catch(NoSuchAlgorithmException e){
            Log.e(TAG,"NoSuchAlgorithmException. Msg: "+e.getMessage());
        } catch (InvalidKeyException e) {
            Log.e(TAG,"InvalidKeyException. Msg: "+e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG,"InvalidAlgorithmParameterException. Msg: "+e.getMessage());
        } catch (NoSuchPaddingException e) {
            Log.e(TAG,"NoSuchPaddingException. Msg: "+e.getMessage());
        } catch (BadPaddingException e) {
            Log.e(TAG,"BadPaddingException. Msg: "+e.getMessage());
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG,"IllegalBlockSizeException. Msg: "+e.getMessage());
        } catch (Exception e) {
            Log.e(TAG,"Exception. Msg: "+e.getMessage());
        }
        return encryptedBase64Encoded;
    }

    public String encryptPreM(String clearText) {
        byte[] input = clearText.getBytes();
        byte[] encodedBytes = null;
        String encryptedBase64Encoded = "";
        try {
            Cipher c = Cipher.getInstance(AES_MODE_PRE_M, "BC");
            c.init(Cipher.ENCRYPT_MODE, getSecretKey());
            encodedBytes = c.doFinal(input);
            encryptedBase64Encoded = Base64.encodeToString(encodedBytes, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG,"NoSuchAlgorithmException. Msg: "+e.getMessage());
        } catch (NoSuchPaddingException e) {
            Log.e(TAG,"NoSuchPaddingException. Msg: "+e.getMessage());
        } catch (InvalidKeyException e) {
            Log.e(TAG,"InvalidKeyException. Msg: "+e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG,"InvalidAlgorithmParameterException. Msg: "+e.getMessage());
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG,"IllegalBlockSizeException. Msg: "+e.getMessage());
        } catch (BadPaddingException e) {
            Log.e(TAG,"BadPaddingException. Msg: "+e.getMessage());
        } catch (Exception e) {
            Log.e(TAG,"Exception. Msg: "+e.getMessage());
        }
        return encryptedBase64Encoded;
    }


    @SuppressLint("NewApi")
    public String decryptPostM(String encrypted){
        Cipher c = null;
        byte[] decodedBytes = null;
        try {
            SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStore.getEntry(Constants.KEYSTORE_ALIAS, null)).getSecretKey();
            c = Cipher.getInstance(AES_MODE_POST_M);
            c.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, FIXED_IV.getBytes()));
            decodedBytes = c.doFinal(Base64.decode(encrypted, Base64.DEFAULT));
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG,"NoSuchAlgorithmException. Msg: "+e.getMessage());
        } catch (NoSuchPaddingException e) {
            Log.e(TAG,"NoSuchPaddingException. Msg: "+e.getMessage());
        } catch (InvalidKeyException e) {
            Log.e(TAG,"InvalidKeyException. Msg: "+e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG,"InvalidAlgorithmParameterException. Msg: "+e.getMessage());
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG,"IllegalBlockSizeException. Msg: "+e.getMessage());
        } catch (BadPaddingException e) {
            Log.e(TAG,"BadPaddingException. Msg: "+e.getMessage());
        } catch (Exception e) {
            Log.e(TAG,"Exception. Msg: "+e.getMessage());
        }
        if(decodedBytes == null)
            return null;
        else
            return new String(decodedBytes);
    }

    @SuppressLint("NewApi")
    public byte[] decryptPreM(String cipher) {
        byte[] encrypted = Base64.decode(cipher, Base64.DEFAULT);
        byte[] decodedBytes = null;
        try {
            Cipher c = Cipher.getInstance(AES_MODE_PRE_M, "BC");
            c.init(Cipher.DECRYPT_MODE, getSecretKey());
            decodedBytes = c.doFinal(encrypted);
        }catch (NoSuchAlgorithmException e) {
            Log.e(TAG,"NoSuchAlgorithmException. Msg: "+e.getMessage());
        } catch (NoSuchPaddingException e) {
            Log.e(TAG,"NoSuchPaddingException. Msg: "+e.getMessage());
        } catch (InvalidKeyException e) {
            Log.e(TAG,"InvalidKeyException. Msg: "+e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG,"InvalidAlgorithmParameterException. Msg: "+e.getMessage());
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG,"IllegalBlockSizeException. Msg: "+e.getMessage());
            for(StackTraceElement element : e.getStackTrace()){
                Log.d(TAG,element.getClassName()+":"+element.getMethodName()+":"+element.getLineNumber());
            }
        } catch (BadPaddingException e) {
            Log.e(TAG,"BadPaddingException. Msg: "+e.getMessage());
        } catch (Exception e) {
            Log.e(TAG,"Exception. Msg: "+e.getMessage());
        }
        return decodedBytes;
    }

    /**
     * Method that returns an encrypted AES key stored in the shared preferences.
     * @return: Encrypted AES key
     * @throws Exception
     */
    private Key getSecretKey() throws Exception{
        SharedPreferences pref = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        String encryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);

        // need to check null, omitted here
        byte[] encryptedKey = Base64.decode(encryptedKeyB64, Base64.DEFAULT);
        byte[] key = rsaDecrypt(encryptedKey);
        return new SecretKeySpec(key, "AES");
    }

    /**
     * Method that will encrypt some data using the RSA algorithm.
     * @param secret: The secret to be encrypted
     * @return: Encrypted secret
     * @throws Exception
     */
    private byte[] rsaEncrypt(byte[] secret) throws Exception{
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(Constants.KEYSTORE_ALIAS, null);

        // Encrypt the text
        Cipher inputCipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret);
        cipherOutputStream.close();

        byte[] vals = outputStream.toByteArray();
        return vals;
    }

    /**
     * Method that will decrypt some data using the RSA algorithm.
     * @param encrypted: The encrypted secret.
     * @return: The unencrypted secret.
     * @throws Exception
     */
    private byte[] rsaDecrypt(byte[] encrypted) throws Exception {
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(Constants.KEYSTORE_ALIAS, null);
        Cipher output = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
        output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(
                new ByteArrayInputStream(encrypted), output);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte)nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i).byteValue();
        }
        return bytes;
    }
}
