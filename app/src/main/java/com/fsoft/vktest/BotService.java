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
    static public MainActivity mainActivity = null;
    static public ApplicationManager applicationManager = null;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("BOT", "ON SERVICE Destroy");
        if(applicationManager!= null && applicationManager.running) {
            applicationManager.close();
            applicationManager.activity.scheduleRestart();//запланируем перезапуск))))
            applicationManager.activity.sleep(1000);
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(mainActivity != null) {
            if(mainActivity.applicationManager == null) {
                applicationManager = new ApplicationManager(mainActivity, "bot");
                mainActivity.applicationManager = applicationManager;
            }
            applicationManager.load();
            ApplicationManager.log(". Система запущена.");
            mainActivity = null;

//            Notification notification = new Notification(R.drawable.bot_noti, ApplicationManager.getShortName() + " работает", System.currentTimeMillis());
//            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 1, new Intent(getApplicationContext(), MainActivity.class),0);
//            notification.setLatestEventInfo(getApplicationContext(), ApplicationManager.getShortName() + " работает", ApplicationManager.getVisibleName(), contentIntent);
//            startForeground(1, notification);


            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.bot_noti)
                    .setContentTitle("VK iHA bot")
                    .setContentText(ApplicationManager.getShortName() + " работает")
                    .setContentIntent(pendingIntent).build();
            startForeground(1, notification);
        }
        else {
            Log.d("BOT", "Планирование перезапуска...");
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 8000, pendingIntent);
            stopForeground(true);
            stopSelf();
        }
    }
}
