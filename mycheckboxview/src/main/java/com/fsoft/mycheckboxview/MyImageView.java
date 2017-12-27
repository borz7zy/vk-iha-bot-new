package com.fsoft.mycheckboxview;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 *
 * Created by Dr. Failov on 08.11.2015.
 */
class MyImageView extends View {
    private int resource = 0;
    private Bitmap cache = null;
    private int neededWidth = 0;
    private int neededHeight = 0;
    private Thread loadingThread = null;
    private Paint paint = new Paint();

    public MyImageView(Context context) {
        super(context);
    }
    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MyImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MyImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    public void setDrawable(int resource) {
        this.resource = resource;
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.argb(20, 255, 255, 255));  //DEBUG
//        neededWidth = getWidth() - getPaddingRight() - getPaddingLeft();
//        neededHeight = getHeight() - getPaddingBottom() - getPaddingTop();
        if(cache == null || (cache.getHeight() != neededHeight && cache.getWidth() != neededWidth)) {
            //в редакторе выполнять загрузку в основном потоке, иначе выносить в отдельный поток
            if(isInEditMode()) {
                loadAsync();
                canvas.drawBitmap(cache, 0, 0, paint);
                //canvas.drawBitmap(cache, getPaddingLeft() + (neededWidth - cache.getWidth())/2, getPaddingTop() + (neededHeight - cache.getHeight())/2, paint);
            }
            else {
                startLoading();
                paint.setColor(Color.GRAY);
                canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom(), paint);
            }
        }
        else
            canvas.drawBitmap(cache, 0, 0, paint);
            //canvas.drawBitmap(cache, getPaddingLeft() + (neededWidth - cache.getWidth())/2, getPaddingTop() + (neededHeight - cache.getHeight())/2, paint);
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        neededHeight = h - getPaddingBottom() - getPaddingTop();
        neededWidth = w - getPaddingRight() - getPaddingLeft();
    }

    private void startLoading(){
        if(loadingThread == null){
            loadingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        loadAsync();
                        postInvalidate();
                    }
                    finally {
                        loadingThread = null;
                    }
                }
            });
            loadingThread.start();
        }
    }
    private void loadAsync(){
        try {
            Bitmap tmp = BitmapFactory.decodeResource(getContext().getResources(), resource);
            float coef = Math.min(neededHeight/(float)tmp.getHeight(), neededWidth/(float)tmp.getWidth());
            cache = Bitmap.createScaledBitmap(tmp, (int)((float)tmp.getWidth()*coef), (int)((float)tmp.getHeight()*coef), false);
        }
        catch (Exception e){
            e.printStackTrace();
            Log.d("MyImageView", "Error decoding: " + e.toString());
        }
    }
}

