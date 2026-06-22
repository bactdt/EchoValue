package cn.it.cast.keshe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Locale;

import cn.it.cast.keshe.adapter.CredentialAdapter;
import cn.it.cast.keshe.data.CredentialRepository;
import cn.it.cast.keshe.model.Credential;
import cn.it.cast.keshe.util.SessionManager;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_ADD = 1;
    private static final int REQ_DETAIL = 2;

    private CredentialRepository repository;
    private CredentialAdapter adapter;
    private TextView scoreValue;
    private TextView scoreHint;
    private View scoreProgress;
    private TextView statSavedCount;
    private LinearLayout emptyState;
    private TextView avatarText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 守卫：必须已登录、已解锁、且持有主密码
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!VaultApp.hasMasterPassword()) {
            session.setUnlocked(false);
            session.setLoggedIn(false);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!session.isUnlocked()) {
            startActivity(new Intent(this, UnlockActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        repository = new CredentialRepository(this);
        repository.setMasterPassword(VaultApp.getMasterPassword());

        bindViews();
        wireEvents();

        if (repository.count() == 0) {
            seedSampleCredentials();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void bindViews() {
        scoreValue = findViewById(R.id.score_value);
        scoreHint = findViewById(R.id.score_hint);
        scoreProgress = findViewById(R.id.score_progress);
        statSavedCount = findViewById(R.id.stat_saved_count);
        emptyState = findViewById(R.id.empty_state);
        avatarText = findViewById(R.id.avatar_text);

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.credentials_recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CredentialAdapter(java.util.Collections.emptyList(), repository,
                cred -> {
                    Intent it = new Intent(this, PasswordDetailActivity.class);
                    it.putExtra(PasswordDetailActivity.EXTRA_CRED_ID, cred.getId());
                    startActivityForResult(it, REQ_DETAIL);
                });
        rv.setAdapter(adapter);

        // 头像缩写
        SessionManager sm = new SessionManager(this);
        String name = sm.getUserName();
        if (name != null && name.length() > 0) {
            String initials = name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ROOT);
            avatarText.setText(initials);
        }
    }

    private void wireEvents() {
        findViewById(R.id.fab).setOnClickListener(v -> {
            Intent it = new Intent(this, AddCredentialActivity.class);
            startActivityForResult(it, REQ_ADD);
        });

        findViewById(R.id.nav_generator).setOnClickListener(v ->
                startActivity(new Intent(this, PasswordGeneratorActivity.class)));

        // chip 切换样式（演示）
        TextView[] chips = {
                findViewById(R.id.chip_all),
                findViewById(R.id.chip_favorites),
                findViewById(R.id.chip_passwords),
                findViewById(R.id.chip_notes)
        };
        for (TextView chip : chips) {
            chip.setOnClickListener(v -> {
                for (TextView c : chips) {
                    c.setTextAppearance(R.style.FilterChipInactive);
                    c.setBackgroundResource(R.drawable.bg_chip_inactive);
                }
                v.setBackgroundResource(R.drawable.bg_chip_active);
            });
        }

        findViewById(R.id.search_btn).setOnClickListener(v ->
                Toast.makeText(this, R.string.home_search_hint, Toast.LENGTH_SHORT).show());

        findViewById(R.id.nav_security).setOnClickListener(v ->
                startActivity(new Intent(this, SecurityActivity.class)));
        findViewById(R.id.nav_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    private void refreshList() {
        List<Credential> list = repository.list();
        adapter = new CredentialAdapter(list, repository,
                cred -> {
                    Intent it = new Intent(this, PasswordDetailActivity.class);
                    it.putExtra(PasswordDetailActivity.EXTRA_CRED_ID, cred.getId());
                    startActivityForResult(it, REQ_DETAIL);
                });
        ((androidx.recyclerview.widget.RecyclerView) findViewById(R.id.credentials_recycler)).setAdapter(adapter);

        // 统计
        int count = list.size();
        statSavedCount.setText(String.valueOf(count));
        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);

        // 计算安全分数：简单算法（密码越多基础分越高，最大 90 + 5/凭据）
        int score = Math.min(95, 50 + count * 5);
        scoreValue.setText(String.valueOf(score));
        android.view.ViewGroup.LayoutParams lp = scoreProgress.getLayoutParams();
        int width = Math.round(getResources().getDisplayMetrics().widthPixels - 48 * 2);
        lp.width = Math.max(40, width * score / 100);
        scoreProgress.setLayoutParams(lp);

        if (score >= 80) {
            scoreHint.setText(R.string.home_score_good);
        } else {
            scoreHint.setText(getString(R.string.home_score_warning, Math.max(1, (100 - score) / 10)));
        }
    }

    private void seedSampleCredentials() {
        repository.saveNew("Google", "julian.smith@gmail.com",
                "G00gle_Pass#2024", "https://accounts.google.com",
                "用于个人邮箱、YouTube 和 Google Drive");
        repository.saveNew("Bank of America", "js_investor_99",
                "B0A_secure!2024", "https://bankofamerica.com",
                "投资账户主登录");
        repository.saveNew("Netflix", "family_account_main",
                "N3tfl1x_Fam!", "https://netflix.com",
                "家庭套餐主账户");
        repository.saveNew("Adobe CC", "julian.smith@work.com",
                "Ad0be_Creative#2024", "https://adobe.com",
                "Photoshop + Illustrator 工作订阅");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADD || requestCode == REQ_DETAIL) {
            refreshList();
        }
    }
}
