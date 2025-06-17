package com.example.axel;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.List;

public class FFTChartView extends LineChart {
    private static final int DEFAULT_COLOR = Color.BLUE;
    private static final float LINE_WIDTH = 1.5f;

    public FFTChartView(Context context) {
        super(context);
        init();
    }

    public FFTChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FFTChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setupChartStyle();
    }

    private void setupChartStyle() {
        getDescription().setEnabled(false);
        setTouchEnabled(true);
        setDragEnabled(true);
        setScaleEnabled(true);
        setPinchZoom(true);
        setDrawGridBackground(false);

        XAxis xAxis = getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);

        getAxisRight().setEnabled(false);
    }

    public void updateFFTData(float[] fftData, float sampleRate, String label, int color) {
        if (fftData == null || fftData.length == 0) {
            clear();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        float freqStep = sampleRate / fftData.length;

        // Отображаем только первую половину спектра
        for (int i = 0; i < fftData.length / 2; i++) {
            entries.add(new Entry(i * freqStep, fftData[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color != 0 ? color : DEFAULT_COLOR);
        dataSet.setLineWidth(LINE_WIDTH);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        setData(lineData);

        // Автоматическое масштабирование по Y
        getAxisLeft().resetAxisMaximum();
        getAxisLeft().resetAxisMinimum();

        // Установка диапазона по X
        getXAxis().setAxisMaximum(sampleRate / 2);

        invalidate();
    }

    public void updateFFTData(float[] fftData, float sampleRate) {
        updateFFTData(fftData, sampleRate, "", DEFAULT_COLOR);
    }

    public void clear() {
        clearAnimation();
        clearFocus();
        if (getData() != null) {
            getData().clearValues();
        }
        invalidate();
    }
}