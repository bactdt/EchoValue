package cn.ntit.passwordmanager;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.fragment.app.Fragment;

import cn.ntit.passwordmanager.data.VaultDbHelper;
import cn.ntit.passwordmanager.model.UserAccount;
import cn.ntit.passwordmanager.util.CryptoUtil;
import cn.ntit.passwordmanager.util.SessionManager;

public class SettingsFragment extends Fragment {

    private SessionManager session;
    private VaultDbHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        session = new SessionManager(requireContext());
        dbHelper = new VaultDbHelper(requireContext());

        bindUser(view);
        bindPinLock(view);
        bindVersion(view);
        wireEvents(view);
    }

    private void bindUser(View view) {
        String name = session.getUserName();
        String email = session.getUserEmail();
        ((TextView) view.findViewById(R.id.settings_user_name)).setText(
                TextUtils.isEmpty(name) ? getString(R.string.settings_unknown_user) : name);
        ((TextView) view.findViewById(R.id.settings_user_email)).setText(
                TextUtils.isEmpty(email) ? getString(R.string.settings_unknown_email) : email);
    }

    private void bindVersion(View view) {
        TextView versionView = view.findViewById(R.id.settings_version);
        String version = "";
        try {
            PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            version = "v" + info.versionName + " (" + PackageInfoCompat.getLongVersionCode(info) + ")";
        } catch (PackageManager.NameNotFoundException e) {
            version = "v?";
        }
        versionView.setText(version);
    }

    private void bindPinLock(View view) {
        EditText pinEdit = view.findViewById(R.id.pin_edit);
        Button savePinButton = view.findViewById(R.id.save_pin_button);
        SwitchCompat pinLockSwitch = view.findViewById(R.id.pin_lock_switch);
        TextView statusText = view.findViewById(R.id.pin_status_text);

        refreshPinLockUi(pinLockSwitch, statusText);

        savePinButton.setOnClickListener(v -> {
            String pin = pinEdit.getText().toString().trim();
            if (!pin.matches("\\d{6}")) {
                Toast.makeText(requireContext(), R.string.settings_pin_invalid, Toast.LENGTH_SHORT).show();
                return;
            }

            String[] hash = CryptoUtil.hashPassword(pin);
            String storedPin = hash[0] + ":" + hash[1];
            int updated = dbHelper.updateUserPin(session.getUserEmail(), storedPin);
            if (updated <= 0) {
                Toast.makeText(requireContext(), R.string.settings_pin_update_failed, Toast.LENGTH_SHORT).show();
                return;
            }

            pinEdit.setText("");
            refreshPinLockUi(pinLockSwitch, statusText);
            Toast.makeText(requireContext(), R.string.settings_pin_saved, Toast.LENGTH_SHORT).show();
        });

        pinLockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !hasConfiguredPin()) {
                buttonView.setChecked(false);
                Toast.makeText(requireContext(), R.string.settings_pin_enable_requires_pin, Toast.LENGTH_SHORT).show();
                return;
            }
            session.setPinLockEnabled(isChecked);
            updatePinStatus(statusText, hasConfiguredPin(), isChecked);
        });
    }

    private void refreshPinLockUi(SwitchCompat pinLockSwitch, TextView statusText) {
        boolean hasPin = hasConfiguredPin();
        boolean enabled = hasPin && session.isPinLockEnabled();
        pinLockSwitch.setEnabled(hasPin);
        pinLockSwitch.setChecked(enabled);
        updatePinStatus(statusText, hasPin, enabled);
    }

    private void updatePinStatus(TextView statusText, boolean hasPin, boolean enabled) {
        if (!hasPin) {
            statusText.setText(R.string.settings_pin_not_configured);
        } else if (enabled) {
            statusText.setText(R.string.settings_pin_enabled);
        } else {
            statusText.setText(R.string.settings_pin_configured_disabled);
        }
    }

    private boolean hasConfiguredPin() {
        UserAccount user = dbHelper.findUserByEmail(session.getUserEmail());
        return user != null && user.getPinHash() != null && !user.getPinHash().trim().isEmpty();
    }

    private void wireEvents(View view) {
        view.findViewById(R.id.logout_button).setOnClickListener(v -> {
            VaultApp.clearMasterPassword();
            session.clearAll();
            MainActivityCallback callback = (MainActivityCallback) requireActivity();
            callback.logout();
        });
    }
}
