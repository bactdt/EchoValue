package cn.it.cast.keshe;

import android.app.Application;

/**
 * 全局应用对象：保存当前会话的主密码（仅内存，进程退出即丢失）。
 */
public class VaultApp extends Application {

    private static char[] sMasterPassword;

    public static void setMasterPassword(char[] password) {
        if (sMasterPassword != null) {
            for (int i = 0; i < sMasterPassword.length; i++) sMasterPassword[i] = '\0';
        }
        sMasterPassword = password;
    }

    public static char[] getMasterPassword() {
        return sMasterPassword;
    }

    public static boolean hasMasterPassword() {
        return sMasterPassword != null && sMasterPassword.length > 0;
    }

    public static void clearMasterPassword() {
        if (sMasterPassword != null) {
            for (int i = 0; i < sMasterPassword.length; i++) sMasterPassword[i] = '\0';
        }
        sMasterPassword = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
