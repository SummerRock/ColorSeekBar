package com.rtugeek.android.colorseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.support.annotation.ArrayRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


public class ColorSeekBar extends View {
    private int[] mColorSeeds = new int[]{0xFF000000, 0xFF9900FF, 0xFF0000FF, 0xFF00FF00, 0xFF00FFFF, 0xFFFF0000, 0xFFFF00FF, 0xFFFF6600, 0xFFFFFF00, 0xFFFFFFFF, 0xFF000000};
    private int mAlpha;
    private OnColorChangeListener mOnColorChangeLister;
    private Context mContext;
    private boolean mIsShowAlphaBar = false;
    private boolean mIsVertical;
    private boolean mMovingColorBar;
    private boolean mMovingAlphaBar;
    private Bitmap mTransparentBitmap;
    private Rect mColorRect;
    private int mThumbHeight = 20;
    private float mThumbRadius;
    private int mBarHeight = 2;
    private Paint mColorRectPaint;
    private int realLeft;
    private int realRight;
    private int realTop;
    private int realBottom;
    private int mBarWidth;
    private int mMaxPosition;
    private Rect mAlphaRect = new Rect();
    private int mColorBarPosition;
    private int mAlphaBarPosition;
    private int mBarMargin = 5;
    private int mAlphaMinPosition = 0;
    private int mAlphaMaxPosition = 255;
    private List<Integer> mColors = new ArrayList<>();
    private int mColorsToInvoke = -1;
    private boolean mInit = false;
    private boolean mFirstDraw = true;
    private OnInitDoneListener mOnInitDoneListener;

    private int[] toAlpha = new int[2];

    private boolean showPreviewCircle;
    private float previewCircleStrokeWidth;
    private float previewCircleRadius;
    private int previewCircleMargin;
    private Paint previewCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Paint previewStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private Paint colorPaint = new Paint();
    private Paint alphaThumbGradientPaint = new Paint();
    private Paint alphaBarPaint = new Paint();
    private Paint thumbGradientPaint = new Paint();

    public ColorSeekBar(Context context) {
        this(context, null);
    }

    public ColorSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyStyle(context, attrs);
    }

    private void applyStyle(Context context, AttributeSet attrs) {
        mContext = context;
        //get attributes

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorSeekBar);
        int colorsId = a.getResourceId(R.styleable.ColorSeekBar_colorSeeds, 0);
        mMaxPosition = a.getInteger(R.styleable.ColorSeekBar_maxPosition, 100);
        mColorBarPosition = a.getInteger(R.styleable.ColorSeekBar_colorBarPosition, 0);
        mAlphaBarPosition = a.getInteger(R.styleable.ColorSeekBar_alphaBarPosition, mAlphaMinPosition);
        mIsVertical = a.getBoolean(R.styleable.ColorSeekBar_isVertical, false);
        mIsShowAlphaBar = a.getBoolean(R.styleable.ColorSeekBar_showAlphaBar, false);
        int mBackgroundColor = a.getColor(R.styleable.ColorSeekBar_bgColor, Color.TRANSPARENT);
        mBarHeight = (int) a.getDimension(R.styleable.ColorSeekBar_barHeight, (float) dp2px(2));
        mThumbHeight = (int) a.getDimension(R.styleable.ColorSeekBar_thumbHeight, (float) dp2px(16));
        mBarMargin = (int) a.getDimension(R.styleable.ColorSeekBar_barMargin, (float) dp2px(5));

        showPreviewCircle = a.getBoolean(R.styleable.ColorSeekBar_previewEnable, false) && !mIsVertical;
        previewCircleStrokeWidth = a.getDimension(R.styleable.ColorSeekBar_previewStrokeWidth, dp2px(2));
        previewCircleRadius = a.getDimension(R.styleable.ColorSeekBar_previewRadius, dp2px(24));
        previewCircleMargin = (int) a.getDimension(R.styleable.ColorSeekBar_previewMargin, dp2px(6));
        a.recycle();

        if (colorsId != 0) {
            mColorSeeds = getColorsById(colorsId);
        }

        setBackgroundColor(mBackgroundColor);

        previewCirclePaint.setColor(Color.TRANSPARENT);
        previewCirclePaint.setStyle(Paint.Style.FILL);
        previewCirclePaint.setStrokeJoin(Paint.Join.ROUND);

        previewStrokePaint.setColor(Color.WHITE);
        previewStrokePaint.setStyle(Paint.Style.STROKE);
        previewStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        previewStrokePaint.setStrokeWidth(previewCircleStrokeWidth * 2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mViewWidth = widthMeasureSpec;
        int mViewHeight = heightMeasureSpec;

        int widthSpeMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpeMode = MeasureSpec.getMode(heightMeasureSpec);

        int barHeight = mIsShowAlphaBar ? mBarHeight * 2 : mBarHeight;
        int thumbHeight = mIsShowAlphaBar ? mThumbHeight * 2 : mThumbHeight;

        if (isVertical()) {
            if (widthSpeMode == MeasureSpec.AT_MOST || widthSpeMode == MeasureSpec.UNSPECIFIED) {
                mViewWidth = thumbHeight + barHeight + mBarMargin;
                setMeasuredDimension(mViewWidth, mViewHeight);
            }

        } else {
            if (heightSpeMode == MeasureSpec.AT_MOST || heightSpeMode == MeasureSpec.UNSPECIFIED) {
                mViewHeight = thumbHeight + barHeight + mBarMargin + getPreviewCircleTotalHeight() + 2;
                setMeasuredDimension(mViewWidth, mViewHeight);
            }
        }
    }

    private int getPreviewCircleTotalHeight() {
        if (showPreviewCircle) {
            return (int) (previewCircleMargin + previewCircleRadius * 2 + previewCircleStrokeWidth * 2);
        } else {
            return 0;
        }
    }

    /**
     * @param id color array resource
     */
    private int[] getColorsById(@ArrayRes int id) {
        if (isInEditMode()) {
            String[] s = mContext.getResources().getStringArray(id);
            int[] colors = new int[s.length];
            for (int j = 0; j < s.length; j++) {
                colors[j] = Color.parseColor(s[j]);
            }
            return colors;
        } else {
            TypedArray typedArray = mContext.getResources().obtainTypedArray(id);
            int[] colors = new int[typedArray.length()];
            for (int j = 0; j < typedArray.length(); j++) {
                colors[j] = typedArray.getColor(j, Color.BLACK);
            }
            typedArray.recycle();
            return colors;
        }
    }

    private void init() {
        //init size
        mThumbRadius = mThumbHeight / 2;
        int horizontalPaddingSize;
        int verticalPaddingSize;
        if (mThumbRadius > previewCircleRadius + previewCircleStrokeWidth) {
            horizontalPaddingSize = (int) mThumbRadius;
        } else {
            horizontalPaddingSize = (int) (previewCircleRadius + previewCircleStrokeWidth);
        }

        verticalPaddingSize = getPreviewCircleTotalHeight() + mThumbHeight / 2;
        int viewBottom = getHeight() - getPaddingBottom() - horizontalPaddingSize;
        int viewRight = getWidth() - getPaddingRight() - horizontalPaddingSize;
        //init l r t b
        realLeft = getPaddingLeft() + horizontalPaddingSize;
        realRight = mIsVertical ? viewBottom : viewRight;

        realTop = getPaddingTop() + verticalPaddingSize;
        //realBottom = mIsVertical ? viewRight : viewBottom;

        mBarWidth = realRight - realLeft;

        //init rect
        mColorRect = new Rect(realLeft, realTop, realRight, realTop + mBarHeight);

        //init paint
        LinearGradient mColorGradient = new LinearGradient(mColorRect.left, 0, mColorRect.right, 0, mColorSeeds, null, Shader.TileMode.CLAMP);
        mColorRectPaint = new Paint();
        mColorRectPaint.setShader(mColorGradient);
        mColorRectPaint.setAntiAlias(true);
        cacheColors();
        setAlphaValue();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mIsVertical) {
            mTransparentBitmap = Bitmap.createBitmap(h, w, Bitmap.Config.ARGB_4444);
        } else {
            mTransparentBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        }
        mTransparentBitmap.eraseColor(Color.TRANSPARENT);
        init();
        mInit = true;
        if (mColorsToInvoke != -1) {
            setColor(mColorsToInvoke);
        }
    }


    private void cacheColors() {
        //if the view's size hasn't been initialized. do not cache.
        if (mBarWidth < 1) {
            return;
        }
        mColors.clear();
        for (int i = 0; i <= mMaxPosition; i++) {
            mColors.add(pickColor(i));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mIsVertical) {
            canvas.rotate(-90);
            canvas.translate(-getHeight(), 0);
            canvas.scale(-1, 1, getHeight() / 2, getWidth() / 2);
        }

        float colorPosition = (float) mColorBarPosition / mMaxPosition * mBarWidth;

        colorPaint.setAntiAlias(true);
        int color = getColor(false);
        int colorStartTransparent = Color.argb(mAlphaMaxPosition, Color.red(color), Color.green(color), Color.blue(color));
        int colorEndTransparent = Color.argb(mAlphaMinPosition, Color.red(color), Color.green(color), Color.blue(color));
        colorPaint.setColor(color);
        //int[] toAlpha = new int[]{colorStartTransparent, colorEndTransparent};
        toAlpha[0] = colorStartTransparent;
        toAlpha[1] = colorEndTransparent;

        //clear
        canvas.drawBitmap(mTransparentBitmap, 0, 0, null);

        //draw color bar
        canvas.drawRect(mColorRect, mColorRectPaint);
        //draw color bar thumb
        float thumbX = colorPosition + realLeft;
        float thumbY = mColorRect.top + mColorRect.height() / 2;
        canvas.drawCircle(thumbX, thumbY, mThumbHeight / 2, previewStrokePaint);
        canvas.drawCircle(thumbX, thumbY, mThumbHeight / 2, colorPaint);

        //draw color bar thumb radial gradient shader
        //RadialGradient thumbShader = new RadialGradient(thumbX, thumbY, mThumbRadius, toAlpha, null, Shader.TileMode.MIRROR);
        //thumbGradientPaint.setAntiAlias(true);
        //thumbGradientPaint.setShader(thumbShader);
        //canvas.drawCircle(thumbX, thumbY, mThumbHeight / 2, thumbGradientPaint);

        if (showPreviewCircle && mMovingColorBar) {
            previewCirclePaint.setColor(color);
            float previewCircleY = mColorRect.top - mThumbHeight / 2 - previewCircleMargin - previewCircleRadius - previewCircleStrokeWidth;
            canvas.drawCircle(thumbX, previewCircleY, previewCircleRadius, previewStrokePaint);
            canvas.drawCircle(thumbX, previewCircleY, previewCircleRadius, previewCirclePaint);
        }

        if (mIsShowAlphaBar) {
            //init rect
            int top = (int) (mThumbHeight + mThumbRadius + mBarHeight + mBarMargin + getPreviewCircleTotalHeight());
            mAlphaRect.set(realLeft, top, realRight, top + mBarHeight);
            //draw alpha bar
            alphaBarPaint.setAntiAlias(true);
            LinearGradient alphaBarShader = new LinearGradient(0, 0, mAlphaRect.width(), 0, toAlpha, null, Shader.TileMode.CLAMP);
            alphaBarPaint.setShader(alphaBarShader);
            canvas.drawRect(mAlphaRect, alphaBarPaint);

            //draw alpha bar thumb
            float alphaPosition = (float) (mAlphaBarPosition - mAlphaMinPosition) / (mAlphaMaxPosition - mAlphaMinPosition) * mBarWidth;
            float alphaThumbX = alphaPosition + realLeft;
            float alphaThumbY = mAlphaRect.top + mAlphaRect.height() / 2;
            canvas.drawCircle(alphaThumbX, alphaThumbY, mBarHeight / 2 + 5, colorPaint);

            //draw alpha bar thumb radial gradient shader
            RadialGradient alphaThumbShader = new RadialGradient(alphaThumbX, alphaThumbY, mThumbRadius, toAlpha, null, Shader.TileMode.MIRROR);

            alphaThumbGradientPaint.setAntiAlias(true);
            alphaThumbGradientPaint.setShader(alphaThumbShader);
            canvas.drawCircle(alphaThumbX, alphaThumbY, mThumbHeight / 2, alphaThumbGradientPaint);
        }

        if (mFirstDraw) {
            if (mOnColorChangeLister != null) {
                mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
            }
            mFirstDraw = false;

            if (mOnInitDoneListener != null) {
                mOnInitDoneListener.done();
            }
        }
        super.onDraw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = mIsVertical ? event.getY() : event.getX();
        float y = mIsVertical ? event.getX() : event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isOnBar(mColorRect, x, y)) {
                    mMovingColorBar = true;
                } else if (mIsShowAlphaBar) {
                    if (isOnBar(mAlphaRect, x, y)) {
                        mMovingAlphaBar = true;
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(true);
                if (mMovingColorBar) {
                    float value = (x - realLeft) / mBarWidth * mMaxPosition;
                    mColorBarPosition = (int) value;
                    if (mColorBarPosition < 0) {
                        mColorBarPosition = 0;
                    }
                    if (mColorBarPosition > mMaxPosition) {
                        mColorBarPosition = mMaxPosition;
                    }
                } else if (mIsShowAlphaBar) {
                    if (mMovingAlphaBar) {
                        float value = (x - realLeft) / (float) mBarWidth * (mAlphaMaxPosition - mAlphaMinPosition) + mAlphaMinPosition;
                        mAlphaBarPosition = (int) value;
                        if (mAlphaBarPosition < mAlphaMinPosition) {
                            mAlphaBarPosition = mAlphaMinPosition;
                        } else if (mAlphaBarPosition > mAlphaMaxPosition) {
                            mAlphaBarPosition = mAlphaMaxPosition;
                        }
                        setAlphaValue();
                    }
                }
                if (mOnColorChangeLister != null && (mMovingAlphaBar || mMovingColorBar)) {
                    mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
                }
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mOnColorChangeLister != null && (mMovingAlphaBar || mMovingColorBar)) {
                    mOnColorChangeLister.onColorChangeActionUp(mColorBarPosition, mAlphaBarPosition, getColor());
                }
                mMovingColorBar = false;
                mMovingAlphaBar = false;
                invalidate();
                break;
            default:
        }
        return mMovingColorBar || mMovingAlphaBar; //如果点击了拖动条 才接受触摸事件
    }

    /***
     *
     * @param alphaMaxPosition <= 255 && > alphaMinPosition
     */
    public void setAlphaMaxPosition(int alphaMaxPosition) {
        mAlphaMaxPosition = alphaMaxPosition;
        if (mAlphaMaxPosition > 255) {
            mAlphaMaxPosition = 255;
        } else if (mAlphaMaxPosition <= mAlphaMinPosition) {
            mAlphaMaxPosition = mAlphaMinPosition + 1;
        }

        if (mAlphaBarPosition > mAlphaMinPosition) {
            mAlphaBarPosition = mAlphaMaxPosition;
        }
        invalidate();
    }

    public int getAlphaMaxPosition() {
        return mAlphaMaxPosition;
    }

    /***
     *
     * @param alphaMinPosition >=0 && < alphaMaxPosition
     */
    public void setAlphaMinPosition(int alphaMinPosition) {
        this.mAlphaMinPosition = alphaMinPosition;
        if (mAlphaMinPosition >= mAlphaMaxPosition) {
            mAlphaMinPosition = mAlphaMaxPosition - 1;
        } else if (mAlphaMinPosition < 0) {
            mAlphaMinPosition = 0;
        }

        if (mAlphaBarPosition < mAlphaMinPosition) {
            mAlphaBarPosition = mAlphaMinPosition;
        }
        invalidate();
    }

    public int getAlphaMinPosition() {
        return mAlphaMinPosition;
    }

    /**
     * @param r
     * @param x
     * @param y
     * @return whether MotionEvent is performing on bar or not
     */
    private boolean isOnBar(Rect r, float x, float y) {
        if (r.left - mThumbRadius < x && x < r.right + mThumbRadius && r.top - mThumbRadius < y && y < r.bottom + mThumbRadius) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return
     * @deprecated use {@link #setOnInitDoneListener(OnInitDoneListener)} instead.
     */
    public boolean isFirstDraw() {
        return mFirstDraw;
    }


    /**
     * @param value
     * @return color
     */
    private int pickColor(int value) {
        return pickColor((float) value / mMaxPosition * mBarWidth);
    }

    /**
     * @param position
     * @return color
     */
    private int pickColor(float position) {
        float unit = position / mBarWidth;
        if (unit <= 0.0) {
            return mColorSeeds[0];
        }


        if (unit >= 1) {
            return mColorSeeds[mColorSeeds.length - 1];
        }

        float colorPosition = unit * (mColorSeeds.length - 1);
        int i = (int) colorPosition;
        colorPosition -= i;
        int c0 = mColorSeeds[i];
        int c1 = mColorSeeds[i + 1];
//         mAlpha = mix(Color.alpha(c0), Color.alpha(c1), colorPosition);
        int mRed = mix(Color.red(c0), Color.red(c1), colorPosition);
        int mGreen = mix(Color.green(c0), Color.green(c1), colorPosition);
        int mBlue = mix(Color.blue(c0), Color.blue(c1), colorPosition);
        return Color.rgb(mRed, mGreen, mBlue);
    }

    /**
     * @param start
     * @param end
     * @param position
     * @return
     */
    private int mix(int start, int end, float position) {
        return start + Math.round(position * (end - start));
    }

    public int getColor() {
        return getColor(mIsShowAlphaBar);
    }

    /**
     * @param withAlpha
     * @return
     */
    public int getColor(boolean withAlpha) {
        //pick mode
        if (mColorBarPosition >= mColors.size()) {
            int color = pickColor(mColorBarPosition);
            if (withAlpha) {
                return color;
            } else {
                return Color.argb(getAlphaValue(), Color.red(color), Color.green(color), Color.blue(color));
            }
        }

        //cache mode
        int color = mColors.get(mColorBarPosition);

        if (withAlpha) {
            return Color.argb(getAlphaValue(), Color.red(color), Color.green(color), Color.blue(color));
        }
        return color;
    }

    public int getAlphaBarPosition() {
        return mAlphaBarPosition;
    }

    public int getAlphaValue() {
        return mAlpha;
    }

    public interface OnColorChangeListener {
        /**
         * @param colorBarPosition between 0-maxValue
         * @param alphaBarPosition between 0-255
         * @param color            return the color contains alpha value whether showAlphaBar is true or without alpha value
         */
        void onColorChangeListener(int colorBarPosition, int alphaBarPosition, int color);

        void onColorChangeActionUp(int colorBarPosition, int alphaBarPosition, int color);
    }

    /**
     * @param onColorChangeListener
     */
    public void setOnColorChangeListener(OnColorChangeListener onColorChangeListener) {
        this.mOnColorChangeLister = onColorChangeListener;
    }


    public int dp2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * Set colors by resource id. The resource's type must be ArrayRes
     *
     * @param resId
     */
    public void setColorSeeds(@ArrayRes int resId) {
        setColorSeeds(getColorsById(resId));
    }

    public void setColorSeeds(int[] colors) {
        mColorSeeds = colors;
        init();
        invalidate();
        if (mOnColorChangeLister != null) {
            mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
        }
    }

    /**
     * @param color
     * @return the color's position in the bar, if not in the bar ,return -1;
     */
    public int getColorIndexPosition(int color) {
        return mColors.indexOf(Color.argb(255, Color.red(color), Color.green(color), Color.blue(color)));
    }

    public List<Integer> getColors() {
        return mColors;
    }

    public boolean isShowAlphaBar() {
        return mIsShowAlphaBar;
    }

    private void refreshLayoutParams() {
        setLayoutParams(getLayoutParams());
    }

//    public void setVertical(boolean vertical) {
//        mIsVertical = vertical;
//        refreshLayoutParams();
//        invalidate();
//    }

    public boolean isVertical() {
        return mIsVertical;
    }

    public void setShowAlphaBar(boolean show) {
        mIsShowAlphaBar = show;
        refreshLayoutParams();
        invalidate();
        if (mOnColorChangeLister != null) {
            mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
        }
    }

    /**
     * @param dp
     */
    public void setBarHeight(float dp) {
        mBarHeight = dp2px(dp);
        refreshLayoutParams();
        invalidate();
    }

    /**
     * @param px
     */
    public void setBarHeightPx(int px) {
        mBarHeight = px;
        refreshLayoutParams();
        invalidate();
    }

    private void setAlphaValue() {
        mAlpha = 255 - mAlphaBarPosition;
    }

    public void setAlphaBarPosition(int value) {
        this.mAlphaBarPosition = value;
        setAlphaValue();
        invalidate();
    }

    public int getMaxValue() {
        return mMaxPosition;
    }

    public void setMaxPosition(int value) {
        this.mMaxPosition = value;
        invalidate();
        cacheColors();
    }

    /**
     * set margin between bars
     *
     * @param mBarMargin
     */
    public void setBarMargin(float mBarMargin) {
        this.mBarMargin = dp2px(mBarMargin);
        refreshLayoutParams();
        invalidate();
    }

    /**
     * set margin between bars
     *
     * @param mBarMargin
     */
    public void setBarMarginPx(int mBarMargin) {
        this.mBarMargin = mBarMargin;
        refreshLayoutParams();
        invalidate();
    }


    /**
     * Set the value of color bar, if out of bounds , it will be 0 or maxValue;
     *
     * @param value
     */
    public void setColorBarPosition(int value) {
        this.mColorBarPosition = value;
        mColorBarPosition = mColorBarPosition > mMaxPosition ? mMaxPosition : mColorBarPosition;
        mColorBarPosition = mColorBarPosition < 0 ? 0 : mColorBarPosition;
        invalidate();
        if (mOnColorChangeLister != null) {
            mOnColorChangeLister.onColorChangeListener(mColorBarPosition, mAlphaBarPosition, getColor());
        }
    }

    public void setOnInitDoneListener(OnInitDoneListener listener) {
        this.mOnInitDoneListener = listener;
    }

    /**
     * Set color, it must correspond to the value, if not , setColorBarPosition(0);
     *
     * @paam color
     */
    public void setColor(int color) {
        int withoutAlphaColor = Color.rgb(Color.red(color), Color.green(color), Color.blue(color));

        if (mInit) {
            int value = mColors.indexOf(withoutAlphaColor);
//            mColorsToInvoke = color;
            setColorBarPosition(value);
        } else {
            mColorsToInvoke = color;
        }

    }

    /**
     * set thumb's height by dpi
     *
     * @param dp
     */
    public void setThumbHeight(float dp) {
        this.mThumbHeight = dp2px(dp);
        mThumbRadius = mThumbHeight / 2;
        refreshLayoutParams();
        invalidate();
    }

    /**
     * set thumb's height by pixels
     *
     * @param px
     */
    public void setThumbHeightPx(int px) {
        this.mThumbHeight = px;
        mThumbRadius = mThumbHeight / 2;
        refreshLayoutParams();
        invalidate();
    }

    public int getBarHeight() {
        return mBarHeight;
    }

    public int getThumbHeight() {
        return mThumbHeight;
    }

    public int getBarMargin() {
        return mBarMargin;
    }

    public float getColorBarValue() {
        return mColorBarPosition;
    }

    public interface OnInitDoneListener {
        void done();
    }

    public int getColorBarPosition() {
        return mColorBarPosition;
    }
}