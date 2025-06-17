package com.example.axel;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class FFTActivity extends AppCompatActivity {
    private FFTChartView fftXChart, fftYChart, fftZChart;
    private TextView samplingRateText;
    private Button recordButton;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fft);

        try {
            initViews();

            Intent intent = getIntent();
            if (intent != null) {
                float[] xData = intent.getFloatArrayExtra("x_data");
                float[] yData = intent.getFloatArrayExtra("y_data");
                float[] zData = intent.getFloatArrayExtra("z_data");
                float sampleRate = intent.getFloatExtra("sample_rate", 50f);

                samplingRateText.setText(String.format(Locale.US,
                        "Частота дискретизации: %.1f Гц", sampleRate));

                // Вычисление и отображение FFT
                processFFTData(xData, yData, zData, sampleRate);
            }
        } catch (Exception e) {
            Log.e("FFT_Activity", "Error in FFT activity", e);
        }

        recordButton.setOnClickListener(v -> toggleRecording());
    }

    private void initViews() {
        fftXChart = findViewById(R.id.fft_x_chart);
        fftYChart = findViewById(R.id.fft_y_chart);
        fftZChart = findViewById(R.id.fft_z_chart);
        samplingRateText = findViewById(R.id.sampling_rate_text);
        recordButton = findViewById(R.id.record_button);
    }

    private void processFFTData(float[] xData, float[] yData, float[] zData, float sampleRate) {
        if (xData != null && xData.length > 0) {
            float[] fftX = calculateFFT(xData);
            fftXChart.updateFFTData(fftX, sampleRate, "FFT X", Color.BLUE);
        }
        if (yData != null && yData.length > 0) {
            float[] fftY = calculateFFT(yData);
            fftYChart.updateFFTData(fftY, sampleRate, "FFT Y", Color.RED);
        }
        if (zData != null && zData.length > 0) {
            float[] fftZ = calculateFFT(zData);
            fftZChart.updateFFTData(fftZ, sampleRate, "FFT Z", Color.GREEN);
        }
    }

    private float[] calculateFFT(float[] input) {
        int n = input.length;
        double[] real = new double[n];
        double[] imag = new double[n];

        for (int i = 0; i < n; i++) {
            real[i] = input[i];
        }

        FFT.computeFFT(real, imag, n);

        float[] magnitudes = new float[n/2];
        for (int i = 0; i < n/2; i++) {
            magnitudes[i] = (float) Math.sqrt(real[i]*real[i] + imag[i]*imag[i]);
        }

        return magnitudes;
    }

    private void toggleRecording() {
        isRecording = !isRecording;
        recordButton.setText(isRecording ? "■" : "▶");
        if (isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        Log.d("FFTActivity", "Recording started");
    }

    private void stopRecording() {
        Log.d("FFTActivity", "Recording stopped");
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
    }
}