package com.fsoft.vktest.AnswerInfrastructure.Functions;

import com.fsoft.vktest.AnswerInfrastructure.BotModule;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;

import java.util.ArrayList;

/**
 * Базовый для всез функций в FunctionProcessor
 *
 * Правила простые:
 * - модуль получает сообщение В СЫРОМ виде. То есть, так, КАК ЕГО НАПИСАЛ ПОЛЬЗОВАТЕЛЬ. С обращением даже.
 * - Модуль возвращает вообщение в ГОТОВОМ для отправки виде. С меткой, обращением, и т.д.
 * После этого сообщение будет только лишь проверено на запрещенный контент, после чего отправлено.
 *
 * Как отвечать
 * - если модуль НЕ ХОЧЕТ ответить на это сообщение - возвращаем message с answer NULL
 * - если модуль считает что на это сообщение не нужно отвечать - возвращаем message с answer ""
 * - если модуль хочет ответить - возвращаем message с answer текст ответа готовый к отправке
 *
 * Зачем вовзращать Message? Чтобы модуль мог редактировать сообщение до других модулей.
 *
 * Edited by Dr. Failov on 14.08.2017.
 * Created by Dr. Failov on 12.02.2017.
 */
public class Function extends BotModule {
    //Стандартное ключевое слово именно для родительского класса. Служит для того, чтобы
    // проверить не переопределил ли модуль наше ключевое слово. Если переопределил - только
    // тогда предлагать ему команду
    private String defaultInvoker = "Ключевое слово в этом модуле не поддерживается.";
    private String invoker = defaultInvoker();

    public Function(ApplicationManager applicationManager) {
        super(applicationManager);
        invoker = getStorage().getString("invoker", invoker);
    }

    //Получить справку по командам этой функции. Если команд нет, вернуть "".
    @Override public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = super.getHelp();
        if (!defaultInvoker().equals(defaultInvoker)) {
            //отображать эту команду только если для этого модуля был задан кастомный инвокер
            result.add(new CommandDesc("Задать ключевое слово для модуля " + getName(),
                    "Сейчас этот модуль для вызова использует слово " + defaultInvoker() + ". " +
                            "Этой командой это слово можно поменять на любое другое.",
                    "botcmd module " + getName() + " SetInvoker <новое ключевое слово>"));
        }
        return result;
    }

    //Обработать команду. На вход поступает сразу фрагмент без botcmd, напимер: "wall add drfailov".
    @Override public String processCommand(Message messageOriginal) {
        CommandParser commandParser = new CommandParser(messageOriginal.getText());             //botcmd
        if (commandParser.getWord().toLowerCase().equals("module")) {                   //module
            if (!defaultInvoker().equals(defaultInvoker)) {
                if (commandParser.getWord().toLowerCase().equals(getName().toLowerCase())) {//dummy
                    if (commandParser.getWord().toLowerCase().equals("setinvoker")) {   //setinvoker
                        String newInv = commandParser.getText();                        //напишы
                        if (newInv.equals(""))
                            return "Не было получено новое ключевое слово для модуля " + getName() + "\n" +
                                    "Формат команды:\n" +
                                    F.commandDescToText(getHelp().get(0));
                        setInvoker(newInv);
                        return "Задано новое ключевое слово для модуля " + getName() + ": " + getInvoker();
                    }
                }
            }
        }
        return super.processCommand(messageOriginal);
    }

    @Override
    public String toString() {
        return super.toString() + " вызывается фразой " + getInvoker();
    }

    //в переопределенных методах переопределять метод и указывать стандартное обращение для модулей
    protected String defaultInvoker(){
        return defaultInvoker;
    }
    protected String getInvoker(){
        //кодовая фраза которая служит ключем для этого модуля. типа фраз "напиши" или "аббревиатура"
        return invoker;
    }
    private void setInvoker(String invoker){
        this.invoker = invoker;
        getStorage().put("invoker", invoker).commit();
    }
}
