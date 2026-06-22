package cn.it.cast.keshe.util;

import java.security.SecureRandom;

/**
 * 密码生成器。按长度+字符集生成。
 */
public final class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordGenerator() {}

    public static String generate(int length, boolean upper, boolean lower, boolean numbers, boolean symbols) {
        StringBuilder charset = new StringBuilder();
        if (upper) charset.append(UPPER);
        if (lower) charset.append(LOWER);
        if (numbers) charset.append(DIGITS);
        if (symbols) charset.append(SYMBOLS);

        if (charset.length() == 0) return "";

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(charset.charAt(RNG.nextInt(charset.length())));
        }
        return sb.toString();
    }
}
