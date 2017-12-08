package com.fsoft.vktest.Modules;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.FileStorage;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Dr. Failov on 12.02.2017.
 */
public class Autoreboot extends CommandModule {
    private FileStorage fileStorage;
    private boolean enabled = false;
    private long interval = 172800000; //2 days
    private Timer timer = null;

    public Autoreboot(ApplicationManager applicationManager) {
        super(applicationManager);
        fileStorage = new FileStorage("autoreboot_config", applicationManager);
        setInterval(fileStorage.getLong("interval", interval));
        setEnabled(fileStorage.getBoolean("enabled", enabled));
    }
    @Override public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<>();
        result.add(new CommandDesc("Включить или выключить автоперезагрузку",
                "Если твой бот работает в полностью автономном режиме " +
                        "(телефон лежит на полочке днями и ты его не трогаешь), " +
                        "полезным может оказаться периодически этот телефон перезагружать, " +
                        "поскольку работая под большой нагрузкой (да, бот - это большая нагрузка) " +
                        "в системе могут возникать ошибки, которые накапливаясь могут замедлять " +
                        "телефон, или вообще привести к его полному зависанию.\n" +
                        "Для того чтобы тебе постоянно не заниматься этим вручную, " +
                        "предусмотрена функция автоматической перезагрузки.\n" +
                        "Бот будет периодически сам перезагружать твой телефон.\n" +
                        "Для работы автоперезагрузки требуется ROOT, потому что без него " +
                        "программы вообще не имеют права перезагружать телефон.",
                "botcmd autoreboot active <on/off>"));



        result.add(new CommandDesc("Задать интервал автоперезагрузки",
                "Интервал автоперезагрузки это время в миллисекундах как часто бот " +
                        "будет перезагружать твой телефон. Я обычно ставлю у себя интевал 2-3 дня.\n" +
                        "Стандартное значение: 2 дня.\n" +
                        "Для работы автоперезагрузки требуется ROOT, потому что без него " +
                        "программы вообще не имеют права перезагружать телефон.",
                "botcmd autoreboot setinterval <новый интервал в миллисекундах>"));



        result.add(new CommandDesc("Получить интервал автоперезагрузки",
                "Интервал автоперезагрузки это время в миллисекундах как часто бот " +
                        "будет перезагружать твой телефон.\n" +
                        "Стандартное значение: 2 дня.\n" +
                        "Также сработает команда botcmd autoreboot status.\n" +
                        "Для работы автоперезагрузки требуется ROOT, потому что без него " +
                        "программы вообще не имеют права перезагружать телефон.",
                "botcmd autoreboot getinterval"));



        result.add(new CommandDesc("Перезагрузить устройство сейчас",
                "Вызвать команду перезагрузки телефона сейчас.\n" +
                        "Для работы перезагрузки требуется ROOT, потому что без него " +
                        "программы вообще не имеют права перезагружать телефон.",
                "botcmd autoreboot reboot"));



        return result;
    }
    @Override public String processCommand(Message message) {
        if(message.getText().equals("status") || message.getText().equals("autoreboot status")){
            return "Интервал автоперезагрузки: " + interval + " мс\n"
                    + "Автоперезагрузка включена: " + enabled + "\n"
                    + "Автоперезагрузка запланирована: " + (timer != null);
        }
        if(message.getText().equals("save")){;
            save();
            return "Настройки автоперезагрузки сохранены.\n";
        }
        CommandParser commandParser = new CommandParser(message.getText());
        if(commandParser.getWord().equals("autoreboot")){
            String word = commandParser.getWord();
            if(word.equals("setinterval"))
                return "Задан новый интервал автоперезагрузки: " + (setInterval(commandParser.getLong()));
            if(word.equals("getinterval"))
                return "Интервал автоперезагрузки: " + interval + " мс\n"
                        + "Автоперезагрузка включена: " + enabled + "\n"
                        + "Автоперезагрузка запланирована: " + (timer != null);
            if(word.equals("active")) {
                boolean value = commandParser.getBoolean();
                setEnabled(value);
                if(value)
                    return "Автоматическая перезагрузка телефона включена. " +
                            "Сейчас её интервал составляет " + interval + " миллисекунд " +
                            "(это в 1000 раз меньше секунды).\n" +
                            "Твой телефон будет периодически автоматически перезагружаться.\n\n" +
                            "Вот тебе пару советов:\n" +
                            "1) Проверь, есть ли у тебя ROOT доступ. " +
                            "Без него автоматическая перезагрузка работать не сможет.\n" +
                            "2) Проверь, не забыл ли ты включить автоматический " +
                            "запуск бота при включении телефона? Если его не включить " +
                            "бот после перезагрузки не запустится.\n" +
                            "3) Также советую поставить интервал отключения экрана в настройках " +
                            "телефона на максимальное значение (10 минут или больше). " +
                            "Иначе экран телефона может потухнуть быстрее чем запустится бот.\n" +
                            "4) Проверь настройки экрана блокировки. В идеале экран блокировки " +
                            "лучше совсем отключить (выбери в списке вариант \"Нет\"), " +
                            "потому, что после перезагрузки телефон обычно висит на локскрине " +
                            "и не позволяет боту запуститься.";
                return "Автоматическая перезагрузка телефона отключена.";
            }
            if(word.equals("reboot")) {
                reboot();
                return "Перезагрузка через 5 сек...";
            }
        }
        return "";
    }
    @Override public void stop() {
        stopTimer();
        super.stop();
    }
    public boolean setEnabled(boolean en){
        if(!enabled && en)
            createTimer();
        else if(enabled && !en)
            stopTimer();
        enabled = en;
        save();
        return en;
    }
    public long setInterval(long in){
        if(in < 120000)
            in = 120000;
        if(enabled){
            if(interval != in){
                interval = in;
                createTimer();
            }
        }
        interval = in;
        save();
        return in;
    }
    public void reboot(){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                log(". Rebooting...");
                //// TODO: 08.12.2017    applicationManager.("shell reboot");
            }
        }, 5000);
    }


    private void save(){
        fileStorage.put("enabled", enabled);
        fileStorage.put("interval", interval);
        fileStorage.commit();
    }
    private void stopTimer(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }
    private void createTimer(){
        requestRoot();
        stopTimer();
        timer = new Timer("autoreboot_timer");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                reboot();
            }
        }, interval, interval);
    }
    public void requestRoot(){
//        applicationManager.handler.post(new Runnable() {
//            @Override
//            public void run() {
//                AlertDialog.Builder builder = new AlertDialog.Builder(applicationManager.getContext());
//                builder.setTitle("Внимание!");
//                builder.setMessage("Через секунду я попробую проверить есть ли у тебя ROOT доступ " +
//                        "на телефоне. Если он есть, у тебя должно отобразиться сообщение с вопросом. " +
//                        "В этом сообщении подтверди доступ и поставь галочку чтобы доступ " +
//                        "автоматически подтверждался всегда. Это нужно для того, чтобы я мог самостоятельно " +
//                        "перезагружать телефон, не отвлекая тебя.\n" +
//                        "(!!!) Если никакого сообщения не появится, значит, скорее всего, у тебя на телефоне " +
//                        "нету рута и автоматическая перезагрузка работать не будет.");
//                builder.setPositiveButton("Понял", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        new Timer().schedule(new TimerTask() {
//                            @Override
//                            public void run() {
//                                log(". Testing root access...");
//                                applicationManager.processCommand("shell ls /");
//                            }
//                        }, 1000);
//                    }
//                });
//            }
//        });
//
//

    }
}
