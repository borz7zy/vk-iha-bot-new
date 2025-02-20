package com.fsoft.vktest;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.fsoft.vktest.NewViewsLayer.MainActivity;
//import kotlin.ULong;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BotService extends Service {
    static public ApplicationManager applicationManager = null;
    private static final String CHANNEL_ID = "vk_bot_service";
    private static boolean isServiceRunning = false;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            applicationManager = new ApplicationManager(BotService.this);
            BotApplication.getInstance().setApplicationManager(applicationManager);

            ApplicationManager.getInstance().setContext(getApplicationContext());

            isServiceRunning = true;
        } catch (Exception e) {
            Log.e("iHA bot", "Ошибка запуска сервиса: " + e.getMessage(), e);
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        createNotificationChannel(); // Создаем канал уведомлений для Android 8+

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.bot_noti)
                .setContentTitle("VK iHA bot")
                .setContentText(ApplicationManager.getShortName() + " работает")
                .setContentIntent(pendingIntent)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "VK Bot Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (applicationManager != null && applicationManager.isRunning()) {
            applicationManager.stop();
        }
        isServiceRunning = false;
        executorService.shutdownNow();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        Log.d("BOT", "ON SERVICE Destroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
