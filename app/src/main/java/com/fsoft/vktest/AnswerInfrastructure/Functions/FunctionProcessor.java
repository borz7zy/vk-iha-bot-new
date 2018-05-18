package com.fsoft.vktest.AnswerInfrastructure.Functions;

import com.fsoft.vktest.AnswerInfrastructure.BotModule;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.AngryClock;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.BanMe;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Binary;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.BrickLanguage;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Cities;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.IliIli;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Infa;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Learning;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Rendom;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.SmileAnswerer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Time;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Translit;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.When;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.WhichDayOfMonth;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.WhichDayOfWeek;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import java.util.*;

/*
 * отвечает на конкретные вопросы
 * Created by Dr. Failov on 02.10.2014.
 */
/**
 * отвечает на конкретные вопросы
 * Edited by Dr. Failov on 16.08.2017.
 */
public class FunctionProcessor extends BotModule {
    //// TODO: 11.03.2017 Как насчёт написать функцию которая будет генерировать изображение - сертификат пидора.
    //// TODO: 11.03.2017 всякая хуйня с чатами. Рулетки и прочая хуйня.

    public FunctionProcessor(ApplicationManager applicationManager) throws Exception {
        super(applicationManager);
        addChildModule(new Binary(applicationManager));
        addChildModule(new BrickLanguage(applicationManager));
        addChildModule(new Cities(applicationManager));
        addChildModule(new IliIli(applicationManager));
        addChildModule(new Infa(applicationManager));
        addChildModule(new WhichDayOfWeek(applicationManager));
        addChildModule(new Rendom(applicationManager));
        addChildModule(new When(applicationManager));
        addChildModule(new Time(applicationManager));
        addChildModule(new AngryClock(applicationManager));
        addChildModule(new BanMe(applicationManager));
        addChildModule(new WhichDayOfMonth(applicationManager));
        addChildModule(new SmileAnswerer(applicationManager));
        addChildModule(new Translit(applicationManager));


        childCommands.add(new GetModules(applicationManager));
    }


    private class GetModules extends CommandModule{
        GetModules(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Вывести список модулей бота",
                    "Текстовые модули отвечают на некоторые сообщения программно-генерируемыми ответами " +
                            "и могут содержать некоторую полезную информацию. Все модули можно включать и выключать, " +
                            "некоторые модули можно настраивать под себя.",
                    "botcmd modules"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().equals("modules")) {
                String result = "Текстовые модули бота: \n";
                for (BotModule function:getChildModules())
                    result += function.toString() +"\n\n";
                return result;
            }
            return "";
        }
    }
}
