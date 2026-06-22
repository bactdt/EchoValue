package cn.it.cast.keshe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.it.cast.keshe.data.VaultDbHelper;
import cn.it.cast.keshe.model.UserAccount;
import cn.it.cast.keshe.util.CryptoUtil;
import cn.it.cast.keshe.util.SessionManager;
import cn.it.cast.keshe.util.StrengthEvaluator;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEdit;
    private EditText passwordEdit;
    private ImageButton togglePasswordBtn;
    private Button loginBtn;
    private Button createAccountBtn;
    private TextView forgotText;
    private LinearLayout strengthContainer;
    private View strengthBar;
    private TextView strengthText;

    private boolean passwordVisible = false;
    private VaultDbHelper dbHelper;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new VaultDbHelper(this);
        session = new SessionManager(this);

        bindViews();
        wireEvents();

        // 只有当前进程仍持有主密码且会话已解锁时，才直接进入 Vault。
        if (session.isLoggedIn() && session.isUnlocked() && VaultApp.hasMasterPassword()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else if (session.isLoggedIn() && !VaultApp.hasMasterPassword()) {
            session.setUnlocked(false);
            session.setLoggedIn(false);
        }
    }

    private void bindViews() {
        emailEdit = findViewById(R.id.email_edit);
        passwordEdit = findViewById(R.id.password_edit);
        togglePasswordBtn = findViewById(R.id.toggle_password_btn);
        loginBtn = findViewById(R.id.login_btn);
        createAccountBtn = findViewById(R.id.create_account_btn);
        forgotText = findViewById(R.id.forgot_text);
        strengthContainer = findViewById(R.id.strength_container);
        strengthBar = findViewById(R.id.strength_bar);
        strengthText = findViewById(R.id.strength_text);
    }

    private void wireEvents() {
        togglePasswordBtn.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            int sel = passwordEdit.getSelectionEnd();
            passwordEdit.setInputType(passwordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            togglePasswordBtn.setImageResource(passwordVisible
                    ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            passwordEdit.setSelection(sel != -1 ? sel : passwordEdit.length());
        });

        passwordEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String p = s.toString();
                if (p.isEmpty()) {
                    strengthContainer.setVisibility(View.GONE);
                } else {
                    strengthContainer.setVisibility(View.VISIBLE);
                    int score = StrengthEvaluator.score(p);
                    updateStrength(score);
                }
            }
        });

        loginBtn.setOnClickListener(v -> attemptLogin());
        createAccountBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        forgotText.setOnClickListener(v ->
                Toast.makeText(this, R.string.login_forgot, Toast.LENGTH_SHORT).show());
    }

    private void updateStrength(int score) {
        int level = StrengthEvaluator.level(score);
        int bgRes;
        String text;
        int color;
        switch (level) {
            case StrengthEvaluator.LEVEL_WEAK:
                bgRes = R.drawable.bg_strength_error;
                text = getString(R.string.register_strength_weak);
                color = R.color.md_error;
                break;
            case StrengthEvaluator.LEVEL_MEDIUM:
                bgRes = R.drawable.bg_strength_secondary;
                text = getString(R.string.register_strength_medium);
                color = R.color.md_secondary;
                break;
            case StrengthEvaluator.LEVEL_STRONG:
                bgRes = R.drawable.bg_strength_primary;
                text = getString(R.string.register_strength_strong);
                color = R.color.md_primary;
                break;
            default:
                bgRes = R.drawable.bg_strength_error;
                text = getString(R.string.register_strength_weak);
                color = R.color.md_error;
        }
        strengthBar.setBackgroundResource(bgRes);
        strengthText.setText(text);
        strengthText.setTextColor(getResources().getColor(color, getTheme()));
    }

    private void attemptLogin() {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.register_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        UserAccount user = dbHelper.findUserByEmail(email);
        if (user == null) {
            // 演示模式：未注册则自动注册（首次进入）
            registerDemoUser(email, password);
            return;
        }

        if (CryptoUtil.verifyPassword(password, user.getPasswordHash(), user.getPasswordSalt())) {
            session.setLoggedIn(true);
            session.setUserEmail(email);
            session.setUserName(user.getName());
            session.setUnlocked(true);
            VaultApp.setMasterPassword(password.toCharArray());

            Toast.makeText(this, R.string.login_auth_success, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, R.string.login_auth_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void registerDemoUser(String email, String password) {
        // 演示用：直接跳到注册页，把邮箱带上
        Intent it = new Intent(this, RegisterActivity.class);
        it.putExtra("prefill_email", email);
        startActivity(it);
    }
}
