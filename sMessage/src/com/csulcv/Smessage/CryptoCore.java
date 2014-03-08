/**
 * CryptoCore.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

import android.util.Log;
import org.spongycastle.crypto.*;
import org.spongycastle.crypto.encodings.OAEPEncoding;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.engines.RSAEngine;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PKCS7Padding;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.RSAKeyGenerationParameters;
import org.spongycastle.util.encoders.Base64;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoCore {
    
    private static final String TAG = "Smessage: CryptoCore";

    /**
     * Generate a 2048 bit RSA key pair consisting of a public and a private key.
     *
     * @return An AsymmetricCipherKeyPair consisting of the public and private keys.
     */
    public static AsymmetricCipherKeyPair generateRSAKeyPair() {

        RSAKeyPairGenerator keyGen = new RSAKeyPairGenerator();

        /*
         * Use Fermat number 4 (F4) for RSA key generation. From Wikipedia:
         *
         * "it is famously known to be prime, large enough to avoid the attacks to which small exponents make RSA
         * vulnerable, and due to its low Hamming weight (number of 1 bits) can be computed extremely quickly on binary
         * computers, which often support shift and increment instructions."
         */
        final String PUBLIC_EXPONENT = "65537";
        final int PUBLIC_EXPONENT_BASE = 10;
        final int RSA_STRENGTH = 2048;
        final int CERTAINTY = 88;

        keyGen.init(new RSAKeyGenerationParameters(new BigInteger(PUBLIC_EXPONENT, PUBLIC_EXPONENT_BASE),
                new SecureRandom(), RSA_STRENGTH, CERTAINTY));

        return keyGen.generateKeyPair();

    }
    
    /**
     * Generate a new 256 bit symmetric key used for AES message encryption.
     * 
     * @return                A byte[] array containing a symmetric key ready to be encrypted and sent to the recipient.
     */
    public static byte[] generateAESKey() {

        final int KEY_SIZE = 256;

        CipherKeyGenerator aesKeyGen = new CipherKeyGenerator();
        aesKeyGen.init(new KeyGenerationParameters(new SecureRandom(), KEY_SIZE));

        return aesKeyGen.generateKey();
        
    }

    /** 
     * Encrypt/decrypt a String message using RSA. In the context of the application, this is used to encrypt the
     * key used by another encryption method.
     * 
     * See Q5: http://www.bouncycastle.org/wiki/display/JA1/Frequently+Asked+Questions
     * 
     * @param message         The message to encrypt.
     * @param key             A secret key for encryption.
     * @param encrypt         True to encrypt a message, false to decrypt a message.
     * @return                A String containing the encrypted message.
     */
    public static String rsa(String message, CipherParameters key, boolean encrypt) throws Exception {
        
        Log.d(TAG, "Message is: " + message);
        
        // Set up the cipher: RSA/OAEP 
        AsymmetricBlockCipher engine = new RSAEngine();
        BufferedAsymmetricBlockCipher cipher = new BufferedAsymmetricBlockCipher(new OAEPEncoding(engine));
        
        byte[] input;

        // Convert the input String into bytes. Decode from base 64 if we're decrypting.
        if (encrypt) {
            input = message.getBytes(Charset.defaultCharset().displayName());
        } else {
            input = Base64.decode(message.getBytes(Charset.defaultCharset().displayName()));
        }
            
        cipher.init(encrypt, key);
        byte[] output = new byte[cipher.getOutputBlockSize()];
        
        Log.d(TAG, "Max RSA block size is " + cipher.getOutputBlockSize());
        
        /*
         * We don't need this but the processBytes method requires it. It tells the cipher where to start processing
         * the input array.
         */
        final int INPUT_OFFSET = 0;
        
        // Process the message 
        cipher.processBytes(input, INPUT_OFFSET, input.length);

        try {
            output = cipher.doFinal();
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
        
        /*
         * Encode the message in base 64 so that it's human readable or decode it if we're dealing with a decrypted
         * message.
         */
        if (encrypt) {
            return new String(Base64.encode(output), Charset.defaultCharset().displayName());
        } else {
            return new String(output, Charset.defaultCharset().displayName());
        }

    }
    
    /** 
     * Encrypt/decrypt a String message using AES. 
     *
     * @param message         The message to encrypt.
     * @param key             A secret key for encryption.
     * @param encrypt         True to encrypt a message, false to decrypt a message.
     * @return                A String containing the encrypted message.
     */
    public static String aes(String message, byte[] key, boolean encrypt) throws Exception {
        
        Log.d(TAG, "Message is: " + message);
        
        // Set up the cipher: AES/CBC/PKCS7 
        // CBC = Cipher-block Chaining
        BlockCipher engine = new AESEngine();
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(engine), new PKCS7Padding());
        byte[] input = null;

        // Convert the input String into bytes. Decode from base 64 if we're decrypting.
        if (encrypt) {
            input = message.getBytes(Charset.defaultCharset().displayName());
        } else {
            input = Base64.decode(message.getBytes(Charset.defaultCharset().displayName()));
        }
            
        cipher.init(encrypt, new KeyParameter(key));
        byte[] output = new byte[cipher.getOutputSize(input.length)];
        
        Log.d(TAG, "Cipher text array size is " + cipher.getOutputSize(input.length));
        
        // We don't need these but the processBytes method requires them. It tells the cipher where to start processing
        // the input array and where to store the processed block in the output array
        final int INPUT_OFFSET = 0;
        final int OUTPUT_OFFSET = 0;
        
        // Process the message 
        int outputLength = cipher.processBytes(input, INPUT_OFFSET, input.length, output, OUTPUT_OFFSET);
        int finalOutputLength;
        int originalMessageSize = 0;
        byte[] croppedOutput = null;
        
        Log.d(TAG, "processBytes: Processed " + outputLength + " bytes.");
        
        try {
            finalOutputLength = cipher.doFinal(output, outputLength);
            
            if (!encrypt) {
                
                /*
                 * Strip padding from the message by first getting the length of the original message sent and then 
                 * creating a new array of that size. Arrays.copyOf() will truncate the array to remove the padding.
                 */ 
                originalMessageSize = outputLength + finalOutputLength;
                
                // output = Arrays.copyOf(output, originalMessageSize); Requires API level 9
                
                // Resize the output array to remove padding, offsets are 0
                croppedOutput = new byte[originalMessageSize];
                System.arraycopy(output, INPUT_OFFSET, croppedOutput, OUTPUT_OFFSET, originalMessageSize);
                
            }
                
            Log.d(TAG, "doFinal: Processed " + finalOutputLength + " bytes.");
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
            return new String(Base64.encode(output), Charset.defaultCharset().displayName());
        } else {
            return new String(croppedOutput, Charset.defaultCharset().displayName());
        }

    }    

}
