package cn.it.cast.keshe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import cn.it.cast.keshe.util.PasswordGenerator;
import cn.it.cast.keshe.util.ClipboardUtil;
import cn.it.cast.keshe.util.SessionManager;
import cn.it.cast.keshe.util.StrengthEvaluator;

public class PasswordGeneratorActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generator);

        bindViews();
        wireEvents();
        generate();
    }

    private void bindViews() {
        passwordDisplay = findViewById(R.id.password_display);
        lengthValue = findViewById(R.id.length_value);
        lengthSlider = findViewById(R.id.length_slider);
        strengthBar = findViewById(R.id.strength_bar);
        strengthLabel = findViewById(R.id.strength_label);

        upperSwitch = ((View) findViewById(R.id.row_upper)).findViewById(R.id.toggle_switch);
        lowerSwitch = ((View) findViewById(R.id.row_lower)).findViewById(R.id.toggle_switch);
        numbersSwitch = ((View) findViewById(R.id.row_numbers)).findViewById(R.id.toggle_switch);
        symbolsSwitch = ((View) findViewById(R.id.row_symbols)).findViewById(R.id.toggle_switch);

        ImageView icon;
        TextView label;

        icon = ((View) findViewById(R.id.row_upper)).findViewById(R.id.toggle_icon);
        icon.setImageResource(R.drawable.ic_match_case);
        label = ((View) findViewById(R.id.row_upper)).findViewById(R.id.toggle_label);
        label.setText(R.string.generator_upper);

        icon = ((View) findViewById(R.id.row_lower)).findViewById(R.id.toggle_icon);
        icon.setImageResource(R.drawable.ic_text_fields);
        label = ((View) findViewById(R.id.row_lower)).findViewById(R.id.toggle_label);
        label.setText(R.string.generator_lower);

        icon = ((View) findViewById(R.id.row_numbers)).findViewById(R.id.toggle_icon);
        icon.setImageResource(R.drawable.ic_numbers);
        label = ((View) findViewById(R.id.row_numbers)).findViewById(R.id.toggle_label);
        label.setText(R.string.generator_numbers);

        icon = ((View) findViewById(R.id.row_symbols)).findViewById(R.id.toggle_icon);
        icon.setImageResource(R.drawable.ic_alternate_email);
        label = ((View) findViewById(R.id.row_symbols)).findViewById(R.id.toggle_label);
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

        findViewById(R.id.regenerate_btn).setOnClickListener(v -> {
            passwordDisplay.setAlpha(0.5f);
            passwordDisplay.postDelayed(() -> {
                generate();
                passwordDisplay.setAlpha(1f);
            }, 100);
        });

        findViewById(R.id.copy_btn).setOnClickListener(v -> {
            String text = passwordDisplay.getText().toString();
            if (text.isEmpty() || text.equals(getString(R.string.generator_no_options))) return;
            ClipboardUtil.copyPassword(this, text);
            Toast.makeText(this, R.string.generator_copied, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
        findViewById(R.id.nav_vault).setOnClickListener(v -> {
            // 返回主页
            startActivity(new Intent(this, MainActivity.class));
        });
        findViewById(R.id.nav_security).setOnClickListener(v ->
                startActivity(new Intent(this, SecurityActivity.class)));
        findViewById(R.id.nav_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
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
        int parentWidth = getResources().getDisplayMetrics().widthPixels - 16 * 2 - 24 * 2; // padding estimates
        lp.width = Math.max(8, (int) (parentWidth * (score / 100f)));
        strengthBar.setLayoutParams(lp);
        strengthLabel.setText(text);
        strengthLabel.setTextColor(getResources().getColor(color, getTheme()));
    }
}
