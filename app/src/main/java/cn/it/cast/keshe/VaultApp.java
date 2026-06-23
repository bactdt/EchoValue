package cn.it.cast.keshe;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import cn.it.cast.keshe.data.VaultDbHelper;
import cn.it.cast.keshe.model.UserAccount;
import cn.it.cast.keshe.util.CryptoUtil;
import cn.it.cast.keshe.util.SessionManager;

/**
 * 全局应用对象：保存当前会话的主密码（仅内存，进程退出即丢失）。
 */
public class VaultApp extends Application {

    private static final long BACKGROUND_LOCK_TIMEOUT_MS = 5 * 60 * 1000;

    private static char[] sMasterPassword;
    private int startedActivities;
    private boolean changingConfiguration;
    private long backgroundedAt;

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

    /** Autofill 服务查询：主密码是否在内存可用。 */
    public static boolean isMasterPasswordAvailable() {
        return hasMasterPassword();
    }

    public static void clearMasterPassword() {
        if (sMasterPassword != null) {
            for (int i = 0; i < sMasterPassword.length; i++) sMasterPassword[i] = '\0';
        }
        sMasterPassword = null;
        CryptoUtil.clearKeyCache();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                if (startedActivities == 0) {
                    changingConfiguration = false;
                }
                startedActivities++;
            }

            @Override
            public void onActivityResumed(Activity activity) {
                expireMasterPasswordIfTimedOut();
                backgroundedAt = 0;
                redirectIfLocked(activity);
            }

            @Override
            public void onActivityStopped(Activity activity) {
                changingConfiguration = activity.isChangingConfigurations();
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0 && !changingConfiguration) {
                    lockForBackground();
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    private void lockForBackground() {
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn() || !session.isUnlocked()) {
            return;
        }
        if (isPinLockAvailable(this)) {
            // 不立即清主密码，记录后台时间戳；5 分钟超时后才清。
            backgroundedAt = SystemClock.elapsedRealtime();
        }
    }

    private void expireMasterPasswordIfTimedOut() {
        if (backgroundedAt == 0) return;
        long elapsed = SystemClock.elapsedRealtime() - backgroundedAt;
        if (elapsed > BACKGROUND_LOCK_TIMEOUT_MS) {
            SessionManager session = new SessionManager(this);
            if (isPinLockAvailable(this)) {
                clearMasterPassword();
                session.setUnlocked(false);
            }
        }
    }

    private void redirectIfLocked(Activity activity) {
        if (isAuthActivity(activity)) {
            return;
        }

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            return;
        }

        if (!hasMasterPassword()) {
            session.setUnlocked(false);
            launch(activity, LoginActivity.class);
            return;
        }

        if (!session.isUnlocked()) {
            launch(activity, isPinLockAvailable(this) ? UnlockActivity.class : LoginActivity.class);
        }
    }

    public static boolean isPinLockAvailable(Context context) {
        SessionManager session = new SessionManager(context);
        return session.isPinLockEnabled() && hasConfiguredPin(context, session);
    }

    private static boolean hasConfiguredPin(Context context, SessionManager session) {
        String email = session.getUserEmail();
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        try {
            UserAccount user = new VaultDbHelper(context).findUserByEmail(email.trim());
            return user != null
                    && user.getPinHash() != null
                    && !user.getPinHash().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAuthActivity(Activity activity) {
        return activity instanceof LoginActivity
                || activity instanceof RegisterActivity
                || activity instanceof UnlockActivity;
    }

    private void launch(Activity activity, Class<? extends Activity> target) {
        Intent intent = new Intent(activity, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }
}
