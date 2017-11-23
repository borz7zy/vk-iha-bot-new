package com.fsoft.vktest.Modules.Commands;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.CommandParser;

import java.util.ArrayList;

/**
 * эта команда требуется для преобразования текста в коды для отладки
 * Created by Dr. Failov on 15.02.2017.
 */
public class Decode extends CommandModule{
    public Decode(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public String processCommand(Message message) {
        CommandParser commandParser = new CommandParser(message.getText());
        if(commandParser.getWord().equals("decode")) {
            String inp = commandParser.getText().replace(" ", "");
            String symbols = "";
            String codes = "";
            for (int i = 0; i < inp.length(); i++) {
                char cc = inp.charAt(i);
                int c = (int)cc;
                symbols += cc;
                codes += c;
                if(i < inp.length()-1) {
                    symbols += ",";
                    codes += ",";
                }
            }
            return "Текст: |" + inp + "|\n" +
                    "Текст.lenght(): "+inp.length()+"\n"+
                    "Символы: |" + symbols + "|\n" +
                    "Коды: |" + codes + "| \n";
        }
        return "";
    }


    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<>();
        result.add(new CommandDesc(
                "Преобразовать текст в набор кодов",
                "Эта команда покажет из каких символов состоит текст.\n" +
                        "Это очень полезная команда, если Вы занимаетесь экспериментами со" +
                        " спецсимволами и т.д.\n" +
                        "Эта команда для продвинутых пользователей.",
                "botcmd decode <текст для преобразования>"));
        return result;
    }
}
