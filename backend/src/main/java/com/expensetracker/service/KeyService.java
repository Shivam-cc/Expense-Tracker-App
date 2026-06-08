package com.expensetracker.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

@Service
public class KeyService {

    private final KeyPair keyPair;

    public KeyService() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048, new SecureRandom());
        this.keyPair = gen.generateKeyPair();
    }

    /** Returns the RSA public key as Base64-encoded DER (SPKI format). */
    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /**
     * Decrypts an RSA-OAEP (SHA-256 / MGF1-SHA-256) encrypted Base64 string.
     * Matches Web Crypto API: { name: 'RSA-OAEP', hash: 'SHA-256' }
     */
    public String decrypt(String encryptedBase64) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        OAEPParameterSpec spec = new OAEPParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT
        );
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), spec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
