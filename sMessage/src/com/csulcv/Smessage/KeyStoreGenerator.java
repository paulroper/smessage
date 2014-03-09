package com.csulcv.Smessage;

import android.content.Context;
import android.util.Log;
import org.spongycastle.cert.X509v1CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

public class KeyStoreGenerator {

    private static final String TAG = "Smessage: KeyStoreGenerator";

    public static final String KEY_STORE_FILE_NAME = "keyStore.bks";
    public static final String OWN_PRIVATE_KEY_ALIAS = "OwnPrivateKey";
    public static final String OWN_PUBLIC_KEY_ALIAS = "OwnPublicKey";
    public static final String OWN_CERT_ALIAS = "OwnCert";

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
        PrivateKey rsaPrivateKey = CryptoUtils.convertToPrivateKey((RSAPrivateCrtKeyParameters) keyPair.getPrivate());
        PublicKey rsaPublicKey = CryptoUtils.convertToPublicKey((RSAKeyParameters) keyPair.getPublic());

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

}
