package com.csulcv.Smessage.test;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.RSAKeyGenerationParameters;

import android.test.AndroidTestCase;
import android.util.Log;

import com.csulcv.Smessage.EncryptionModule;

public class EncryptionModuleTest extends AndroidTestCase {
	
	private String TAG = "Encryption Module Test";
	
	private AsymmetricCipherKeyPair keyPair = null;
	private String TEST_STRING = "This is a test string";
	
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

	}
	
	@Override
	protected void tearDown() {		
	}
	
	public void testRsaEncryption() {
		
		try {
			
			String encryptedString = EncryptionModule.rsaEncrypt(getContext(), TEST_STRING, keyPair.getPublic());		
			System.out.println(encryptedString);
			
			String decryptedString = EncryptionModule.rsaDecrypt(getContext(), encryptedString, keyPair.getPrivate());
			
			assertEquals(TEST_STRING, decryptedString);
			
		} catch (Exception e) {
			Log.e(TAG, "Error running encryption test", e);		
		}
		
		
	}

}
