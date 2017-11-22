package com.fsoft.vktest.ViewsLayer;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;
import com.fsoft.vktest.*;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Communication.Account.VK.VkAccountCore;
import com.fsoft.vktest.Utils.CommandParser;
import com.perm.kate.api.Audio;

import java.io.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Как должно выглядеть главное окно программы:
 * Три вкладки:
 * - Лог
 * - Сообщения
 * - Настройки
 *      - Аккаунты
 *          Список аккаунтов, от имени которых бот отвечает на сообщения.
 *      - Доверенные
 *          Список пользователей, которым бот разрешает исполнять команды (изменять свои настройки).
 *          Никогда не добавляй сюда никого, кому ты не довеяешь! Доверенные могут даже просматривать
 *          данные для входа в аккаунты!
 *      - Игнорируемые
 *          Список пользователей, которым бот не будет отвечать. Это могут быть буйные пользователи,
 *          которые тебя достали, либо пытались сломать бота.
 *      - Учителя
 *          Список пользователей, которые могут обучать бота с помощью команды "Начать обучение".
 *          Бот коллекционирует вопросы, на которые он не знает ответа и будет предлагать учителям на них ответить.
 *          Иногда бот может сам прислать сообщение с вопросом "Как ответить на ....?".
 *          Эти пользователи должны уметь пользоваться обучением и адекватно отвечать на сообщения.
 *          Если кто-то из них даст плохой ответ, бот потом будет плохо отвечать.
 *      - Команды
 *          В боте очень много настроек. Чтобы увидеть их все, открой экран "команды".
 *      - Состояние
 *          Подробное описание состояния основных модулей программы.
 *      - Обращения
 *          Ты можешь управлять, на какие имена будет отвечать бот. Например, бот может отвечать
 *          на "Бот, ...", или на "Вася, ...". И таких обращений может быть много.
 *      - Диалог с ботом
 *          Ты можешь пообщаться с ботом прямо здесь.
 *      - Обучение бота
 *          Бот коллекционирует вопросы, на которые он не знает ответа и будет предлагать тебе на них ответить.
 *      - О программе
 *          - Описание
 *          - Официальная группа
 *          - История изменений
 *          - Авторы
 *      - Закрыть программу
 *          Если просто закрыть программу кнопкой "назад", бот будет дальше работать в фоновом режиме.
 *          Если ты хочешь именно остановить бота, используй "закрыть программу".
 *
 * Created by Dr. Failov on 11.11.2014.
 * Изменено by Dr. Failov on 14.08.2017.
 */
public class MainActivity extends FragmentActivity implements Command {

    public ApplicationManager applicationManager = null;
    private ArrayList<Command> commands = new ArrayList<>();
    public ConsoleView consoleView = null;
    public MessagesListFragment.MessageList messageList = null;
    private MainActivity context = this;
    private boolean running = false;
    private SimplePagerAdapter.Tab messagesTab = null;
    private SimplePagerAdapter.Tab logTab = null;
    private SimplePagerAdapter.Tab settingsTab = null;
    private ViewPager viewPager = null;
    private SimplePagerAdapter simplePagerAdapter = null;
    private AlertDialog alertWaitingDialog = null;
    private int messageBoxes = 0;
    private Timer memoryStateRefreshTimer = null;
    private MediaPlayer mediaPlayer = null;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //CREATE PROTECTION
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                throwable.printStackTrace();
                if(throwable.toString().contains("OutOfMemoryError")) {
                    log("No memory, restart.");
                    applicationManager.processCommands("restart", applicationManager.getUserID());
                }
                log("Exception handled: " + throwable.toString());
            }
        });
        // prepare log
        consoleView = new ConsoleView(this);
        //connect
        if(isBotRunning())
            connectToRunningBot();
        else
            runBot();

    }
    @Override protected void onStart() {
        super.onStart();
        running = true;
        if(simplePagerAdapter == null) {
            log(". Загрузка страничного адаптера...");
            setContentView(R.layout.view_pager_layout);
            viewPager = (ViewPager) findViewById(R.id.pager);
            if(viewPager != null) {
                if(simplePagerAdapter == null) {
                    log(". Создание страничного адаптера...");
                    simplePagerAdapter = new SimplePagerAdapter(getSupportFragmentManager());
                }
                viewPager.setOffscreenPageLimit(10);
            }
            prepareTabs();
            viewPager.setAdapter(simplePagerAdapter);
        }
        if(commands.size() == 0) {
            commands.add(new Shell());
            commands.add(new GetLog());
            commands.add(new ShowMessage());
            commands.add(new PowerOff());
            commands.add(new Restart());
            commands.add(new Error());
            commands.add(new OutOfMemory());
            commands.add(new ScreenBrightness());
            commands.add(new PlayMusic());
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if(applicationManager != null && applicationManager.loaded)
            openMessagesTab();
        else
            openLogTab();
        if(memoryStateRefreshTimer == null){
            memoryStateRefreshTimer = new Timer();
            memoryStateRefreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    refreshMemoryState();
                }
            }, 10000, 10000);
        }
    }
    @Override protected void onPause() {
        if(memoryStateRefreshTimer != null) {
            memoryStateRefreshTimer.cancel();
            memoryStateRefreshTimer = null;
        }
        super.onPause();
    }
    @Override protected void onStop() {
        super.onStop();
        running = false;
    }
    @Override  protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override protected void onDestroy() {
        Log.d("BOT", "ON Destroy");
        consoleView = null;
        messageList = null;
//        if(applicationManager != null && applicationManager.running) {
//            //Этот флаг сейчас может быть true только в случае когда программа завершилась с ошибкой
//            applicationManager.close();
//            scheduleRestart();//запланируем перезапуск))))
//            sleep(1000);
//            android.os.Process.killProcess(Process.myPid());
//        }
        super.onDestroy();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK)
            return true;
        if(event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                if (getSelectedTab() == settingsTab)
                    openMessagesTab();
                else
                    openSettingsTab();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public String process(String text, Long senderId){
        String result = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).processCommand(text, senderId);
        return result;
    }
    @Override public String getHelp(){
        String result  = "";
        for (int i = 0; i < commands.size(); i++)
            result += commands.get(i).getHelp();
        return result;
    }
    private void prepareTabs(){
        log(". Заполнение вкладок...");
        simplePagerAdapter.tabs.clear();
        {
            String label1 = "Сообщения";
            messagesTab = simplePagerAdapter.newTab(new MessagesListFragment());
            messagesTab.setTitle(label1);
            simplePagerAdapter.addTab(messagesTab);
        }
        {
            String label2 = "Состояние";
            SimplePagerAdapter.Tab tab = settingsTab = simplePagerAdapter.newTab(new SettingsFragment());
            tab.setTitle(label2);
            simplePagerAdapter.addTab(tab);
        }
        {
            String label2 = "Аккаунты";
            SimplePagerAdapter.Tab tab = simplePagerAdapter.newTab(new AccountListFragment());
            tab.setTitle(label2);
            simplePagerAdapter.addTab(tab);
        }
        {
            String label1 = "Лог";
            if(logTab == null)
                logTab = simplePagerAdapter.newTab(new LogFragment());
            SimplePagerAdapter.Tab messagesTab = logTab;
            messagesTab.setTitle(label1);
            simplePagerAdapter.addTab(messagesTab);
        }
        {
            String label2 = "Команды";
            SimplePagerAdapter.Tab tab = simplePagerAdapter.newTab(new CommandsFragment());
            tab.setTitle(label2);
            simplePagerAdapter.addTab(tab);
        }
        {
            String label2 = "Стены";
            SimplePagerAdapter.Tab tab = simplePagerAdapter.newTab(new WallListFragment());
            tab.setTitle(label2);
            simplePagerAdapter.addTab(tab);
        }
        {
            String label2 = "Доверенные";
            SimplePagerAdapter.Tab tab = simplePagerAdapter.newTab(new AllowListFragment());
            tab.setTitle(label2);
            simplePagerAdapter.addTab(tab);
        }
        {
            String label2 = "Игнорируемые";
            SimplePagerAdapter.Tab tab = simplePagerAdapter.newTab(new IgnorListFragment());
            tab.setTitle(label2);
            simplePagerAdapter.addTab(tab);
        }
        {
            String label2 = "Учителя";
            SimplePagerAdapter.Tab tab = simplePagerAdapter.newTab(new TeacherListFragment());
            tab.setTitle(label2);
            simplePagerAdapter.addTab(tab);
        }
        simplePagerAdapter.notifyDataSetChanged();
        openLogTab();
    }
    public void runBot(){
        log("Запуск сервиса...");
        BotService.mainActivity = context;
        startService(new Intent(context, BotService.class));
    }
    public void connectToRunningBot(){
        applicationManager = BotService.applicationManager;
        applicationManager.activity = this;
        new Timer().schedule(new TimerTask() {
            //окно не сразу инициализируется, поэтому вывод текста производится с задержкой
            @Override public void run() {
                log("Подключено. Программа работает.");
            }
        }, 1000);
    }
    public boolean isBotRunning(){
        return BotService.applicationManager != null;
    }

    private void refreshMemoryState(){
        try {
            String name = "FP iHA bot™";
            long maxMemory = Runtime.getRuntime().maxMemory();
            long curMemory = Runtime.getRuntime().totalMemory();
            final String title = name + (applicationManager.isDonated()?"+ ":"") + " ram" + ((curMemory * 100L) / maxMemory) + "%";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setTitle(title);
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
            log("Ошибка обновления заголовка программы: " + e.toString());
        }
    }
    public void openMessagesTab(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(viewPager != null && simplePagerAdapter != null && messagesTab != null)
                        viewPager.setCurrentItem(simplePagerAdapter.getItemIndex(messagesTab), true);
                } catch (Exception e) {
                    e.printStackTrace();
                    log("Error openMessagesTab: " + e.toString());
                }
            }
        });
    }
    public void openSettingsTab(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(viewPager != null && simplePagerAdapter != null && settingsTab != null)
                    viewPager.setCurrentItem(simplePagerAdapter.getItemIndex(settingsTab), true);
            }
        });
    }
    public void openLogTab(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(viewPager != null && simplePagerAdapter != null && logTab != null)
                    viewPager.setCurrentItem(simplePagerAdapter.getItemIndex(logTab), true);
            }
        });
    }
    public SimplePagerAdapter.Tab getSelectedTab(){
        if(simplePagerAdapter != null && viewPager != null)
            return simplePagerAdapter.getTab(viewPager.getCurrentItem());
        return null;
    }
    public boolean isRunning(){
        return running;
    }
    public  void sleep(int ms){
        try {
            Thread.sleep(ms);
        }
        catch (Exception e){}
    }
    public void scheduleRestart(){
        Log.d("BOT", "Планирование перезапуска...");
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 60000, pendingIntent);
    }
    public String log(String text){
        if(consoleView != null)
            consoleView.log(text);
        return text;
    }
    public void messageBox(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (context.isRunning() && messageBoxes < 5) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            messageBoxes--;
                        }
                    });
                    builder.setMessage(text);
                    AlertDialog alertDialog = builder.show();
                    Timer timer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        AlertDialog alertDialog = null;

                        @Override
                        public void run() {
                            if (alertDialog != null) {
                                alertDialog.dismiss();
                                messageBoxes--;
                            }
                        }

                        @Override
                        public boolean equals(Object o) {
                            if (o.getClass() == AlertDialog.class)
                                alertDialog = (AlertDialog) o;
                            return super.equals(o);
                        }
                    };
                    timerTask.equals(alertDialog);
                    timer.schedule(timerTask, 60000);
                    messageBoxes++;
                }
            }
        });
    }
    public void messageBoxPermanent(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (context.isRunning()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Внимание!");
                    builder.setPositiveButton("OK", null);
                    builder.setMessage(text);
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setCancelable(false);
                    builder.show();
                }
            }
        });
    }
    public void showToast(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        });
    }
    public void turnoff(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                openLogTab();
                if(MainActivity.consoleView != null)
                    MainActivity.consoleView.setEnabled(true);
                sleep(500);
                applicationManager.close();
                sleep(500);
                stopService(new Intent(context, BotService.class));
                log("Приложение закроется СЕЙЧАС.");
                sleep(1000);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }).start();
    }
    public void restart(){
        scheduleRestart();
        turnoff();
    }
    public void showWaitingDialog(){
        showWaitingDialog("Операция выполняется...");
    }
    public void showWaitingDialog(String message){
        showWaitingDialog("Подождите...", message);
    }
    public void showWaitingDialog(final String title, final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (alertWaitingDialog != null) {
                    alertWaitingDialog.dismiss();
                    alertWaitingDialog = null;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setCancelable(false);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                alertWaitingDialog = builder.show();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (alertWaitingDialog != null)
                                    alertWaitingDialog.dismiss();
                                alertWaitingDialog = null;
                            }
                        });
                    }
                }, 20000);
            }
        });
    }
    public void setWaitingDialogMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (alertWaitingDialog != null)
                    alertWaitingDialog.setMessage(message);
            }
        });
    }
    public void hideWaitingDialog(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (alertWaitingDialog != null) {
                    alertWaitingDialog.dismiss();
                    alertWaitingDialog = null;
                }
            }
        });
    }
    public void setScreenBrightness(final float screenBrightness){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = screenBrightness;// 100 / 100.0f;
                getWindow().setAttributes(lp);
            }
        });
    }
    public void totalCrash(){
        throw new StackOverflowError("Intervention detected");
    }

    private class SimplePagerAdapter extends FragmentPagerAdapter{
        ArrayList <Tab> tabs = new ArrayList<>();

        @Override
        public Fragment getItem(int i) {
            return tabs.get(i).fragment;
        }

        public SimplePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public int getItemIndex(Tab tab) {
            return tabs.indexOf(tab);
        }

        public Tab newTab(Fragment fragment){
            return new Tab(fragment);
        }

        public Tab getTab(int i){
            return tabs.get(i);
        }

        public void addTab(Tab tab){
            tabs.add(tab);
        }

        @Override
        public int getCount() {
            return tabs.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tabs.get(position).title;
        }

        public class Tab {
            Fragment fragment = null;
            String title = "not defined";

            public Tab(Fragment fragment){
                this.fragment = fragment;
            }

            public void setTitle(String title){
                this.title = title;
            }
        }
    }
    private class GetLog implements Command{
        @Override
        public String getHelp() {
            return "[ Получить весь лог ]\n" +
                    "[ (Осторожно, может сработать система защиты!) ]\n" +
                    "---| botcmd getlog \n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("getlog")){
                return consoleView.log;
            }
            return "";
        }
    }
    private class ShowMessage implements Command{
        @Override
        public String getHelp() {
            return "[ Показать на экране устройства сообщение ]\n" +
                    "---| botcmd messagebox <текст сообщения> \n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("messagebox")){
                String text = commandParser.getText();
                messageBox(text);
                return "Сообщение "+text+" показано.\n";
            }
            return "";
        }
    }
    private class PowerOff implements Command{
        @Override
        public String getHelp() {
            return "[ Завершить работу программы ]\n" +
                    "---| botcmd turnoff \n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("turnoff") || word.equals("poweroff")) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        turnoff();
                    }
                }, 5000);
                return "Полное завершение программы через 5 секунд.\n";
            }
            return "";
        }
    }
    private class Restart implements Command{
        @Override
        public String getHelp() {
            return "[ Перезапустить программу ]\n" +
                    "[ Не использовать если программа свёрнута!!! ]\n" +
                    "---| botcmd restart \n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("restart") || word.equals("reboot")) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        restart();
                    }
                }, 5000);
                return "Перезапуск программы через 5 секунд. Повторная загрузка программы займет около 3 минут.";
            }
            return "";
        }
    }
    private class ScreenBrightness implements Command{
        @Override
        public String getHelp() {
            return "[ Задать яркость экрана ]\n" +
                    "[ Не использовать если программа свёрнута!!! ]\n" +
                    "---| botcmd setscreenbrightness <число от 0 до 100>\n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("setscreenbrightness")) {
                float b = ((float)commandParser.getInt())/100f;
                setScreenBrightness(b);
                return "Яркость изменена на "+b+".";
            }
            return "";
        }
    }
    private class PlayMusic implements Command{
        @Override
        public String getHelp() {
            return "[ Воспроизвести песню ]\n" +
                    "---| botcmd playmusic <ссылка на песню>\n\n"+
                    "[ Остановить песню (если играет) ]\n" +
                    "---| botcmd stopmusic\n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("stopmusic")){
                if(mediaPlayer != null)
                    mediaPlayer.stop();
                return "Музыка остановлена.";
            }
            else if(word.equals("playmusic")) {
                String result = log(". Вопроизведение песни....") + "\n";
                try {
                    String link = commandParser.getWord();
                    result += log(". Ссылка на песню: " + link) + "\n";
                    link = link.replace("https://", "");
                    link = link.replace("http://", "");
                    link = link.replace("m.vk.com/", "");
                    link = link.replace("vk.com/", "");
                    link = link.replace("audio", "");
                    result += log(". ID записи: " + link) + "\n";
                    VkAccountCore vkAccount = applicationManager.vkAccounts.getActive();
                    result += log(". Получен аккаунт: " + vkAccount.userName) + "\n";
                    ArrayList<Audio> audios = vkAccount.api().getAudioById(link, null, null);
                    if(audios.size() <= 0)
                        result += log("! Ошибка! Не получено ни одной аудиозаписи!") + "\n";
                    else {
                        Audio audio = audios.get(0);
                        result += log(". Получена аудиозапись: " + audio.artist + " - " + audio.title) + "\n";
                        String mp3Link = audio.url;
                        result += log(". Ссылка на аудиофайл: " + mp3Link) + "\n";
                        result += log(". Загрузка файла...") + "\n";
                        File downloaded = applicationManager.vkCommunicator.downloadDocument(mp3Link);
                        if(downloaded == null)
                            result += log("! Файл не скачался.") + "\n";
                        else {
                            result += log(". Файл загружен: " + downloaded.getPath()) + "\n";
                            result += log(". Попытка воспроизвести файл...") + "\n";
                            String filePath = downloaded.getPath();
                            if (mediaPlayer != null)
                                mediaPlayer.stop();
                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setDataSource(filePath);
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            result += log(". Воспроизведение запущено.") + "\n";
                        }
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                    result += log("! Ошибка: " + e.toString()) + "\n";
                }
                return result;
            }
            return "";
        }
    }
    private class Error implements Command{
        @Override
        public String getHelp() {
            return "[ Вызвать ошибку в программе]\n" +
                    "[ используется разработчиком для отладки ]\n" +
                    "---| botcmd error \n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("error")) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //переполнение стека гарантировано
                        run();
                    }
                }, 5000);
                return "Вылет программы из-за переполнения стека через 5 секунд.";
            }
            return "";
        }
    }
    private class OutOfMemory implements Command{
        @Override
        public String getHelp() {
            return "[ Вызвать переполнение памяти в программе ]\n" +
                    "[ используется разработчиком для отладки ]\n" +
                    "---| botcmd outofmemory \n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("outofmemory")) {
                new Timer().schedule(new TimerTask() {
                    ArrayList<Bitmap> test = new ArrayList<>();
                    @Override
                    public void run() {
                        int filled = 0;
                        while (true){
                            filled += 3;
                            log("Занято "+filled+" MB");
                            for (int i = 0; i < 3; i++) {
                                test.add(Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888));
                            }
                            sleep(500);
                        }
                    }
                }, 5000);
                return "Вылет программы из-за переполнения памяти через 5 секунд.";
            }
            return "";
        }
    }
    private class Shell implements Command{
        Process process = null;
        BufferedOutputStream STDIN = null;
        BufferedInputStream STDOUT = null;

        @Override
        public String getHelp() {
            return "[ Выполнить ROOT команду в системе ]\n" +
                    "[ Очевидно, требуется ROOT в системе ]\n" +
                    "[ Может понадобиться Ваше подтверждение ]\n" +
                    "---| botcmd shell <команда> \n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("shell")) {
                String command = commandParser.getText();
                String result = "";
                try {
                    //if(processCommand == null)
                    {
                        result += log(". Get Process...\n");
                        process = Runtime.getRuntime().exec("su");
                        result += log(". Get DataOutputStream...\n");
                        STDIN = new BufferedOutputStream(new DataOutputStream(process.getOutputStream()));
                        result += log(". Get DataInputStream...\n");
                        STDOUT = new BufferedInputStream(new DataInputStream(process.getInputStream()));
                    }
                    result += log(". run command: " + command + "\n");
                    STDIN.write((command + "\n").getBytes("UTF-8"));
                    STDIN.flush();
                    Thread.sleep(1000);
                    result += log(". reading answer: \n");
                    result += log("---------------------------------------------------------\n");
                    while(STDOUT.available() > 0)
                        result += (char)STDOUT.read();
                    result += "\n";
                    result += log("---------------------------------------------------------\n");
                }
                catch (Exception e){
                    result += "Error: " + e.toString();
                    e.printStackTrace();
                }
                return result;
            }
            return "";
        }
    }
}
