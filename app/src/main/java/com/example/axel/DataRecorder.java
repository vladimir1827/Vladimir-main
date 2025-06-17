package com.example.axel;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DataRecorder {
    private Context context;
    private File tempCsvFile;

    public DataRecorder(Context context) {
        this.context = context;
    }

    public void startRecording(boolean isFFT) {
        // Логика для начала записи данных
        tempCsvFile = new File(context.getFilesDir(), "data.csv");
    }

    public void stopRecording() {
        // Логика для остановки записи данных
    }

    public File getTempCsvFile() {
        return tempCsvFile;
    }
}