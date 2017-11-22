package com.fsoft.vktest.Modules.Commands;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.CommandParser;

import java.util.ArrayList;

/**
 * Created by Dr. Failov on 15.02.2017.
 */
public class Version extends CommandModule {
    public Version(ApplicationManager applicationManager) {
        super(applicationManager);
    }


    @Override
    public String processCommand(Message message) {
        CommandParser commandParser = new CommandParser(message.getText());
        if(commandParser.getWord().equals("version")) {
            return "Версия бота: " + applicationManager.getVisibleName() + "\n";
        }
        return "";
    }


    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<>();
        result.add(new CommandDesc(
                "Получить версию программы",
                "В зависимости от того, какая у тебя версия, могут отличаться некоторые команды, " +
                        "могут быть или отсутствовать какие-то функции, или содержаться разные " +
                        "ошибки.\n" +
                        "Я стараюсь с каждой новой версией исправлять ошибки и не допускать новых, но " +
                        "это не всегда получается.",
                "botcmd version"));
        return result;
    }
}
