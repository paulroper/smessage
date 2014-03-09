package com.csulcv.Smessage;

import android.content.Context;
import android.util.Log;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.crypto.util.PublicKeyFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class KeyStoreManager {

    private static final String TAG = "Smessage: KeyStoreManager";

    private String keyStorePassword = null;
    private KeyStore keyStore = null;
    private Certificate[] certificateChain = null;

    /**
     * Setup a KeyStoreManager object to control the addition and removal of keys from the key store. Loads the key store
     * and the user's self-signed certificate.
     *
     * @param activityContext The activity that the object was instantiated from.
     * @param password        The user's key store password.
     * @throws Exception
     */
    public KeyStoreManager(Context activityContext, String password) throws Exception {

        keyStorePassword = password;

        FileInputStream keyStoreFile = activityContext.openFileInput(KeyStoreGenerator.KEY_STORE_FILE_NAME);

        keyStore = KeyStore.getInstance("BKS");
        keyStore.load(keyStoreFile, password.toCharArray());

        certificateChain = new Certificate[] {keyStore.getCertificate(KeyStoreGenerator.OWN_CERT_ALIAS)};

    }

    public void addPublicKey(String alias, PublicKey publicKey) throws Exception {
        keyStore.setKeyEntry(alias, publicKey, keyStorePassword.toCharArray(), certificateChain);
    }

    public void addPrivateKey(String alias, PrivateKey privateKey) throws Exception {
        keyStore.setKeyEntry(alias, privateKey, keyStorePassword.toCharArray(), certificateChain);
    }

    public void addSecretKey(String alias, byte[] secretKey) throws Exception {
        keyStore.setKeyEntry(alias, secretKey, certificateChain);
    }

    public AsymmetricKeyParameter getPublicKey(String alias) throws Exception {

        Key storedPublicKey = keyStore.getKey(alias, keyStorePassword.toCharArray());
        AsymmetricKeyParameter publicKey = new PublicKeyFactory().createKey(storedPublicKey.getEncoded());

        if (publicKey.isPrivate()) {
            throw new Exception("The alias given does not correspond to a public key");
        } else {
            return publicKey;
        }

    }

    public AsymmetricKeyParameter getPrivateKey(String alias) throws Exception {

        Key storedPrivateKey = keyStore.getKey(alias, keyStorePassword.toCharArray());
        AsymmetricKeyParameter privateKey = new PrivateKeyFactory().createKey(storedPrivateKey.getEncoded());

        if (!privateKey.isPrivate()) {
            throw new Exception("The alias given does not correspond to a private key");
        } else {
            return privateKey;
        }

    }

    public byte[] getSecretKey(String alias) throws Exception {

        return keyStore.getKey(alias, keyStorePassword.toCharArray()).getEncoded();

    }

    /**
     * Check whether a key associated with the given alias exists.
     */
    public boolean keyExists(String alias) throws KeyStoreException {

        if (keyStore.isKeyEntry(alias)) {
            return true;
        } else {
            return false;
        }

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

}
