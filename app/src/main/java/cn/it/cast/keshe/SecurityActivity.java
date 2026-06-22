package cn.it.cast.keshe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import cn.it.cast.keshe.data.CredentialRepository;
import cn.it.cast.keshe.model.Credential;
import cn.it.cast.keshe.util.SessionManager;
import cn.it.cast.keshe.util.StrengthEvaluator;

public class SecurityActivity extends AppCompatActivity {

    private CredentialRepository repository;
    private TextView scoreValue;
    private TextView scoreHint;
    private TextView savedCount;
    private TextView weakCount;
    private TextView attentionCount;
    private View scoreTrack;
    private View scoreBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            goToLogin();
            return;
        }
        if (!VaultApp.hasMasterPassword()) {
            Toast.makeText(this, R.string.security_relogin_required, Toast.LENGTH_SHORT).show();
            session.setUnlocked(false);
            session.setLoggedIn(false);
            goToLogin();
            return;
        }
        if (!session.isUnlocked()) {
            goToUnlock();
            return;
        }

        setContentView(R.layout.activity_security);

        repository = new CredentialRepository(this);
        repository.setMasterPassword(VaultApp.getMasterPassword());

        bindViews();
        wireEvents();
        populateOverview();
    }

    private void bindViews() {
        scoreValue = findViewById(R.id.security_score_value);
        scoreHint = findViewById(R.id.security_score_hint);
        savedCount = findViewById(R.id.security_saved_count);
        weakCount = findViewById(R.id.security_weak_count);
        attentionCount = findViewById(R.id.security_attention_count);
        scoreTrack = findViewById(R.id.security_score_track);
        scoreBar = findViewById(R.id.security_score_bar);
    }

    private void wireEvents() {
        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.nav_vault).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
        findViewById(R.id.nav_generator).setOnClickListener(v ->
                startActivity(new Intent(this, PasswordGeneratorActivity.class)));
        findViewById(R.id.nav_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void populateOverview() {
        List<Credential> credentials = repository.list();
        int saved = credentials.size();
        int weak = 0;
        int attention = 0;

        for (Credential credential : credentials) {
            String password = repository.decrypt(credential.getPasswordEncrypted());
            int level = StrengthEvaluator.level(StrengthEvaluator.score(password));
            if (level == StrengthEvaluator.LEVEL_WEAK || level == StrengthEvaluator.LEVEL_NONE) {
                weak++;
            }
            if (level != StrengthEvaluator.LEVEL_STRONG) {
                attention++;
            }
        }

        int score = saved == 0 ? 100 : Math.max(0, 100 - weak * 15 - (attention - weak) * 8);
        scoreValue.setText(String.valueOf(score));
        savedCount.setText(String.valueOf(saved));
        weakCount.setText(String.valueOf(weak));
        attentionCount.setText(String.valueOf(attention));
        scoreHint.setText(attention == 0
                ? R.string.security_score_good
                : R.string.security_score_attention);
        updateScoreBar(score);
    }

    private void updateScoreBar(int score) {
        scoreTrack.post(() -> {
            android.view.ViewGroup.LayoutParams lp = scoreBar.getLayoutParams();
            lp.width = Math.max(0, scoreTrack.getWidth() * score / 100);
            scoreBar.setLayoutParams(lp);
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void goToUnlock() {
        Intent intent = new Intent(this, UnlockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
