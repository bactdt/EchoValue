package cn.ntit.passwordmanager.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.UUID;

public final class ClipboardUtil {

    private static final long PASSWORD_CLEAR_DELAY_MS = 30_000L;
    private static final String PASSWORD_LABEL_PREFIX = "vault-password-";

    private ClipboardUtil() {}

    public static void copyPassword(Context context, String text) {
        String value = text == null ? "" : text;
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;

        String label = PASSWORD_LABEL_PREFIX + UUID.randomUUID();
        cm.setPrimaryClip(ClipData.newPlainText(label, value));

        new Handler(Looper.getMainLooper()).postDelayed(() -> clearIfStillOwned(cm, label, value),
                PASSWORD_CLEAR_DELAY_MS);
    }

    private static void clearIfStillOwned(ClipboardManager cm, String expectedLabel, String expectedText) {
        ClipData current = cm.getPrimaryClip();
        if (current == null || current.getItemCount() == 0 || current.getDescription() == null) {
            return;
        }

        CharSequence label = current.getDescription().getLabel();
        CharSequence text = current.getItemAt(0).coerceToText(null);
        if (expectedLabel.contentEquals(label) && expectedText.contentEquals(text)) {
            cm.clearPrimaryClip();
        }
    }
}
