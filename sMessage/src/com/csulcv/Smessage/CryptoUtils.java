package com.csulcv.Smessage;


import android.util.Log;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.RSAKeyParameters;
import org.spongycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.spongycastle.crypto.util.PrivateKeyFactory;
import org.spongycastle.crypto.util.PublicKeyFactory;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class CryptoUtils {

    private static final String TAG = "Smessage: CryptoUtils";

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

}
