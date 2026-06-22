package cn.it.cast.keshe.util;

public final class PinVerifier {

    private static final String HASH_SEPARATOR = ":";

    private PinVerifier() {}

    public static boolean verify(String pin, String storedPinHash) {
        if (isBlank(pin) || isBlank(storedPinHash)) {
            return false;
        }

        int separator = storedPinHash.indexOf(HASH_SEPARATOR);
        if (separator <= 0
                || separator >= storedPinHash.length() - 1
                || separator != storedPinHash.lastIndexOf(HASH_SEPARATOR)) {
            return false;
        }

        String hash = storedPinHash.substring(0, separator);
        String salt = storedPinHash.substring(separator + 1);
        if (isBlank(hash) || isBlank(salt)) {
            return false;
        }

        try {
            return CryptoUtil.verifyPassword(pin, hash, salt);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
