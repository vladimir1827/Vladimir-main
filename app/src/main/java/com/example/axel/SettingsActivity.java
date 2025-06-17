package com.example.axel;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "AppSettings";
    private static final int MIN_REFRESH_RATE = 1;
    private static final int MAX_REFRESH_RATE = 60;
    private static final int DEFAULT_REFRESH_RATE = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_settings);
            initializeUI();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации", e);
            Toast.makeText(this, "Ошибка загрузки настроек", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeUI() {
        // Инициализация элементов
        MaterialButton backButton = findViewById(R.id.back_button);
        SwitchMaterial keepScreenOnSwitch = findViewById(R.id.keep_screen_on);
        Slider refreshRateSlider = findViewById(R.id.refresh_rate_slider);
        TextView refreshRateValue = findViewById(R.id.refresh_rate_value);

        // Настройка ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Обработчик кнопки "Назад"
        backButton.setOnClickListener(v -> finish());

        // Загрузка настроек
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Настройка переключателя
        boolean keepScreenOn = prefs.getBoolean("KeepScreenOn", false);
        keepScreenOnSwitch.setChecked(keepScreenOn);
        updateScreenOnState(keepScreenOn);

        // Настройка слайдера
        int refreshRate = prefs.getInt("RefreshRate", DEFAULT_REFRESH_RATE);
        refreshRateSlider.setValueTo(MAX_REFRESH_RATE);
        refreshRateSlider.setValueFrom(MIN_REFRESH_RATE);
        refreshRateSlider.setValue(refreshRate);
        refreshRateSlider.setStepSize(1);
        refreshRateValue.setText(String.format(Locale.getDefault(), "%d Hz", refreshRate));

        // Обработчики изменений
        keepScreenOnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                prefs.edit().putBoolean("KeepScreenOn", isChecked).apply();
                updateScreenOnState(isChecked);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения настроек экрана", e);
            }
        });

        refreshRateSlider.addOnChangeListener((slider, value, fromUser) -> {
            try {
                int rate = (int) value;
                refreshRateValue.setText(String.format(Locale.getDefault(), "%d Hz", rate));
                prefs.edit().putInt("RefreshRate", rate).apply();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения частоты обновления", e);
            }
        });
    }

    private void updateScreenOnState(boolean keepScreenOn) {
        try {
            if (keepScreenOn) {
                getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обновления состояния экрана", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == android.R.id.home) {
                finish();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обработки нажатия кнопки", e);
        }
        return super.onOptionsItemSelected(item);
    }
}