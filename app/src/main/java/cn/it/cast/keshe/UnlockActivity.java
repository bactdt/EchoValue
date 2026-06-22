package cn.it.cast.keshe;

import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;

import cn.it.cast.keshe.data.VaultDbHelper;
import cn.it.cast.keshe.model.UserAccount;
import cn.it.cast.keshe.util.PinVerifier;
import cn.it.cast.keshe.util.SessionManager;

public class UnlockActivity extends AppCompatActivity {

    private static final int MAX_PIN = 6;

    private final StringBuilder pin = new StringBuilder();
    private final List<Integer> dotIds = Arrays.asList(
            R.id.dot_1, R.id.dot_2, R.id.dot_3,
            R.id.dot_4, R.id.dot_5, R.id.dot_6);

    private SessionManager session;
    private VaultDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);

        session = new SessionManager(this);
        dbHelper = new VaultDbHelper(this);

        // 若未登录，跳回登录页
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // 若已解锁，直接进入主页
        if (session.isUnlocked() && VaultApp.hasMasterPassword()) {
            goToVault();
            return;
        }

        if (!hasConfiguredPin()) {
            returnToLogin();
            return;
        }

        // 数字按键
        int[] numberKeys = {
                R.id.key_1, R.id.key_2, R.id.key_3,
                R.id.key_4, R.id.key_5, R.id.key_6,
                R.id.key_7, R.id.key_8, R.id.key_9,
                R.id.key_0
        };
        for (int id : numberKeys) {
            Button b = findViewById(id);
            b.setOnClickListener(v -> addPin(b.getText().toString()));
        }

        findViewById(R.id.key_backspace).setOnClickListener(v -> removePin());
        findViewById(R.id.key_biometric).setOnClickListener(v -> attemptBiometric());
        findViewById(R.id.biometric_btn).setOnClickListener(v -> attemptBiometric());

        TextView forgot = findViewById(R.id.forgot_text);
        forgot.setOnClickListener(v -> {
            // 演示：返回登录页重置
            VaultApp.clearMasterPassword();
            session.clearAll();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void addPin(String digit) {
        if (pin.length() >= MAX_PIN) return;
        pin.append(digit);
        updateDots();
        if (pin.length() == MAX_PIN) {
            getWindow().getDecorView().postDelayed(this::validatePin, 150);
        }
    }

    private void removePin() {
        if (pin.length() == 0) return;
        pin.deleteCharAt(pin.length() - 1);
        updateDots();
    }

    private void updateDots() {
        for (int i = 0; i < dotIds.size(); i++) {
            View dot = findViewById(dotIds.get(i));
            if (i < pin.length()) {
                dot.setBackgroundResource(R.drawable.bg_pin_dot_active);
            } else {
                dot.setBackgroundResource(R.drawable.bg_pin_dot);
            }
        }
    }

    private void setDotsError(boolean error) {
        for (int id : dotIds) {
            View dot = findViewById(id);
            dot.setBackgroundResource(error
                    ? R.drawable.bg_pin_dot_error
                    : R.drawable.bg_pin_dot);
        }
    }

    private void validatePin() {
        UserAccount user = dbHelper.findUserByEmail(session.getUserEmail());
        if (user == null || user.getPinHash() == null || user.getPinHash().isEmpty()) {
            returnToLogin();
            return;
        }
        if (PinVerifier.verify(pin.toString(), user.getPinHash())) {
            session.setUnlocked(true);
            Toast.makeText(this, R.string.unlock_success, Toast.LENGTH_SHORT).show();
            goToVault();
        } else {
            setDotsError(true);
            shakeDots();
            vibrate();
            Toast.makeText(this, R.string.unlock_wrong_pin, Toast.LENGTH_SHORT).show();
            pin.setLength(0);
            getWindow().getDecorView().postDelayed(() -> {
                setDotsError(false);
                updateDots();
            }, 600);
        }
    }

    private void shakeDots() {
        View container = findViewById(R.id.pin_dots_container);
        TranslateAnimation shake = new TranslateAnimation(-20, 20, 0, 0);
        shake.setDuration(50);
        shake.setRepeatMode(Animation.REVERSE);
        shake.setRepeatCount(3);
        container.startAnimation(shake);
    }

    private void vibrate() {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {
        }
    }

    private void clearPinEntry() {
        pin.setLength(0);
        updateDots();
    }

    private boolean hasConfiguredPin() {
        UserAccount user = dbHelper.findUserByEmail(session.getUserEmail());
        return user != null && user.getPinHash() != null && !user.getPinHash().trim().isEmpty();
    }

    private void returnToLogin() {
        VaultApp.clearMasterPassword();
        session.setUnlocked(false);
        Toast.makeText(this, R.string.unlock_no_pin, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void attemptBiometric() {
        if (!VaultApp.hasMasterPassword()) {
            Toast.makeText(this, R.string.unlock_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricManager bm = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK;
        if (bm.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, R.string.unlock_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        if (!VaultApp.hasMasterPassword()) {
                            Toast.makeText(UnlockActivity.this, R.string.unlock_failed, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        session.setUnlocked(true);
                        Toast.makeText(UnlockActivity.this, R.string.unlock_success, Toast.LENGTH_SHORT).show();
                        goToVault();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        Toast.makeText(UnlockActivity.this, errString, Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_biometric_prompt_title))
                .setSubtitle(getString(R.string.unlock_biometric_prompt_subtitle))
                .setNegativeButtonText(getString(R.string.unlock_biometric_negative))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        prompt.authenticate(info);
    }

    private void goToVault() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
