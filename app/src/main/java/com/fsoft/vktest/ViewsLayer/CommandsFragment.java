package com.fsoft.vktest.ViewsLayer;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Utils.SimpleEntry;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by Dr. Failov on 25.01.2015.
 */
public class CommandsFragment extends Fragment {
    LinearLayout linearLayoutItems = null;
    MainActivity context = null;
    ApplicationManager applicationManager = null;
    TextView textViewDescription;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        if(context.getClass().equals(MainActivity.class))
            this.context = (MainActivity)context;
        if(this.context != null)
            applicationManager = this.context.applicationManager;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        {
            TextView textView = new TextView(context);
            textView.setText(getFragmentName());
            textView.setTextSize(20);
            linearLayout.addView(textView);
        }
        textViewDescription = new TextView(context);
        textViewDescription.setText(getListDescription());
        linearLayout.addView(textViewDescription);
        linearLayout.addView(getDelimiter(context));
        linearLayoutItems = new LinearLayout(context);
        linearLayoutItems.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayoutItems.setOrientation(LinearLayout.VERTICAL);
        {
            TextView textView = new TextView(context);
            textView.setGravity(Gravity.CENTER);
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setTextSize(20);
            textView.setTextColor(Color.argb(255, 100, 100, 100));
            textView.setText("Нажмите кнопку \"Обновить\", чтобы отобразить содержимое списка.");
            linearLayoutItems.addView(textView);
        }
        linearLayout.addView(linearLayoutItems);
        scrollView.addView(linearLayout);
        Button buttonRefresh = new Button(context);
        buttonRefresh.setText("Обновить список");
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildListAsync();
            }
        });
        linearLayout.addView(buttonRefresh);
        //buildListAsync();
        return scrollView;
    }
    @Override public void onResume() {
        super.onResume();
    }
    @Override public void onPause() {
        super.onPause();
    }
    private String getFragmentName(){
        return "Все команды";
    }
    private String getListDescription(){
        return "На этом экране собран быстрый доступ ко всем командам программы.\n" +
                "Также все эти команды доступны дистанционно. Их текст указывается мелким шрифтом.";
    }
    private void buildListAsync(){
        linearLayoutItems.removeAllViews();
        textViewDescription.setText("Загрузка, подождите...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    buildList();
                } catch (Exception e){
                    e.printStackTrace();
                    messageBox(log("Error buildListAsync: " + e.toString()));
                }
            }
        }).start();
    }
    private void buildList(){
        applicationManager.activity.showWaitingDialog();
        String helpText = applicationManager.getCommandsHelp();
        String[] commands = helpText.split("\n\n");
        sleep(100);
        for (int i = 0; i < commands.length; i++) {
            addCommandView(commands[i]);
            sleep(50);
        }
        while(linearLayoutItems.getChildCount() < commands.length)
            sleep(100);
        setDescription(getListDescription());
        applicationManager.activity.hideWaitingDialog();
    }
    private void setDescription(final String description){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(textViewDescription != null && description != null)
                    textViewDescription.setText(description);
            }
        });
    }
    private String log(String text){
        ApplicationManager.log(text);
        return text;
    }
    private void messageBox(final String text){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setPositiveButton("OK", null);
                builder.setMessage(text);
                builder.setTitle("Результат");
                builder.show();
            }
        });
    }
    private View getDelimiter(Context context){
        View delimiter = new View(context);
        delimiter.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
        delimiter.setBackgroundColor(Color.DKGRAY);
        return delimiter;
    }
    private void addCommandView(final String commandHelpText){
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(linearLayoutItems != null && commandHelpText != null && context != null)
                    linearLayoutItems.addView(new CommandView(context, commandHelpText));
            }
        });
    }
    private void sleep(int ms){
        try{
            Thread.sleep(ms);
        }
        catch (Exception e){}
    }

    private class CommandView extends LinearLayout{
        String commandHelpText = null;
        String commandText = null;
        ArrayList<SimpleEntry<String, String>> arguments = new ArrayList<>(); //pattern | description
        String name = null;
        String help = null;

        public CommandView(Context context, String commandHelpText) {
            super(context);
            this.commandHelpText = commandHelpText;
            //log("Received command: \"" + commandHelpText + "\"");
            setOrientation(VERTICAL);
            setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            addView(getDelimiter(context));
            parseCommand();
            if(name != null){
                TextView textView = new TextView(context);
                textView.setPadding(30, 30, 0, (help == null?30:0));
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(20);
                textView.setText(name);
                addView(textView);
            }
            if(help != null){
                TextView textView = new TextView(context);
                textView.setPadding(40, 0, 0, 30);
                textView.setTextColor(Color.GRAY);
                textView.setTextSize(13);
                textView.setText(help);
                addView(textView);
            }
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    click();
                }
            });
            addView(getDelimiter(context));
        }
        private void addHelp(String line){
            if(help == null)
                help = line;
            else
                help += "\n" + line;
        }
        private void parseCommand(){
            String[] lines = commandHelpText.split("\n");
            for(String line:lines){
                if(line.matches(".*\\[.+\\].*")){
                    line = line.replace("[", "").replace("]", "").trim();
                    //log("description: " + line);
                    if(name == null)
                        name = line;
                    else
                        addHelp(line);
                }
                else if(line.matches("\\-\\-\\-\\| .+")){
                    //log("command: " + line);
                    //---| botcmd setpsmessage <текст> <текст>
                    commandText = line.replace("---| ", "");
                    addHelp(commandText);
                    commandText = commandText.replace("botcmd ", "");
                    //log("command: " + commandText);
                    //botcmd setpsmessage <текст> <текст>
                    Pattern p = Pattern.compile("<([^>]+)>");
                    Matcher m = p.matcher(commandText);
                    int i=0;
                    while (m.find())
                    {
                        String argumentDescription = m.group(1);
                        String argumentPattern = "ARGUMENT"+i;
                        //botcmd setpsmessage ARGUMENT0 <текст>
                        commandText = commandText.replace("<"+argumentDescription+">", argumentPattern);
                        //log("argumentDescription: " + argumentDescription + "   argumentPattern: " +argumentPattern);
                        arguments.add(new SimpleEntry<>(argumentPattern, argumentDescription));
                        i++;
                        m = p.matcher(commandText);
                    }
                    log("parsed command: " + commandText);
                }
                //log("total arguments: " + arguments.size());
            }
        }
        private void click(){
            new ArgumentsDialog().show();
        }
        private class ArgumentsDialog{
            AlertDialog alertDialog = null;
            EditText[] editTexts = null;

            public ArgumentsDialog() {
                editTexts = new EditText[arguments.size()];
            }

            public void show(){
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(name);
                LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                ScrollView scrollView = new ScrollView(context);
                scrollView.addView(linearLayout);
                builder.setView(scrollView);
                {
                    if(help != null){
                        TextView textView = new TextView(context);
                        textView.setTextColor(Color.GRAY);
                        textView.setText(help);
                        textView.setTextSize(13);
                        textView.setPadding(5, 5, 5, 5);
                        linearLayout.addView(textView);
                    }
                    if(arguments.size() == 0){
                        TextView textView = new TextView(context);
                        textView.setTextColor(Color.WHITE);
                        textView.setText("Вы точно хотите выполнить команду?");
                        textView.setTextSize(22);
                        linearLayout.addView(textView);
                    }
                    for (int i = 0; i < arguments.size(); i++) {
                        EditText editText = editTexts[i] = new EditText(context);
                        editText.setHint(arguments.get(i).getValue());
                        linearLayout.addView(editText);
                    }
                    {
                        Button button = new Button(context);
                        button.setText("Выполнить");
                        button.setTextColor(Color.WHITE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Toast.makeText(context, "Выполнение команды...", Toast.LENGTH_SHORT).show();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String commandToRun = commandText;
                                            for (int i = 0; i < arguments.size(); i++) {
                                                commandToRun = commandToRun.replace(arguments.get(i).getKey(), editTexts[i].getText().toString());
                                            }
                                            log("Processing command: \"" + commandToRun + "\"");
                                            messageBox("Команда: \n" + commandToRun + "\nРезультат: \n" + applicationManager.processCommands(commandToRun, applicationManager.getUserID()));
                                        }
                                        catch (Exception e){
                                            e.printStackTrace();
                                            log("Error Выполнить: " + e.toString());
                                        }
                                    }
                                }).start();
                                if (alertDialog != null)
                                    alertDialog.cancel();
                            }
                        });
                        linearLayout.addView(button);
                    }
                    {
                        Button button = new Button(context);
                        button.setText("Отмена");
                        button.setTextColor(Color.WHITE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (alertDialog != null)
                                    alertDialog.cancel();
                            }
                        });
                        linearLayout.addView(button);
                    }
                }
                alertDialog = builder.show();
            }
        }
    }
}
