package com.fsoft.vktest.ViewsLayer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.BotService;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.ViewsLayer.AccountTgFragment.AccountTgFragment;
import com.fsoft.vktest.ViewsLayer.AccountsFragment.AccountsFragment;
import com.fsoft.vktest.ViewsLayer.MessagesFragment.MessagesFragment;

import java.util.ArrayList;

import me.tangke.slidemenu.SlideMenu;

/**
 * Created by Dr. Failov on 28.11.2017.
 */


//todo Надо чтобы "отправитель", "автор" были у нас Stringами, потому что в том же телеграме это строка
    //примеры vk:id1488    tg:drfailov



public class MainActivity extends FragmentActivity {
    private static MainActivity instance = null;
    public static MainActivity getInstance() {
        return instance;
    }

    private String TAG = "MainActivity";
    private Handler handler = new Handler();
    private FrameLayout mainFrame = null;
    private SlideMenu slideMenu = null;

    private ArrayList<Fragment> fragmentQueue = new ArrayList<>(); //zero activity = active,    first = last,     second = prelast


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mainFrame = findViewById(R.id.main_frame);
        slideMenu = findViewById(R.id.main_slideMenu);
        configureSideMenu();

        //проверить запущен ли сервис. если нет - запустить.
        Log.d(TAG, "Starting service...");
        Intent intent = new Intent(getApplicationContext(), BotService.class);
        startService(intent);

    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(ApplicationManager.getInstance() == null)
                    F.sleep(10);
                while(ApplicationManager.getInstance().getMessageHistory() == null)
                    F.sleep(10);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        openMessagesTab();
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(BotService.applicationManager != null)
//            BotService.applicationManager.setActivity(this);
    }

    @Override
    protected void onDestroy() {
        instance = null;
        super.onDestroy();
        //сообщить сервису что активити больше не
    }

    @Override
    public void onBackPressed() {
        if(fragmentQueue.size() <= 1) {
            finish();
            return;
        }
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.remove(fragmentQueue.get(0));
        fragmentQueue.remove(0);
        fragmentTransaction.add(R.id.main_frame, fragmentQueue.get(0));
        fragmentTransaction.commit();
        //super.onBackPressed();
    }

    public void showMessage(final String text){
        Log.d(TAG, "Message: " + text);
        handler.post(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(mainFrame, text, Snackbar.LENGTH_SHORT).show();
            }
        });
    }
    public void configureSideMenu(){
        try {
            findViewById(R.id.side_menu_button_accounts).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openAccountsTab();
                }
            });
            findViewById(R.id.side_menu_button_messages).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openMessagesTab();
                }
            });
            findViewById(R.id.side_menu_button_close).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(BotService.applicationManager != null)
                        BotService.applicationManager.stop();
                    stopService(new Intent(MainActivity.this, BotService.class));
                    finish();
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
            showMessage("Ошибка настройки бокового меню: " + e.getMessage());
        }
    }

    public void openMessagesTab(){
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if(!fragmentQueue.isEmpty())
            fragmentTransaction.remove(fragmentQueue.get(0));
        fragmentQueue.clear();
        fragmentQueue.add(0, new MessagesFragment());
        fragmentTransaction.add(R.id.main_frame, fragmentQueue.get(0));
        fragmentTransaction.commit();
        slideMenu.close(true);
    }
    public void openAccountsTab(){
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if(!fragmentQueue.isEmpty())
            fragmentTransaction.remove(fragmentQueue.get(0));
        fragmentQueue.clear();
        fragmentQueue.add(0, new AccountsFragment());
        fragmentTransaction.add(R.id.main_frame, fragmentQueue.get(0));
        fragmentTransaction.commit();
        slideMenu.close(true);
    }
    public void openAccountTab(TgAccount tgAccount){
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if(!fragmentQueue.isEmpty())
            fragmentTransaction.remove(fragmentQueue.get(0));
        //fragmentQueue.clear();
        fragmentQueue.add(0, new AccountTgFragment(tgAccount));
        fragmentTransaction.add(R.id.main_frame, fragmentQueue.get(0));
        fragmentTransaction.commit();
        slideMenu.close(true);
    }
}
