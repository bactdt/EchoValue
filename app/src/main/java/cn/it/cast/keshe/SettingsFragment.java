package cn.it.cast.keshe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.it.cast.keshe.util.SessionManager;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindUser(view);
        wireEvents(view);

        MainActivityCallback callback = (MainActivityCallback) requireActivity();
        callback.setToolbarTitle(getString(R.string.nav_settings), false, false);
        callback.setFabVisible(false);
    }

    private void bindUser(View view) {
        SessionManager session = new SessionManager(requireContext());
        String name = session.getUserName();
        String email = session.getUserEmail();
        ((TextView) view.findViewById(R.id.settings_user_name)).setText(
                TextUtils.isEmpty(name) ? getString(R.string.settings_unknown_user) : name);
        ((TextView) view.findViewById(R.id.settings_user_email)).setText(
                TextUtils.isEmpty(email) ? getString(R.string.settings_unknown_email) : email);
    }

    private void wireEvents(View view) {
        view.findViewById(R.id.logout_button).setOnClickListener(v -> {
            VaultApp.clearMasterPassword();
            new SessionManager(requireContext()).clearAll();
            MainActivityCallback callback = (MainActivityCallback) requireActivity();
            callback.logout();
        });
    }
}
