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

import com.fsoft.vktest.BotService;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.R;
import com.fsoft.vktest.ViewsLayer.AccountTgFragment.AccountTgFragment;
import com.fsoft.vktest.ViewsLayer.AccountsFragment.AccountsFragment;

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
    private Fragment activeFragment = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mainFrame = findViewById(R.id.main_frame);
        slideMenu = findViewById(R.id.main_slideMenu);
        configureSideMenu();

        openMessagesTab();
        //проверить запущен ли сервис. если нет - запустить.
        Log.d(TAG, "Starting service...");
        Intent intent = new Intent(getApplicationContext(), BotService.class);
        startService(intent);
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
        if(activeFragment != null) {
            fragmentTransaction.remove(activeFragment);
            activeFragment = null;
        }
        activeFragment = new MessagesFragment();
        fragmentTransaction.add(R.id.main_frame, activeFragment);
        fragmentTransaction.commit();
        slideMenu.close(true);
    }

    public void openAccountsTab(){
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if(activeFragment != null) {
            fragmentTransaction.remove(activeFragment);
            activeFragment = null;
        }
        activeFragment = new AccountsFragment();
        fragmentTransaction.add(R.id.main_frame, activeFragment);
        fragmentTransaction.commit();
        slideMenu.close(true);
    }
    public void openAccountTab(TgAccount tgAccount){
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        if(activeFragment != null) {
            fragmentTransaction.remove(activeFragment);
            activeFragment = null;
        }
        activeFragment = new AccountTgFragment(tgAccount);
        fragmentTransaction.add(R.id.main_frame, activeFragment);
        fragmentTransaction.commit();
        slideMenu.close(true);
    }
}
