package com.example.mhqltt;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
public class AES {
    public static byte[] encrypt(byte[] data, String password) throws Exception {
        SecretKeySpec keySpec = getKey(password);
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] encryptedData, String password) throws Exception {
        SecretKeySpec keySpec = getKey(password);
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(encryptedData);
    }

    private static SecretKeySpec getKey(String password) throws NoSuchAlgorithmException {
        byte[] key = password.getBytes();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();
        byte[] encoded = secretKey.getEncoded();
        return new SecretKeySpec(key, "AES");
    }


}
