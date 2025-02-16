package com.fsoft.vktest;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.fsoft.vktest.ViewsLayer.MainActivity;

public class BotService extends Service {
    static public ApplicationManager applicationManager = null;
    private static final String CHANNEL_ID = "vk_bot_service";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            applicationManager = new ApplicationManager(BotService.this);
            BotApplication.getInstance().setApplicationManager(applicationManager);

            ApplicationManager.getInstance().setContext(getApplicationContext());

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
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build();

            startForeground(1, notification);
        } catch (Exception e) {
            Log.e("iHA bot", "Ошибка запуска сервиса: " + e.getMessage(), e);
        }
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
        Log.d("BOT", "ON SERVICE Destroy");
        if (applicationManager != null && applicationManager.isRunning()) {
            applicationManager.stop();
        }
    }
}
