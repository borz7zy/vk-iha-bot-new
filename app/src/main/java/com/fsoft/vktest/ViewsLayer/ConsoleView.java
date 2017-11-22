package com.fsoft.vktest.ViewsLayer;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * console
 * Created by Dr. Failov on 05.08.2014.
 */
public class ConsoleView extends ScrollView {
    private final Object syncObject = new Object();
    TextView textView;
    public String log = "Начало лога.\n";
    public boolean logEnabled = true;
    Timer scrollDownTimer;
    Handler handler;
    long lastTouchTime = 0;
    boolean holding = false;

    public ConsoleView(Context context) {
        super(context);
        init();
    }
    public ConsoleView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        init();
    }
    public ConsoleView(Context context, AttributeSet attributeSet, int style){
        super(context, attributeSet, style);
        init();
    }

    private void init(){
        handler = new Handler();
        textView = new TextView(getContext());
        textView.setGravity(Gravity.LEFT);
        textView.setPadding(10, 10, 10, 10);
        textView.setTextSize(11);
        textView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switchEnabled();
            }
        });
        addView(textView);
        setEnabled(true);
    }
    private void sleep(int mili){
        try{
            Thread.sleep(mili);
        }
        catch (Exception e){}
    }
    private void setColor(final int color){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (textView != null)
                    textView.setTextColor(color);
            }
        });
    }
    public void log(String text){
        Log.d("BOT", text);
        synchronized (syncObject) {
            if (log.length() > 11000 && isScroll())
                log = log.substring(log.length() - 9000);
            log = log + "\n" + text;
        }
        if(logEnabled) {
            sleep(20);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        textView.setText(log);
                        scrollDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                        log("Error log: " + e.toString());
                    }
                }
            });
        }
    }
    public void setEnabled(boolean enabled){
        if(enabled){
            setColor(Color.rgb(30, 255, 100));
            logEnabled = true;
            log("! Лог включен.");
        } else {
            setColor(Color.rgb(150, 150, 150));
            log("! Лог выключен. Нажмите на лог, чтобы включить.");
            logEnabled = false;
        }
    }
    public void switchEnabled(){
        setEnabled(!logEnabled);
    }
    public void clearLastLine(){
        synchronized (syncObject) {
            if (log.length() > 11000)
                log = log.substring(log.length() - 9000);
            int cropTo = 0;
            for (int i = log.length() - 1; i >= 0; i--) {
                if (log.charAt(i) == '\n') {
                    cropTo = i;
                    break;
                }
            }
            log = log.substring(0, cropTo);
        }
    }
    public  void scrollDown(){
        if(!isScroll())
            return;
        if (scrollDownTimer != null)
            scrollDownTimer.cancel();
        scrollDownTimer = new Timer();
        scrollDownTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        scrollDownTimer = null;
                        fullScroll(FOCUS_DOWN);
                    }
                });
            }
        }, 50);
    }
    public boolean isInEnd(){
        float bottomY = getHeight() + (int) getTopY();
        float totalHeight = getDocumentHeight();
        return Math.abs(bottomY - totalHeight)<20;
    }
    public float getTopY(){
        return (getDocumentHeight() * computeVerticalScrollOffset())/computeVerticalScrollRange();
    }
    public boolean isScroll(){
        long now = System.currentTimeMillis();
        long dif = now - lastTouchTime;
        return dif > 3000 && !holding;
    }

    @Override  public boolean onTouchEvent(MotionEvent ev) {
        lastTouchTime = System.currentTimeMillis();
        if(ev.getAction() == MotionEvent.ACTION_DOWN)
            holding = true;
        else if(ev.getAction() == MotionEvent.ACTION_UP)
            holding = false;
        return super.onTouchEvent(ev);
    }

    public float getDocumentHeight(){
        float screenHeight = getHeight();
        return (computeVerticalScrollRange() * screenHeight)/computeVerticalScrollExtent();
    }
    static public String getLoadingBar(int totalLength, int percentage){
        int filled = (int)((double)totalLength * ((double)percentage / 100d));
        int notFilled = totalLength - filled;
        String result = "|";
        for (int i = 0; i < filled; i++)
            result += "█";
        for (int i = 0; i < notFilled; i++)
            result += "─";
        result += "|";
//        result = result.substring(0, result.length()/2)+"("+percentage+"%)" + result.substring(result.length()/2, result.length());
        result = result+" ("+percentage+"%)";
        return result;
    }
}
