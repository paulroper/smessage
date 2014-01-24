/**
 * EncryptionModule.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.KeyGenerationParameters;
import org.spongycastle.crypto.engines.RSAEngine;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.util.encoders.Base64;

import android.content.Context;
import android.util.Log;

public class EncryptionModule {
    
    private static String TAG = "EncryptionModule";

    /**
     * Used to generate the private/public key pair for the user.
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param keySizeInBits   The size of the key to generate.
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
    
    /** 
     * Encrypt/decrypt a String message using RSA. 
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param message         The message to encrypt/decrypt.
     * @param key             A public key for encryption or private key for decryption.
     * @param encrypt         True to encrypt a message, false to decrypt one. 
     * @return                A String containing the encrypted/decrypted message.
     */
    public static String rsa(Context activityContext, String message, AsymmetricKeyParameter key, boolean encrypt) {
        
        Log.d(TAG, "Message to encrypt: " + message);
        
        // TODO: Explain reasons for base 64 encoding
        byte[] data = Base64.encode(message.getBytes());        
        RSAEngine engine = new RSAEngine();
        
        Log.d(TAG, "Base64 encoded message " + Arrays.toString(data));
        
        // encrypt = true encrypts and encrypt = false decrypts
        engine.init(encrypt, key);        
        int blockSize = engine.getInputBlockSize();
        
        ArrayList<byte[]> blockList = new ArrayList<byte[]>();        
        
        // Turn message into chunks that are encrypted/decrypted with each iteration
        for (int chunkPos = 0; chunkPos < data.length; chunkPos += blockSize) {             
            int chunkSize = Math.min(blockSize, data.length - (chunkPos * blockSize));
            blockList.add(engine.processBlock(data, chunkPos, chunkSize));  
        }
                
        // Rebuild the message by concatenating the blocks together into a String
        StringBuilder rsaMessage = new StringBuilder();
        
        for (byte[] block : blockList) {
            rsaMessage.append(block.toString());            
        }
        
        Log.d(TAG, "RSA'd message " + rsaMessage.toString());
        
        return rsaMessage.toString();        

    }
    
}
