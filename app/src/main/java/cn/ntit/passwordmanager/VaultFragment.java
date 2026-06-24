package cn.ntit.passwordmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.ntit.passwordmanager.adapter.CredentialAdapter;
import cn.ntit.passwordmanager.data.CredentialRepository;
import cn.ntit.passwordmanager.model.Credential;
import cn.ntit.passwordmanager.util.SessionManager;

public class VaultFragment extends Fragment {

    private CredentialRepository repository;
    private TextView scoreValue;
    private TextView scoreHint;
    private View scoreProgress;
    private TextView statSavedCount;
    private TextView statFavoriteCount;
    private LinearLayout emptyState;

    private final ActivityResultLauncher<Intent> detailLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> refreshList());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vault, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new CredentialRepository(requireContext());
        repository.setMasterPassword(VaultApp.getMasterPassword());

        bindViews(view);
        wireEvents(view);
        refreshList();

        if (repository.count() == 0) {
            seedSampleCredentials();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isVisible()) {
            refreshList();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshList();
        }
    }

    private void bindViews(View view) {
        scoreValue = view.findViewById(R.id.score_value);
        scoreHint = view.findViewById(R.id.score_hint);
        scoreProgress = view.findViewById(R.id.score_progress);
        statSavedCount = view.findViewById(R.id.stat_saved_count);
        statFavoriteCount = view.findViewById(R.id.stat_favorite_count);
        emptyState = view.findViewById(R.id.empty_state);

        RecyclerView rv = view.findViewById(R.id.credentials_recycler);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CredentialAdapter(java.util.Collections.emptyList(), repository,
                cred -> launchDetail(cred));
        rv.setAdapter(adapter);
    }

    private CredentialAdapter adapter;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_FAVORITES = 1;
    private static final int FILTER_PASSWORDS = 2;
    private static final int FILTER_NOTES = 3;
    private int currentFilter = FILTER_ALL;
    private String searchQuery = "";

    private void launchDetail(Credential cred) {
        Intent it = new Intent(requireContext(), PasswordDetailActivity.class);
        it.putExtra(PasswordDetailActivity.EXTRA_CRED_ID, cred.getId());
        detailLauncher.launch(it);
    }

    private void wireEvents(View view) {
        TextView[] chips = {
                view.findViewById(R.id.chip_all),
                view.findViewById(R.id.chip_favorites),
                view.findViewById(R.id.chip_passwords),
                view.findViewById(R.id.chip_notes)
        };
        int[] filterForChip = { FILTER_ALL, FILTER_FAVORITES, FILTER_PASSWORDS, FILTER_NOTES };
        for (int i = 0; i < chips.length; i++) {
            final int filter = filterForChip[i];
            chips[i].setOnClickListener(v -> {
                currentFilter = filter;
                for (TextView c : chips) {
                    c.setTextAppearance(R.style.FilterChipInactive);
                    c.setBackgroundResource(R.drawable.bg_chip_inactive);
                }
                TextView tv = (TextView) v;
                tv.setTextAppearance(R.style.FilterChipActive);
                tv.setBackgroundResource(R.drawable.bg_chip_active);
                refreshList();
            });
        }
    }

    public void refreshList() {
        if (repository == null || getView() == null) return;
        List<Credential> all = repository.list();

        // 统计基于全部数据
        int totalCount = all.size();
        statSavedCount.setText(String.valueOf(totalCount));

        int favCount = 0;
        for (Credential c : all) {
            if (c.isFavorite()) favCount++;
        }
        statFavoriteCount.setText(String.valueOf(favCount));

        int score = Math.min(95, 50 + totalCount * 5);
        scoreValue.setText(String.valueOf(score));
        ViewGroup.LayoutParams lp = scoreProgress.getLayoutParams();
        int width = Math.round(getResources().getDisplayMetrics().widthPixels - 48 * 2);
        lp.width = Math.max(40, width * score / 100);
        scoreProgress.setLayoutParams(lp);

        if (score >= 80) {
            scoreHint.setText(R.string.home_score_good);
        } else {
            scoreHint.setText(getString(R.string.home_score_warning, Math.max(1, (100 - score) / 10)));
        }

        // 列表根据 chip 过滤
        List<Credential> filtered = new ArrayList<>();
        for (Credential c : all) {
            switch (currentFilter) {
                case FILTER_FAVORITES:
                    if (c.isFavorite() && matchesSearch(c)) filtered.add(c);
                    break;
                case FILTER_PASSWORDS:
                    if (c.getUsername() != null
                            && !c.getUsername().trim().isEmpty()
                            && matchesSearch(c)) {
                        filtered.add(c);
                    }
                    break;
                case FILTER_NOTES:
                    if (c.getNotes() != null
                            && !c.getNotes().trim().isEmpty()
                            && matchesSearch(c)) {
                        filtered.add(c);
                    }
                    break;
                default:
                    if (matchesSearch(c)) filtered.add(c);
            }
        }

        adapter = new CredentialAdapter(filtered, repository, cred -> launchDetail(cred));
        RecyclerView rv = getView().findViewById(R.id.credentials_recycler);
        rv.setAdapter(adapter);

        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    public void filter(String query) {
        searchQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        refreshList();
    }

    private boolean matchesSearch(Credential credential) {
        if (searchQuery.isEmpty()) return true;
        return contains(credential.getName(), searchQuery)
                || contains(credential.getUsername(), searchQuery)
                || contains(credential.getWebsite(), searchQuery)
                || contains(credential.getNotes(), searchQuery);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
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
}
