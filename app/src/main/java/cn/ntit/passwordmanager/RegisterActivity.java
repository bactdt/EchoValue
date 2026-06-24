package cn.ntit.passwordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.ntit.passwordmanager.data.VaultDbHelper;
import cn.ntit.passwordmanager.model.UserAccount;
import cn.ntit.passwordmanager.util.CryptoUtil;
import cn.ntit.passwordmanager.util.SessionManager;
import cn.ntit.passwordmanager.util.StrengthEvaluator;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEdit, emailEdit, passwordEdit, confirmEdit;
    private ImageButton togglePasswordBtn, toggleConfirmBtn;
    private Button registerBtn;
    private TextView backToLogin, strengthText, strengthPercent;
    private View strengthBar;

    private boolean passwordVisible = false;
    private boolean confirmVisible = false;

    private VaultDbHelper dbHelper;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = new VaultDbHelper(this);
        session = new SessionManager(this);

        bindViews();
        wireEvents();

        String prefillEmail = getIntent().getStringExtra("prefill_email");
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            emailEdit.setText(prefillEmail);
        }
    }

    private void bindViews() {
        nameEdit = findViewById(R.id.name_edit);
        emailEdit = findViewById(R.id.email_edit);
        passwordEdit = findViewById(R.id.password_edit);
        confirmEdit = findViewById(R.id.confirm_edit);
        togglePasswordBtn = findViewById(R.id.toggle_password_btn);
        toggleConfirmBtn = findViewById(R.id.toggle_confirm_btn);
        registerBtn = findViewById(R.id.register_btn);
        backToLogin = findViewById(R.id.back_to_login);
        strengthText = findViewById(R.id.strength_text);
        strengthPercent = findViewById(R.id.strength_percent);
        strengthBar = findViewById(R.id.strength_bar);
    }

    private void wireEvents() {
        togglePasswordBtn.setOnClickListener(v -> toggleVisibility(passwordEdit, togglePasswordBtn, !passwordVisible));
        toggleConfirmBtn.setOnClickListener(v -> toggleVisibility(confirmEdit, toggleConfirmBtn, !confirmVisible));

        passwordEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateStrength(s.toString());
            }
        });

        registerBtn.setOnClickListener(v -> attemptRegister());
        backToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void toggleVisibility(EditText et, ImageButton btn, boolean makeVisible) {
        boolean wasVisible = (et.getInputType() & android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0;
        if (et == passwordEdit) passwordVisible = makeVisible;
        else confirmVisible = makeVisible;

        int sel = et.getSelectionEnd();
        et.setInputType(makeVisible
                ? android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        btn.setImageResource(makeVisible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
        et.setSelection(sel != -1 ? sel : et.length());
    }

    private void updateStrength(String password) {
        int score = StrengthEvaluator.score(password);
        int level = StrengthEvaluator.level(score);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) strengthBar.getLayoutParams();
        int parentWidth = getResources().getDisplayMetrics().widthPixels - 48 * 2; // approximate
        lp.width = Math.max(8, (int) (parentWidth * (score / 100f)));
        strengthBar.setLayoutParams(lp);

        int bgRes;
        String text;
        int color;
        switch (level) {
            case StrengthEvaluator.LEVEL_NONE:
                bgRes = R.drawable.bg_strength_error;
                text = getString(R.string.register_strength_none);
                color = R.color.md_on_surface_variant;
                break;
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
            default:
                bgRes = R.drawable.bg_strength_primary;
                text = getString(R.string.register_strength_strong);
                color = R.color.md_primary;
        }
        strengthBar.setBackgroundResource(bgRes);
        strengthText.setText(text);
        strengthText.setTextColor(getResources().getColor(color, getTheme()));
        strengthPercent.setText(score + "%");
    }

    private void attemptRegister() {
        String name = nameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString();
        String confirm = confirmEdit.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, R.string.register_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.register_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.findUserByEmail(email) != null) {
            Toast.makeText(this, R.string.register_email_taken, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] hash = CryptoUtil.hashPassword(password);
        UserAccount user = new UserAccount();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(hash[0]);
        user.setPasswordSalt(hash[1]);
        user.setPinHash(null);
        user.setCreatedAt(System.currentTimeMillis());
        dbHelper.insertUser(user);

        session.setLoggedIn(true);
        session.setUserEmail(email);
        session.setUserName(name);
        session.setUnlocked(true);
        VaultApp.setMasterPassword(password.toCharArray());

        Toast.makeText(this, R.string.register_success, Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
