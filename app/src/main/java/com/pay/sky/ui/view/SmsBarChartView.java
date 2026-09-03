package com.pay.sky.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;
import com.pay.sky.data.SmsDatabaseHelper;
import java.util.ArrayList;
import java.util.List;

public class SmsBarChartView extends View {

    private final List<SmsDatabaseHelper.DailyStat> dataList = new ArrayList<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int selectedIndex = -1;
    private int maxCount = 1;
    private int maxIndex = -1;

    public SmsBarChartView(Context context) {
        super(context);
        init();
    }

    public SmsBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SmsBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint.setColor(0xFF0284C7);
        highlightBarPaint.setColor(0xFF00B2FE);
        
        textPaint.setColor(0xFF94A3B8);
        textPaint.setTextSize(dpToPx(10));
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint.setColor(0x1594A3B8);
        gridPaint.setStrokeWidth(dpToPx(1));

        tooltipBgPaint.setColor(0xFF0F172A);
        tooltipBgPaint.setShadowLayer(dpToPx(4), 0, dpToPx(2), 0x40000000);

        tooltipTextPaint.setColor(0xFFFFFFFF);
        tooltipTextPaint.setTextSize(dpToPx(11));
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);
        tooltipTextPaint.setFakeBoldText(true);
    }

    public void setData(List<SmsDatabaseHelper.DailyStat> stats) {
        dataList.clear();
        maxCount = 1;
        maxIndex = -1;
        selectedIndex = -1;

        if (stats != null) {
            dataList.addAll(stats);
            for (int i = 0; i < dataList.size(); i++) {
                int c = dataList.get(i).count;
                if (c > maxCount) {
                    maxCount = c;
                    maxIndex = i;
                }
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataList.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        float paddingBottom = dpToPx(24);
        float paddingTop = dpToPx(36);
        float paddingLeft = dpToPx(12);
        float paddingRight = dpToPx(12);

        float chartHeight = height - paddingBottom - paddingTop;
        float chartWidth = width - paddingLeft - paddingRight;

        canvas.drawLine(paddingLeft, paddingTop + chartHeight, width - paddingRight, paddingTop + chartHeight, gridPaint);
        canvas.drawLine(paddingLeft, paddingTop + chartHeight * 0.5f, width - paddingRight, paddingTop + chartHeight * 0.5f, gridPaint);

        int count = dataList.size();
        float totalSlotWidth = chartWidth / count;
        float barWidth = Math.max(dpToPx(6), totalSlotWidth * 0.55f);

        for (int i = 0; i < count; i++) {
            SmsDatabaseHelper.DailyStat stat = dataList.get(i);
            float cx = paddingLeft + (i * totalSlotWidth) + (totalSlotWidth / 2.0f);
            float barH = (stat.count / (float) maxCount) * chartHeight;
            if (stat.count > 0 && barH < dpToPx(4)) {
                barH = dpToPx(4);
            }

            float left = cx - (barWidth / 2.0f);
            float top = paddingTop + chartHeight - barH;
            float right = cx + (barWidth / 2.0f);
            float bottom = paddingTop + chartHeight;

            RectF rect = new RectF(left, top, right, bottom);
            Paint p = (i == maxIndex || i == selectedIndex) ? highlightBarPaint : barPaint;
            float corner = Math.min(barWidth / 2.0f, dpToPx(4));
            canvas.drawRoundRect(rect, corner, corner, p);

            boolean showLabel = (count <= 7) || (i % 5 == 0) || (i == count - 1);
            if (showLabel) {
                canvas.drawText(stat.dateLabel, cx, height - dpToPx(6), textPaint);
            }
        }

        if (selectedIndex >= 0 && selectedIndex < count) {
            SmsDatabaseHelper.DailyStat sel = dataList.get(selectedIndex);
            float cx = paddingLeft + (selectedIndex * totalSlotWidth) + (totalSlotWidth / 2.0f);
            float barH = (sel.count / (float) maxCount) * chartHeight;
            float tipY = Math.max(dpToPx(12), paddingTop + chartHeight - barH - dpToPx(10));

            String text = sel.dateLabel + ": " + sel.count + " SMS";
            float textW = tooltipTextPaint.measureText(text);
            float tipW = textW + dpToPx(18);
            float tipH = dpToPx(24);

            float tipLeft = cx - (tipW / 2.0f);
            if (tipLeft < dpToPx(8)) tipLeft = dpToPx(8);
            if (tipLeft + tipW > width - dpToPx(8)) tipLeft = width - dpToPx(8) - tipW;

            RectF tipRect = new RectF(tipLeft, tipY - tipH, tipLeft + tipW, tipY);
            canvas.drawRoundRect(tipRect, dpToPx(6), dpToPx(6), tooltipBgPaint);
            canvas.drawText(text, tipRect.centerX(), tipRect.centerY() + dpToPx(4), tooltipTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            if (dataList.isEmpty()) return false;

            float x = event.getX();
            float paddingLeft = dpToPx(12);
            float paddingRight = dpToPx(12);
            float chartWidth = getWidth() - paddingLeft - paddingRight;

            int count = dataList.size();
            float totalSlotWidth = chartWidth / count;

            int index = (int) ((x - paddingLeft) / totalSlotWidth);
            if (index >= 0 && index < count) {
                selectedIndex = index;
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
