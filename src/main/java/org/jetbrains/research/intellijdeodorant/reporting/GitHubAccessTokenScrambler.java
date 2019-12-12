package org.jetbrains.research.intellijdeodorant.reporting;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Provides functionality to decode secret token.
 */
class GitHubAccessTokenScrambler {
    private static final String myInitVector = "RandomInitVector";
    private static final String myKey = "GitHubErrorToken";

    static String decrypt(InputStream inputStream) throws Exception {
        String in;
        final ObjectInputStream o = new ObjectInputStream(inputStream);
        in = (String) o.readObject();
        IvParameterSpec iv = new IvParameterSpec(myInitVector.getBytes(StandardCharsets.UTF_8));
        SecretKeySpec keySpec = new SecretKeySpec(myKey.getBytes(StandardCharsets.UTF_8), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

        byte[] original = cipher.doFinal(Base64.decodeBase64(in));
        return new String(original);
    }
}