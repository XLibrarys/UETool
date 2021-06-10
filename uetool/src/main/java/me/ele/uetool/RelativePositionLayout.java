package me.ele.uetool;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import me.ele.uetool.base.Element;

import static me.ele.uetool.base.DimenUtil.dip2px;

/**
 * 相对位置布局，继承CollectViewsLayout收集页面上所有元素信息
 */
public class RelativePositionLayout extends CollectViewsLayout {

    //相对布局，只保存选中序列中最近2个的元素，计算相对位置信息
    private final int elementsNum = 2;

    private Paint areaPaint = new Paint() {
        {
            setAntiAlias(true);
            setColor(Color.RED);
            setStyle(Style.STROKE);
            setStrokeWidth(dip2px(1));
        }
    };

    private Element[] relativeElements = new Element[elementsNum];
    private int searchCount = 0;

    public RelativePositionLayout(Context context) {
        this(context, null);
    }

    public RelativePositionLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RelativePositionLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_UP:
                //获取当前选中的目标element
                final Element element = getTargetElement(event.getX(), event.getY());
                if (element != null) {
                    //更新最近选中的2个element
                    relativeElements[searchCount % elementsNum] = element;
                    searchCount++;

                    //触发onDraw绘制两个View的相关信息
                    invalidate();
                }
                break;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (relativeElements == null) {
            return;
        }

        boolean doubleNotNull = true;
        //绘制当前选中的元素标注信息
        for (Element element : relativeElements) {
            if (element != null) {
                Rect rect = element.getRect();
                canvas.drawLine(0, rect.top, screenWidth, rect.top, dashLinePaint);
                canvas.drawLine(0, rect.bottom, screenWidth, rect.bottom, dashLinePaint);
                canvas.drawLine(rect.left, 0, rect.left, screenHeight, dashLinePaint);
                canvas.drawLine(rect.right, 0, rect.right, screenHeight, dashLinePaint);
                canvas.drawRect(rect, areaPaint);
            } else {
                doubleNotNull = false;
            }
        }

        //如果两个View都不为null，则绘制两个View的相对标注信息
        if (doubleNotNull) {
            Rect firstRect = relativeElements[searchCount % elementsNum].getRect();
            Rect secondRect = relativeElements[(searchCount - 1) % elementsNum].getRect();

            //如果有交叉的区域，则绘制交叉的信息
            if (secondRect.top > firstRect.bottom) {
                int x = secondRect.left + secondRect.width() / 2;
                drawLineWithText(canvas, x, firstRect.bottom, x, secondRect.top);
            }

            if (firstRect.top > secondRect.bottom) {
                int x = secondRect.left + secondRect.width() / 2;
                drawLineWithText(canvas, x, secondRect.bottom, x, firstRect.top);
            }

            if (secondRect.left > firstRect.right) {
                int y = secondRect.top + secondRect.height() / 2;
                drawLineWithText(canvas, secondRect.left, y, firstRect.right, y);
            }

            if (firstRect.left > secondRect.right) {
                int y = secondRect.top + secondRect.height() / 2;
                drawLineWithText(canvas, secondRect.right, y, firstRect.left, y);
            }

            //如果没有交叉区域，则绘制距离坐标
            drawNestedAreaLine(canvas, firstRect, secondRect);
            drawNestedAreaLine(canvas, secondRect, firstRect);
        }
    }

    private void drawNestedAreaLine(Canvas canvas, Rect firstRect, Rect secondRect) {
        if (secondRect.left >= firstRect.left && secondRect.right <= firstRect.right && secondRect.top >= firstRect.top && secondRect.bottom <= firstRect.bottom) {

            drawLineWithText(canvas, secondRect.left, secondRect.top + secondRect.height() / 2,
                    firstRect.left, secondRect.top + secondRect.height() / 2);

            drawLineWithText(canvas, secondRect.right, secondRect.top + secondRect.height() / 2,
                    firstRect.right, secondRect.top + secondRect.height() / 2);

            drawLineWithText(canvas, secondRect.left + secondRect.width() / 2, secondRect.top,
                    secondRect.left + secondRect.width() / 2, firstRect.top);

            drawLineWithText(canvas, secondRect.left + secondRect.width() / 2, secondRect.bottom,
                    secondRect.left + secondRect.width() / 2, firstRect.bottom);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        relativeElements = null;
    }

    @Override
    protected void drawLineWithText(Canvas canvas, int startX, int startY, int endX, int endY) {
        drawLineWithText(canvas, startX, startY, endX, endY, dip2px(2));
    }
}
