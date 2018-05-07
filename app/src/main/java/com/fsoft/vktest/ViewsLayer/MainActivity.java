package com.fsoft.vktest.ViewsLayer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.fsoft.vktest.R;

/**
 * Created by Dr. Failov on 28.11.2017.
 */

public class MainActivity extends Activity {
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //проверить запущен ли сервис. если нет - запустить.
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //сообщить сервису что активити больше нег
    }


    public void showMessage(final String text){
        handler.post(new Runnable() {
            @Override
            public void run() {

            }
        });
    }
}
