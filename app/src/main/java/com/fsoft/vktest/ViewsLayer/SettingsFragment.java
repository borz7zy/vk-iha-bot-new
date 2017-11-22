package com.fsoft.vktest.ViewsLayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.*;
import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.AnswerDatabase;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.BootReceiver;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Modules.SecurityProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 *
 * Created by Dr. Failov on 11.11.2014.
 */
public class SettingsFragment  extends Fragment {
    private MainActivity context = null;
    private ApplicationManager applicationManager = null;
    TextView textViewStatus = null;
    View buttonBuyFullVersion = null;
    Handler handler = new Handler();

    TextView textViewResult = null;
    Dialog dialogWithBot = null;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        if(context.getClass().equals(MainActivity.class))
            this.context = (MainActivity)context;
        if(this.context != null)
            applicationManager = this.context.applicationManager;
        else
            return getMessage("Ошибка отрисовки меню. Перезапустите программу.", context);
        return tuneInterface();
    }
    private View tuneInterface() {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(F.dp(10), F.dp(10), F.dp(10), F.dp(10));
        linearLayout.setBackgroundColor(Color.BLACK);

        linearLayout.addView(getTitle("Действия:"));
        linearLayout.addView(getSpace());

        linearLayout.addView(getButton("Сменить обращение у бота", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ChangeTreatmentWindow(getActivity());
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("Вместо \"Бот, \" можно использовать любое другое обращение. " +
                "Или даже несколько разных."));
        linearLayout.addView(getSpace());




        linearLayout.addView(getButton("Убрать метку у бота", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageToBot("botcmd setbotname empty");
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("Если ты не хочешь чтобы бот в начале каждого сообщения писал \"(bot)\", " +
                "то это можно отключить. Но это можно сделать только если куплена донатка:)"));
        linearLayout.addView(getSpace());




        linearLayout.addView(getButton("Показать справку по всем командам", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageToBot("botcmd help");
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("У бота много команд, и их назначение можно " +
                "прочитать здесь. Команды боту может отправлять любой доверенный у бота пользователь." +
                " Команды можно отправлять боту в личку, на стену, или писать прямо тут через кнопку " +
                "\"Отправить боту сообщение\"."));
        linearLayout.addView(getSpace());




        linearLayout.addView(getButton("Написать боту сообщение", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageToBot("");
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("Боту можно написать прямо здесь, чтобы не " +
                "делать этого в ВК. То что ты напишешь здесь бот получит так, будто это было получено " +
                "в ВК от имени той страницы которую ты сюда добавил."));
        linearLayout.addView(getSpace());

        linearLayout.addView(getButton("Обучать бота", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                learnBot();
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("Это - самый простой способ обучать своего бота:)"));
        linearLayout.addView(getSpace());

//        linearLayout.addView(getButton("Сохранить базы", new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                saveSettings();
//            }
//        }));

        linearLayout.addView(buttonBuyFullVersion = getButton("Купить донатку для бота", new View.OnClickListener() {
            @Override public void onClick(View view) {
                openFullVersion();
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("Покупка донатки - это способ сказать \"Спасибо!\" мне " +
                "за бессонные ночи написания этой программы:) \nВ качестве бонуса вы получите: " +
                "\n1) Бот будет лучше работать когда свёрнут (но это не точно)" +
                "\n2) Можно общаться с ботом без обращения \"Бот,\"" +
                "\n3) Можно убрать метку бота \"(bot)\"" +
                "\n4) Можно научить бота отправлять картинки, музыку, видео, гифки."));
        linearLayout.addView(getSpace());

        linearLayout.addView(getButton("Перейти в группу проекта", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openProjectGroup();
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("У нас в группе можно найти много полезных " +
                "инструкций (смотрите в описании группы). Также у нас можно найти себе друзей и ещё ботов:)"));
        linearLayout.addView(getSpace());



        linearLayout.addView(getRedButton("Выключить бота", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.turnoff();
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("Если бота надо выключить, выключай его" +
                " только этой кнопкой. Иначе он сам запустится снова."));
        linearLayout.addView(getSpace());



        linearLayout.addView(getButton("История изменений", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChangelog();
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("Здесь описана вся история создания проекта, включая мои личные комментарии:) "));
        linearLayout.addView(getSpace());


        linearLayout.addView(getCheckBox("Запуск при загрузке телефона", BootReceiver.isRun(getActivity()), new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                BootReceiver.setRun(getActivity(), b);
            }
        }));
        linearLayout.addView(textViewStatus = getTextBlock("Если ты хочешь, чтобы на этом телефоне " +
                "бот работал постоянно, то можно сделать чтобы при включении телефона бот сам запускался " +
                "и начинал работать."));
        linearLayout.addView(getSpace());


        linearLayout.addView(getTitle("Состояние программы:"));
        linearLayout.addView(textViewStatus = getTextBlock("Нажмите \"Обновить\", чтобы увидеть статус программы"));
        linearLayout.addView(getLittleButton("Обновить состояние", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshData();
            }
        }));
        linearLayout.addView(getSpace());


        linearLayout.addView(getTitle("В разработке участвовали:"));
        linearLayout.addView(getTextBlock(ApplicationManager.getDevelopersList()));


        if(applicationManager != null) {
            applicationManager.securityProvider.addDonationStateChangedListener(new SecurityProvider.OnDonationStateChangedListener() {
                @Override
                public void donationStateChanged(Boolean donated) {
                    donationChanged(donated);
                }
            });
        }
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(linearLayout);
        return scrollView;
    }
    private void refreshData(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(applicationManager == null)
                    writeStatus("Программа загружена неправильно.");
                else
                    writeStatus(applicationManager.processCommands("status", applicationManager.getUserID()));
            }
        }).start();
    }
    private void showChangelog(){
        String changelog = "";
        Resources resources = applicationManager.activity.getResources();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(resources.openRawResource(R.raw.changelog)));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                changelog = sb.toString();
            } finally {
                br.close();
            }
        }catch (Exception e){
            e.printStackTrace();
            changelog = "Ошибка чтения файла: " + e.toString();
        }
        new AlertDialog.Builder(applicationManager.activity).setTitle("История изменений").setMessage(changelog).setPositiveButton("OK", null).show();
    }
    private void sendMessageToBot(String in){
        try {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_send_message_to_bot, null, false);
            textViewResult = (TextView) view.findViewById(R.id.dialog_send_message_to_bot_label_result);
            final EditText editTextMessage = (EditText) view.findViewById(R.id.dialog_send_message_to_bot_field_message);
            View buttonSend = view.findViewById(R.id.dialog_send_message_to_bot_button_send);
            View buttonClose = view.findViewById(R.id.dialog_send_message_to_bot_button_close);

            if (in.equals(""))
                textViewResult.setText("Введи сообщение и нажми \"Отправить\"");
            else {
                String reply;
                if (applicationManager.vkAccounts.size() == 0)
                    reply = "Ошибка: нет аккаунтов.";
                else
                    reply = applicationManager.processMessage(in, applicationManager.vkAccounts.get(0).id);
                textViewResult.setText(reply);
            }

            buttonSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String message = editTextMessage.getText().toString();
                    processMessageToBot(message);
                    textViewResult.setText("Подождите...");
                }
            });

            buttonClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (dialogWithBot != null)
                        dialogWithBot.dismiss();
                }
            });


            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(view);
            dialogWithBot = builder.show();
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getActivity(), "Error: " + e.toString(), Toast.LENGTH_SHORT);
        }
    }
    private void processMessageToBot(final String message){
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String reply;
                if(applicationManager.vkAccounts.size() == 0)
                    reply = "Ошибка: нет аккаунтов.";
                else
                    reply = applicationManager.processMessage(message, applicationManager.vkAccounts.get(0).id);
                ApplicationManager.log("message = {"+message+"}");
                ApplicationManager.log("reply = {" + reply + "}");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textViewResult.setText(reply);
                    }
                });
            }
        }).start();
    }
    private void learnBot(){
        new LearnBotWindow(getActivity()).show();
    }
    private void saveSettings(){
        String result = applicationManager.processCommands("save", applicationManager.getUserID());
        messageBox(result);
    }
    private void messageBox(String text){
        if(context != null)
            context.messageBox(text);
    }
    private void runOnUiThread(Runnable runnable){
        if(context != null)
            context.runOnUiThread(runnable);
    }
    private String log(String text){
        return ApplicationManager.log(text);
    }
    private void writeStatus(final String text){
        if(textViewStatus != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (textViewStatus != null && text != null) {
                        textViewStatus.setText(text);
                    } else {
                        log("! Не удаётся вывести статус.");
                    }
                }
            });
        }
        else {
            log("! Не удаётся вывести статус.");
        }
    }
    private void openProjectGroup(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(ApplicationManager.getGroupLink()));
        applicationManager.activity.startActivity(intent);
    }
    private void openFullVersion(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(ApplicationManager.getFullVersionLink()));
        applicationManager.activity.startActivity(intent);
    }
    private void donationChanged(final boolean donated){
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (buttonBuyFullVersion != null) {
                        if (donated)
                            buttonBuyFullVersion.setVisibility(View.GONE);
                        else
                            buttonBuyFullVersion.setVisibility(View.VISIBLE);
                    }
                }
                catch (Exception e){/*nothing*/}
            }
        });
    }
    private View getSpace(){
        View view = new View(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, F.dp(20));
        layoutParams.gravity = Gravity.CENTER;
        view.setLayoutParams(layoutParams);
        return view;
    }
    private TextView getTitle(String text){
        TextView textView = new TextView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        textView.setLayoutParams(layoutParams);
        textView.setText(text);
        textView.setTextSize(20);
        textView.setTextColor(Color.WHITE);
        return textView;
    }
    private TextView getButton(String text, View.OnClickListener onClickListener){
        TextView button = new TextView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.LEFT;
        button.setLayoutParams(layoutParams);
        button.setText(text);
        button.setTextSize(16);
        button.setPadding(F.dp(20), F.dp(8), F.dp(20), F.dp(9));
        button.setBackgroundColor(Color.DKGRAY);
        button.setTextColor(Color.WHITE);
        button.setOnClickListener(onClickListener);
        return button;
    }
    private TextView getRedButton(String text, View.OnClickListener onClickListener){
        TextView v = getButton(text, onClickListener);
        v.setBackgroundColor(Color.rgb(130, 50, 50));
        return v;
    }
    private TextView getMessage(String text, Context context1){
        TextView textView = new TextView(context1);
        textView.setText(text);
        return textView;
    }
    private CheckBox getCheckBox(String text, boolean checked, CheckBox.OnCheckedChangeListener onCheckedChangeListener){
        CheckBox checkBox = new CheckBox(context);
        checkBox.setChecked(checked);
        checkBox.setText(text);
        checkBox.setTextSize(17);
        checkBox.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        checkBox.setLayoutParams(layoutParams);
        checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
        return checkBox;
    }
    private TextView getTextBlock(String text){
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(Color.GRAY);
        textView.setTextSize(12);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(F.dp(10), F.dp(10), F.dp(10), F.dp(10));
        layoutParams.gravity = Gravity.LEFT;
        textView.setLayoutParams(layoutParams);
        return textView;
    }
    private TextView getLittleButton(String text, View.OnClickListener onClickListener){
        TextView button = getButton(text, onClickListener);
        button.setTextSize(13);
        return button;
    }

    class LearnBotWindow extends Dialog{
        AnswerDatabase answerDatabase;
        Context context;
        Button buttonNext;
        Button buttonClose;
        TextView textViewRemaining;
        TextView textViewInstruction;
        TextView textViewMessage;
        EditText editTextAnswer;

        LearnBotWindow(Context context) {
            super(context);
            this.context = context;
            answerDatabase = applicationManager.brain.answerDatabase;
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);


            buttonNext = new Button(context);
            buttonNext.setText("Дальше");
            buttonNext.setTextSize(12);
            buttonNext.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            buttonNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    next();
                }
            });


            buttonClose = new Button(context);
            buttonClose.setText("Закрыть");
            buttonClose.setTextSize(12);
            buttonClose.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            buttonClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    close();
                }
            });


            textViewInstruction = new TextView(context);
            textViewInstruction.setText("Все сообщения, на которые бот не знает ответа он вносит в специальный спискок неизвестных фраз. " +
                    "Ваша задача сейчас - последовательно отвечать на сообщения, потображаемые в текстовом поле ниже и нажимать кнопку Далее.");
            textViewInstruction.setTextColor(Color.GRAY);
            textViewInstruction.setPadding(5, 2, 5, 0);


            textViewRemaining = new TextView(context);
            textViewRemaining.setText("Осталось ... сообщений.");
            textViewRemaining.setTextColor(Color.GRAY);
            textViewRemaining.setPadding(5, 0, 5, 10);


            textViewMessage = new TextView(context);
            textViewMessage.setText("NOTHING");
            textViewMessage.setTextColor(Color.WHITE);
            textViewMessage.setPadding(15, 10, 5, 10);


            editTextAnswer = new EditText(context);
            editTextAnswer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            editTextAnswer.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER){
                        next();
                        return true;
                    }
                    return false;
                }
            });


            LinearLayout linearLayoutBottom = new LinearLayout(context);
            linearLayoutBottom.setOrientation(LinearLayout.HORIZONTAL);
            linearLayoutBottom.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            linearLayoutBottom.addView(buttonClose);
            linearLayoutBottom.addView(buttonNext);


            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
            linearLayout.addView(textViewInstruction);
            linearLayout.addView(textViewRemaining);
            linearLayout.addView(textViewMessage);
            linearLayout.addView(editTextAnswer);
            linearLayout.addView(linearLayoutBottom);


            ScrollView scrollView = new ScrollView(context);
            scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
            scrollView.addView(linearLayout);
            setContentView(scrollView);
        }
        @Override  public void show() {
            super.show();
            next();
        }
        void next(){
            String message = textViewMessage.getText().toString();
            String reply = editTextAnswer.getText().toString();
            if(!message.equals("NOTHING")) {
                if (!reply.equals("")) {
                    String res = answerDatabase.addToDatabase(message, reply);
                    ApplicationManager.log(res);
                    textViewInstruction.setText(res);
                }
                if(answerDatabase.unknownMessages.size() > 0)
                    answerDatabase.unknownMessages.remove(0);
            }
            editTextAnswer.setText("");
            if(answerDatabase.unknownMessages.size() > 0){
                message = answerDatabase.unknownMessages.get(0);
                textViewMessage.setText(message);
                textViewRemaining.setText("Осталось "+answerDatabase.unknownMessages.size()+" сообщений.");
            }
            else {
                textViewRemaining.setText("Больше нет сообщений. Общайтесь с ботом больше, чтобы он пополнил списки фразами, которых он не знает.");
                 textViewMessage.setText("NOTHING");
            }

        }
        void close(){
            dismiss();
        }
        void text(String text){

        }
    }

    class ChangeTreatmentWindow{
        Context context = null;
        Dialog window = null;
        EditText editTextTreatments = null;
        Button buttonCancel = null;
        Button buttonClear = null;
        Button buttonSave = null;

        public ChangeTreatmentWindow(Context context) {
            this.context = context;
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_change_treatment, null, false);

            editTextTreatments = (EditText)view.findViewById(R.id.dialog_change_treatment_field_treatment);
            buttonCancel = (Button)view.findViewById(R.id.dialog_change_treatment_button_close);
            buttonClear = (Button)view.findViewById(R.id.dialog_change_treatment_button_clear);
            buttonSave = (Button)view.findViewById(R.id.dialog_change_treatment_button_save);


            //todo finish it
            buttonSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            buttonCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(window != null)
                        window.dismiss();
                }
            });
            buttonClear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            if(editTextTreatments != null){
                //editTextTreatments.setText(ApplicationManager);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(view);
            window = builder.show();
        }


    }
}
