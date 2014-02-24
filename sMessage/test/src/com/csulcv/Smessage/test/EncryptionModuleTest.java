/**
 * EncryptionModule.java
 * @author Paul Roper
 */
package com.csulcv.Smessage.test;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.KeyGenerationParameters;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.RSAKeyGenerationParameters;
import org.spongycastle.util.encoders.Base64;

import android.test.AndroidTestCase;
import android.util.Log;

import com.csulcv.Smessage.EncryptionModule;

public class EncryptionModuleTest extends AndroidTestCase {
	
	private String TAG = "Encryption Module Test";
	
	private AsymmetricCipherKeyPair keyPair = null;
	private byte[] aesKey = null;
		
	public EncryptionModuleTest() {		
	}
	
	@Override
	protected void setUp() {
				
		RSAKeyPairGenerator keyGen = new RSAKeyPairGenerator();

        String PUBLIC_EXPONENT = "65537";
        int PUBLIC_EXPONENT_BASE = 10;
	    int RSA_STRENGTH = 2048;
        int CERTAINTY = 88;
		
		/*
		 * The BigInteger uses Fermat number 4 (2^(2^4)+1). From Wikipedia:
		 * 
		 * "...it is famously known to be prime, large enough to avoid the attacks to which small exponents make RSA 
		 * vulnerable, and due to its low Hamming weight (number of 1 bits) can be computed extremely quickly on binary
		 * computers, which often support shift and increment instructions."
		 * 
		 * Certainty value obtained from an equivalent symmetric key size for an asymmetric key size of 2048 bits. Tables
		 * available from: http://www.keylength.com/en/compare/
		 */
		keyGen.init(new RSAKeyGenerationParameters(new BigInteger(PUBLIC_EXPONENT, PUBLIC_EXPONENT_BASE), 
				new SecureRandom(), RSA_STRENGTH, CERTAINTY));    
		
		keyPair = keyGen.generateKeyPair();
		
		CipherKeyGenerator aesKeyGen = new CipherKeyGenerator();
		int AES_STRENGTH = 256;
		
		aesKeyGen.init(new KeyGenerationParameters(new SecureRandom(), AES_STRENGTH));
		aesKey = aesKeyGen.generateKey();

	}
	
	@Override
	protected void tearDown() {		
	}
	
	public void testPrivateKeyCasting() {
	    
	    String algorithm = EncryptionModule.generateAsymmetricKeys(getContext(), 256);
	    Log.d(TAG, algorithm);
	    
	}
	
	public void testRSAEncryption() {
		
		String TEST_STRING = "hjMIJC2ixV3RjmAFFhRNuTxI8xdGYHijZJLU5iHPGxN7iYpwnhMtLX1XSBzhhHE";
		
		try {
			
			final boolean ENCRYPT = true;
			
			String encryptedString = EncryptionModule.rsa(getContext(), TEST_STRING, keyPair.getPublic(), ENCRYPT);
			String decryptedString = EncryptionModule.rsa(getContext(), encryptedString, keyPair.getPrivate(), !ENCRYPT);
			
			assertEquals(TEST_STRING, decryptedString);			
			
		} catch (Exception e) {
			Log.e(TAG, "Error running ESA encryption test", e);
		}
		
	}

	public void testAESEncryption() {
		
		String TEST_STRING = "The lorem ipsum dolor sit amet, nonummy ligula volutpat hac integer nonummy. Suspendisse "
				+ "ultricies, congue etiam tellus, erat libero, nulla eleifend, mauris pellentesque. Suspendisse integer"
				+ " praesent vel, integer gravida mauris, fringilla vehicula lacinia non" + "ultricies, congue etiam tellus, "
				+ "erat libero, nulla eleifend, mauris pellentesque. Suspendisse integer"
				+ " praesent vel, integer gravida mauris, fringilla vehicula lacinia non";
		
		try {
			
			final boolean ENCRYPT = true;
			
			String encryptedString = EncryptionModule.aes(getContext(), TEST_STRING, aesKey, ENCRYPT);
			String decryptedString = EncryptionModule.aes(getContext(), encryptedString, aesKey, !ENCRYPT);
			
			assertEquals(TEST_STRING, decryptedString);
			
		} catch (Exception e) {
			Log.e(TAG, "Error running AES encryption test", e);
		}
		
		
	}
	
	public void testKeyExchange() {
		
		String message = "This is a message to test key exchange. It is fairly long to test how well AES handles lots"
				+ "of blocks to encrypt.";

		final int AES_STRENGTH = 256;
		String aesKey = new String(Base64.encode(EncryptionModule.generateSymmetricKey(getContext(), AES_STRENGTH)));
		
		Log.d(TAG, "The AES key is " + aesKey);
		
		try {
			
			final boolean ENCRYPT = true;
			
			// Generate an encrypted message using AES.
			String encryptedMessage = EncryptionModule.aes(getContext(), message, Base64.decode(aesKey.getBytes()), 
					ENCRYPT); 

			// Use a public key and RSA encryption to produce an encrypted secret key.
			String encryptedKey = EncryptionModule.rsa(getContext(), aesKey, keyPair.getPublic(), ENCRYPT);		
			
			// Decrypt the symmetric key using a private key
			String decryptedKey = EncryptionModule.rsa(getContext(), encryptedKey, keyPair.getPrivate(), !ENCRYPT);
			
			// Decrypt the original message
			String decryptedMessage = EncryptionModule.aes(getContext(), encryptedMessage, 
					Base64.decode(decryptedKey.getBytes()), !ENCRYPT);
			
			assertEquals(message, decryptedMessage);
			
		} catch (Exception e) {
			Log.e(TAG, "Error during key exchange test", e);
		}
			
	}

}
