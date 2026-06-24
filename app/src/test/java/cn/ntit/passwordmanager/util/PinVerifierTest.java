package cn.ntit.passwordmanager.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinVerifierTest {

    @Test
    public void verifyReturnsFalseWhenHashOrSaltMissing() {
        assertFalse(PinVerifier.verify("123456", null));
        assertFalse(PinVerifier.verify("123456", ""));
        assertFalse(PinVerifier.verify("123456", "hash"));
        assertFalse(PinVerifier.verify("123456", "hash:"));
        assertFalse(PinVerifier.verify("123456", ":salt"));
    }

    @Test
    public void verifyAcceptsOnlyMatchingPinHash() {
        String[] hash = CryptoUtil.hashPassword("248613");
        String storedPinHash = hash[0] + ":" + hash[1];

        assertTrue(PinVerifier.verify("248613", storedPinHash));
        assertFalse(PinVerifier.verify("123456", storedPinHash));
    }

    @Test
    public void verifyReturnsFalseForMalformedHashData() {
        assertFalse(PinVerifier.verify("248613", "not-base64:also-not-base64"));
    }
}
