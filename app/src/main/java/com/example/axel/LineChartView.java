package com.example.axel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LineChartView extends View {
    private Paint linePaint, gridPaint, textPaint, spectrumPaint;
    private List<Float> xValues = new ArrayList<>();
    private List<Float> yValues = new ArrayList<>();
    private List<Float> zValues = new ArrayList<>();
    private float[] spectrumValues = new float[0];

    private static final float TIME_AXIS_MIN = -1.0f;
    private static final float TIME_AXIS_MAX = 1.0f;
    private static final float FREQ_AXIS_MIN = 0f;
    private static final float FREQ_AXIS_MAX = 25f;
    private static final int MAX_POINTS = 100;
    private static final int PADDING = 50;

    private boolean showSpectrum = false;
    private float samplingRate = 50f;

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        spectrumPaint = new Paint();
        spectrumPaint.setColor(Color.MAGENTA);
        spectrumPaint.setStrokeWidth(3f);
        spectrumPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        spectrumPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
    }

    // Новый метод для обновления данных
    public void updateData(float[] values) {
        if (values == null || values.length < 3) return;

        addDataPoint(values[0], values[1], values[2], 0);
    }

    public void addDataPoint(float x, float y, float z, float total) {
        xValues.add(x);
        yValues.add(y);
        zValues.add(z);

        if (xValues.size() > MAX_POINTS) xValues.remove(0);
        if (yValues.size() > MAX_POINTS) yValues.remove(0);
        if (zValues.size() > MAX_POINTS) zValues.remove(0);

        invalidate();
    }

    public void updateSpectrum(float[] magnitudes, float samplingRate) {
        this.spectrumValues = magnitudes;
        this.samplingRate = samplingRate;
        this.showSpectrum = true;
        invalidate();
    }

    public void hideSpectrum() {
        this.showSpectrum = false;
        invalidate();
    }

    public boolean isShowingSpectrum() {
        return showSpectrum;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (showSpectrum) {
            drawSpectrum(canvas);
        } else {
            drawTimeDomain(canvas);
        }
    }

    private void drawTimeDomain(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        drawGrid(canvas, width, height, PADDING, TIME_AXIS_MIN, TIME_AXIS_MAX, "Time Domain");

        drawLineChart(canvas, xValues, Color.BLUE, width, height, PADDING, TIME_AXIS_MIN, TIME_AXIS_MAX);
        drawLineChart(canvas, yValues, Color.RED, width, height, PADDING, TIME_AXIS_MIN, TIME_AXIS_MAX);
        drawLineChart(canvas, zValues, Color.GREEN, width, height, PADDING, TIME_AXIS_MIN, TIME_AXIS_MAX);
    }

    private void drawSpectrum(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        drawGrid(canvas, width, height, PADDING, FREQ_AXIS_MIN, FREQ_AXIS_MAX, "Frequency Domain");

        if (spectrumValues.length < 2) return;

        float graphHeight = height - 2 * PADDING;
        float graphWidth = width - 2 * PADDING;
        float binWidth = graphWidth / (spectrumValues.length / 2);
        float maxMagnitude = findMax(spectrumValues);

        if (maxMagnitude <= 0) return;

        for (int i = 0; i < spectrumValues.length / 2; i++) {
            float freq = i * (samplingRate / spectrumValues.length);
            if (freq > FREQ_AXIS_MAX) break;

            float x = PADDING + i * binWidth;
            float barHeight = (spectrumValues[i] / maxMagnitude) * graphHeight;
            float y = height - PADDING - barHeight;

            canvas.drawRect(x, y, x + binWidth, height - PADDING, spectrumPaint);
        }
    }

    private void drawGrid(Canvas canvas, int width, int height, int padding, float yMin, float yMax, String title) {
        float graphHeight = height - 2 * padding;
        float graphWidth = width - 2 * padding;

        canvas.drawText(title, width / 2 - title.length() * 10, padding - 10, textPaint);

        for (float i = yMin; i <= yMax; i += (yMax - yMin)/5) {
            float y = padding + graphHeight * (1 - (i - yMin) / (yMax - yMin));
            canvas.drawLine(padding, y, width - padding, y, gridPaint);
            canvas.drawText(String.format("%.1f", i), padding - 45, y + 10, textPaint);
        }

        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            float x = padding + i * (graphWidth / steps);
            canvas.drawLine(x, padding, x, height - padding, gridPaint);
        }
    }

    private void drawLineChart(Canvas canvas, List<Float> values, int color,
                               int width, int height, int padding, float yMin, float yMax) {
        if (values.size() < 2) return;

        float graphHeight = height - 2 * padding;
        float graphWidth = width - 2 * padding;

        linePaint.setColor(color);

        int points = Math.min(values.size(), MAX_POINTS);
        float pointSpacing = graphWidth / (MAX_POINTS - 1);

        for (int i = 1; i < points; i++) {
            float prevValue = values.get(i-1);
            float currValue = values.get(i);

            float normalizedPrev = (prevValue - yMin) / (yMax - yMin);
            float normalizedCurr = (currValue - yMin) / (yMax - yMin);

            float startX = padding + (i-1) * pointSpacing;
            float startY = padding + graphHeight * (1 - normalizedPrev);
            float stopX = padding + i * pointSpacing;
            float stopY = padding + graphHeight * (1 - normalizedCurr);

            canvas.drawLine(startX, startY, stopX, stopY, linePaint);
        }
    }

    private float findMax(float[] array) {
        float max = Float.MIN_VALUE;
        for (float value : array) {
            if (value > max) max = value;
        }
        return max;
    }
}