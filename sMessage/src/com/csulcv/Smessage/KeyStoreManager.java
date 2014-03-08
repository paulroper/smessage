package com.csulcv.Smessage;

import android.content.Context;
import android.util.Log;
import org.spongycastle.cert.X509v1CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.crypto.util.PublicKeyFactory;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

public class KeyStoreManager {

    private static final String TAG = "Smessage: KeyStoreManager";

    private static final String KEY_STORE_FILE_NAME = "keyStore.bks";
    private static final String OWN_PRIVATE_KEY_ALIAS = "OwnPrivateKey";
    private static final String OWN_PUBLIC_KEY_ALIAS = "OwnPublicKey";
    private static final String OWN_CERT_ALIAS = "OwnCert";

    /**
     * Generates an RSA public/private key pair and stores them to a local key store unavailable to the user.
     *
     * @param activityContext  The context of the activity that this method was called from.
     * @param certificateName  The name to go on the self-signed certificate.
     * @param keyStorePassword The password to store the key store file with.
     */
    public static void setupKeyStore(Context activityContext, String certificateName, String keyStorePassword) {

        // Generate the user's RSA keys
        AsymmetricCipherKeyPair keyPair = CryptoCore.generateRSAKeyPair();

        // Convert the AsymmetricKeyParameters into JCE format PrivateKey/PublicKey objects
        PrivateKey rsaPrivateKey = convertToPrivateKey( (RSAPrivateCrtKeyParameters) keyPair.getPrivate() );
        PublicKey rsaPublicKey = convertToPublicKey( (RSAKeyParameters) keyPair.getPublic() );

        /*
         * Create a certificate chain consisting of a single self-signed certificate. Essentially a dummy so that we can
         * use a KeyStoreManager.
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

            // Get an empty key store
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null, keyStorePassword.toCharArray());

            // Store the keys using the self-signed certificate then store the certificate itself
            keyStore.setKeyEntry(OWN_PRIVATE_KEY_ALIAS, rsaPrivateKey, keyStorePassword.toCharArray(), certificateChain);
            keyStore.setKeyEntry(OWN_PUBLIC_KEY_ALIAS, rsaPublicKey, keyStorePassword.toCharArray(), certificateChain);
            keyStore.setCertificateEntry(OWN_CERT_ALIAS, certificateChain[0]);

            try {

                // Password protect the key store and save it to a file
                keyStore.store(outputStream, keyStorePassword.toCharArray());

            } catch (Exception e) {
                Log.e(TAG, "Error saving key store to file", e);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error creating key store", e);
        }

    }

    /**
     * Create a self-signed certificate to save keys into the key store.
     *
     * @param rsaPublicKey  The user's RSA public key.
     * @param rsaPrivateKey The user's RSA private key.
     * @return              A self-signed X.509 certificate created using the user's RSA keys.
     */
    private static X509Certificate generateSelfSignedCertificate(String name, PublicKey rsaPublicKey, PrivateKey rsaPrivateKey) {

        ContentSigner signatureGenerator = null;

        try {
            signatureGenerator = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(rsaPrivateKey);
        } catch (Exception e) {
            Log.e(TAG, "Error generating content signer");
        }

        // The certificate expires after one year
        Date certStartDate = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000));
        Date certEndDate = new Date(System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000));

        // Use the provided name to sign the certificate with
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

            // Get the certificate from the certificate builder
            cert = new JcaX509CertificateConverter().setProvider("BC").
                    getCertificate(certificateBuilder.build(signatureGenerator));

        } catch (Exception e) {
            Log.e(TAG, "Error generating X.509 certificate");
        }

        return cert;

    }


    /**
     * Convert an RSA AsymmetricKeyParameter into a PrivateKey so that it can be stored in a local key store.
     *
     * @param rsaPrivateKeyParameters The private key parameters from the asymmetric key pair.
     * @return                        A PrivateKey object.
     */
    public static PrivateKey convertToPrivateKey(RSAPrivateCrtKeyParameters rsaPrivateKeyParameters) {

        // Turn the key parameter into a specification that can be used to build the key
        // The correct "gets" were found from the source code for RSAPrivateCrtParameters:
        // http://dev.telnic.org/trac/browser/apps/blackberry/trunk/blackberry/src/org/bouncycastle/crypto/params/RSAPrivateCrtKeyParameters.java?rev=339
        RSAPrivateCrtKeySpec rsaPrivateKeySpec = new RSAPrivateCrtKeySpec(
                rsaPrivateKeyParameters.getModulus(),
                rsaPrivateKeyParameters.getPublicExponent(),
                rsaPrivateKeyParameters.getExponent(),
                rsaPrivateKeyParameters.getP(),
                rsaPrivateKeyParameters.getQ(),
                rsaPrivateKeyParameters.getDP(),
                rsaPrivateKeyParameters.getDQ(),
                rsaPrivateKeyParameters.getQInv()
        );

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

        /* Turn the key parameter into a specification that can be used to build the key.
         * The correct "gets" were found from the source code for RSAPrivateCrtParameters:
         *  http://dev.telnic.org/trac/browser/apps/blackberry/trunk/blackberry/src/org/bouncycastle/crypto/params/RSAPrivateCrtKeyParameters.java?rev=339
         */
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(   rsaPublicKeyParameters.getModulus(),
                rsaPublicKeyParameters.getExponent() );

        PublicKey rsaPublicKey = null;

        try {

            // Use the Java crypto key factory to generate a JCA PrivateKey from the RSA key specification
            rsaPublicKey = KeyFactory.getInstance("RSA").generatePublic(rsaPublicKeySpec);

        } catch (Exception e) {
            Log.e(TAG, "Error converting RSA key spec into private key");
        }

        return rsaPublicKey;

    }

    /**
     * Turn a Java Cryptography Architecture PrivateKey object into a Bouncy Castle AsymmetricKeyParameter
     *
     * @param publicKey  The public Key to convert.
     * @return           The public key passed in converted to an AsymmetricKeyParameter.
     */
    public static AsymmetricKeyParameter convertToAsymmetricKeyParameter(PublicKey publicKey) {

        AsymmetricKeyParameter convertedKey = null;

        try {
            convertedKey = PublicKeyFactory.createKey(publicKey.getEncoded());
        } catch (IOException e) {
            Log.e(TAG, "Error converting PublicKey to AsymmetricKeyParameter");
        }

        return convertedKey;

    }


    /**
     * Turn a Java Cryptography Architecture PrivateKey object into a Bouncy Castle AsymmetricKeyParameter
     *
     * @param privateKey The private key to convert.
     * @return           The private key passed in converted to an AsymmetricKeyParameter.
     */
    public static AsymmetricKeyParameter convertToAsymmetricKeyParameter(PrivateKey privateKey) {

        AsymmetricKeyParameter convertedKey = null;

        try {
            convertedKey = PrivateKeyFactory.createKey(privateKey.getEncoded());
        } catch (IOException e) {
            Log.e(TAG, "Error converting PublicKey to AsymmetricKeyParameter");
        }

        return convertedKey;

    }

    /**
     * Check whether the key store has already been generated. Used at app startup to decide whether or not to begin
     * key store setup.
     *
     * @return True if the file exists, false otherwise.
     */
    public static boolean keyStoreExists(Context activityContext) {

        File keyStoreFile = activityContext.getFileStreamPath(KEY_STORE_FILE_NAME);
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
