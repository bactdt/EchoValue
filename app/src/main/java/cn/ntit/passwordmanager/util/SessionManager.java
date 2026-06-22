package cn.ntit.passwordmanager.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 会话与当前用户信息。使用 SharedPreferences（非加密，仅本地）。
 * 实际密码哈希/加密由 CryptoUtil + 数据库负责。
 */
public class SessionManager {

    private static final String PREFS = "vault_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_UNLOCKED = "unlocked";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public boolean isUnlocked() {
        return prefs.getBoolean(KEY_UNLOCKED, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public void setUnlocked(boolean unlocked) {
        prefs.edit().putBoolean(KEY_UNLOCKED, unlocked).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void setUserEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    /** 全部清空（注销） */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
