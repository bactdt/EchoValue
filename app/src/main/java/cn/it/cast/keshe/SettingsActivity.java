package cn.it.cast.keshe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import cn.it.cast.keshe.util.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin(false);
            return;
        }

        setContentView(R.layout.activity_settings);
        bindUser();
        wireEvents();
    }

    private void bindUser() {
        String name = session.getUserName();
        String email = session.getUserEmail();
        ((TextView) findViewById(R.id.settings_user_name)).setText(
                TextUtils.isEmpty(name) ? getString(R.string.settings_unknown_user) : name);
        ((TextView) findViewById(R.id.settings_user_email)).setText(
                TextUtils.isEmpty(email) ? getString(R.string.settings_unknown_email) : email);
    }

    private void wireEvents() {
        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.logout_button).setOnClickListener(v -> {
            VaultApp.clearMasterPassword();
            session.clearAll();
            goToLogin(true);
        });

        findViewById(R.id.nav_vault).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.nav_generator).setOnClickListener(v ->
                startActivity(new Intent(this, PasswordGeneratorActivity.class)));
        findViewById(R.id.nav_security).setOnClickListener(v ->
                startActivity(new Intent(this, SecurityActivity.class)));
    }

    private void goToLogin(boolean clearTask) {
        Intent intent = new Intent(this, LoginActivity.class);
        if (clearTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
        finish();
    }
}
