package com.fsoft.vktest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.ViewsLayer.MainActivity;

/**
 * Служит для обработки события перезагрузки устройства
 * Created by Dr. Failov on 28.12.2014.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(isRun(context)) {
            Log.d("BOT", "BOOT COMPLETED EVENT. Scheduling bot running after 15 seconds...");

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent1 = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent1, 0);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 15000, pendingIntent);
        }
    }
    public static boolean isRun(Context context){
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(ApplicationManager.programName, Context.MODE_PRIVATE);
            return sharedPreferences.getBoolean("autorun", false);
        }
        catch (Exception e){
            e.printStackTrace();
            return true;
        }
    }
    public static void setRun(Context context, boolean newVelue){
        try {

            SharedPreferences sharedPreferences = context.getSharedPreferences(ApplicationManager.programName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("autorun", newVelue);
            editor.commit();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
