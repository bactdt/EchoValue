package cn.ntit.passwordmanager;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.ntit.passwordmanager.model.Credential;
import cn.ntit.passwordmanager.data.CredentialRepository;
import cn.ntit.passwordmanager.util.ClipboardUtil;
import cn.ntit.passwordmanager.util.StrengthEvaluator;

public class PasswordDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CRED_ID = "cred_id";

    private CredentialRepository repository;
    private Credential credential;
    private String plainPassword;

    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_detail);

        repository = new CredentialRepository(this);
        repository.setMasterPassword(VaultApp.getMasterPassword());

        long id = getIntent().getLongExtra(EXTRA_CRED_ID, -1);
        if (id == -1) {
            finish();
            return;
        }

        // 读取 + 解密
        Credential encrypted = repository.get(id);
        if (encrypted == null) {
            Toast.makeText(this, "凭据不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        plainPassword = encrypted.getPasswordEncrypted(); // repository.get() 已解密
        credential = encrypted;

        bindViews();
        wireEvents();
        populate();
    }

    private void bindViews() {}

    private void wireEvents() {
        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.edit_btn).setOnClickListener(v -> {
            Intent it = new Intent(this, AddCredentialActivity.class);
            it.putExtra(AddCredentialActivity.EXTRA_CRED_ID, credential.getId());
            startActivity(it);
        });

        findViewById(R.id.copy_username_btn).setOnClickListener(v -> {
            copy(credential.getUsername(), getString(R.string.detail_username_copied));
        });

        findViewById(R.id.copy_password_btn).setOnClickListener(v -> {
            copyPassword(plainPassword, getString(R.string.detail_password_copied));
        });

        findViewById(R.id.toggle_password_btn).setOnClickListener(v -> togglePassword());

        findViewById(R.id.open_btn).setOnClickListener(v -> openWebsite());

        findViewById(R.id.delete_btn).setOnClickListener(v -> confirmDelete());
    }

    private void populate() {
        char first = credential.getName() != null && !credential.getName().isEmpty()
                ? Character.toUpperCase(credential.getName().charAt(0)) : '?';
        ((TextView) findViewById(R.id.icon_letter)).setText(String.valueOf(first));
        ((TextView) findViewById(R.id.name_text)).setText(credential.getName());
        ((TextView) findViewById(R.id.email_text)).setText(credential.getUsername());
        ((TextView) findViewById(R.id.username_text)).setText(credential.getUsername());
        ((TextView) findViewById(R.id.website_text)).setText(credential.getWebsite() == null ? "" : credential.getWebsite());
        ((TextView) findViewById(R.id.notes_text)).setText(credential.getNotes() == null ? "" : credential.getNotes());

        // 安全检查（密码强度）
        int score = StrengthEvaluator.score(plainPassword);
        int level = StrengthEvaluator.level(score);
        TextView secTitle = findViewById(R.id.security_title);
        TextView secHint = findViewById(R.id.security_hint);
        View secProgress = findViewById(R.id.security_progress);
        String monthsAgo = getString(R.string.detail_months_ago, 3);
        switch (level) {
            case StrengthEvaluator.LEVEL_STRONG:
                secTitle.setText(R.string.detail_strong_password);
                secHint.setText(monthsAgo);
                secProgress.setBackgroundResource(R.drawable.bg_strength_primary);
                break;
            case StrengthEvaluator.LEVEL_MEDIUM:
                secTitle.setText(R.string.detail_strong_password);
                secHint.setText(R.string.detail_medium_warning);
                secProgress.setBackgroundResource(R.drawable.bg_strength_secondary);
                break;
            default:
                secTitle.setText(R.string.detail_strong_password);
                secHint.setText(R.string.detail_weak_warning);
                secProgress.setBackgroundResource(R.drawable.bg_strength_error);
                break;
        }
        android.view.ViewGroup.LayoutParams lp = secProgress.getLayoutParams();
        lp.width = Math.round(96 * (score / 100f));
        secProgress.setLayoutParams(lp);
    }

    private void togglePassword() {
        passwordVisible = !passwordVisible;
        TextView mask = findViewById(R.id.password_mask);
        TextView real = findViewById(R.id.password_real);
        ImageButton btn = findViewById(R.id.toggle_password_btn);
        if (passwordVisible) {
            mask.setVisibility(View.GONE);
            real.setVisibility(View.VISIBLE);
            real.setText(plainPassword);
            btn.setImageResource(R.drawable.ic_visibility_off);
        } else {
            mask.setVisibility(View.VISIBLE);
            real.setVisibility(View.GONE);
            btn.setImageResource(R.drawable.ic_visibility);
        }
    }

    private void openWebsite() {
        String url = credential.getWebsite();
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, R.string.detail_website, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!url.startsWith("http")) url = "https://" + url;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, url, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.detail_delete)
                .setMessage(R.string.detail_delete_confirm)
                .setPositiveButton(R.string.common_delete, (d, w) -> {
                    repository.delete(credential.getId());
                    Toast.makeText(this, R.string.detail_deleted, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private void copy(String text, String toast) {
        if (text == null) text = "";
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("vault", text));
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }

    private void copyPassword(String text, String toast) {
        if (text == null) text = "";
        ClipboardUtil.copyPassword(this, text);
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }
}
