/**
 * EncryptionModule.java
 * @author Paul Roper
 * 
 * 
 */
package com.csulcv.Smessage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.KeyGenerationParameters;
import org.spongycastle.crypto.engines.RSAEngine;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;

import android.content.Context;
import android.util.Log;

public class EncryptionModule {
    
    private static String TAG = "EncryptionModule";

    /**
     * Used to generate the private/public key pair for the user.
     */
    public static void generatePublicKey(Context activityContext, int keySizeInBits) {
        
        RSAKeyPairGenerator keyGen = new RSAKeyPairGenerator();
        String publicKeyFileName = "rsa_public_key";
        String privateKeyFileName = "rsa_private_key";
        
        keyGen.init(new KeyGenerationParameters(new SecureRandom(), keySizeInBits));        
        AsymmetricCipherKeyPair keyPair = keyGen.generateKeyPair();
               
        try {
            
            // Open a couple of private files to write keys to
            FileOutputStream privateKeyStream = activityContext.openFileOutput(publicKeyFileName, Context.MODE_PRIVATE);
            FileOutputStream publicKeyStream = activityContext.openFileOutput(privateKeyFileName, Context.MODE_PRIVATE);
            
            // Get the keys to write and write them in to the file
            publicKeyStream.write(keyPair.getPublic().toString().getBytes());
            privateKeyStream.write(keyPair.getPrivate().toString().getBytes());            
            
            publicKeyStream.close();
            privateKeyStream.close();
            
        } catch (IOException exception) {
            Log.e(TAG, "Error opening key file");            
        }        

    }    
    
    public static void encryptStringRSA(Context activityContext, byte[] data, AsymmetricKeyParameter key) {
        
        RSAEngine rsa = new RSAEngine();
        rsa.init(true, key);
        
        int blockSize = rsa.getInputBlockSize();

    }
    
}
