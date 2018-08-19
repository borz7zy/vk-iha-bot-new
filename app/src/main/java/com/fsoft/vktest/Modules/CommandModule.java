package com.fsoft.vktest.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Modules.Commands.CommandDesc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * этот класс будет собирать в себе весь общий функционал необходимый для работы модулей внутри самой программы
 * Created by Dr. Failov on 12.02.2017.
 */
public class CommandModule implements Command {
    protected ApplicationManager applicationManager = null;
    protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    protected ArrayList<CommandModule> childCommands = new ArrayList<>();



    protected CommandModule() {
        //вызывая этот конструктор обязательно задать applicationManager
    }
    public CommandModule(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }
    @Override
    public String processCommand(Message message) {
            String result = "";
            for (CommandModule child : childCommands) {
                try {
                    result += child.processCommand(message);
                }
                catch (Exception e){
                    e.printStackTrace();
                    return log(
                            "! Ошибка обрабоки команды дочерним модулем:\n"+
                            "Модуль: "+this.getClass().getName()+"\n" +
                            "Ошибка: " + e.toString()
                    );
                }
            }
            return result;

    }
    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result=new ArrayList<>();
        for (CommandModule child : childCommands)
            result.addAll(child.getHelp());
        return result;
    }
    public void stop() {
        //при закрытии программы останавливает процессы
        //НЕ ИСПОЛЬЗОВАТЬ ДЛЯ СОХРАНЕНИЯ!!!!!!!!!!!!!!!
        for (CommandModule child : childCommands)
            child.stop();

    }

    public String log(String text){
        if(applicationManager != null)
            return applicationManager.log(text);
        return text;
    }
    public String messageBox(String text){
        //if(applicationManager != null)
        log("! Can\'t show messageBox because it still not implemented");
        new Exception("\"! Can\\'t show messageBox because it still not implemented\"").printStackTrace();
            //// TODO: 13.12.2017 apply it when will be ready
            /// applicationManager.messageBox(text);
        return text;
    }
    public ApplicationManager getApplicationManager() {
        return applicationManager;
    }
}
