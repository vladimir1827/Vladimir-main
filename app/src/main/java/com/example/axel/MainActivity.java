package com.example.axel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private static final int FFT_SIZE = 1024;
    private static final float SIGNIFICANCE_THRESHOLD = 0.01f;
    private static final float FILTER_ALPHA = 0.1f;
    private static final int SENSOR_DELAY_US = 20000; // 20ms (50Hz)
    private static final String CSV_HEADER = "Time(s),X,Y,Z\n";
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50MB
    private static final long MIN_STORAGE_REQUIRED = 10 * 1024 * 1024; // 10MB

    // UI elements
    private LineChartView lineChartView;
    private TextView gyroXText, gyroYText, gyroZText, modeIndicator;
    private Button recordButton;

    // Sensor work
    private SensorManager sensorManager;
    private Sensor gyroscope;
    private int currentRefreshRate = 50;
    private long lastUpdateTime = 0;
    private float[] filteredValues = new float[3];

    // Data recording
    private boolean isRecording = false;
    private BufferedWriter csvWriter;
    private File tempCsvFile;
    private long recordingStartTime;

    // FFT analysis
    private double[] fftBufferX = new double[FFT_SIZE];
    private double[] fftBufferY = new double[FFT_SIZE];
    private double[] fftBufferZ = new double[FFT_SIZE];
    private int bufferIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initSensorManager();
        loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensorListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSensorListener();
        if (isRecording) {
            stopRecording();
        }
    }

    private void initViews() {
        lineChartView = findViewById(R.id.line_chart_view);
        gyroXText = findViewById(R.id.gyro_x);
        gyroYText = findViewById(R.id.gyro_y);
        gyroZText = findViewById(R.id.gyro_z);
        modeIndicator = findViewById(R.id.mode_indicator);
        recordButton = findViewById(R.id.record_button);

        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        Button fftButton = findViewById(R.id.fft_button);
        fftButton.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, FFTActivity.class);

                float[] xData = new float[FFT_SIZE];
                float[] yData = new float[FFT_SIZE];
                float[] zData = new float[FFT_SIZE];

                int samplesToCopy = Math.min(bufferIndex, FFT_SIZE);
                for (int i = 0; i < samplesToCopy; i++) {
                    xData[i] = (float)fftBufferX[i];
                    yData[i] = (float)fftBufferY[i];
                    zData[i] = (float)fftBufferZ[i];
                }

                intent.putExtra("x_data", xData);
                intent.putExtra("y_data", yData);
                intent.putExtra("z_data", zData);
                intent.putExtra("sample_rate", currentRefreshRate);

                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "FFT button error", e);
                showToast("Ошибка FFT: " + e.getMessage());
            }
        });

        ImageButton fileListButton = findViewById(R.id.open_file_list_button);
        fileListButton.setOnClickListener(v -> startActivity(new Intent(this, FileListActivity.class)));

        recordButton.setOnClickListener(v -> toggleRecording());
    }

    private void initSensorManager() {
        try {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                if (gyroscope == null) {
                    showToast("Гироскоп не найден!");
                    Log.e(TAG, "Гироскоп не доступен");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации сенсоров", e);
            showToast("Ошибка инициализации датчиков");
        }
    }

    private void registerSensorListener() {
        try {
            if (sensorManager != null && gyroscope != null) {
                sensorManager.registerListener(this, gyroscope, SENSOR_DELAY_US);
                Log.d(TAG, "Сенсор зарегистрирован");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка регистрации слушателя", e);
        }
    }

    private void unregisterSensorListener() {
        try {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
                Log.d(TAG, "Сенсор отключен");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка отключения слушателя", e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < 1000 / currentRefreshRate) {
            return;
        }
        lastUpdateTime = currentTime;

        float[] filtered = applyLowPassFilter(event.values);
        if (isSignificantChange(filtered)) {
            updateUI(filtered);
            if (isRecording) {
                writeToCsv(filtered);
            }
            updateFFTBuffers(filtered);
        }
    }

    private void updateFFTBuffers(float[] values) {
        if (bufferIndex < FFT_SIZE) {
            fftBufferX[bufferIndex] = values[0];
            fftBufferY[bufferIndex] = values[1];
            fftBufferZ[bufferIndex] = values[2];
            bufferIndex++;
        } else {
            System.arraycopy(fftBufferX, 1, fftBufferX, 0, FFT_SIZE - 1);
            System.arraycopy(fftBufferY, 1, fftBufferY, 0, FFT_SIZE - 1);
            System.arraycopy(fftBufferZ, 1, fftBufferZ, 0, FFT_SIZE - 1);

            fftBufferX[FFT_SIZE - 1] = values[0];
            fftBufferY[FFT_SIZE - 1] = values[1];
            fftBufferZ[FFT_SIZE - 1] = values[2];
        }
    }

    private void loadSettings() {
        try {
            SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
            currentRefreshRate = prefs.getInt("RefreshRate", 50);
            boolean keepScreenOn = prefs.getBoolean("KeepScreenOn", false);

            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки настроек", e);
            currentRefreshRate = 50;
        }
    }

    private float[] applyLowPassFilter(float[] values) {
        for (int i = 0; i < 3; i++) {
            filteredValues[i] = filteredValues[i] + FILTER_ALPHA * (values[i] - filteredValues[i]);
        }
        return filteredValues;
    }

    private boolean isSignificantChange(float[] values) {
        return Math.abs(values[0]) > SIGNIFICANCE_THRESHOLD ||
                Math.abs(values[1]) > SIGNIFICANCE_THRESHOLD ||
                Math.abs(values[2]) > SIGNIFICANCE_THRESHOLD;
    }

    private void updateUI(float[] values) {
        runOnUiThread(() -> {
            gyroXText.setText(String.format(Locale.getDefault(), "X: %.4f", values[0]));
            gyroYText.setText(String.format(Locale.getDefault(), "Y: %.4f", values[1]));
            gyroZText.setText(String.format(Locale.getDefault(), "Z: %.4f", values[2]));
            lineChartView.updateData(values);
        });
    }

    private void writeToCsv(float[] values) {
        if (csvWriter == null) {
            Log.e(TAG, "CSV writer is not initialized");
            return;
        }

        if (tempCsvFile != null && tempCsvFile.length() > MAX_FILE_SIZE_BYTES) {
            showToast("Достигнут максимальный размер файла");
            stopRecording();
            return;
        }

        try {
            String timeStamp = String.format(Locale.US, "%.3f",
                    (System.currentTimeMillis() - recordingStartTime) / 1000f);
            String dataLine = String.format(Locale.US, "%s,%.6f,%.6f,%.6f%n",
                    timeStamp, values[0], values[1], values[2]);

            csvWriter.write(dataLine);
            csvWriter.flush();

            Log.d(TAG, "Data written: " + dataLine.trim());
        } catch (IOException e) {
            Log.e(TAG, "CSV write error", e);
            stopRecording();
            showToast("Ошибка записи данных");
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
            recordButton.setText("▶");
        } else {
            startRecording();
            recordButton.setText("■");
        }
    }

    private void startRecording() {
        if (!hasEnoughStorage()) {
            showToast("Недостаточно места для записи");
            return;
        }

        try {
            File storageDir = getFilesDir();
            if (storageDir == null || !storageDir.exists()) {
                showToast("Ошибка доступа к хранилищу");
                return;
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "seismic_" + timeStamp + ".csv";
            tempCsvFile = new File(storageDir, fileName);

            csvWriter = new BufferedWriter(new FileWriter(tempCsvFile));
            csvWriter.write(CSV_HEADER);
            csvWriter.flush();

            recordingStartTime = System.currentTimeMillis();
            isRecording = true;

            showToast("Запись начата: " + fileName);
            Log.d(TAG, "Recording started: " + tempCsvFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Recording start failed", e);
            showToast("Ошибка начала записи");
            isRecording = false;
            closeWriter();
        }
    }

    private void stopRecording() {
        isRecording = false;
        try {
            if (csvWriter != null) {
                csvWriter.flush();
            }

            if (tempCsvFile != null && tempCsvFile.exists() && tempCsvFile.length() > 0) {
                showToast("Запись сохранена: " + tempCsvFile.getName());
                showSaveDialog(tempCsvFile);
            } else if (tempCsvFile != null && tempCsvFile.exists()) {
                tempCsvFile.delete();
                showToast("Пустая запись удалена");
            }
        } catch (Exception e) {
            Log.e(TAG, "Recording stop failed", e);
            showToast("Ошибка сохранения записи");
        } finally {
            closeWriter();
        }
    }

    private boolean hasEnoughStorage() {
        try {
            File path = getFilesDir();
            if (path == null) return false;

            StatFs stat = new StatFs(path.getPath());
            long availableBytes = stat.getAvailableBytes();
            return availableBytes > MIN_STORAGE_REQUIRED;
        } catch (Exception e) {
            Log.e(TAG, "Storage check failed", e);
            return false;
        }
    }

    private void closeWriter() {
        try {
            if (csvWriter != null) {
                csvWriter.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing writer", e);
        } finally {
            csvWriter = null;
        }
    }

    private void showSaveDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сохранить запись");

        final EditText input = new EditText(this);
        String defaultName = file.getName().replace(".csv", "");
        input.setText(defaultName);
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String fileName = input.getText().toString().trim() + ".csv";
            File newFile = new File(getFilesDir(), fileName);
            if (file.renameTo(newFile)) {
                shareFile(newFile);
            } else {
                showToast("Ошибка переименования файла");
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void shareFile(File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Поделиться файлом через"));
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при отправке файла", e);
            showToast("Ошибка при отправке файла");
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Точность датчика изменилась: " + accuracy);
    }

    private static class FFT {
        static void computeFFT(double[] real, double[] imag, int n) {
            if (n <= 1) return;

            double[] evenReal = new double[n/2];
            double[] evenImag = new double[n/2];
            double[] oddReal = new double[n/2];
            double[] oddImag = new double[n/2];

            for (int i = 0; i < n/2; i++) {
                evenReal[i] = real[2*i];
                evenImag[i] = imag[2*i];
                oddReal[i] = real[2*i+1];
                oddImag[i] = imag[2*i+1];
            }

            computeFFT(evenReal, evenImag, n/2);
            computeFFT(oddReal, oddImag, n/2);

            for (int k = 0; k < n/2; k++) {
                double angle = -2 * Math.PI * k / n;
                double tReal = Math.cos(angle) * oddReal[k] - Math.sin(angle) * oddImag[k];
                double tImag = Math.sin(angle) * oddReal[k] + Math.cos(angle) * oddImag[k];

                real[k] = evenReal[k] + tReal;
                imag[k] = evenImag[k] + tImag;
                real[k + n/2] = evenReal[k] - tReal;
                imag[k + n/2] = evenImag[k] - tImag;
            }
        }

        static double[] calculateMagnitude(double[] real, double[] imag) {
            double[] mag = new double[real.length/2];
            for (int i = 0; i < mag.length; i++) {
                mag[i] = Math.sqrt(real[i]*real[i] + imag[i]*imag[i]);
            }
            return mag;
        }
    }
}