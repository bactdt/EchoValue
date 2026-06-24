package cn.ntit.passwordmanager;

import android.content.Intent;
import android.graphics.Insets;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Locale;

import cn.ntit.passwordmanager.util.SessionManager;

public class MainActivity extends AppCompatActivity implements MainActivityCallback {

    private static final int PAGE_VAULT = 0;
    private static final int PAGE_GENERATOR = 1;
    private static final int PAGE_SECURITY = 2;
    private static final int PAGE_SETTINGS = 3;
    private static final int TOP_BAR_HEIGHT_DP = 56;
    private static final int BOTTOM_NAV_HEIGHT_DP = 80;
    private static final int FAB_BOTTOM_MARGIN_DP = 104;

    private ViewPager2 viewPager;
    private VaultFragment vaultFragment;
    private GeneratorFragment generatorFragment;
    private SecurityFragment securityFragment;
    private SettingsFragment settingsFragment;

    private View root;
    private View topBar;
    private View titleGroup;
    private View fabContainer;
    private View fab;
    private TextView topBarTitle;
    private EditText searchInput;
    private ImageView searchBtn;
    private View avatarContainer;
    private TextView avatarText;
    private View[] navItems = new View[4];
    private ImageView[] navIcons = new ImageView[4];
    private TextView[] navLabels = new TextView[4];
    private View navIndicator;
    private int currentPage = PAGE_VAULT;
    private int indicatorWidth;
    private int tabWidth;
    private int topInset;
    private int bottomInset;
    private boolean searchVisible;
    private boolean userDragging;

    private ActivityResultLauncher<Intent> addCredentialLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(false);

        // 守卫：必须已登录、已解锁、且持有主密码
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!VaultApp.hasMasterPassword()) {
            session.setUnlocked(false);
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (!session.isUnlocked()) {
            if (VaultApp.isPinLockAvailable(this)) {
                startActivity(new Intent(this, UnlockActivity.class));
                finish();
                return;
            }
            session.setUnlocked(true);
        }

        setContentView(R.layout.activity_main);
        bindViews();
        applyWindowInsets();

        // 注册 ActivityResultLauncher（替代 startActivityForResult）
        addCredentialLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && vaultFragment != null) {
                        vaultFragment.refreshList();
                    }
                });

        // 头像
        String name = session.getUserName();
        if (name != null && name.length() > 0) {
            avatarText.setText(name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ROOT));
        }

        wireEvents();
        setupViewPager();
        initIndicator();
        updateToolbarForPage(PAGE_VAULT);
        updateFabForPage(PAGE_VAULT);
        updateNavStyles(PAGE_VAULT);
    }

    private void bindViews() {
        root = findViewById(R.id.root);
        topBar = findViewById(R.id.top_bar);
        titleGroup = findViewById(R.id.title_group);
        fabContainer = findViewById(R.id.fab_container);
        fab = findViewById(R.id.fab);
        topBarTitle = findViewById(R.id.top_bar_title);
        searchInput = findViewById(R.id.search_input);
        searchBtn = findViewById(R.id.search_btn);
        avatarContainer = findViewById(R.id.avatar_container);
        avatarText = findViewById(R.id.avatar_text);
        viewPager = findViewById(R.id.view_pager);
        navIndicator = findViewById(R.id.nav_indicator);

        navItems[0] = findViewById(R.id.nav_vault);
        navItems[1] = findViewById(R.id.nav_generator);
        navItems[2] = findViewById(R.id.nav_security);
        navItems[3] = findViewById(R.id.nav_settings);

        for (int i = 0; i < 4; i++) {
            navIcons[i] = navItems[i].findViewById(R.id.nav_icon);
            navLabels[i] = navItems[i].findViewById(R.id.nav_label);
        }
    }

    private void applyWindowInsets() {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets statusInsets = insets.getInsets(WindowInsets.Type.statusBars());
            Insets navInsets = insets.getInsets(WindowInsets.Type.navigationBars());
            topInset = statusInsets.top;
            bottomInset = navInsets.bottom;
            applySystemBarInsets();
            return insets;
        });
        root.requestApplyInsets();
    }

    private void applySystemBarInsets() {
        int topBarHeight = dp(TOP_BAR_HEIGHT_DP) + topInset;
        ViewGroup.LayoutParams topBarLp = topBar.getLayoutParams();
        topBarLp.height = topBarHeight;
        topBar.setLayoutParams(topBarLp);
        topBar.setPadding(topBar.getPaddingLeft(), topInset, topBar.getPaddingRight(), 0);

        ViewGroup.MarginLayoutParams pagerLp = (ViewGroup.MarginLayoutParams) viewPager.getLayoutParams();
        pagerLp.topMargin = topBarHeight;
        pagerLp.bottomMargin = dp(BOTTOM_NAV_HEIGHT_DP) + bottomInset;
        viewPager.setLayoutParams(pagerLp);

        View bottomNav = findViewById(R.id.bottom_nav);
        ViewGroup.LayoutParams bottomNavLp = bottomNav.getLayoutParams();
        bottomNavLp.height = dp(BOTTOM_NAV_HEIGHT_DP) + bottomInset;
        bottomNav.setLayoutParams(bottomNavLp);
        bottomNav.setPadding(bottomNav.getPaddingLeft(), 0, bottomNav.getPaddingRight(), bottomInset);

        ViewGroup.MarginLayoutParams fabLp = (ViewGroup.MarginLayoutParams) fabContainer.getLayoutParams();
        fabLp.bottomMargin = dp(FAB_BOTTOM_MARGIN_DP) + bottomInset;
        fabContainer.setLayoutParams(fabLp);

        updateIndicatorSize(false);
    }

    private void wireEvents() {
        searchBtn.setOnClickListener(v -> {
            if (searchVisible) {
                closeSearch();
            } else if (currentPage == PAGE_VAULT) {
                openSearch();
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (vaultFragment != null) {
                    vaultFragment.filter(s.toString());
                }
            }
        });

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (vaultFragment != null) {
                    vaultFragment.filter(searchInput.getText().toString());
                }
                return true;
            }
            return false;
        });

        fab.setOnClickListener(v ->
                addCredentialLauncher.launch(new Intent(this, AddCredentialActivity.class)));

        avatarContainer.setOnClickListener(v -> switchToPage(PAGE_SETTINGS));

        for (int i = 0; i < navItems.length; i++) {
            final int page = i;
            navItems[i].setOnClickListener(v -> switchToPage(page));
        }
    }

    private void switchToPage(int page) {
        if (currentPage == page) {
            if (searchVisible) {
                closeSearch();
            }
            return;
        }
        closeSearch(false);
        viewPager.setCurrentItem(page, true);
    }

    private void setupViewPager() {
        vaultFragment = new VaultFragment();
        generatorFragment = new GeneratorFragment();
        securityFragment = new SecurityFragment();
        settingsFragment = new SettingsFragment();

        viewPager.setAdapter(new PagerAdapter(this));
        viewPager.setOffscreenPageLimit(1);
        viewPager.setUserInputEnabled(true);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (tabWidth == 0 || indicatorWidth == 0) return;
                if (userDragging) {
                    float targetX = (position + positionOffset) * tabWidth + (tabWidth - indicatorWidth) / 2f;
                    navIndicator.setTranslationX(targetX);
                }
            }

            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updateNavStyles(position);
                updateToolbarForPage(position);
                updateFabForPage(position);
                if (position != PAGE_VAULT) {
                    closeSearch();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                userDragging = state == ViewPager2.SCROLL_STATE_DRAGGING;
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    positionIndicator(currentPage, true);
                }
            }
        });
    }

    private void initIndicator() {
        navIndicator.post(() -> updateIndicatorSize(false));
    }

    private void updateIndicatorSize(boolean animate) {
        navIndicator.post(() -> {
            int totalWidth = navIndicator.getRootView().getWidth();
            if (totalWidth == 0) return;
            tabWidth = totalWidth / 4;
            indicatorWidth = Math.max(0, tabWidth - dp(20));
            ViewGroup.LayoutParams lp = navIndicator.getLayoutParams();
            if (lp.width != indicatorWidth) {
                lp.width = indicatorWidth;
                navIndicator.setLayoutParams(lp);
            }
            positionIndicator(currentPage, animate);
        });
    }

    private void positionIndicator(int page, boolean animate) {
        if (tabWidth == 0 || indicatorWidth == 0) return;
        float targetX = page * tabWidth + (tabWidth - indicatorWidth) / 2f;
        if (animate) {
            navIndicator.animate()
                    .translationX(targetX)
                    .setDuration(260)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                    .start();
        } else {
            navIndicator.setTranslationX(targetX);
        }
    }

    private void updateNavStyles(int selected) {
        for (int i = 0; i < 4; i++) {
            boolean active = i == selected;
            navItems[i].setBackgroundResource(0);
            navIcons[i].setColorFilter(ContextCompat.getColor(this,
                    active ? R.color.md_on_secondary_container : R.color.md_on_surface_variant));
            navLabels[i].setTextColor(ContextCompat.getColor(this,
                    active ? R.color.md_on_secondary_container : R.color.md_on_surface_variant));
            navLabels[i].getPaint().setFakeBoldText(active);
            navLabels[i].invalidate();
        }
    }

    private void updateToolbarForPage(int page) {
        switch (page) {
            case PAGE_GENERATOR:
                setToolbarTitle(getString(R.string.nav_generator), false, false);
                break;
            case PAGE_SECURITY:
                setToolbarTitle(getString(R.string.nav_security), false, false);
                break;
            case PAGE_SETTINGS:
                setToolbarTitle(getString(R.string.nav_settings), false, false);
                break;
            default:
                setToolbarTitle(getString(R.string.nav_vault), true, true);
                break;
        }
    }

    private void updateFabForPage(int page) {
        fabContainer.setVisibility(page == PAGE_VAULT && !searchVisible ? View.VISIBLE : View.GONE);
    }

    private void openSearch() {
        searchVisible = true;
        titleGroup.setVisibility(View.GONE);
        searchInput.setVisibility(View.VISIBLE);
        avatarContainer.setVisibility(View.GONE);
        searchInput.setText("");
        searchInput.requestFocus();
        searchBtn.setImageResource(R.drawable.ic_close);
        fabContainer.setVisibility(View.GONE);
    }

    private void closeSearch() {
        closeSearch(true);
    }

    private void closeSearch(boolean restoreCurrentUi) {
        if (!searchVisible) return;
        searchVisible = false;
        searchInput.setVisibility(View.GONE);
        searchInput.setText("");
        titleGroup.setVisibility(View.VISIBLE);
        searchBtn.setImageResource(R.drawable.ic_search);
        if (restoreCurrentUi) {
            updateToolbarForPage(currentPage);
            updateFabForPage(currentPage);
        }
        if (vaultFragment != null) {
            vaultFragment.filter("");
        }
    }

    @Override
    public void onBackPressed() {
        if (searchVisible) {
            closeSearch();
            return;
        }
        if (currentPage != PAGE_VAULT) {
            viewPager.setCurrentItem(PAGE_VAULT, true);
            return;
        }
        super.onBackPressed();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // ─── MainActivityCallback ────────────────────────────────────────────

    @Override
    public void setFabVisible(boolean visible) {
        if (currentPage == PAGE_VAULT && !searchVisible) {
            fabContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setToolbarTitle(String title, boolean showSearch, boolean showAvatar) {
        titleGroup.setVisibility(View.VISIBLE);
        topBarTitle.setText(title);
        searchBtn.setVisibility(showSearch && !searchVisible ? View.VISIBLE : View.GONE);
        avatarContainer.setVisibility(showAvatar ? View.VISIBLE : View.GONE);
    }

    @Override
    public void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private class PagerAdapter extends FragmentStateAdapter {

        PagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case PAGE_GENERATOR:
                    return generatorFragment;
                case PAGE_SECURITY:
                    return securityFragment;
                case PAGE_SETTINGS:
                    return settingsFragment;
                default:
                    return vaultFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
