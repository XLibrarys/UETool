package me.ele.uetool;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import me.ele.uetool.base.Element;

import static me.ele.uetool.base.DimenUtil.dip2px;
import static me.ele.uetool.base.DimenUtil.px2dip;

/**
 * 编辑属性Layout，继承CollectViewsLayout获取所有Views的信息
 */
public class EditAttrLayout extends CollectViewsLayout {

    private final int moveUnit = dip2px(1);
    private final int lineBorderDistance = dip2px(5);

    private Paint areaPaint = new Paint() {
        {
            setAntiAlias(true);
            setColor(0x30000000);
        }
    };

    private Element targetElement;
    private AttrsDialog dialog;
    private IMode mode = new ShowMode();
    private float lastX, lastY;
    private OnDragListener onDragListener;

    public EditAttrLayout(Context context) {
        super(context);
    }

    public EditAttrLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EditAttrLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (targetElement != null) {
            canvas.drawRect(targetElement.getRect(), areaPaint);//绘制元素的边界
            mode.onDraw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                //点击获取当前事件,根据事件的x,y坐标，获取指定Element即View，然后展示View的相关信息
                mode.triggerActionUp(event);
                break;
            case MotionEvent.ACTION_MOVE:
                mode.triggerActionMove(event);
                break;
        }
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        targetElement = null;
        dismissAttrsDialog();
    }

    public void setOnDragListener(OnDragListener onDragListener) {
        this.onDragListener = onDragListener;
    }

    public void dismissAttrsDialog() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    class MoveMode implements IMode {

        @Override
        public void onDraw(Canvas canvas) {
            Rect rect = targetElement.getRect();
            Rect originRect = targetElement.getOriginRect();
            canvas.drawRect(originRect, dashLinePaint);
            Element parentElement = targetElement.getParentElement();
            if (parentElement != null) {
                Rect parentRect = parentElement.getRect();
                int x = rect.left + rect.width() / 2;
                int y = rect.top + rect.height() / 2;
                drawLineWithText(canvas, rect.left, y, parentRect.left, y, dip2px(2));
                drawLineWithText(canvas, x, rect.top, x, parentRect.top, dip2px(2));
                drawLineWithText(canvas, rect.right, y, parentRect.right, y, dip2px(2));
                drawLineWithText(canvas, x, rect.bottom, x, parentRect.bottom, dip2px(2));
            }
            if (onDragListener != null) {
                onDragListener.showOffset("Offset:\n" + "x -> " + px2dip(rect.left - originRect.left, true) + " y -> " + px2dip(rect.top - originRect.top, true));
            }
        }

        @Override
        public void triggerActionMove(MotionEvent event) {
            if (targetElement != null) {
                boolean changed = false;
                View view = targetElement.getView();
                float diffX = event.getX() - lastX;
                if (Math.abs(diffX) >= moveUnit) {
                    view.setTranslationX(view.getTranslationX() + diffX);
                    lastX = event.getX();
                    changed = true;
                }
                float diffY = event.getY() - lastY;
                if (Math.abs(diffY) >= moveUnit) {
                    view.setTranslationY(view.getTranslationY() + diffY);
                    lastY = event.getY();
                    changed = true;
                }
                if (changed) {
                    targetElement.reset();
                    invalidate();
                }
            }
        }

        @Override
        public void triggerActionUp(MotionEvent event) {

        }
    }

    /**
     * Show模式，用于使用Dialog显示View的属性，在View的周边绘制标注信息
     */
    class ShowMode implements IMode {

        @Override
        public void onDraw(Canvas canvas) {
            //获取选中元素的React
            Rect rect = targetElement.getRect();
            //绘制元素的左右和上下标线
            drawLineWithText(canvas, rect.left, rect.top - lineBorderDistance, rect.right, rect.top - lineBorderDistance);
            drawLineWithText(canvas, rect.right + lineBorderDistance, rect.top, rect.right + lineBorderDistance, rect.bottom);
        }

        @Override
        public void triggerActionMove(MotionEvent event) {

        }

        @Override
        public void triggerActionUp(final MotionEvent event) {
            //点击抬起后，获取选中的元素
            final Element element = getTargetElement(event.getX(), event.getY());
            if (element != null) {
                targetElement = element; //更新选中的元素
                invalidate();//触发onDraw()重新绘制，绘制元素的标注信息
                if (dialog == null) {//显示元素信息对话框
                    dialog = new AttrsDialog(getContext());
                    dialog.setAttrDialogCallback(new AttrsDialog.AttrDialogCallback() {
                        @Override
                        public void enableMove() {
                            mode = new MoveMode();
                            dismissAttrsDialog();
                        }

                        @Override
                        public void showValidViews(int position, boolean isChecked) {
                            int positionStart = position + 1;
                            if (isChecked) {
                                dialog.notifyValidViewItemInserted(positionStart, getTargetElements(lastX, lastY), targetElement);
                            } else {
                                dialog.notifyItemRangeRemoved(positionStart);
                            }
                        }

                        @Override
                        public void selectView(Element element) {
                            targetElement = element;
                            dismissAttrsDialog();
                            dialog.show(targetElement);
                        }
                    });
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (targetElement != null) {
                                targetElement.reset();
                                invalidate();
                            }
                        }
                    });
                }
                //使用匹配的targetElement显示它相关属性的Dialog
                dialog.show(targetElement);
            }
        }
    }

    /**
     * 模式接口，定义了在不同模式下执行的动作
     */
    public interface IMode {
        //绘制动作，负责绘制选中元素的标注信息
        void onDraw(Canvas canvas);

        void triggerActionMove(MotionEvent event);

        //点击操作，负责查找到选中的目标元素，执行后续的元素Dialog显示，触发元素绘制
        void triggerActionUp(MotionEvent event);
    }

    public interface OnDragListener {
        void showOffset(String offsetContent);
    }
}
