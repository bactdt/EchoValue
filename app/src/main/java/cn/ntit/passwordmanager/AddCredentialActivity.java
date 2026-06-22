package cn.ntit.passwordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.ntit.passwordmanager.data.CredentialRepository;
import cn.ntit.passwordmanager.model.Credential;
import cn.ntit.passwordmanager.util.PasswordGenerator;
import cn.ntit.passwordmanager.util.StrengthEvaluator;

public class AddCredentialActivity extends AppCompatActivity {

    public static final String EXTRA_CRED_ID = "cred_id";

    private EditText nameEdit, usernameEdit, passwordEdit, websiteEdit, notesEdit;
    private View strengthBar;

    private CredentialRepository repository;
    private long editId = -1;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_credential);

        repository = new CredentialRepository(this);
        repository.setMasterPassword(VaultApp.getMasterPassword());

        bindViews();
        wireEvents();

        if (getIntent().hasExtra(EXTRA_CRED_ID)) {
            editId = getIntent().getLongExtra(EXTRA_CRED_ID, -1);
            loadForEdit();
        }
    }

    private void bindViews() {
        nameEdit = findViewById(R.id.name_edit);
        usernameEdit = findViewById(R.id.username_edit);
        passwordEdit = findViewById(R.id.password_edit);
        websiteEdit = findViewById(R.id.website_edit);
        notesEdit = findViewById(R.id.notes_edit);
        strengthBar = findViewById(R.id.strength_bar);
    }

    private void wireEvents() {
        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.save_btn).setOnClickListener(v -> save());
        findViewById(R.id.save_button).setOnClickListener(v -> save());

        findViewById(R.id.generate_btn).setOnClickListener(v -> {
            String pw = PasswordGenerator.generate(16, true, true, true, true);
            passwordEdit.setText(pw);
        });

        findViewById(R.id.toggle_password_btn).setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            int sel = passwordEdit.getSelectionEnd();
            passwordEdit.setInputType(passwordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ((ImageButton) findViewById(R.id.toggle_password_btn))
                    .setImageResource(passwordVisible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            passwordEdit.setSelection(sel != -1 ? sel : passwordEdit.length());
        });

        passwordEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateStrength(s.toString());
            }
        });
    }

    private void loadForEdit() {
        Credential c = repository.get(editId);
        if (c == null) return;
        ((TextView) findViewById(R.id.title_text)).setText(R.string.edit_title);
        nameEdit.setText(c.getName());
        usernameEdit.setText(c.getUsername());
        passwordEdit.setText(c.getPasswordEncrypted()); // repository.get 已解密
        websiteEdit.setText(c.getWebsite());
        notesEdit.setText(c.getNotes());
    }

    private void updateStrength(String p) {
        int score = StrengthEvaluator.score(p);
        int level = StrengthEvaluator.level(score);
        int bgRes;
        switch (level) {
            case StrengthEvaluator.LEVEL_STRONG:
                bgRes = R.drawable.bg_strength_primary;
                break;
            case StrengthEvaluator.LEVEL_MEDIUM:
                bgRes = R.drawable.bg_strength_secondary;
                break;
            default:
                bgRes = R.drawable.bg_strength_error;
        }
        strengthBar.setBackgroundResource(bgRes);
        android.view.ViewGroup.LayoutParams lp = strengthBar.getLayoutParams();
        int parentWidth = getResources().getDisplayMetrics().widthPixels - 32 - 48;
        lp.width = Math.max(8, (int) (parentWidth * (score / 100f)));
        strengthBar.setLayoutParams(lp);
    }

    private void save() {
        String name = nameEdit.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.add_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String username = usernameEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString();
        String website = websiteEdit.getText().toString().trim();
        String notes = notesEdit.getText().toString().trim();

        if (editId == -1) {
            repository.saveNew(name, username, password, website, notes);
        } else {
            repository.update(editId, name, username, password, website, notes);
        }

        Toast.makeText(this, editId == -1 ? R.string.add_saved : R.string.edit_updated, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
}
