package cn.ntit.passwordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

    private ViewPager2 viewPager;
    private VaultFragment vaultFragment;
    private GeneratorFragment generatorFragment;
    private SecurityFragment securityFragment;
    private SettingsFragment settingsFragment;

    private LinearLayout titleGroup;
    private TextView toolbarTitle;
    private EditText searchInput;
    private ImageButton searchBtn;
    private TextView avatarText;
    private View fabWrapper;
    private ImageButton fab;

    private View[] navItems;
    private View navIndicator;
    private int currentPage = PAGE_VAULT;
    private int indicatorWidth;
    private int tabWidth;

    private boolean searchVisible = false;
    private boolean isUserDragging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        wireEvents();
        setupViewPager();
        initAvatar();
        initIndicator();
        updateToolbarForPage(PAGE_VAULT);
    }

    private void bindViews() {
        titleGroup = findViewById(R.id.title_group);
        toolbarTitle = findViewById(R.id.toolbar_title);
        searchInput = findViewById(R.id.search_input);
        searchBtn = findViewById(R.id.search_btn);
        avatarText = findViewById(R.id.avatar_text);
        fabWrapper = findViewById(R.id.fab_wrapper);
        fab = findViewById(R.id.fab);
        viewPager = findViewById(R.id.view_pager);
        navIndicator = findViewById(R.id.nav_indicator);

        navItems = new View[]{
                findViewById(R.id.nav_vault),
                findViewById(R.id.nav_generator),
                findViewById(R.id.nav_security),
                findViewById(R.id.nav_settings)
        };
    }

    private void wireEvents() {
        searchBtn.setOnClickListener(v -> {
            if (searchVisible) {
                closeSearch();
            } else {
                if (currentPage == PAGE_VAULT) {
                    openSearch();
                }
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

        fab.setOnClickListener(v -> {
            Intent it = new Intent(this, AddCredentialActivity.class);
            startActivityForResult(it, 1);
        });

        for (int i = 0; i < navItems.length; i++) {
            final int page = i;
            navItems[i].setOnClickListener(v -> {
                if (currentPage != page) {
                    closeSearch();
                    viewPager.setCurrentItem(page, true);
                }
            });
        }
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
                if (isUserDragging) {
                    float targetX = (position + positionOffset) * tabWidth + (tabWidth - indicatorWidth) / 2f;
                    navIndicator.setTranslationX(targetX);
                }
            }

            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updateNavColors(position);
                updateToolbarForPage(position);
                updateFabVisibility(position);
                if (position != PAGE_VAULT) {
                    closeSearch();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                isUserDragging = (state == ViewPager2.SCROLL_STATE_DRAGGING);
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    positionIndicator(currentPage, true);
                }
            }
        });
    }

    private void initAvatar() {
        SessionManager sm = new SessionManager(this);
        String name = sm.getUserName();
        if (name != null && name.length() > 0) {
            String initials = name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ROOT);
            avatarText.setText(initials);
        }
    }

    private void initIndicator() {
        navIndicator.post(() -> {
            int screenWidth = navIndicator.getRootView().getWidth();
            tabWidth = screenWidth / 4;
            indicatorWidth = tabWidth - dp(10) * 2;
            ViewGroup.LayoutParams lp = navIndicator.getLayoutParams();
            lp.width = indicatorWidth;
            navIndicator.setLayoutParams(lp);
            positionIndicator(PAGE_VAULT, false);
        });
    }

    private void positionIndicator(int page, boolean animate) {
        float targetX = page * tabWidth + (tabWidth - indicatorWidth) / 2f;
        if (animate) {
            navIndicator.animate()
                    .translationX(targetX)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                    .start();
        } else {
            navIndicator.setTranslationX(targetX);
        }
    }

    private void updateNavColors(int activePage) {
        for (int i = 0; i < navItems.length; i++) {
            LinearLayout item = (LinearLayout) navItems[i];
            ImageView icon = (ImageView) item.getChildAt(0);
            TextView label = (TextView) item.getChildAt(1);

            if (i == activePage) {
                icon.setColorFilter(getColor(R.color.md_on_secondary_container));
                label.setTextColor(getColor(R.color.md_on_secondary_container));
                label.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                icon.setColorFilter(getColor(R.color.md_on_surface_variant));
                label.setTextColor(getColor(R.color.md_on_surface_variant));
                label.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private void updateToolbarForPage(int page) {
        switch (page) {
            case PAGE_VAULT:
                titleGroup.setVisibility(View.VISIBLE);
                toolbarTitle.setText(R.string.login_title);
                searchBtn.setVisibility(View.VISIBLE);
                break;
            case PAGE_GENERATOR:
                titleGroup.setVisibility(View.VISIBLE);
                toolbarTitle.setText(R.string.nav_generator);
                searchBtn.setVisibility(View.GONE);
                break;
            case PAGE_SECURITY:
                titleGroup.setVisibility(View.VISIBLE);
                toolbarTitle.setText(R.string.nav_security);
                searchBtn.setVisibility(View.GONE);
                break;
            case PAGE_SETTINGS:
                titleGroup.setVisibility(View.VISIBLE);
                toolbarTitle.setText(R.string.nav_settings);
                searchBtn.setVisibility(View.GONE);
                break;
        }
    }

    private void updateFabVisibility(int page) {
        fabWrapper.setVisibility(page == PAGE_VAULT ? View.VISIBLE : View.GONE);
    }

    private void openSearch() {
        searchVisible = true;
        titleGroup.setVisibility(View.GONE);
        searchInput.setVisibility(View.VISIBLE);
        searchInput.requestFocus();
        searchInput.setText("");
        searchBtn.setImageResource(R.drawable.ic_close);
        fabWrapper.setVisibility(View.GONE);
    }

    private void closeSearch() {
        searchVisible = false;
        searchInput.setVisibility(View.GONE);
        searchInput.setText("");
        titleGroup.setVisibility(View.VISIBLE);
        searchBtn.setImageResource(R.drawable.ic_search);
        if (currentPage == PAGE_VAULT) {
            fabWrapper.setVisibility(View.VISIBLE);
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
        finishAffinity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (vaultFragment != null) {
            vaultFragment.refreshList();
        }
    }

    @Override
    public void setFabVisible(boolean visible) {
        if (currentPage == PAGE_VAULT && !searchVisible) {
            fabWrapper.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setToolbarForVault() {
        updateToolbarForPage(PAGE_VAULT);
    }

    @Override
    public void setToolbarForTab(String title) {
        titleGroup.setVisibility(View.VISIBLE);
        toolbarTitle.setText(title);
        searchBtn.setVisibility(View.GONE);
    }

    @Override
    public void setToolbarTitle(String title, boolean showAvatar, boolean showSearch) {
        titleGroup.setVisibility(View.VISIBLE);
        toolbarTitle.setText(title);
        searchBtn.setVisibility(showSearch ? View.VISIBLE : View.GONE);
    }

    @Override
    public void logout() {
        startActivity(new Intent(this, LoginActivity.class));
        finishAffinity();
    }

    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density);
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