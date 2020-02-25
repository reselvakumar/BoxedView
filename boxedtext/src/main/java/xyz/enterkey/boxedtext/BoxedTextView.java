package xyz.enterkey.boxedtext;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.InputFilter;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.ViewCompat;


public class BoxedTextView extends AppCompatTextView {

    public Canvas canvas1;
    private static final boolean DBG = false;
    private static final int DEFAULT_COUNT = 4;

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];

    private static final int VIEW_TYPE_RECTANGLE = 0;
    private static final int VIEW_TYPE_LINE = 1;
    private int mViewType;
    private int mPinItemCount;
    private float mPinItemWidth;
    private float mPinItemHeight;
    private int mPinItemRadius;
    private int mPinItemSpacing;
    private final Paint mPaint;
    private final TextPaint mTextPaint;
    private final Paint mAnimatorTextPaint;
    private ColorStateList mLineColor;
    private int mCurLineColor = Color.BLACK;
    private int mLineWidth;
    private final Rect mTextRect = new Rect();
    private final RectF mItemBorderRect = new RectF();
    private final RectF mItemLineRect = new RectF();
    private final Path mPath = new Path();
    private final PointF mItemCenterPoint = new PointF();
    private ValueAnimator mDefaultAddAnimator;
    private boolean isAnimationEnable = false;

    public BoxedTextView(Context context) {
        this(context, null);
    }

    public BoxedTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoxedTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources res = getResources();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.density = res.getDisplayMetrics().density;
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(getTextSize());

        mAnimatorTextPaint = new TextPaint(mTextPaint);

        final Resources.Theme theme = context.getTheme();

        TypedArray a = theme.obtainStyledAttributes(attrs, R.styleable.BoxedTextView, 0, 0);

        mViewType = a.getInt(R.styleable.BoxedTextView_bt_viewType, VIEW_TYPE_RECTANGLE);

        mPinItemCount = a.getInt(R.styleable.BoxedTextView_bt_itemCount, DEFAULT_COUNT);
        mPinItemHeight = a.getDimensionPixelSize(R.styleable.BoxedTextView_bt_itemHeight,
                res.getDimensionPixelSize(R.dimen.box_text_item_size));
        mPinItemWidth = a.getDimensionPixelSize(R.styleable.BoxedTextView_bt_itemWidth,
                res.getDimensionPixelSize(R.dimen.box_text_item_spacing));
        mPinItemSpacing = a.getDimensionPixelSize(R.styleable.BoxedTextView_bt_itemSpacing,
                res.getDimensionPixelSize(R.dimen.box_text_item_spacing));
        mPinItemRadius = a.getDimensionPixelSize(R.styleable.BoxedTextView_bt_itemRadius, 0);
        mLineWidth = a.getDimensionPixelSize(R.styleable.BoxedTextView_bt_lineWidth,
                res.getDimensionPixelSize(R.dimen.box_text_item_line_width));
        mLineColor = a.getColorStateList(R.styleable.BoxedTextView_bt_lineColor);

        a.recycle();

    }

    private Float calculateItemWidth(int widthSize,int count,float padding,float spacing,float linewidth) {
        float spacing1;
        if((int)spacing==0){
            spacing1=-(count-1)*linewidth;
        }else{
            spacing1=(count-1)*spacing;
        }
        Float calcWidth1= (widthSize-padding-spacing1)/count;
        return calcWidth1;
    }

    private void setMaxLength(int maxLength) {
        if (maxLength >= 0) {
            setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        } else {
            setFilters(NO_FILTERS);
        }
    }

    private void checkItemRadius() {
        if (mViewType == VIEW_TYPE_LINE) {
            int halfOfLineWidth = mLineWidth / 2;
            if (mPinItemRadius > halfOfLineWidth) {
                throw new RuntimeException("The itemRadius can not be greater than lineWidth when viewType is line");
            }
        }
        int halfOfItemWidth = (int) (mPinItemWidth / 2);
        if (mPinItemRadius > halfOfItemWidth) {
            throw new RuntimeException("The itemRadius can not be greater than itemWidth");
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (mLineColor != null) {
            mCurLineColor = mLineColor.getDefaultColor();
        }

        if (mPinItemWidth == 0) {
            mPinItemWidth = calculateItemWidth(widthSize,mPinItemCount, ( ViewCompat.getPaddingEnd(this) + ViewCompat.getPaddingStart(this)),  mPinItemSpacing,mLineWidth);
        }

        checkItemRadius();
        setMaxLength(mPinItemCount);
        mPaint.setStrokeWidth(mLineWidth);

        float boxHeight = mPinItemHeight;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            final int width1 = getWidth();

            if ( width1 == ViewGroup.LayoutParams.MATCH_PARENT) {
                width = widthSize;
            }else if ( width1 == ViewGroup.LayoutParams.WRAP_CONTENT){
                float spacing1=(mPinItemCount-1)*mPinItemSpacing;
                Float calcWidth1= (mPinItemWidth*mPinItemCount)+ ViewCompat.getPaddingEnd(this) + ViewCompat.getPaddingStart(this)+spacing1;
                width = Math.round(calcWidth1);
            }else{
                width = widthSize;
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = Math.round(boxHeight + getPaddingTop() + getPaddingBottom());
        }
        setMeasuredDimension(width, height);
    }
    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        canvas1=canvas;
        canvas.save();
        updatePaints();
        drawPinView(canvas);
        canvas.restore();
    }

    private void updatePaints() {
        mPaint.setColor(mCurLineColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mLineWidth);
        mTextPaint.setColor(getCurrentTextColor());
    }

    private void drawPinView(Canvas canvas) {
        for (int i = 0; i < mPinItemCount; i++) {
            updateItemRectF(i);
            updateCenterPoint();

            if (mViewType == VIEW_TYPE_RECTANGLE) {
                drawPinBox(canvas, i);
            } else {
                drawPinLine(canvas, i);
            }

            if (DBG) {
                drawAnchorLine(canvas);
            }

            if (getText().length() > i) {

                    drawText(canvas, i);

            } else if (!TextUtils.isEmpty(getHint()) && getHint().length() == mPinItemCount) {
                drawHint(canvas, i);
            }
        }


    }

    private void drawPinBox(Canvas canvas, int i) {
        boolean drawRightCorner = false;
        boolean drawLeftCorner = false;
        if (mPinItemSpacing != 0) {
            drawLeftCorner = drawRightCorner = true;
        } else {
            if (i == 0 && i != mPinItemCount - 1) {
                drawLeftCorner = true;
            }
            if (i == mPinItemCount - 1 && i != 0) {
                drawRightCorner = true;
            }
        }
        updateRoundRectPath(mItemBorderRect, mPinItemRadius, mPinItemRadius, drawLeftCorner, drawRightCorner);
        canvas.drawPath(mPath, mPaint);
    }

    private void drawPinLine(Canvas canvas, int i) {
        boolean l, r;
        l = r = true;
        if (mPinItemSpacing == 0 && mPinItemCount > 1) {
            if (i == 0) {
                // draw only left round
                r = false;
            } else if (i == mPinItemCount - 1) {
                // draw only right round
                l = false;
            } else {
                // draw rect
                l = r = false;
            }
        }
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(((float) mLineWidth) / 10);
        float halfLineWidth = (float) mLineWidth / 2;
        mItemLineRect.set(mItemBorderRect.left, mItemBorderRect.bottom - halfLineWidth, mItemBorderRect.right, mItemBorderRect.bottom + halfLineWidth);

        updateRoundRectPath(mItemLineRect, mPinItemRadius, mPinItemRadius, l, r);
        canvas.drawPath(mPath, mPaint);
    }

    private void updateRoundRectPath(RectF rectF, float rx, float ry, boolean l, boolean r) {
        updateRoundRectPath(rectF, rx, ry, l, r, r, l);
    }

    private void updateRoundRectPath(RectF rectF, float rx, float ry,
                                     boolean tl, boolean tr, boolean br, boolean bl) {
        mPath.reset();

        float l = rectF.left;
        float t = rectF.top;
        float r = rectF.right;
        float b = rectF.bottom;

        float w = r - l;
        float h = b - t;

        float lw = w - 2 * rx;// line width
        float lh = h - 2 * ry;// line height

        mPath.moveTo(l, t + ry);

        if (tl) {
            mPath.rQuadTo(0, -ry, rx, -ry);// top-left corner
        } else {
            mPath.rLineTo(0, -ry);
            mPath.rLineTo(rx, 0);
        }

        mPath.rLineTo(lw, 0);

        if (tr) {
            mPath.rQuadTo(rx, 0, rx, ry);// top-right corner
        } else {
            mPath.rLineTo(rx, 0);
            mPath.rLineTo(0, ry);
        }

        mPath.rLineTo(0, lh);

        if (br) {
            mPath.rQuadTo(0, ry, -rx, ry);// bottom-right corner
        } else {
            mPath.rLineTo(0, ry);
            mPath.rLineTo(-rx, 0);
        }

        mPath.rLineTo(-lw, 0);

        if (bl) {
            mPath.rQuadTo(-rx, 0, -rx, -ry);// bottom-left corner
        } else {
            mPath.rLineTo(-rx, 0);
            mPath.rLineTo(0, -ry);
        }

        mPath.rLineTo(0, -lh);

        mPath.close();
    }

    private void updateItemRectF(int i) {
        float halfLineWidth = (float) mLineWidth / 2;
        float left = getScrollX() + ViewCompat.getPaddingStart(this) + i * (mPinItemSpacing + mPinItemWidth) + halfLineWidth;
        if (mPinItemSpacing == 0 && i > 0) {
            left = left - (mLineWidth) * i;
        }
        float right = left + mPinItemWidth - mLineWidth;
        float top = getScrollY() + getPaddingTop() + halfLineWidth;
        float bottom = top + mPinItemHeight - mLineWidth;

        mItemBorderRect.set(left, top, right, bottom);
    }

    private void drawText(Canvas canvas, int i) {
        Paint paint = getPaintByIndex(i);
        drawTextAtBox(canvas, paint, getText(), i);
    }

    private void drawHint(Canvas canvas, int i) {
        Paint paint = getPaintByIndex(i);
        paint.setColor(getCurrentHintTextColor());
        drawTextAtBox(canvas, paint, getHint(), i);
    }

    private void drawTextAtBox(Canvas canvas, Paint paint, CharSequence text, int charAt) {
        paint.getTextBounds(text.toString(), charAt, charAt + 1, mTextRect);
        float cx = mItemCenterPoint.x;
        float cy = mItemCenterPoint.y;
        float x = cx - Math.abs(mTextRect.width()) / 2 - mTextRect.left;
        float y = cy + Math.abs(mTextRect.height()) / 2 - mTextRect.bottom;// always center vertical
        canvas.drawText(text, charAt, charAt + 1, x, y, paint);
    }

    private Paint getPaintByIndex(int i) {
        if (isAnimationEnable && i == getText().length() - 1) {
            mAnimatorTextPaint.setColor(mTextPaint.getColor());
            return mAnimatorTextPaint;
        } else {
            return mTextPaint;
        }
    }

    private void drawAnchorLine(Canvas canvas) {
        float cx = mItemCenterPoint.x;
        float cy = mItemCenterPoint.y;
        mPaint.setStrokeWidth(1);
        cx -= mPaint.getStrokeWidth() / 2;
        cy -= mPaint.getStrokeWidth() / 2;

        mPath.reset();
        mPath.moveTo(cx, mItemBorderRect.top);
        mPath.lineTo(cx, mItemBorderRect.top + Math.abs(mItemBorderRect.height()));
        canvas.drawPath(mPath, mPaint);

        mPath.reset();
        mPath.moveTo(mItemBorderRect.left, cy);
        mPath.lineTo(mItemBorderRect.left + Math.abs(mItemBorderRect.width()), cy);
        canvas.drawPath(mPath, mPaint);

        mPath.reset();

        mPaint.setStrokeWidth(mLineWidth);
    }

    private void updateCenterPoint() {
        float cx = mItemBorderRect.left + Math.abs(mItemBorderRect.width()) / 2;
        float cy = mItemBorderRect.top + Math.abs(mItemBorderRect.height()) / 2;
        mItemCenterPoint.set(cx, cy);
    }

}