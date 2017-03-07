package com.fsck.k9.preferences;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * This class is used to create a hash of the master password to avoid saving the raw password.
 * <p>
 * Created by da-mkay on 27.02.17.
 */

public class PasswordHash {

    private final static int BASE64_FLAGS = Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE;

    /**
     * Hash the password using the number of iterations, the keyLength and a
     * given salt.
     *
     * @param password
     * @param iterationCount
     * @param keyLength
     * @param saltBytes
     * @return
     */
    public static byte[] hash(String password, int iterationCount, int keyLength, byte[] saltBytes) {
        try {
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), saltBytes, iterationCount, keyLength);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return keyFactory.generateSecret(keySpec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException exc) {
            return null;
        }
    }

    /**
     * Hash the password and return a string of the format "iterations$salt$hash"
     * where salt and hash are Base64-encoded.
     *
     * @param password
     * @param iterationCount
     * @param keyLength
     * @param saltLength
     * @return
     */
    public static String generateHashString(String password, int iterationCount, int keyLength, int saltLength) {
        byte[] saltBytes = new byte[saltLength / 8];
        SecureRandom random = new SecureRandom();
        random.nextBytes(saltBytes);
        byte[] keyBytes = hash(password, iterationCount, keyLength, saltBytes);
        String hPass = Base64.encodeToString(keyBytes, BASE64_FLAGS);
        String hSalt = Base64.encodeToString(saltBytes, BASE64_FLAGS);
        return iterationCount + "$" + hSalt + "$" + hPass;
    }

    /**
     * Same as {@link #generateHashString(String, int, int, int)} with a keyLength
     * of 512 bit and a saltLength of 128 bit.
     *
     * @param password
     * @return
     */
    public static String generateHashString(String password) {
        return generateHashString(password, 10000, 512, 128);
    }

    public static boolean validatePassword(String hashString, String password) {
        String[] hashData = hashString.split("\\$");
        if (hashData.length != 3) {
            return false;
        }
        int iterations = Integer.valueOf(hashData[0]);
        byte[] salt = Base64.decode(hashData[1], BASE64_FLAGS);
        byte[] hash = Base64.decode(hashData[2], BASE64_FLAGS);
        int keyLength = hash.length * 8;
        byte[] hashNew = hash(password, iterations, keyLength, salt);

        // Secure comparison of hashes (time-based attacks)
        int res = hash.length ^ hashNew.length;
        for (int i = 0; i < hash.length && i < hashNew.length; i++) {
            res |= hash[i] ^ hashNew[i];
        }
        return res == 0;
    }
}
