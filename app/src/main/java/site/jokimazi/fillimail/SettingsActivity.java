package site.jokimazi.fillimail;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_settings);
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        RadioGroup rgTheme = findViewById(R.id.rg_theme);
        RadioGroup rgLang = findViewById(R.id.rg_lang);

        int theme = prefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (theme == AppCompatDelegate.MODE_NIGHT_NO) {
            rgTheme.check(R.id.rb_theme_light);
        } else if (theme == AppCompatDelegate.MODE_NIGHT_YES) {
            rgTheme.check(R.id.rb_theme_dark);
        } else {
            rgTheme.check(R.id.rb_theme_system);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.rb_theme_light) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.rb_theme_dark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            }
            prefs.edit().putInt("theme", mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
        });

        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (currentLocales.isEmpty()) {
            rgLang.check(R.id.rb_lang_system);
        } else {
            String lang = currentLocales.get(0).getLanguage();
            if (lang.equals("ru")) {
                rgLang.check(R.id.rb_lang_ru);
            } else if (lang.equals("en")) {
                rgLang.check(R.id.rb_lang_en);
            } else {
                rgLang.check(R.id.rb_lang_system);
            }
        }

        rgLang.setOnCheckedChangeListener((group, checkedId) -> {
            LocaleListCompat locales = LocaleListCompat.getEmptyLocaleList();
            if (checkedId == R.id.rb_lang_ru) {
                locales = LocaleListCompat.forLanguageTags("ru");
            } else if (checkedId == R.id.rb_lang_en) {
                locales = LocaleListCompat.forLanguageTags("en");
            }
            AppCompatDelegate.setApplicationLocales(locales);
        });
    }
}