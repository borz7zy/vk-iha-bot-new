package com.fsoft.mycheckboxview;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Dr. Failov on 17.12.2017.
 */

public class MyCheckbox extends LinearLayout {
    private LinearLayout root;
    private LinearLayout linearLayout;
    private TextView textView;
    private TextView hintView;
    private Check check = null;
    private MyImageView imageView;
    private boolean locked = false;
    private int DPI = 0;

    public MyCheckbox(Context context) {
        super(context);
        init();
    }
    public MyCheckbox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        applyAttrib(attrs);
    }
    public MyCheckbox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        applyAttrib(attrs);
    }
    public MyCheckbox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
        applyAttrib(attrs);
    }

    @Override public void setHovered(boolean hovered) {
        if(!isEnabled())
            return;
        if(check != null) {
            setBackgroundColor(Color.argb(hovered ? 10 : 0, 255, 255, 255));
            invalidate();
        }
        super.setHovered(hovered);
    }
    @Override public boolean performClick() {
        setChecked(!getChecked());
        return super.performClick();
    }
    @Override protected void dispatchSetPressed(boolean pressed) {
        if(!isEnabled())
            return;
        if(check != null) {
            setBackgroundColor(Color.argb(pressed ? 50 : 0, 255, 255, 255));
            check.setPressed(pressed);
            invalidate();
        }
        super.dispatchSetPressed(pressed);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setPadding(Tools.dp(10), Tools.dp(5), Tools.dp(10), Tools.dp(5));
    }
    public void setText(String text){
        if(textView != null)
            textView.setText(text);
    }
    public void setTextColor(int color){}
    public void setHint(String text){
        if(hintView != null){
            hintView.setText(text);
        }
    }
    public void setChecked(boolean checked){
        if(check != null) {
            check.setChecked(checked);
            invalidate();
        }
    }
    public boolean getChecked(){
        if(check == null)
            return false;
        return check.getChecked();
    }
    public void setImage(int res){
        if(imageView != null)
            imageView.setDrawable(res);
    }
    public boolean isChecked(){
        return getChecked();
    }

    protected void init(){
        removeAllViews();
        root = this;
        root.setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        DPI = Tools.dpi();

        linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(Tools.dp(5), Tools.dp(5), Tools.dp(10), Tools.dp(5));
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        imageView = new MyImageView(getContext());
        imageView.setLayoutParams(new LinearLayout.LayoutParams(Tools.dp(30), Tools.dp(30), 0));
        imageView.setPadding(0, 0, Tools.dp(10), 0);
        //imageView.setDrawable(R.drawable.menu_accurate);

        textView = new TextView(getContext());
        textView.setText("CheckBox");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(18);
        textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        textView.setPadding(0, 0, 0, Tools.dp(2));
        linearLayout.addView(textView);

        root.addView(imageView);
        root.addView(linearLayout);


        if(locked){
            ImageView imageView = new ImageView(getContext());
            imageView.setImageResource(R.drawable.ic_lock);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(Tools.dp(35), Tools.dp(35), 0));
            imageView.setPadding(Tools.dp(5), Tools.dp(5), Tools.dp(5), Tools.dp(5));
            root.addView(imageView);
        }
        else {
            check = new Check(getContext());
            check.setLayoutParams(new LinearLayout.LayoutParams(Tools.dp(25), Tools.dp(25), 0));
            check.setPadding(Tools.dp(5), Tools.dp(5), Tools.dp(5), Tools.dp(5));
            root.addView(check);
        }
    }
    protected void applyAttrib(AttributeSet attrs){
        boolean checked = attrs.getAttributeBooleanValue("app", "checked", false);
        String text = attrs.getAttributeValue("app", "text");
        String hint = attrs.getAttributeValue("app", "hint");
        int image = attrs.getAttributeResourceValue("app", "image", R.drawable.ic_lock);

        setChecked(checked);
        setImage(image);
        setText(text);
        setHint(hint);
    }

    private class Check extends View{
        private MarkerPart markerPart = new MarkerPart();
        private BackroundPart backroundPart = new BackroundPart();
        private Paint backgroundPaint = new Paint();
        private int backgroundColor = Color.argb(255, 255, 255, 255);
        private int strokeColor = Color.BLACK;

        public Check(Context context) {
            super(context);
        }
        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(getMeasuredHeight() + getPaddingBottom() + getPaddingTop(), getMeasuredHeight() + getPaddingLeft() + getPaddingRight());
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            backroundPart.draw(canvas);
            markerPart.draw(canvas);
        }
        public void setPressed(boolean pressed){
            backroundPart.setPressed(pressed);
        }
        public void setChecked(boolean checked){
            markerPart.setChecked(checked);
        }
        public boolean getChecked(){
            return markerPart.pressed;
        }

        class BackroundPart{
            boolean pressed = false;
            ArrayList<Thread> animationThreads = new ArrayList();
            float minPercent = 0;
            float maxPercent = 90;
            float animPercent = minPercent;
            float animTime = 100;
            float frameRate = 60;
            int circleColor = Color.argb(255, 255, 255, 255);
            Paint paint = new Paint();
            RectF rect = new RectF();

            public BackroundPart() {
                paint.setColor(circleColor);
                paint.setAntiAlias(true);
            }
            void draw(Canvas canvas){
                //Logger.log("percent = " + animPercent);
                float maxWidth = getWidth() - getPaddingRight() - getPaddingLeft();
                float maxHeight = getHeight() - getPaddingBottom() - getPaddingTop();
                float maxStroke = maxWidth / 6f;
                float stroke = maxStroke*(animPercent/100f);
                float height = maxHeight - stroke*2;
                float width = maxWidth - stroke*2;
                float x = getPaddingLeft()+stroke;
                float y = getPaddingTop()+stroke;

                rect.set(x, y, x+width, y+height);
                canvas.drawRoundRect(rect, height/5, height/5, paint);
                //canvas.drawCircle(cx, cy, radius, paint);
                //canvas.drawOval(x, y, x + width, y+height, paint);

            }
            void setPressed(boolean p){
                if(pressed != p){
                    pressed = p;
                    if(p)
                        startPressing(false);
                    else
                        startPressing(true);
                }
            }
            void setColor(int newColor){
                paint.setColor(circleColor = newColor);
            }
            void startPressing( final  boolean unpress){
                for(Thread t:animationThreads)
                    t.interrupt();
                Thread animationThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        float frameTime = 1000 / frameRate;
                        float frames = animTime / frameTime;
                        float percentStep = maxPercent/frames;
                        if(unpress)
                            percentStep = -percentStep;
                        try {
                            for (; (unpress ? animPercent > minPercent : animPercent < maxPercent) && !Thread.currentThread().isInterrupted(); animPercent += percentStep) {
                                Thread.sleep((int) frameTime);
                                if(unpress)
                                    Thread.sleep((int) frameTime*2);
                                Check.this.postInvalidate();
                            }

                            if (!Thread.interrupted())
                                animationThreads.remove(Thread.currentThread());
                        }
                        catch (Exception e){
                            //Logger.log("thread stopped");
                        }
                        finally {
                            if(animPercent < minPercent) animPercent = minPercent;
                            if(animPercent > maxPercent) animPercent = maxPercent;
                        }
                    }
                });
                animationThreads.add(animationThread);
                animationThread.start();
            }
        }
        class MarkerPart{
            boolean pressed = false;
            ArrayList<Thread> animationThreads = new ArrayList();
            float minPercent = 0;
            float maxPercent = 100;
            float animPercent = minPercent;
            float animTime = 200;
            float frameRate = 60;
            int lineColor = Color.parseColor("#16A085");
            Paint paint = new Paint();

            public MarkerPart() {
                paint.setColor(lineColor);
                paint.setAntiAlias(true);
            }
            void draw(Canvas canvas){
                //Logger.log("percent = " + animPercent);
                if(animPercent < 2)
                    return;
                if(isInEditMode())
                    animPercent = 100;
                float maxWidth = getWidth() - getPaddingRight() - getPaddingLeft();
                float maxHeight = getHeight() - getPaddingBottom() - getPaddingTop();
                paint.setStrokeWidth(maxWidth*0.21f);
                float[] points = new float[]{
                        getPaddingLeft() + maxWidth*0.2f, getPaddingTop() + maxHeight*0.35f,         getPaddingLeft() + maxWidth*0.4f, getPaddingTop() + maxHeight*0.55f,
                        getPaddingLeft() + maxWidth*0.4f, getPaddingTop() + maxHeight*0.7f,         getPaddingLeft() + maxWidth*1.0f, getPaddingTop() + maxHeight*0.1f
                }; //bx, by, ex, ey, ...

                float totalLength = 0;
                for(int i=0; i< points.length; i+=4) {
                    float bx = points[i];
                    float by = points[i+1];
                    float ex = points[i+2];
                    float ey = points[i+3];
                    float dx = ex-bx;
                    float dy = ey-by;
                    totalLength += Math.sqrt(dx*dx+dy*dy);
                }

                float allowedLength = totalLength*(animPercent/100f);
                float drawedLength = 0;
                for(int i=0; i< points.length; i+=4) {
                    float bx = points[i];
                    float by = points[i+1];
                    float ex = points[i+2];
                    float ey = points[i+3];
                    float dx = ex-bx;
                    float dy = ey-by;
                    float d = (float)Math.sqrt(dx*dx+dy*dy);
                    float remainingLength = allowedLength - drawedLength;
                    float lineCoef = Math.min(1f, Math.max(0, remainingLength / d));
                    ex = bx + lineCoef*dx;
                    ey = by + lineCoef*dy;
                    canvas.drawLine(bx, by, ex, ey, paint);
                    float drawed_d = (float)Math.sqrt(dx*dx+dy*dy);
                    drawedLength += drawed_d;
                }
            }
            void setChecked(boolean p){
                if(isInEditMode()){
                    animPercent = p?100:0;
                    return;
                }
                if(pressed != p){
                    pressed = p;
                    if(p)
                        startPressing(false);
                    else
                        startPressing(true);
                }
            }
            void setColor(int newColor){
                paint.setColor(lineColor = newColor);
            }
            void startPressing( final  boolean unpress){
                for(Thread t:animationThreads)
                    t.interrupt();
                Thread animationThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        float frameTime = 1000 / frameRate;
                        float frames = animTime / frameTime;
                        float percentStep = maxPercent/frames;
                        if(unpress)
                            percentStep = -percentStep;
                        try {
                            for (; (unpress ? animPercent > minPercent : animPercent < maxPercent) && !Thread.currentThread().isInterrupted(); animPercent += percentStep) {
                                Thread.sleep((int) frameTime);
                                Check.this.postInvalidate();
                            }

                            if (!Thread.interrupted())
                                animationThreads.remove(Thread.currentThread());
                        }
                        catch (Exception e){
                            //Logger.log("thread stopped");
                        }
                        finally {
                            if(animPercent < minPercent) animPercent = minPercent;
                            if(animPercent > maxPercent) animPercent = maxPercent;
                        }
                    }
                });
                animationThreads.add(animationThread);
                animationThread.start();
            }
        }
    }
}
