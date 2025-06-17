package com.example.axel;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorDataManager {
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorDataListener listener;
    private boolean fftEnabled = false;
    private int fftSize = 1024;

    public SensorDataManager(Context context, SensorDataListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void registerListener() {
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterListener() {
        sensorManager.unregisterListener(sensorEventListener);
    }

    public void setFFTEnabled(boolean enabled, int size) {
        fftEnabled = enabled;
        fftSize = size;
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (listener != null) {
                listener.onSensorDataUpdated(event.values[0], event.values[1], event.values[2], event.values[0]);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Обработка изменения точности датчика
        }
    };

    public interface SensorDataListener {
        void onSensorDataUpdated(float x, float y, float z, float total);
        void onSamplingRateUpdated(float samplingRate);
        void onFFTDataProcessed(float[] fftX, float[] fftY, float[] fftZ);
    }
}