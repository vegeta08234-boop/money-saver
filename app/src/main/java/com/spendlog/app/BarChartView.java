package com.spendlog.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class BarChartView extends View {
    private long[] values = new long[0];
    private int highlight = -1;
    private int accent = 0xFF4F46E5;
    private int textColor = 0xFF6B7280;
    private int lineColor = 0xFFE5E7EB;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setData(long[] v, int highlightIndex) {
        this.values = v == null ? new long[0] : v;
        this.highlight = highlightIndex;
        invalidate();
    }

    void setColors(int accent, int textColor, int lineColor) {
        this.accent = accent;
        this.textColor = textColor;
        this.lineColor = lineColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.length;
        if (n == 0) {
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        float w = getWidth();
        float h = getHeight();
        float labelH = 18 * d;
        float top = 6 * d;
        float chartH = h - labelH - top;
        float baseY = top + chartH;

        long max = 1;
        for (long v : values) {
            if (v > max) {
                max = v;
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(lineColor);
        canvas.drawRect(0, baseY, w, baseY + Math.max(1f, d), paint);

        float slot = w / n;
        float bw = Math.max(d, slot * 0.62f);
        for (int i = 0; i < n; i++) {
            long v = values[i];
            if (v <= 0) {
                continue;
            }
            float bh = Math.max(2 * d, chartH * v / max);
            float left = i * slot + (slot - bw) / 2f;
            rect.set(left, baseY - bh, left + bw, baseY);
            paint.setColor(accent);
            paint.setAlpha(i == highlight ? 255 : 150);
            canvas.drawRoundRect(rect, 2 * d, 2 * d, paint);
        }

        paint.setAlpha(255);
        paint.setColor(textColor);
        paint.setTextSize(10 * d);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            int day = i + 1;
            boolean show = day == 1 || (day % 5 == 0 && n - day >= 3) || day == n;
            if (show) {
                canvas.drawText(String.valueOf(day), i * slot + slot / 2f, h - 3 * d, paint);
            }
        }
    }
}
