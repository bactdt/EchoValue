package cn.it.cast.keshe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.Locale;

import cn.it.cast.keshe.data.CredentialRepository;
import cn.it.cast.keshe.util.SessionManager;

public class MainActivity extends AppCompatActivity implements MainActivityCallback {

    private Fragment[] fragments;
    private int currentTab = 0;
    private static final int TAB_VAULT = 0;
    private static final int TAB_GENERATOR = 1;
    private static final int TAB_SECURITY = 2;
    private static final int TAB_SETTINGS = 3;

    private View fabContainer;
    private TextView topBarTitle;
    private View searchBtn;
    private View avatarContainer;
    private TextView avatarText;
    private View bottomNav;
    private View[] navItems = new View[4];
    private ImageView[] navIcons = new ImageView[4];
    private TextView[] navLabels = new TextView[4];

    private ActivityResultLauncher<Intent> addCredentialLauncher;

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
        bindViews();

        // 注册 ActivityResultLauncher（替代 startActivityForResult）
        addCredentialLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && currentTab == TAB_VAULT) {
                        VaultFragment vf = (VaultFragment) fragments[TAB_VAULT];
                        if (vf != null) vf.onResume();
                    }
                });

        // 初始化 Fragment
        fragments = new Fragment[4];
        fragments[TAB_VAULT] = new VaultFragment();
        fragments[TAB_GENERATOR] = new GeneratorFragment();
        fragments[TAB_SECURITY] = new SecurityFragment();
        fragments[TAB_SETTINGS] = new SettingsFragment();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, fragments[TAB_VAULT], "tab_vault");
        for (int i = 1; i < fragments.length; i++) {
            ft.add(R.id.fragment_container, fragments[i], "tab_" + i);
            ft.hide(fragments[i]);
        }
        ft.commit();

        // 设置导航点击
        for (int i = 0; i < navItems.length; i++) {
            final int tab = i;
            navItems[i].setOnClickListener(v -> switchTab(tab));
        }

        // FAB 点击
        fabContainer.setOnClickListener(v -> {
            addCredentialLauncher.launch(new Intent(this, AddCredentialActivity.class));
        });

        // 搜索按钮
        searchBtn.setOnClickListener(v ->
                Toast.makeText(this, R.string.home_search_hint, Toast.LENGTH_SHORT).show());

        // 头像
        String name = session.getUserName();
        if (name != null && name.length() > 0) {
            avatarText.setText(name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ROOT));
        }

        // 更新初始 tab 样式
        updateNavStyles(TAB_VAULT);
        currentTab = TAB_VAULT;
    }

    private void bindViews() {
        fabContainer = findViewById(R.id.fab_container);
        topBarTitle = findViewById(R.id.top_bar_title);
        searchBtn = findViewById(R.id.search_btn);
        avatarContainer = findViewById(R.id.avatar_container);
        avatarText = findViewById(R.id.avatar_text);
        bottomNav = findViewById(R.id.bottom_nav);

        navItems[0] = findViewById(R.id.nav_vault);
        navItems[1] = findViewById(R.id.nav_generator);
        navItems[2] = findViewById(R.id.nav_security);
        navItems[3] = findViewById(R.id.nav_settings);

        for (int i = 0; i < 4; i++) {
            navIcons[i] = navItems[i].findViewById(R.id.nav_icon);
            navLabels[i] = navItems[i].findViewById(R.id.nav_label);
        }
    }

    private void switchTab(int index) {
        if (index == currentTab) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.hide(fragments[currentTab]);
        ft.show(fragments[index]);
        ft.commit();

        updateNavStyles(index);
        currentTab = index;

        // 通知 Fragment 选中
        if (fragments[index] instanceof VaultFragment) {
            ((VaultFragment) fragments[index]).onResume();
        }
        if (fragments[index] instanceof SecurityFragment) {
            ((SecurityFragment) fragments[index]).onResume();
        }
    }

    private void updateNavStyles(int selected) {
        for (int i = 0; i < 4; i++) {
            boolean active = i == selected;
            navItems[i].setBackgroundResource(0);
            if (active) {
                navItems[i].setBackgroundResource(R.drawable.bg_chip_active);
            }
            navIcons[i].setColorFilter(ContextCompat.getColor(this,
                    active ? R.color.md_on_secondary_container : R.color.md_on_surface_variant));
            navLabels[i].setTextColor(ContextCompat.getColor(this,
                    active ? R.color.md_on_secondary_container : R.color.md_on_surface_variant));
            navLabels[i].getPaint().setFakeBoldText(active);
            navLabels[i].invalidate();
        }
    }

    // ─── MainActivityCallback ────────────────────────────────────────────

    @Override
    public void setFabVisible(boolean visible) {
        fabContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setToolbarTitle(String title, boolean showSearch, boolean showAvatar) {
        topBarTitle.setText(title);
        searchBtn.setVisibility(showSearch ? View.VISIBLE : View.GONE);
        avatarContainer.setVisibility(showAvatar ? View.VISIBLE : View.GONE);
    }

    @Override
    public void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
