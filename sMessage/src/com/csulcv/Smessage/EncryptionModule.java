/**
 * EncryptionModule.java
 * @author Paul Roper
 */
package com.csulcv.Smessage;

import android.content.Context;
import android.util.Log;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cert.X509v1CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.spongycastle.crypto.*;
import org.spongycastle.crypto.encodings.OAEPEncoding;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.engines.RSAEngine;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PKCS7Padding;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.*;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.crypto.util.PublicKeyFactory;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.util.encoders.Base64;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Date;

public class EncryptionModule {
    
    private static final String TAG = "EncryptionModule";

    private static final String KEY_STORE_FILE_NAME = "keyStore.bks";
    private static final String OWN_PRIVATE_KEY_ALIAS = "OwnPrivateKey";
    private static final String OWN_PUBLIC_KEY_ALIAS = "OwnPublicKey";
    private static final String OWN_CERT_ALIAS = "OwnCert";

    /**
     * Generates an RSA public/private key pair and stores them to a local key store unavailable to the user.
     * 
     * @param activityContext The context of the activity that this method was called from.
     * @param certificateName The name to go on the self-signed certificate.
     * @param keyStorePassword The password to store the key store file with.
     */
    public static void encryptionSetup(Context activityContext, String certificateName, String keyStorePassword) {

        // Generate the user's RSA keys
        AsymmetricCipherKeyPair keyPair = generateRSAKeyPair();

        // Convert the AsymmetricKeyParameters into JCE format PrivateKey/PublicKey objects
        PrivateKey rsaPrivateKey = convertToPrivateKey( (RSAPrivateCrtKeyParameters) keyPair.getPrivate() );
        PublicKey rsaPublicKey = convertToPublicKey( (RSAKeyParameters) keyPair.getPublic() );

        /*
         * Create a certificate chain consisting of a single self-signed certificate. Essentially a dummy so that we can
         * use a KeyStore.
         */
        Certificate[] certificateChain = {generateSelfSignedCertificate(certificateName, rsaPublicKey, rsaPrivateKey)};

        // Create a new keys file that we'll use as the key store
        File file = new File(activityContext.getFilesDir(), KEY_STORE_FILE_NAME);
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error opening file");
        }

        try {
            
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null, keyStorePassword.toCharArray());

            // Store the keys using the self-signed certificate then store the certificate itself
            keyStore.setKeyEntry(OWN_PRIVATE_KEY_ALIAS, rsaPrivateKey, keyStorePassword.toCharArray(), certificateChain);
            keyStore.setKeyEntry(OWN_PUBLIC_KEY_ALIAS, rsaPublicKey, keyStorePassword.toCharArray(), certificateChain);
            keyStore.setCertificateEntry(OWN_CERT_ALIAS, certificateChain[0]);

            try {
                keyStore.store(outputStream, keyStorePassword.toCharArray());
            } catch (Exception e) {
                // TODO: Exception handling
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating key store");
            e.printStackTrace();
        }
        
    }

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
        String PUBLIC_EXPONENT = "65537";
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
    public static byte[] generateSymmetricKey() {

        final int KEY_SIZE = 256;

        CipherKeyGenerator aesKeyGen = new CipherKeyGenerator();
        aesKeyGen.init(new KeyGenerationParameters(new SecureRandom(), KEY_SIZE));

        return aesKeyGen.generateKey();
        
    }

    /**
     * Create a self-signed certificate to save keys into the key store.
     *
     * @param rsaPublicKey The user's RSA public key.
     * @param rsaPrivateKey The user's RSA private key.
     * @return A self-signed X.509 certificate created using the user's RSA keys.
     */
    private static X509Certificate generateSelfSignedCertificate(String name, PublicKey rsaPublicKey, PrivateKey rsaPrivateKey) {

        ContentSigner signatureGenerator = null;

        try {
            signatureGenerator = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(rsaPrivateKey);
        } catch (Exception e) {
            Log.e(TAG, "Error generating content signer");
        }

        // The certificate expires after...
        Date certStartDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date certEndDate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000);

        // Use the user's phone number as the name to sign with
        X500Principal certificateSigner = new X500Principal("CN=" + name);

        X509v1CertificateBuilder certificateBuilder = new JcaX509v1CertificateBuilder(
                certificateSigner,
                BigInteger.ONE,
                certStartDate, certEndDate,
                certificateSigner,
                rsaPublicKey
        );

        X509Certificate cert = null;

        try {
            cert = new JcaX509CertificateConverter().setProvider("BC").
                    getCertificate(certificateBuilder.build(signatureGenerator));
        } catch (Exception e) {
            Log.e(TAG, "Error generating X.509 certificate");
        }

        return cert;

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
        BufferedAsymmetricBlockCipher cipher = 
                new BufferedAsymmetricBlockCipher(new OAEPEncoding(engine));
        
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
        
        // We don't need this but the processBytes method requires it. It tells the cipher where to start processing
        // the input array
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
        
        // Encode the message in base 64 so that it's human readable or decode it if we're dealing with a decrypted
        // message
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
        byte[] outputResized = null;
        
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
                outputResized = new byte[originalMessageSize];
                System.arraycopy(output, INPUT_OFFSET, outputResized, OUTPUT_OFFSET, originalMessageSize);
                
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
            return new String(outputResized, Charset.defaultCharset().displayName());
        }

    }    

    /**
     * Convert an RSA AsymmetricKeyParameter into a PrivateKey so that it's storable in a local key store.
     * 
     * @param rsaPrivateKeyParameters The private key parameters from the asymmetric key pair.
     * @return                        A PrivateKey object.
     */
    public static PrivateKey convertToPrivateKey(RSAPrivateCrtKeyParameters rsaPrivateKeyParameters) {
        
        // Turn the key parameter into a specification that can be used to build the key
        // The correct "gets" were found from the source code for RSAPrivateCrtParameters:
        // http://dev.telnic.org/trac/browser/apps/blackberry/trunk/blackberry/src/org/bouncycastle/crypto/params/RSAPrivateCrtKeyParameters.java?rev=339
        RSAPrivateCrtKeySpec rsaPrivateKeySpec = new RSAPrivateCrtKeySpec(  rsaPrivateKeyParameters.getModulus(), 
                                                                            rsaPrivateKeyParameters.getPublicExponent(), 
                                                                            rsaPrivateKeyParameters.getExponent(), 
                                                                            rsaPrivateKeyParameters.getP(), 
                                                                            rsaPrivateKeyParameters.getQ(), 
                                                                            rsaPrivateKeyParameters.getDP(), 
                                                                            rsaPrivateKeyParameters.getDQ(), 
                                                                            rsaPrivateKeyParameters.getQInv() );
        
        PrivateKey rsaPrivateKey = null;
        
        try {
            
            // Use the Java crypto key factory to generate a PrivateKey from the spec
            rsaPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(rsaPrivateKeySpec);   
        
        } catch (Exception e) {
            Log.e(TAG, "Error converting RSA key spec into private key");
        }
        
        return rsaPrivateKey;
        
    }
    
    /**
     * Convert an RSA AsymmetricKeyParameter into a PublicKey so that it's storable in a local key store.
     * 
     * @param rsaPublicKeyParameters The public key parameters from the asymmetric key pair.
     * @return                       A PublicKey object.
     */
    public static PublicKey convertToPublicKey(RSAKeyParameters rsaPublicKeyParameters) {
        
        // Turn the key parameter into a specification that can be used to build the key
        // The correct "gets" were found from the source code for RSAPrivateCrtParameters:
        // http://dev.telnic.org/trac/browser/apps/blackberry/trunk/blackberry/src/org/bouncycastle/crypto/params/RSAPrivateCrtKeyParameters.java?rev=339
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(   rsaPublicKeyParameters.getModulus(), 
                                                                    rsaPublicKeyParameters.getExponent() );
        
        PublicKey rsaPublicKey = null;
        
        try {
            
            // Use the Java crypto key factory to generate a PrivateKey from the spec
            rsaPublicKey = KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec);   
        
        } catch (Exception e) {
            Log.e(TAG, "Error converting RSA key spec into private key");
        }
        
        return rsaPublicKey;
        
    }

    public static AsymmetricKeyParameter convertToAsymmetricKeyParameter(PublicKey key) {

        AsymmetricKeyParameter convertedKey = null;

        try {
            convertedKey = PublicKeyFactory.createKey(key.getEncoded());
        } catch (IOException e) {
            Log.e(TAG, "Error converting PublicKey to AsymmetricKeyParameter");
        }

        return convertedKey;

    }


    public static AsymmetricKeyParameter convertToAsymmetricKeyParameter(PrivateKey key) {

        AsymmetricKeyParameter convertedKey = null;

        try {
            convertedKey = PrivateKeyFactory.createKey(key.getEncoded());
        } catch (IOException e) {
            Log.e(TAG, "Error converting PublicKey to AsymmetricKeyParameter");
        }

        return convertedKey;

    }

    /**
     * Check whether the key store has been generated.
     *
     * @return True if the file exist, false otherwise.
     */
    public static boolean keyStoreExists(Context activityContext) {

        File keyStoreFile = activityContext.getFileStreamPath(EncryptionModule.getKeyStoreFileName());
        return keyStoreFile.exists();

    }

    public static String getKeyStoreFileName() {
        return KEY_STORE_FILE_NAME;
    }

    public static String getOwnPrivateKeyAlias() {
        return OWN_PRIVATE_KEY_ALIAS;
    }

    public static String getOwnPublicKeyAlias() {
        return OWN_PUBLIC_KEY_ALIAS;
    }

    public static String getOwnCertAlias() {
        return OWN_CERT_ALIAS;
    }
    
}
