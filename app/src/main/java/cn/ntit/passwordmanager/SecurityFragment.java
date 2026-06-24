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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.ntit.passwordmanager.data.CredentialRepository;
import cn.ntit.passwordmanager.model.Credential;
import cn.ntit.passwordmanager.util.SessionManager;
import cn.ntit.passwordmanager.util.StrengthEvaluator;

public class SecurityFragment extends Fragment {

    private CredentialRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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

        // 守卫：必须已登录、已解锁
        SessionManager session = new SessionManager(requireContext());
        if (!session.isLoggedIn() || !session.isUnlocked() || !VaultApp.hasMasterPassword()) {
            ((MainActivityCallback) requireActivity()).logout();
            return;
        }

        repository = new CredentialRepository(requireContext());
        repository.setMasterPassword(VaultApp.getMasterPassword());

        bindViews(view);
        showPlaceholders();
        refreshOverview();
    }

    @Override
    public void onDestroyView() {
        executor.shutdownNow();
        super.onDestroyView();
    }

    private void showPlaceholders() {
        scoreValue.setText(R.string.security_placeholder);
        savedCount.setText(R.string.security_placeholder);
        weakCount.setText(R.string.security_placeholder);
        attentionCount.setText(R.string.security_placeholder);
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

    public void refreshOverview() {
        if (repository == null) return;
        final CredentialRepository repo = repository;
        executor.execute(() -> {
            List<Credential> credentials = repo.list();
            int saved = credentials.size();
            int weak = 0;
            int attention = 0;

            for (Credential credential : credentials) {
                String password = repo.decrypt(credential.getPasswordEncrypted());
                int level = StrengthEvaluator.level(StrengthEvaluator.score(password));
                if (level == StrengthEvaluator.LEVEL_WEAK || level == StrengthEvaluator.LEVEL_NONE) {
                    weak++;
                }
                if (level != StrengthEvaluator.LEVEL_STRONG) {
                    attention++;
                }
            }

            int score = saved == 0 ? 100 : Math.max(0, 100 - weak * 15 - (attention - weak) * 8);
            final int fScore = score, fSaved = saved, fWeak = weak, fAttention = attention;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || getView() == null) return;
                scoreValue.setText(String.valueOf(fScore));
                savedCount.setText(String.valueOf(fSaved));
                weakCount.setText(String.valueOf(fWeak));
                attentionCount.setText(String.valueOf(fAttention));
                scoreHint.setText(fAttention == 0
                        ? R.string.security_score_good
                        : R.string.security_score_attention);
                updateScoreBar(fScore);
            });
        });
    }

    private void updateScoreBar(int score) {
        scoreTrack.post(() -> {
            ViewGroup.LayoutParams lp = scoreBar.getLayoutParams();
            lp.width = Math.max(0, scoreTrack.getWidth() * score / 100);
            scoreBar.setLayoutParams(lp);
        });
    }
}
