package com.fsoft.vktest.Communication.Account.VK;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Этот обьект должен принимать на вход аккаунт и показывать его состояние
 * Created by Dr. Failov on 21.02.2017.
 */
public class AccountView extends LinearLayout {
    Handler handler;
    boolean lastState = false;
    Context context = null;
    AlertDialog alertDialog = null;

    AccountView(Context context) {
        super(context);
        this.context = context;
        handler = new Handler();
        setOrientation(LinearLayout.VERTICAL);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
        addView(getDelimiter(context));

        int color = isActive() ? (isReady() ? Color.GREEN : Color.YELLOW) : Color.GRAY;

        {
            TextView textView = new TextView(context);
            textView.setPadding(20, 0, 0, 0);
            textView.setTextColor(color);
            textView.setText(userName);
            textView.setTextSize(18);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(isActive() ? color : Color.rgb(255, 100, 100));
            textView.setText("Состояние: " + (isActive() ? "активен" : "выключен") + " (" + getState() + ")");
            textView.setTextSize(isActive()?10:13);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("Использование: " + (isReady()?"свободен":"занят"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("обращений к API: " + getApiCounter() + " запросов");
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("обработка сообщений: " + (messageProcessing ? "включено" : "отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("работа в беседах: " + (processChats ? "включено" : "отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("пересылать сообщение собеседника в чатах: " + (replyMessagesInChats ? "включено" : "отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("отвечать инструкцией: " + (replyAnyMessage?"включено":"отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("принято сообщений: " + messageCounter);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("отправлено сообщений: " + messageSent);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("принято друзей: " + acceptedRequests);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("отклонено подписок: " + rejectedFolowers);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("заблокировано удалившихся друзей: " + blacklistedFolowers);
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("трансляция статуса: " + (statusBroadcasting?"включено":"отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("принятие всех друзей: " + (acceptAnyRequest?"включено":"отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("отклонение подписок: " + (rejectFollowers?"включено":"отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("отправка в ЧС тех кто удалил из друзей: " + (blacklistFollowers?(rejectFollowers?"включено":"требуется включить отклонение подписок"):"отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("выход из бесед где не общаются с ботом: " + (exitFromOfftopChats?"включено":"отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        {
            TextView textView = new TextView(context);
            textView.setTextColor(color);
            textView.setText("выход из бесед с ботами: " + (exitFromBotsChats?"включено":"отключено"));
            textView.setTextSize(10);
            addView(textView);
        }
        addView(getDelimiter(context));
    }
    private View getDelimiter(Context context){
        View delimiter = new View(context);
        delimiter.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
        delimiter.setBackgroundColor(Color.DKGRAY);
        return delimiter;
    }
    public void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(VERTICAL);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(linearLayout);
        builder.setView(scrollView);
        {
            TextView textView = new TextView(context);
            textView.setText("Аккаунт " + userName);
            textView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(20);
            textView.setTextColor(Color.WHITE);
            linearLayout.addView(textView);
            linearLayout.addView(getDelimiter(context));
        }

        linearLayout.addView(getOnOffRow("Активный ("+isActive()+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setActive(true, "Включено вручную");
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setActive(false, "Выключено вручную");
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Отвечать инструкцией ("+replyAnyMessage+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                replyAnyMessage = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                replyAnyMessage = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Принимать все заявки ("+acceptAnyRequest+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setAcceptAnyRequest(true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setAcceptAnyRequest(false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Трансляция статуса ("+statusBroadcasting+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setStatusBroadcasting(true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setStatusBroadcasting(false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Обрабатывать сообщения ("+messageProcessing+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setMessageProcessing(true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setMessageProcessing(false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Выходить из оффтопных бесед ("+exitFromOfftopChats+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromOfftopChats = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromOfftopChats = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Выходить из бесед с ботами ("+exitFromBotsChats+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromBotsChats = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromBotsChats = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Отвечать в беседах ("+processChats+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                processChats = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                processChats = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Отписываться от удаливших из друзей ("+rejectFollowers+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setRejectFollowers(true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                setRejectFollowers(false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Отправлять в ЧС тех кто удалил из друзей ("+(rejectFollowers && blacklistFollowers)+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                setRejectFollowers(true);
                blacklistFollowers = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                blacklistFollowers = (false);
                closeDialog();
            }
        }));


        linearLayout.addView(getOnOffRow("Пересылать сообщение собеседника в чатах ("+(replyMessagesInChats)+")",new OnClickListener() {
            @Override
            public void onClick(View view) {
                replyMessagesInChats = (true);
                closeDialog();
            }
        }, new OnClickListener() {
            @Override
            public void onClick(View view) {
                replyMessagesInChats = (false);
                closeDialog();
            }
        }));


        {
            Button button = new Button(context);
            button.setText("Перезайти в аккаунт");
            button.setTextColor(Color.YELLOW);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLoginWindow();
                    closeDialog();
                }
            });
            linearLayout.addView(button);
        }
        {
            Button button = new Button(context);
            button.setText("Удалить");
            button.setTextColor(Color.RED);
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    applicationManager.vkAccounts.removeAccount(thisAccount);
                    closeDialog();
                }
            });
            linearLayout.addView(button);
        }
        alertDialog = builder.show();
    }
    public void closeDialog(){
        if(alertDialog != null)
            alertDialog.dismiss();
    }
    private LinearLayout getOnOffRow(String text, OnClickListener onClickListener, OnClickListener offClickListener){
        TextView textView = new TextView(context);
        textView.setText(text.trim());
        textView.setPadding(0, 0, 0, 0);
        textView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button buttonOn = new Button(context);
        buttonOn.setText("вкл");
        buttonOn.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        buttonOn.setTextColor(Color.GREEN);
        buttonOn.setOnClickListener(onClickListener);

        Button buttonOff = new Button(context);
        buttonOff.setText("выкл");
        buttonOff.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        buttonOff.setTextColor(Color.YELLOW);
        buttonOff.setOnClickListener(offClickListener);

        LinearLayout horizontalLayout = new LinearLayout(context);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        horizontalLayout.setPadding(10, 10, 10, 10);
        horizontalLayout.setOrientation(HORIZONTAL);
        horizontalLayout.setGravity(Gravity.CENTER);
        horizontalLayout.addView(textView);
        horizontalLayout.addView(buttonOn);
        horizontalLayout.addView(buttonOff);
        return horizontalLayout;
    }
}
