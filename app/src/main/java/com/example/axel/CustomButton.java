package com.example.axel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class CustomButton extends View {

    private List<Float> data;
    private Paint linePaint;

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint = new Paint();
        linePaint.setColor(Color.BLUE); // Используем синий цвет для линий
        linePaint.setStrokeWidth(2);
    }

    public void setData(List<Float> data) {
        this.data = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float stepX = width / (data.size() - 1);

        for (int i = 0; i < data.size() - 1; i++) {
            float startX = i * stepX;
            float startY = height - data.get(i) * height;
            float endX = (i + 1) * stepX;
            float endY = height - data.get(i + 1) * height;
            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }
    }
}