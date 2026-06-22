package cn.ntit.passwordmanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import cn.ntit.passwordmanager.util.ClipboardUtil;
import cn.ntit.passwordmanager.util.PasswordGenerator;
import cn.ntit.passwordmanager.util.StrengthEvaluator;

public class GeneratorFragment extends Fragment {

    private static final int MIN_LENGTH = 8;

    private TextView passwordDisplay;
    private TextView lengthValue;
    private SeekBar lengthSlider;
    private SwitchCompat upperSwitch;
    private SwitchCompat lowerSwitch;
    private SwitchCompat numbersSwitch;
    private SwitchCompat symbolsSwitch;
    private View strengthBar;
    private TextView strengthLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_generator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        wireEvents();
        generate();

        MainActivityCallback callback = (MainActivityCallback) requireActivity();
        callback.setToolbarForTab(getString(R.string.nav_generator));
        callback.setFabVisible(false);
    }

    private void bindViews(View view) {
        passwordDisplay = view.findViewById(R.id.password_display);
        lengthValue = view.findViewById(R.id.length_value);
        lengthSlider = view.findViewById(R.id.length_slider);
        strengthBar = view.findViewById(R.id.strength_bar);
        strengthLabel = view.findViewById(R.id.strength_label);

        upperSwitch = ((View) view.findViewById(R.id.row_upper)).findViewById(R.id.toggle_switch);
        lowerSwitch = ((View) view.findViewById(R.id.row_lower)).findViewById(R.id.toggle_switch);
        numbersSwitch = ((View) view.findViewById(R.id.row_numbers)).findViewById(R.id.toggle_switch);
        symbolsSwitch = ((View) view.findViewById(R.id.row_symbols)).findViewById(R.id.toggle_switch);

        ImageView icon;
        TextView label;

        icon = ((View) view.findViewById(R.id.row_upper)).findViewById(R.id.toggle_icon);
        icon.setImageResource(R.drawable.ic_match_case);
        label = ((View) view.findViewById(R.id.row_upper)).findViewById(R.id.toggle_label);
        label.setText(R.string.generator_upper);

        icon = ((View) view.findViewById(R.id.row_lower)).findViewById(R.id.toggle_icon);
        icon.setImageResource(R.drawable.ic_text_fields);
        label = ((View) view.findViewById(R.id.row_lower)).findViewById(R.id.toggle_label);
        label.setText(R.string.generator_lower);

        icon = ((View) view.findViewById(R.id.row_numbers)).findViewById(R.id.toggle_icon);
        icon.setImageResource(R.drawable.ic_numbers);
        label = ((View) view.findViewById(R.id.row_numbers)).findViewById(R.id.toggle_label);
        label.setText(R.string.generator_numbers);

        icon = ((View) view.findViewById(R.id.row_symbols)).findViewById(R.id.toggle_icon);
        icon.setImageResource(R.drawable.ic_alternate_email);
        label = ((View) view.findViewById(R.id.row_symbols)).findViewById(R.id.toggle_label);
        label.setText(R.string.generator_symbols);
    }

    private void wireEvents() {
        lengthSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lengthValue.setText(String.valueOf(progress + MIN_LENGTH));
                generate();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        SwitchCompat[] switches = { upperSwitch, lowerSwitch, numbersSwitch, symbolsSwitch };
        for (SwitchCompat s : switches) {
            s.setOnCheckedChangeListener((b, c) -> generate());
        }

        requireView().findViewById(R.id.regenerate_btn).setOnClickListener(v -> {
            passwordDisplay.setAlpha(0.5f);
            passwordDisplay.postDelayed(() -> {
                generate();
                passwordDisplay.setAlpha(1f);
            }, 100);
        });

        requireView().findViewById(R.id.copy_btn).setOnClickListener(v -> {
            String text = passwordDisplay.getText().toString();
            if (text.isEmpty() || text.equals(getString(R.string.generator_no_options))) return;
            ClipboardUtil.copyPassword(requireContext(), text);
            Toast.makeText(requireContext(), R.string.generator_copied, Toast.LENGTH_SHORT).show();
        });
    }

    private void generate() {
        int len = lengthSlider.getProgress() + MIN_LENGTH;
        boolean upper = upperSwitch.isChecked();
        boolean lower = lowerSwitch.isChecked();
        boolean numbers = numbersSwitch.isChecked();
        boolean symbols = symbolsSwitch.isChecked();

        if (!upper && !lower && !numbers && !symbols) {
            passwordDisplay.setText(R.string.generator_no_options);
            updateStrength(0);
            return;
        }

        String pw = PasswordGenerator.generate(len, upper, lower, numbers, symbols);
        passwordDisplay.setText(pw);
        updateStrength(StrengthEvaluator.score(pw));
    }

    private void updateStrength(int score) {
        int level = StrengthEvaluator.level(score);
        int color;
        int bgRes;
        String text;
        switch (level) {
            case StrengthEvaluator.LEVEL_WEAK:
                color = R.color.md_error;
                bgRes = R.drawable.bg_strength_error;
                text = getString(R.string.generator_strength_weak);
                break;
            case StrengthEvaluator.LEVEL_MEDIUM:
                color = R.color.md_secondary;
                bgRes = R.drawable.bg_strength_secondary;
                text = getString(R.string.generator_strength_medium);
                break;
            case StrengthEvaluator.LEVEL_STRONG:
                color = R.color.md_primary;
                bgRes = R.drawable.bg_strength_primary;
                text = getString(R.string.generator_strength_strong);
                break;
            default:
                color = R.color.md_error;
                bgRes = R.drawable.bg_strength_error;
                text = getString(R.string.generator_strength_weak);
        }
        strengthBar.setBackgroundResource(bgRes);
        android.view.ViewGroup.LayoutParams lp = strengthBar.getLayoutParams();
        int parentWidth = getResources().getDisplayMetrics().widthPixels - 16 * 2 - 24 * 2;
        lp.width = Math.max(8, (int) (parentWidth * (score / 100f)));
        strengthBar.setLayoutParams(lp);
        strengthLabel.setText(text);
        strengthLabel.setTextColor(ContextCompat.getColor(requireContext(), color));
    }
}
