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

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.CryptoException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.engines.RSAEngine;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
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
            Log.d(TAG, "Block: " + new String(block));
            rsaMessage.append(new String(block));                
        }

        Log.d(TAG, "Decrypted message: " + rsaMessage.toString());
        
        return rsaMessage.toString();        

    }
    
    /** 
     * Encrypt/decrypt a String message using AES. 
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param message         The message to encrypt.
     * @param key             A secret key for encryption.
     * @param encrypt         True to encrypt a message, false to decrypt a message.
     * @return                A String containing the encrypted message.
     */
    public static String aes(Context activityContext, String message, byte[] key, boolean encrypt) throws Exception {
        
        Log.d(TAG, "Message is: " + message);
        
        // Set up the cipher: AES/CBC/PKCS7 
        // CBC = Cipher-block Chaining
        BlockCipher engine = new AESEngine();
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(engine));
        byte[] input = null;

        // Convert the input String into bytes
        if (encrypt) {
            input = message.getBytes();
        } else {
            input = Base64.decode(message.getBytes());
        }
            
        cipher.init(encrypt, new KeyParameter(key));
        byte[] output = new byte[cipher.getOutputSize(input.length)];
        
        Log.d(TAG, "Cipher text array size is " + cipher.getOutputSize(input.length));
        
        // We don't need these but the processBytes method requires them. It tells the cipher where to start processing
        // the input array and where to store the processed block in the output array
        final int INPUT_OFFSET = 0;
        final int OUTPUT_OFFSET = 0;
        
        // Process the message 
        int outputLen = cipher.processBytes(input, INPUT_OFFSET, input.length, output, OUTPUT_OFFSET);
        
        try {
            int finalOutputBytes = cipher.doFinal(output, outputLen);
            Log.d(TAG, "Processed " + finalOutputBytes + " bytes.");
        } catch (CryptoException ce) {
            Log.e(TAG, "Error encrypting message", ce);
        }
        
        if (encrypt) {            
            Log.d(TAG, "Output bytes are " + Arrays.toString(output));
            Log.d(TAG, "Output is " + new String(Base64.encode(output)));
        } else {            
            Log.d(TAG, "Output bytes are " + Arrays.toString(output));
            Log.d(TAG, "Output is " + new String(output));
        }
        
        // Encode the message in base 64 so that it's human readable or decode it if we're dealing with a decrypted
        // message
        if (encrypt) {
            return new String(Base64.encode(output));
        } else {
            // TODO: Why does PCKS7 keep padding with 0s?
            return new String(stripPCKS7Padding(output));
        }

    }
    
    /**
     * Removes PCKS7 padding from a byte array. The final byte of the input array using this padding scheme indicates
     * how many bytes were added for padding.
     * 
     * @param input The array of bytes to remove padding from.
     * @return      A byte array containing the input bytes without padding.
     */
    public static byte[] stripPCKS7Padding(byte[] input) {
        
        // TODO: Check that PCKS7 padding has actually been used
        
        // Get indicator value for number of bytes to strip
        int bytesToStrip = input[input.length - 1];
        
        // If 0 bytes were added for padding, we still need to remove the indicator byte from the array
        if (bytesToStrip == 0) {
            bytesToStrip = 1;
        }
        
        Log.d(TAG, "Bytes to strip: " + bytesToStrip);
        
        return Arrays.copyOf(input, (input.length - bytesToStrip));
        
    }
    
}
