package com.fsoft.vktest.ViewsLayer;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;

/**
 * Created by Dr. Failov on 28.11.2017.
 */

public class MainActivity extends FragmentActivity {
    private Handler handler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //проверить запущен ли сервис. если нет - запустить.
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
