package app.kidswarmup;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class FrameProgress extends View {

    private static final String TAG = FrameProgress.class.getSimpleName();

    private boolean sizeFixed = false;
    private TextPaint pTextLeft;
    private TextPaint pTextRight;
    private Paint pBigCircle;
    private Paint pSmallCircle;
    private Paint pMainLine;
    private Paint pBigArc;

    private int sectorNum = 0;
    private int selectNum = 0;

    public FrameProgress(Context context) {
        super(context);
        init(null, 0);
    }

    public FrameProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public FrameProgress(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        pTextLeft = new TextPaint();
        pTextLeft.setFlags(Paint.ANTI_ALIAS_FLAG);
        pTextLeft.setTextAlign(Paint.Align.LEFT);
        pTextLeft.setTextSize(120);
        pTextLeft.setStrokeWidth(2.0f);
        pTextLeft.setStyle(Paint.Style.FILL_AND_STROKE);
        pTextLeft.setColor(Color.BLACK);

        pTextRight = new TextPaint(pTextLeft);
        pTextRight.setColor(Color.BLACK);
        pTextRight.setTextAlign(Paint.Align.RIGHT);

        pBigCircle = new Paint();
        pBigCircle.setColor(Color.YELLOW);
        pBigCircle.setStyle(Paint.Style.FILL);

        pSmallCircle = new Paint();
        pSmallCircle.setColor(Color.BLUE);
        pSmallCircle.setStyle(Paint.Style.FILL);

        pMainLine = new Paint();
        pMainLine.setColor(Color.BLACK);
        pMainLine.setStyle(Paint.Style.STROKE);
        pMainLine.setStrokeWidth(4);

        pBigArc = new Paint();
        pBigArc.setColor(0x9F9F0000);  // RED
        pBigArc.setStyle(Paint.Style.FILL);

        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        //mTextPaint.setTextSize(mExampleDimension);
        //mTextPaint.setColor(mExampleColor);
        //mTextWidth = mTextPaint.measureText(mExampleString);
        //Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        //mTextHeight = fontMetrics.bottom;
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent me, int pointerIndex) {
        return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_NULL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int orientation = getResources().getConfiguration().orientation;
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (!sizeFixed) {
            sizeFixed = true;
            Log.i(TAG, "onDraw: orientation = " + orientation + ", width = " + getWidth() + ", height = " + getHeight() + " (first draw)");
            LinearLayout.LayoutParams lp = null;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                lp = new LinearLayout.LayoutParams(viewHeight, -1, 0);
                setLayoutParams(lp);
                setMinimumWidth(viewHeight);
            } else {
                lp = new LinearLayout.LayoutParams(-1, viewWidth, 0);
                setLayoutParams(lp);
                setMinimumHeight(viewWidth);
            }
        }
        //if (getHeight() != getWidth())
        //    return;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        //Log.i(TAG, "onDraw: padding: L = " + paddingLeft + ", T = " + paddingTop + ", R = " + paddingRight + ", B = " + paddingBottom);

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;
        int canvasSize = Math.min(contentWidth, contentHeight) - 2;
        int radius = canvasSize / 2;
        int radius2 = radius * 40 / 100;
        int canvasCX = contentWidth / 2;
        int canvasCY = contentHeight / 2;

        canvas.drawCircle(canvasCX, canvasCY, radius, pBigCircle);
        canvas.drawCircle(canvasCX, canvasCY, radius, pMainLine);

        float startAngle = -90;
        float sweepAngle = 360f / (float)sectorNum;

        final RectF oval = new RectF();
        oval.set(canvasCX - radius, canvasCY - radius, canvasCX + radius,canvasCY + radius);
        for (int i = 0; i < sectorNum; i++) {
            float a = startAngle + i * sweepAngle;
            if (i < selectNum) {
                canvas.drawArc(oval, a, sweepAngle, true, pBigArc);
            }
            canvas.drawArc(oval, a, sweepAngle, true, pMainLine);
        }
        canvas.drawCircle(canvasCX, canvasCY, radius2, pSmallCircle);
        canvas.drawCircle(canvasCX, canvasCY, radius2, pMainLine);

        canvas.drawText(Integer.toString(selectNum),4, (int)pTextLeft.getTextSize(), pTextLeft);
        canvas.drawText(Integer.toString(sectorNum),viewWidth - 4, (int)pTextRight.getTextSize(), pTextRight);
    }

    public int getSelectNumber() {
        return selectNum;
    }

    public void setSelectNumber(int num) {
        selectNum = num;
        //invalidateTextPaintAndMeasurements();
    }

    public int getSectorNumber() {
        return sectorNum;
    }

    public void setSectorNumber(int num) {
        sectorNum = num;
        //invalidateTextPaintAndMeasurements();
    }

}
