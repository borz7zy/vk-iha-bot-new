package com.fsoft.vktest.Modules.Commands;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Dr. Failov on 15.02.2017.
 */
public class ClearCache extends CommandModule{
    public ClearCache(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public String processCommand(Message message){
        if(message.getText().equals("clearcache")) {
            String result = "Очистка кэша...\n";
            try {
                File root = new File(applicationManager.getHomeFolder());
                result += log(". Загрузка обьектов...") + "\n";
                File[] files = root.listFiles();
                for (File file : files)
                    if (file.getName().matches("download_.+"))
                        result += log(". Удаление файла " + file.getName()) + " : " + file.delete() + " \n";
                result += log(". Готово.") + "\n";
            } catch (Exception e) {
                e.printStackTrace();
                result += log(". Ошибка: " + e.toString()) + "\n";
            }
            return result;
        }
        return "";
    }

    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<>();
        result.add(new CommandDesc(
                "Очистить папку загрузок бота",
                "Когда бот что-либо скачивает, в его папке сохраняется файл с именем " +
                        "download_(дата). Обычно такие файлы после того как были скачаны и " +
                        "использованы больше не нужны. Но бот их не удаляет - на всякий случай. " +
                        "Чтобы эти файлы не занимали память, их можно этой командой удалить.",
                "botcmd clearcache"));
        return result;
    }
}
