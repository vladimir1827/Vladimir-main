package com.example.axel;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RecordingService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean isFFT = intent.getBooleanExtra("isFFT", false);
        // Логика для записи данных
        return START_NOT_STICKY;
    }
}
