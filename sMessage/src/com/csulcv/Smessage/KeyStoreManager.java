package com.csulcv.Smessage;

import android.content.Context;
import android.util.Log;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.crypto.util.PublicKeyFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class KeyStoreManager {

    private static final String TAG = "Smessage: KeyStoreManager";

    private String keyStorePassword = null;
    private KeyStore keyStore = null;
    private Certificate[] certificateChain = null;
    private Context activityContext = null;

    /**
     * Setup a KeyStoreManager object to control the addition and removal of keys from the key store. Loads the key store
     * and the user's self-signed certificate.
     *
     * @param activityContext The activity that the object was instantiated from.
     * @param password        The user's key store password.
     * @throws Exception
     */
    public KeyStoreManager(Context activityContext, String password) throws Exception {

        // Store the context that the manager was created in and the key store password
        this.activityContext = activityContext;
        keyStorePassword = password;

        FileInputStream keyStoreFileInput = activityContext.openFileInput(KeyStoreGenerator.KEY_STORE_FILE_NAME);

        // Load the key store from the file
        keyStore = KeyStore.getInstance("BKS");
        keyStore.load(keyStoreFileInput, password.toCharArray());

        // Close the file
        try {
            keyStoreFileInput.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing key store file", e);
        }

        // Load the user's self-signed certificate
        certificateChain = new Certificate[] {keyStore.getCertificate(KeyStoreGenerator.OWN_CERT_ALIAS)};

    }

    public void addPublicKey(String alias, PublicKey publicKey) throws Exception {

        FileOutputStream keyStoreFileOutput = activityContext.openFileOutput(KeyStoreGenerator.KEY_STORE_FILE_NAME,
                Context.MODE_PRIVATE);

        keyStore.setKeyEntry(alias, publicKey, keyStorePassword.toCharArray(), certificateChain);
        keyStore.store(keyStoreFileOutput, keyStorePassword.toCharArray());

        keyStoreFileOutput.close();

    }

    public void addPrivateKey(String alias, PrivateKey privateKey) throws Exception {

        FileOutputStream keyStoreFileOutput = activityContext.openFileOutput(KeyStoreGenerator.KEY_STORE_FILE_NAME,
                Context.MODE_PRIVATE);

        keyStore.setKeyEntry(alias, privateKey, keyStorePassword.toCharArray(), certificateChain);
        keyStore.store(keyStoreFileOutput, keyStorePassword.toCharArray());

        keyStoreFileOutput.close();

    }

    public void addSecretKey(String alias, byte[] secretKeyBytes) throws Exception {

        FileOutputStream keyStoreFileOutput = activityContext.openFileOutput(KeyStoreGenerator.KEY_STORE_FILE_NAME,
                Context.MODE_PRIVATE);

        SecretKey secretKey = new SecretKeySpec(secretKeyBytes, "AES");

        keyStore.setKeyEntry(alias, secretKey, keyStorePassword.toCharArray(), certificateChain);
        keyStore.store(keyStoreFileOutput, keyStorePassword.toCharArray());

        if (keyExists(alias)) {
            Log.d(TAG, "The key exists!");
        } else {
            throw new Exception("The alias given does not correspond to a key");
        }

        keyStoreFileOutput.close();

    }

    public AsymmetricKeyParameter getPublicKey(String alias) throws Exception {

        Key storedPublicKey = keyStore.getKey(alias, keyStorePassword.toCharArray());

        // TODO: Uncomment if this doesn't work!
        //AsymmetricKeyParameter publicKey = new PublicKeyFactory().createKey(storedPublicKey.getEncoded());
        AsymmetricKeyParameter publicKey = PublicKeyFactory.createKey(storedPublicKey.getEncoded());

        if (publicKey.isPrivate()) {
            throw new Exception("The alias given does not correspond to a public key");
        } else {
            return publicKey;
        }

    }

    public AsymmetricKeyParameter getPrivateKey(String alias) throws Exception {

        Key storedPrivateKey = keyStore.getKey(alias, keyStorePassword.toCharArray());

        // TODO: Uncomment if this doesn't work!
        //AsymmetricKeyParameter privateKey = new PrivateKeyFactory().createKey(storedPrivateKey.getEncoded());
        AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(storedPrivateKey.getEncoded());

        if (!privateKey.isPrivate()) {
            throw new Exception("The alias given does not correspond to a private key");
        } else {
            return privateKey;
        }

    }

    public byte[] getSecretKey(String alias) throws Exception {

        Key storedSecretKey = keyStore.getKey(alias, keyStorePassword.toCharArray());
        return storedSecretKey.getEncoded();

    }

    /**
     * Check whether a key associated with the given alias exists.
     */
    public boolean keyExists(String alias) {

        boolean keyExists = false;

        try {
            return keyStore.isKeyEntry(alias);
        } catch (Exception e) {
            Log.e(TAG, "Error checking key store for key", e);
        }

        return keyExists;

    }

    /**
     * Check whether the key store has already been generated. Used at app startup to decide whether or not to begin
     * key store setup.
     *
     * @return True if the file exists, false otherwise.
     */
    public static boolean keyStoreExists(Context activityContext) {

        File keyStoreFile = activityContext.getFileStreamPath(KeyStoreGenerator.KEY_STORE_FILE_NAME);
        return keyStoreFile.exists();

    }

    /**
     * Delete the key store file from internal storage. Used when the user needs to reset their password.
     *
     * @param activityContext The activity that the method was called from.
     * @return true if the file was deleted successfully, false otherwise.
     */
    public static boolean deleteKeyStore(Context activityContext) {

        File keyStoreFile = activityContext.getFileStreamPath(KeyStoreGenerator.KEY_STORE_FILE_NAME);

        if (keyStoreFile.delete()) {
            Log.d(TAG, "Key store deleted");
            return true;
        } else {
            Log.d(TAG, "Error deleting key store");
            return false;
        }

    }

}
