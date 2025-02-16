package com.fsoft.vktest.ViewsLayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.fsoft.vktest.BotApplication;
import com.google.android.material.snackbar.Snackbar;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.BotService;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.ViewsLayer.AccountTgFragment.AccountTgFragment;
import com.fsoft.vktest.ViewsLayer.AccountsFragment.AccountsFragment;
import com.fsoft.vktest.ViewsLayer.MessagesFragment.MessagesFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static MainActivity instance = null;
    public static MainActivity getInstance() {
        return instance;
    }

    private String TAG = "MainActivity";
    private Handler handler = new Handler();
    private FrameLayout mainFrame = null;
    private DrawerLayout drawerLayout = null;

    private ArrayList<Fragment> fragmentQueue = new ArrayList<>(); // zero activity = active, first = last, second = prelast

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

        mainFrame = findViewById(R.id.main_frame);
        drawerLayout = findViewById(R.id.drawer_layout);

        configureSideMenu();

        // Проверка, запущен ли сервис. Если нет — запуск.
        Log.d(TAG, "Starting service...");
        Intent intent = new Intent(getApplicationContext(), BotService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent); // Использование startForegroundService для API 26 и выше
        } else {
            startService(intent);
        }

        // Get instance of ApplicationManager and pass the context
        new Thread(new Runnable() {
            @Override
            public void run() {
                //while (ApplicationManager.getInstance() == null) {
                while (BotApplication.getInstance().getApplicationManager() == null) {
                    F.sleep(10);
                }
                // Pass context to ApplicationManager
                ApplicationManager.getInstance().setContext(getApplicationContext());

                while (ApplicationManager.getInstance().getMessageHistory() == null) {
                    F.sleep(10);
                }
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
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (fragmentQueue.size() <= 1) {
            finish();
            return;
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.remove(fragmentQueue.get(0));
        fragmentQueue.remove(0);
        fragmentTransaction.add(R.id.main_frame, fragmentQueue.get(0));
        fragmentTransaction.commit();
    }

    public void showMessage(final String text) {
        Log.d(TAG, "Message: " + text);
        handler.post(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(mainFrame, text, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    public void configureSideMenu() {
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
                    if (BotService.applicationManager != null)
                        BotService.applicationManager.stop();
                    stopService(new Intent(MainActivity.this, BotService.class));
                    finish();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Ошибка настройки бокового меню: " + e.getMessage());
        }
    }

    public void openMessagesTab() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (!fragmentQueue.isEmpty())
            fragmentTransaction.remove(fragmentQueue.get(0));
        fragmentQueue.clear();
        fragmentQueue.add(0, new MessagesFragment());
        fragmentTransaction.add(R.id.main_frame, fragmentQueue.get(0));
        fragmentTransaction.commit();
        drawerLayout.closeDrawers();
    }

    public void openAccountsTab() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (!fragmentQueue.isEmpty())
            fragmentTransaction.remove(fragmentQueue.get(0));
        fragmentQueue.clear();
        fragmentQueue.add(0, new AccountsFragment(BotApplication.getInstance().getApplicationManager()));
        fragmentTransaction.add(R.id.main_frame, fragmentQueue.get(0));
        fragmentTransaction.commit();
        drawerLayout.closeDrawers();
    }

    public void openAccountTab(TgAccount tgAccount) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        if (!fragmentQueue.isEmpty())
            fragmentTransaction.remove(fragmentQueue.get(0));

        fragmentQueue.add(0, new AccountTgFragment(tgAccount, BotApplication.getInstance().getApplicationManager()));
        fragmentTransaction.add(R.id.main_frame, fragmentQueue.get(0));
        fragmentTransaction.commit();
        drawerLayout.closeDrawers();
    }
}