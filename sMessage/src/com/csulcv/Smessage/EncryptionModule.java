/**
 * EncryptionModule.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

import org.spongycastle.crypto.AsymmetricBlockCipher;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.encodings.OAEPEncoding;
import org.spongycastle.crypto.engines.RSAEngine;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.RSAKeyGenerationParameters;
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
        
        /* 
         * Use Fermat number 4 (F4) for RSA key generation. From Wikipedia:
         * 
         * "it is famously known to be prime, large enough to avoid the attacks to which small exponents make RSA 
         * vulnerable, and due to its low Hamming weight (number of 1 bits) can be computed extremely quickly on binary
         * computers, which often support shift and increment instructions."
         */
        String PUBLIC_EXPONENT = "65537";
        final int PUBLIC_EXPONENT_BASE = 10;
        final int RSA_STRENGTH = 2048;
        final int CERTAINTY = 88;
 
        keyGen.init(new RSAKeyGenerationParameters(new BigInteger(PUBLIC_EXPONENT, PUBLIC_EXPONENT_BASE), 
                new SecureRandom(), RSA_STRENGTH, CERTAINTY));     
        
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
     * Encrypt a String message using RSA. 
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param message         The message to encrypt.
     * @param key             A public key for encryption.
     * @return                A String containing the encrypted message.
     */
    public static String rsaEncrypt(Context activityContext, String message, CipherParameters key) throws Exception {

        Log.d(TAG, "Message to encrypt: " + message);

        byte[] data = message.getBytes();           
        RSAEngine engine = new RSAEngine();
        final boolean ENCRYPT = true;
                
        // encrypt = true encrypts and encrypt = false decrypts
        engine.init(ENCRYPT, key);        
        int blockSize = engine.getInputBlockSize();
        
        Log.d(TAG, "Block size is " + blockSize + ", data array size is " + data.length);
        
        ArrayList<byte[]> blockList = new ArrayList<byte[]>();        
        
        // Turn message into chunks that are encrypted/decrypted with each iteration
        for (int chunkPos = 0; chunkPos < data.length; chunkPos += blockSize) {   
            
            Log.d(TAG, chunkPos + " / " +  blockSize + " blocks processed.");
            
            //int chunkSize = Math.min(blockSize, data.length - (chunkPos * blockSize));
            int chunkSize = Math.min(blockSize, data.length - chunkPos);
            
            Log.d(TAG, "Chunk size is: " + chunkSize + " min(" + blockSize + ", " 
                    + (data.length - chunkPos) + ")");
            
            blockList.add(engine.processBlock(data, chunkPos, chunkSize));  
        }
                
        // Rebuild the message by concatenating the blocks together into a String
        StringBuilder rsaMessage = new StringBuilder();
        
        for (byte[] block : blockList) {

            Log.d(TAG, "Block: " + Arrays.toString(block));

            // Base 64 is used so that the cipher text can be sent in a human readable form (i.e. using characters from
            // a character set).
            rsaMessage.append(new String(Base64.encode(block)));
                
        }

        Log.d(TAG, "Encrypted message, base 64 " + rsaMessage.toString());
        
        return rsaMessage.toString();        

    }
    
    /** 
     * Encrypt a String message using RSA. 
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param message         The message to decrypt.
     * @param key             A private key for decryption.
     * @return                A String containing the decrypted message.
     */
    public static String rsaDecrypt(Context activityContext, String message, CipherParameters key) throws Exception {
      
        Log.d(TAG, "Message to decrypt: " + message);
        
        // We need to decode the the message from base 64 before it can be decrypted
        byte[] data = Base64.decode(message.getBytes());        
        
        RSAEngine engine = new RSAEngine();
        final boolean ENCRYPT = false;
                
        // encrypt = true encrypts and encrypt = false decrypts
        engine.init(ENCRYPT, key);                   
        int blockSize = engine.getInputBlockSize();

        Log.d(TAG, "Block size is " + blockSize + ", data array size is " + data.length);
        
        ArrayList<byte[]> blockList = new ArrayList<byte[]>();        
        
        // Turn message into chunks that are decrypted with each iteration
        for (int chunkPos = 0; chunkPos < data.length; chunkPos += blockSize) {  
            
            Log.d(TAG, chunkPos + " / " +  blockSize + " blocks processed.");
            
            // As we're working on the message in chunks of 256, we'll need more than one iteration to work on messages
            // longer than this.
            //int chunkSize = Math.min(blockSize, data.length - (chunkPos * blockSize));
            int chunkSize = Math.min(blockSize, data.length - chunkPos);            
            
            Log.d(TAG, "Chunk size is: " + chunkSize + " min(" + blockSize + ", " 
                    + (data.length - chunkPos) + ")");
            
            blockList.add(engine.processBlock(data, chunkPos, chunkSize));  
        }                
        
        // Rebuild the message by concatenating the blocks together into a String
        StringBuilder rsaMessage = new StringBuilder();
        
        for (byte[] block : blockList) {
            Log.d(TAG, "Block: " + Arrays.toString(block));
            rsaMessage.append(new String(block));                
        }

        Log.d(TAG, "Decrypted message: " + rsaMessage.toString());
        
        return rsaMessage.toString();        

    }
    
    /** 
     * Encrypt a String message using RSA. 
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param message         The message to encrypt.
     * @param key             A public key for encryption.
     * @return                A String containing the encrypted message.
     */
    public static void simplerRSAEncrypt(Context activityContext, String message, CipherParameters key) throws Exception {

        // TODO: Try implementing simpler encryption approach
        // http://www.bouncycastle.org/specifications.html
        Log.d(TAG, "Message to encrypt: " + message);
        
        byte[] data = message.getBytes();
        boolean ENCRYPT = true;
        
        AsymmetricBlockCipher engine = new RSAEngine();
        OAEPEncoding cipher = new OAEPEncoding(engine);
        
        
        cipher.init(ENCRYPT, key);
       
        try {
            
        } catch (Exception e) {
            
        }
        
        

    }
    
}
