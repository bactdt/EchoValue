package cn.ntit.passwordmanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import cn.ntit.passwordmanager.data.CredentialRepository;
import cn.ntit.passwordmanager.model.Credential;
import cn.ntit.passwordmanager.util.StrengthEvaluator;

public class SecurityFragment extends Fragment {

    private CredentialRepository repository;
    private TextView scoreValue;
    private TextView scoreHint;
    private TextView savedCount;
    private TextView weakCount;
    private TextView attentionCount;
    private View scoreTrack;
    private View scoreBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_security, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new CredentialRepository(requireContext());
        repository.setMasterPassword(VaultApp.getMasterPassword());

        bindViews(view);
        populateOverview();

        MainActivityCallback callback = (MainActivityCallback) requireActivity();
        callback.setToolbarForTab(getString(R.string.nav_security));
        callback.setFabVisible(false);
    }

    private void bindViews(View view) {
        scoreValue = view.findViewById(R.id.security_score_value);
        scoreHint = view.findViewById(R.id.security_score_hint);
        savedCount = view.findViewById(R.id.security_saved_count);
        weakCount = view.findViewById(R.id.security_weak_count);
        attentionCount = view.findViewById(R.id.security_attention_count);
        scoreTrack = view.findViewById(R.id.security_score_track);
        scoreBar = view.findViewById(R.id.security_score_bar);
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
            ViewGroup.LayoutParams lp = scoreBar.getLayoutParams();
            lp.width = Math.max(0, scoreTrack.getWidth() * score / 100);
            scoreBar.setLayoutParams(lp);
        });
    }
}
