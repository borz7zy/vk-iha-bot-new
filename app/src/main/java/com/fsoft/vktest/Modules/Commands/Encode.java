package com.fsoft.vktest.Modules.Commands;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.CommandParser;

import java.util.ArrayList;

/**
 *
 * Created by Dr. Failov on 15.02.2017.
 */
public class Encode extends CommandModule{
    public Encode(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public String processCommand(Message message) {
        CommandParser commandParser = new CommandParser(message.getText());
        if(commandParser.getWord().equals("encode")) {
            String codes = commandParser.getText().replace(" ", "").replace("\n", "");
            String[] parts = codes.split(",");
            String symbols = "";
            String text = "";
            for (int i = 0; i < parts.length; i++) {
                int c = Integer.parseInt(parts[i]);
                char cc = (char)c;
                text += cc;
                symbols += cc;
                if(i < parts.length-1)
                    symbols += ",";
            }
            return "Коды: |" + codes + "|\n" +
                    "Символы: |" + symbols + "|\n" +
                    "Текст: |" + text + "| \n";
        }
        return "";
    }


    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<>();
        result.add(new CommandDesc(
                "Преобразовать коды символов в символы",
                "Иногда при отладке и экспериментах с программой может быть полезно увидеть как " +
                        "выглядит последоваткльность кодов символов в программе. \n" +
                        "Эта команда покажет, как выгдялит каждый символ по отдельности и весь текст вместе.\n" +
                        "Особенно полезной эта команда будет при изучении разных спецсимволов.\n" +
                        "Эта команда для опытных пользователей.",
                "botcmd encode <числа разделенные запятыми: 56,495,8459,278,...>"));
        return result;
    }
}
