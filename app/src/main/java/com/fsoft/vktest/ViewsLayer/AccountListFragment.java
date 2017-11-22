package com.fsoft.vktest.ViewsLayer;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccountCore;

import java.util.Timer;

/**
 *
 * Created by Dr. Failov on 11.11.2014.
 */
public class AccountListFragment extends Fragment {
    Button addAccountButton = null;
    LinearLayout linearLayoutAccounts = null;
    MainActivity mainActivity = null;
    ApplicationManager applicationManager = null;
    Handler handler = null;
    Timer timer = null;
    int lastListSize = 0;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        handler = new Handler();
        if(context.getClass().equals(MainActivity.class))
            mainActivity = (MainActivity)context;
        if(mainActivity != null)
            applicationManager = mainActivity.applicationManager;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        {
            TextView textView = new TextView(context);
            textView.setText("Аккаунты");
            textView.setTextSize(20);
            linearLayout.addView(textView);
            TextView textView1 = new TextView(context);
            textView1.setText("Внимание! Если добавить несколько дублирующихся аккаунтов, программа будет работать некорректно. Следите за отсутствием повторов!");
            linearLayout.addView(textView1);
        }

        linearLayoutAccounts = new LinearLayout(context);
        linearLayoutAccounts.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayoutAccounts.setOrientation(LinearLayout.VERTICAL);
        {
            TextView textView = new TextView(mainActivity);
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setTextSize(20);
            textView.setTextColor(Color.argb(255, 100, 100, 100));
            textView.setText("Нажмите кнопку \"Обновить\", чтобы отобразить содержимое списка.");
            linearLayoutAccounts.addView(textView);
        }
        linearLayout.addView(linearLayoutAccounts);
        addAccountButton = new Button(context);
        addAccountButton.setText("Добавить аккаунт");
        addAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applicationManager.vkAccounts.addAccount();
            }
        });
        linearLayout.addView(addAccountButton);
        Button buttonRefresh = new Button(context);
        buttonRefresh.setText("Обновить список");
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rebuildList();
            }
        });
        linearLayout.addView(buttonRefresh);
        scrollView.addView(linearLayout);
        return scrollView;
    }
    void rebuildList(){
        applicationManager.activity.showWaitingDialog();
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    linearLayoutAccounts.removeAllViews();
                    if(applicationManager.vkAccounts.size() == 0){
                        TextView textView = new TextView(mainActivity);
                        textView.setGravity(Gravity.CENTER);
                        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        textView.setTextSize(20);
                        textView.setTextColor(Color.argb(100, 255, 255, 255));
                        textView.setText("Аккаунтов нет. Программа не может работать без аккаунтов. Нажмите кнопку \"Добавить\", чтобы внести аккаунт в программу. Чем больше аккаунтов, тем быстрее будет работать бот.");
                        linearLayoutAccounts.addView(textView);
                    }
                    for (int i = 0; i < applicationManager.vkAccounts.size(); i++) {
                        VkAccountCore vkAccount = applicationManager.vkAccounts.get(i);
                        linearLayoutAccounts.addView(vkAccount.getView(mainActivity));
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                    log("Error rebuildList: " + e.toString());
                }
                applicationManager.activity.hideWaitingDialog();
            }
        });
    }
    void log(String text){
        ApplicationManager.log(text);
    }
}
