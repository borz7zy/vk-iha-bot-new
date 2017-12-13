package com.fsoft.vktest;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fsoft.vktest.ViewsLayer.MainActivity;

/**
 *
 * Created by Dr. Failov on 28.12.2014.
 */
public class BotService extends Service {
    static public ApplicationManager applicationManager = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationManager = new ApplicationManager(this);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.bot_noti)
                .setContentTitle("VK iHA bot")
                .setContentText(ApplicationManager.getShortName() + " работает")
                .setContentIntent(pendingIntent).build();
        startForeground(1, notification);

//            Log.d("BOT", "Планирование перезапуска...");
//            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
//            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 8000, pendingIntent);
//            stopForeground(true);
//            stopSelf();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("BOT", "ON SERVICE Destroy");
        if(applicationManager!= null && applicationManager.isRunning()) {
            applicationManager.stop();
//            applicationManager.activity.scheduleRestart();//запланируем перезапуск))))
//            applicationManager.activity.sleep(1000);
//            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
}
