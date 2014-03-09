/**
 * CryptoCore.java
 * @author Paul Roper
 */
package com.csulcv.Smessage.test;

import android.test.AndroidTestCase;
import android.util.Log;
import com.csulcv.Smessage.CryptoCore;
import com.csulcv.Smessage.KeyStoreGenerator;
import com.csulcv.Smessage.KeyStoreManager;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.crypto.util.PublicKeyFactory;
import org.spongycastle.util.encoders.Base64;

import java.io.FileInputStream;
import java.security.*;

public class EncryptionTest extends AndroidTestCase {
	
	private static String TAG = "Smessage: Encryption Test";

    private AsymmetricCipherKeyPair rsaKeys = null;
	private byte[] aesKey = null;	
    
    static {    	
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);        	
    }

	public EncryptionTest() {
	}
	
	@Override
	protected void setUp() {

        if (KeyStoreManager.keyStoreExists(getContext())) {

            try {
                getContext().deleteFile(KeyStoreGenerator.KEY_STORE_FILE_NAME);
            } catch (Exception e) {
                e.printStackTrace();
                fail("Key store file not found");
            }

            Log.d(TAG, "Key store deleted");

        }

        rsaKeys = CryptoCore.generateRSAKeyPair();
	    aesKey = CryptoCore.generateAESKey();

	}
	
	@Override
	protected void tearDown() {		
	}

    public void testKeyStore() {

        String password = "TEST";
        String testName = "TEST_NAME";

        // Create a new key store with the password TEST
        KeyStoreGenerator.setupKeyStore(getContext(), testName, password);
        FileInputStream keyStoreFile = null;

        try {
            keyStoreFile = getContext().openFileInput(KeyStoreGenerator.KEY_STORE_FILE_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error opening key store file");
        }

        KeyStore keyStore = null;
        Key storedPrivateKey = null;
        Key storedPublicKey = null;

        // Try and load the key store from the file generated during setup
        try {

            keyStore = KeyStore.getInstance("BKS");
            keyStore.load(keyStoreFile, password.toCharArray());

            // Try loading the self-signed certificate and both RSA keys from the key store
            java.security.cert.Certificate storedCert = keyStore.getCertificate(KeyStoreGenerator.OWN_CERT_ALIAS);
            storedPrivateKey = keyStore.getKey(KeyStoreGenerator.OWN_PRIVATE_KEY_ALIAS, password.toCharArray());
            storedPublicKey = keyStore.getKey(KeyStoreGenerator.OWN_PUBLIC_KEY_ALIAS, password.toCharArray());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error loading from the key store");
        }

        AsymmetricKeyParameter rsaPrivateKey = null;
        AsymmetricKeyParameter rsaPublicKey = null;

        try {
            rsaPrivateKey = new PrivateKeyFactory().createKey(storedPrivateKey.getEncoded());
            rsaPublicKey = new PublicKeyFactory().createKey(storedPublicKey.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error creating AsymmetricKeyParameters");
        }

        // RSA is used for encrypting keys so give it a fake key to encrypt
        String TEST_STRING = "hjMIJC2ixV3RjmAFFhRNuTxI8xdGYHijZJLU5iHPGxN7iYpwnhMtLX1XSBzhhHE";

        try {

            final boolean ENCRYPT = true;

            String encryptedString = CryptoCore.rsa(TEST_STRING, rsaPublicKey, ENCRYPT);
            String decryptedString = CryptoCore.rsa(encryptedString, rsaPrivateKey, !ENCRYPT);

            assertEquals(TEST_STRING, decryptedString);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error running RSA encryption test");
        }

    }

    public void testKeyStoreManager() {

        String password = "TEST";
        String testName = "TEST_NAME";

        // Create a new key store with the password TEST
        KeyStoreGenerator.setupKeyStore(getContext(), testName, password);
        KeyStoreManager keyStoreManager = null;

        try {
            keyStoreManager = new KeyStoreManager(getContext(), password);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error creating a key store manager");
        }

        AsymmetricKeyParameter rsaPrivateKey = null;
        AsymmetricKeyParameter rsaPublicKey = null;

        try {
            rsaPrivateKey = keyStoreManager.getPrivateKey(KeyStoreGenerator.OWN_PRIVATE_KEY_ALIAS);
            rsaPublicKey = keyStoreManager.getPublicKey(KeyStoreGenerator.OWN_PUBLIC_KEY_ALIAS);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error loading keys from the key store");
        }

        // RSA is used for encrypting keys so give it a fake key to encrypt
        String TEST_STRING = "hjMIJC2ixV3RjmAFFhRNuTxI8xdGYHijZJLU5iHPGxN7iYpwnhMtLX1XSBzhhHE";

        try {

            final boolean ENCRYPT = true;

            String encryptedString = CryptoCore.rsa(TEST_STRING, rsaPublicKey, ENCRYPT);
            String decryptedString = CryptoCore.rsa(encryptedString, rsaPrivateKey, !ENCRYPT);

            assertEquals(TEST_STRING, decryptedString);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error running RSA encryption test");
        }

    }

	/**
	 * Test the methods for converting AsymmetricCipherParameter objects into PrivateKey/PublicKey objects and then back
	 * again to make sure that the keys are still valid.
	 */
	public void testRSAKeyDataTypeConversion() {
		
		String TEST_STRING = "hjMIJC2ixV3RjmAFFhRNuTxI8xdGYHijZJLU5iHPGxN7iYpwnhMtLX1XSBzhhHE";
        AsymmetricCipherKeyPair keyPair = CryptoCore.generateRSAKeyPair();
	
		PrivateKey rsaPrivateKey = KeyStoreManager.convertToPrivateKey( (RSAPrivateCrtKeyParameters) keyPair.getPrivate() );
		PublicKey rsaPublicKey = KeyStoreManager.convertToPublicKey( (RSAKeyParameters) keyPair.getPublic() );
		
		AsymmetricKeyParameter convertedRsaPrivateKey = KeyStoreManager.convertToAsymmetricKeyParameter(rsaPrivateKey);
		AsymmetricKeyParameter convertedRsaPublicKey = KeyStoreManager.convertToAsymmetricKeyParameter(rsaPublicKey);

		try {
			
			final boolean ENCRYPT = true;
			
			String encryptedString = CryptoCore.rsa(TEST_STRING, convertedRsaPublicKey, ENCRYPT);
			String decryptedString = CryptoCore.rsa(encryptedString, convertedRsaPrivateKey, !ENCRYPT);
			
			assertEquals(TEST_STRING, decryptedString);			
			
		} catch (Exception e) {
			e.printStackTrace();
            fail("Error running RSA encryption test");
		}		
		
	}
	
	public void testRSAEncryption() {
		
		// RSA is used for encrypting keys so give it a fake key to encrypt
		String TEST_STRING = "hjMIJC2ixV3RjmAFFhRNuTxI8xdGYHijZJLU5iHPGxN7iYpwnhMtLX1XSBzhhHE";
		
		try {
			
			final boolean ENCRYPT = true;
			
			String encryptedString = CryptoCore.rsa(TEST_STRING, rsaKeys.getPublic(), ENCRYPT);
			String decryptedString = CryptoCore.rsa(encryptedString, rsaKeys.getPrivate(), !ENCRYPT);
			
			assertEquals(TEST_STRING, decryptedString);			
			
		} catch (Exception e) {
			e.printStackTrace();
            fail("Error running RSA encryption test");
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
			
			String encryptedString = CryptoCore.aes(TEST_STRING, aesKey, ENCRYPT);
			String decryptedString = CryptoCore.aes(encryptedString, aesKey, !ENCRYPT);
			
			assertEquals(TEST_STRING, decryptedString);
			
		} catch (Exception e) {
			e.printStackTrace();
            fail("Error running AES encryption test");
		}
		
		
	}

    /**
     * RSA is used to encrypt the secret key to send via text. Try encrypting an AES key, sending it across the insecure
     * channel and decrypting it on the other side.
     */
	public void testKeyExchange() {
		
		String message = "This is a message to test key exchange. It is fairly long to test how well AES handles lots"
				+ "of blocks to encrypt.";

		String base64AESKey = new String(Base64.encode(aesKey));
		String decryptedMessage = null;
		
		Log.d(TAG, "The AES key is " + base64AESKey);
		
		try {
			
			final boolean ENCRYPT = true;
			
			// Generate an encrypted message using AES.
			String encryptedMessage = CryptoCore.aes(message, Base64.decode(base64AESKey.getBytes()),
                    ENCRYPT);

			// Use a public key and RSA encryption to produce an encrypted secret key.
			String encryptedKey = CryptoCore.rsa(base64AESKey, rsaKeys.getPublic(), ENCRYPT);
			
			// Decrypt the symmetric key using a private key
			String decryptedKey = CryptoCore.rsa(encryptedKey, rsaKeys.getPrivate(), !ENCRYPT);
			
			// Decrypt the original message
			decryptedMessage = CryptoCore.aes(encryptedMessage,
                    Base64.decode(decryptedKey.getBytes()), !ENCRYPT);
			
		} catch (Exception e) {
			e.printStackTrace();
            fail("Error during key exchange test");
		}		
        
        assertEquals(message, decryptedMessage);
			
	}

}
