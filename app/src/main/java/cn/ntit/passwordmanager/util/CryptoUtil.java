package cn.ntit.passwordmanager.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-GCM 加密 + PBKDF2 密钥派生。
 * 用于凭据密码的加密存储以及主密码/PIN 的哈希校验。
 */
public final class CryptoUtil {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String PBKDF2 = "PBKDF2WithHmacSHA256";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final int PBKDF2_ITERATIONS = 60000;
    private static final int KEY_BITS = 256;

    private CryptoUtil() {}

    // 会话内按 salt 缓存已派生的 AES 密钥，避免重复 PBKDF2。
    // 注：每条凭据的 salt 是独立随机的，所以缓存只对"同一条记录再次解密"生效。
    private static final int KEY_CACHE_MAX = 32;
    private static final Map<String, SecretKey> KEY_CACHE = new LinkedHashMap<String, SecretKey>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SecretKey> eldest) {
            return size() > KEY_CACHE_MAX;
        }
    };

    public static void clearKeyCache() {
        synchronized (KEY_CACHE) {
            KEY_CACHE.clear();
        }
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new SecureRandom().nextBytes(b);
        return b;
    }

    private static SecretKey deriveKey(char[] password, byte[] salt) {
        String cacheKey = Base64.getEncoder().encodeToString(salt);
        synchronized (KEY_CACHE) {
            SecretKey cached = KEY_CACHE.get(cacheKey);
            if (cached != null) return cached;
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2);
            byte[] key = skf.generateSecret(spec).getEncoded();
            SecretKey result = new SecretKeySpec(key, "AES");
            synchronized (KEY_CACHE) {
                KEY_CACHE.put(cacheKey, result);
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("密钥派生失败", e);
        }
    }

    /** 使用主密码加密明文。输出格式：base64(iv || ciphertext) */
    public static String encrypt(String plaintext, char[] masterPassword) {
        if (plaintext == null) plaintext = "";
        try {
            byte[] salt = randomBytes(16);
            SecretKey key = deriveKey(masterPassword, salt);
            byte[] iv = randomBytes(GCM_IV_BYTES);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[salt.length + iv.length + ct.length];
            System.arraycopy(salt, 0, out, 0, salt.length);
            System.arraycopy(iv, 0, out, salt.length, iv.length);
            System.arraycopy(ct, 0, out, salt.length + iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    public static String decrypt(String payloadBase64, char[] masterPassword) {
        if (payloadBase64 == null || payloadBase64.isEmpty()) return "";
        try {
            byte[] all = Base64.getDecoder().decode(payloadBase64);
            byte[] salt = Arrays.copyOfRange(all, 0, 16);
            byte[] iv = Arrays.copyOfRange(all, 16, 16 + GCM_IV_BYTES);
            byte[] ct = Arrays.copyOfRange(all, 16 + GCM_IV_BYTES, all.length);
            SecretKey key = deriveKey(masterPassword, salt);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败：主密码可能不正确", e);
        }
    }

    /** 计算密码/PIN 的 PBKDF2 哈希（base64）。salt 也返回。 */
    public static String[] hashPassword(String password) {
        byte[] salt = randomBytes(16);
        String hash = hashWithSalt(password, salt);
        return new String[]{hash, Base64.getEncoder().encodeToString(salt)};
    }

    public static String hashWithSalt(String password, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        return hashWithSalt(password, salt);
    }

    private static String hashWithSalt(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("哈希失败", e);
        }
    }

    public static boolean verifyPassword(String password, String hashBase64, String saltBase64) {
        String computed = hashWithSalt(password, saltBase64);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                hashBase64.getBytes(StandardCharsets.UTF_8)
        );
    }
}
