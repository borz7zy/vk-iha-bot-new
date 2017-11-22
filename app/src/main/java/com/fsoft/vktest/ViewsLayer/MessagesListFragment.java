package com.fsoft.vktest.ViewsLayer;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.fsoft.vktest.ApplicationManager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * Created by Dr. Failov on 11.11.2014.
 */
public class MessagesListFragment extends Fragment {
    MainActivity mainActivity = null;
    ApplicationManager applicationManager = null;
    Timer timer = null;
    TextView textViewState = null;
    MessageList messageList = null;
    int messagesProcessed = 0;
    float processorUsage = 0;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        if(context.getClass().equals(MainActivity.class))
            mainActivity = (MainActivity)context;

        return MainActivity.messageList = messageList = new MessageList(mainActivity);
    }
    @Override public void onResume() {
        super.onResume();
        if(mainActivity != null)
            applicationManager = mainActivity.applicationManager;
        if(timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    processorUsage = readUsage();
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                refreshState();
                            }
                            catch (Exception e){
                                e.printStackTrace();
                                log("Error refreshState: " + e.toString());
                            }
                        }
                    });
                }
            }, 1000, 1000);
        }
    }
    @Override public void onPause() {
        super.onPause();
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    void log(String text){
        ApplicationManager.log(text);
    }
    private View getDelimiter(Context context){
        View delimiter = new View(context);
        delimiter.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
        delimiter.setBackgroundColor(Color.argb(60, 255, 255, 255));
        return delimiter;
    }
    private void refreshState(){
        if(applicationManager == null || applicationManager.vkAccounts == null){
            textViewState.setText("Потеряна связь с программой.");
            textViewState.setTextColor(Color.RED);
            return;
        }
        int working = applicationManager.vkAccounts.getActiveCount();
        int total = applicationManager.vkAccounts.size();
        int state = total == 0?0:(working * 100)/total;
        String stateString = "";
        if(state > 95) {
            stateString = "в норме.";
            textViewState.setTextColor(Color.rgb(100, 255, 100));
        }
        else if(state > 10) {
            stateString = "требуется вмешательство.("+state+"%)";
            textViewState.setTextColor(Color.rgb(255, 255, 100));
        }
        else {
            stateString = "всё плохо.";
            textViewState.setTextColor(Color.rgb(255, 0, 100));
        }
        textViewState.setText("- Состояние программы: " + stateString + "\n" +
                (state < 95 ? "- Проверьте состояние на вкладке \"Аккаунты\"!\n" : "") +
                "- Обработано сообщений: " + messagesProcessed + "\n" +
                "- Время работы: " + applicationManager.vkCommunicator.getWorkingTime() + "\n" +
                //"- Нагрузка на процессор: " + (int)(getUsage() * 100) + "%\n" +
                "- Сообщений в обработке: " + getPendingCount());
    }
    private float getUsage(){
        return processorUsage;
    }
    private float readUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" +");  // Split on one or more spaces

            long idle1 = Long.parseLong(toks[4]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            try {
                Thread.sleep(360);
            } catch (Exception e) {}

            reader.seek(0);
            load = reader.readLine();
            reader.close();

            toks = load.split(" +");

            long idle2 = Long.parseLong(toks[4]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }
    private int getPendingCount(){
        int cnt = 0;
        if(messageList != null) {
            LinearLayout linearLayout = messageList.linearLayout;
            for (int i = 0; i <linearLayout.getChildCount(); i++) {
                View view = linearLayout.getChildAt(i);
                if(view.getClass().equals(MessageList.MessageListElement.class)) {
                    try {
                        MessageList.MessageListElement messageListElement = (MessageList.MessageListElement) view;
                        if(messageListElement.isPending())
                            cnt++;
                    }
                    catch (Exception e){
                        //skip
                    }
                }
            }
        }
        return cnt;
    }

    public class MessageList extends ScrollView{
        LinearLayout linearLayout;

        public MessageList(Context context) {
            super(context);
            LinearLayout mainLinear = new LinearLayout(context);
            mainLinear.setOrientation(LinearLayout.VERTICAL);
            addView(mainLinear);
            {
                TextView textView = new TextView(context);
                textView.setTextSize(15);
                textView.setTextColor(Color.WHITE);
                LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER;
                textView.setLayoutParams(layoutParams);
                textView.setPadding(10, 0, 10, 0);
                textView.setText("Здесь будут отображаться сообщения, адресованные боту.");
                //mainLinear.addView(textView);
            }
            {
                TextView textView = textViewState = new TextView(context);
                textView.setTextSize(12);
                LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER;
                textView.setLayoutParams(layoutParams);
                textView.setPadding(10, 0, 10, 0);
                textView.setText("Чтение состояния программы...");
                mainLinear.addView(textView);
            }

            linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            mainLinear.addView(linearLayout);
            {
                TextView textView = new TextView(context);
                textView.setTextSize(25);
                textView.setTextColor(Color.argb(68, 255, 255, 255));
                LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER;
                textView.setLayoutParams(layoutParams);
                textView.setText("Ожидание сообщений...");
                linearLayout.addView(textView);
            }
            {
                TextView textView = new TextView(context);
                textView.setTextSize(13);
                textView.setTextColor(Color.argb(120, 255, 255, 255));
                textView.setGravity(Gravity.CENTER);
                LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                layoutParams.gravity = Gravity.CENTER;
                textView.setLayoutParams(layoutParams);
                textView.setText("Напишите боту \"Бот, привет\", чтобы убедиться что он работает.");
                linearLayout.addView(textView);
            }
        }
        public MessageListElement registerNewMessage(String text, Long sender, String source){
            messagesProcessed ++;
            final MessageListElement messageListElement = new MessageListElement(mainActivity, text, sender, source);
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        linearLayout.addView(messageListElement, 0);
                        while (linearLayout.getChildCount() > 50) {
                            linearLayout.removeViewAt(50);
                        }
                        linearLayout.destroyDrawingCache();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        log("Error registerNewMessage: " + e.toString());
                    }
                }
            });
            return messageListElement;
        }
        public int getMessagesProcessed(){
            return messagesProcessed;
        }

        public class MessageListElement extends LinearLayout{
            final int PREPARING_ANSWER = 0;
            final int SENDING_ANSWER = 1;
            final int PROCESSED_SUCCESSFULLY = 2;
            final int IGNORED = 3;

            TextView textViewSource = new TextView(mainActivity);
            TextView textViewTime = new TextView(mainActivity);
            TextView textViewUserName = new TextView(mainActivity);
            TextView textViewMessage = new TextView(mainActivity);
            TextView textViewReply = new TextView(mainActivity);
            String source;
            int state = PREPARING_ANSWER;
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());

            public MessageListElement(final Context context, final String text, final long sender, final String source) {
                super(context);
                this.source = source;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setOrientation(VERTICAL);
                            addView(getDelimiter(context));
                            setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    try {
                                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        clipboard.setText(Long.toString(sender));
                                        Toast.makeText(context, "ID пользователя скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
                                    }
                                    catch (Exception e){
                                        log("! Ошибка копирования ID пользователя в буфер обмена: " + e.toString());
                                        e.printStackTrace();
                                    }
                                }
                            });
                            {
                                LinearLayout linearLayout = new LinearLayout(context);
                                linearLayout.setOrientation(HORIZONTAL);
                                linearLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                addView(linearLayout);
                                {
                                    textViewSource.setTextColor(Color.argb(128, 255, 255, 255));
                                    textViewSource.setTextSize(10);
                                    textViewSource.setGravity(Gravity.LEFT);
                                    textViewSource.setPadding(10, 0, 10, 0);
                                    LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                                    layoutParams.gravity = Gravity.LEFT;
                                    layoutParams.weight = 0;
                                    textViewSource.setLayoutParams(layoutParams);
                                    textViewSource.setText(source);
                                    linearLayout.addView(textViewSource);
                                }
                                {
                                    textViewUserName.setTextColor(Color.argb(128, 255, 255, 255));
                                    textViewUserName.setTextSize(10);
                                    textViewUserName.setGravity(Gravity.CENTER);
                                    textViewUserName.setPadding(10, 0, 10, 0);
                                    LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
                                    layoutParams.gravity = Gravity.CENTER;
                                    layoutParams.weight = 1;
                                    textViewUserName.setLayoutParams(layoutParams);
                                    textViewUserName.setText(String.valueOf(sender));
                                    linearLayout.addView(textViewUserName);
                                }
                                {
                                    textViewTime.setTextColor(Color.argb(128, 255, 255, 255));
                                    textViewTime.setTextSize(10);
                                    textViewTime.setGravity(Gravity.RIGHT);
                                    textViewTime.setPadding(10, 0, 10, 0);
                                    LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                                    layoutParams.gravity = Gravity.RIGHT;
                                    layoutParams.weight = 0;
                                    textViewTime.setLayoutParams(layoutParams);
                                    textViewTime.setText("("+time+")");
                                    linearLayout.addView(textViewTime);
                                }
                            }
                            {
                                textViewMessage.setTextColor(Color.argb(255, 100, 255, 255));
                                textViewMessage.setTextSize(12);
                                textViewMessage.setPadding(10, 0, 50, 0);
                                LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                                layoutParams.gravity = Gravity.LEFT;
                                textViewMessage.setLayoutParams(layoutParams);
                                if (text != null)
                                    textViewMessage.setText(cropMessage(text));
                                else
                                    textViewMessage.setText("null");
                                addView(textViewMessage);
                            }
                            {
                                textViewReply.setTextColor(Color.argb(255, 255, 100, 30));
                                textViewReply.setTextSize(12);
                                textViewReply.setPadding(50, 0, 10, 0);
                                LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                                layoutParams.gravity = Gravity.RIGHT;
                                textViewReply.setLayoutParams(layoutParams);
                                textViewReply.setText("Подготовка ответа...");
                                addView(textViewReply);
                            }
                            addView(getDelimiter(context));

                            setBackgroundColor(Color.argb(60, 255, 158, 0));
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            log("Error MessageListElement: " + e.toString());
                        }
                    }
                });
            }
            public void registerAnswer(final String text){
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            state = PROCESSED_SUCCESSFULLY;
                            textViewReply.setTextColor(Color.argb(255, 150, 255, 150));
                            setBackgroundColor(Color.argb(60, 0, 128, 100));
                            if (text != null)
                                textViewReply.setText(cropMessage(text));
                            else
                                markIgnored();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            log("Error registerAnswer: " + e.toString());
                        }
                    }
                });
            }
            public void registerSenderName(final String text){
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(textViewUserName != null && text != null)
                            textViewUserName.setText(text);
                    }
                });
            }
            public void markSending(){
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            state = SENDING_ANSWER;
                            textViewReply.setText("Отправка ответа...");
                            textViewReply.setTextColor(Color.argb(255, 255, 255, 150));
                            setBackgroundColor(Color.argb(60, 128, 128, 45));
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            log("Error markSending: " + e.toString());
                        }
                    }
                });
            }
            public void markIgnored(){
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            state = IGNORED;
                            setVisibility(GONE);
//                            textViewReply.setText("Игнорировать.");
//                            textViewReply.setTextColor(Color.argb(200, 200, 200, 200));
//                            textViewReply.setVisibility(GONE);
//                            setBackgroundColor(Color.argb(60, 128, 128, 128));
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            log("Error markIgnored: " + e.toString());
                        }
                    }
                });
            }
            public boolean isPending(){
                return state == PREPARING_ANSWER || state == SENDING_ANSWER;
            }
            private String cropMessage(String in){
                if(in == null)
                    return in;
                int ncounter = 0;
                for(int i=0; i<in.length(); i++){
                    if(in.charAt(i) == '\n')
                        ncounter ++;
                    if(ncounter > 2 || i > 60){
                        return in.substring(0, i-1)+"...";
                    }
                }
                return in;
            }
        }
    }
}
