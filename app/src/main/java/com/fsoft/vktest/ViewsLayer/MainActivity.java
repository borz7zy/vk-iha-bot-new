package com.fsoft.vktest.ViewsLayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import java.util.List;
import java.util.Map;

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

///////////////////////////////////////////////////////////////////////////
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int MANAGE_EXTERNAL_STORAGE_CODE = 102;

    private ActivityResultLauncher<String[]> multiplePermissionsLauncher;
    private ActivityResultLauncher<Intent> manageExternalStorageLauncher;
///////////////////////////////////////////////////////////////////////////

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


        // Initialize ActivityResultLauncher for multiple permissions
        multiplePermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::handlePermissionResult
        );

        // Initialize ActivityResultLauncher for MANAGE_EXTERNAL_STORAGE
        manageExternalStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Environment.isExternalStorageManager()) {
                        // MANAGE_EXTERNAL_STORAGE permission granted
                        Toast.makeText(this, "MANAGE_EXTERNAL_STORAGE permission granted", Toast.LENGTH_SHORT).show();
                        // TODO: Proceed with operations requiring this permission
                    } else {
                        // MANAGE_EXTERNAL_STORAGE permission denied
                        Toast.makeText(this, "MANAGE_EXTERNAL_STORAGE permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        requestAllPermissions();
    }

    private void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Add permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above: Use new media permissions
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            // Before Android 13: Use legacy storage permissions (if needed)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // Add other permissions (always request)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.INTERNET);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.VIBRATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WAKE_LOCK);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_WIFI_STATE);
        }



        // Handle MANAGE_EXTERNAL_STORAGE separately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                manageExternalStorageLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                manageExternalStorageLauncher.launch(intent);
            }
        }

        // Request the remaining permissions using ActivityResultLauncher
        if (!permissionsToRequest.isEmpty()) {
            multiplePermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }

    }

    private void handlePermissionResult(Map<String, Boolean> permissions) {
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            String permission = entry.getKey();
            boolean granted = entry.getValue();

            if (granted) {
                Toast.makeText(this, permission + " permission granted", Toast.LENGTH_SHORT).show();
                // TODO: Proceed with operations requiring this permission
            } else {
                Toast.makeText(this, permission + " permission denied", Toast.LENGTH_SHORT).show();
                // Explain to the user why the permission is needed and potentially guide them to settings
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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