package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.FileStorage;

import java.util.ArrayList;

/**
 * От этого ебаного класса должно наследоваться всё, что может принимать решения про ответ на сообщение
 *
 * Правила простые:
 * - модуль получает сообщение В СЫРОМ виде. То есть, так, КАК ЕГО НАПИСАЛ ПОЛЬЗОВАТЕЛЬ. С обращением даже.
 * - Модуль возвращает вообщение в ГОТОВОМ для отправки виде. С меткой, обращением, и т.д.
 *
 * В ОБЯЗАННОСТИ модуля входит:
 * - проверить наличие метки функцией
 *      boolean hasTreatment(Message message)     +
 * - удалить метку из входного сообщения для анализа сообщения без метки командой
 *      String remTreatment(Message message)      +
 * - подобрать ответ и поместить его в объект сообщения командой
 *      message.setAnswer(new Answer(...))        +
 * - добавить к сообщению обращение командой:
 *      Message addTreatmentToAnswer(Message message) +
 *      (если обращение отключено, команда ничего не сделает)
 * - добавить к сообщению метку командой:
 *      Message addMarkToAnswer(Message message)
 *      (если метка отключена, команда ничего не сделает)
 * - провести фильтрацию ответа функцией
 *      Message filter(Message)
 *
 * Как отвечать
 * - если модуль НЕ ХОЧЕТ ответить на это сообщение - возвращаем message с answer NULL
 * - если модуль считает что на это сообщение не нужно отвечать - возвращаем message с answer ""
 * - если модуль хочет ответить - возвращаем message с answer текст ответа готовый к отправке
 *
 * Зачем вовзращать Message? Чтобы модуль мог редактировать сообщение до других модулей.
 * Created by Dr. Failov on 12.02.2017.
 */

public class BotModule extends CommandModule {
    private ArrayList<BotModule> childModules = new ArrayList<>();
    private boolean enabled = true;
    private FileStorage storage = null;

    public BotModule(ApplicationManager applicationManager) {
        super(applicationManager);
        storage = new FileStorage(getName()+"_settings", applicationManager);
        enabled = getStorage().getBoolean("enabled", enabled);
    }


    //Обработать входящее сообщение. Сюда поступают не только сообщения на которые этот модуль может
    // дать ответ, а все. Поэтому если этот модуль не должен отвечать на него, вернуть null.
    //Если ответ есть - писать сразу ответ. С большой буквы вместе с обращением и меткой и т.д.
    //все проверки необходимо реализовывать на стороне модуля
    public Message processMessage(Message message){
        for(BotModule botModule:childModules) {
            if(botModule.isEnabled()) {
                message = botModule.processMessage(message);
                log(botModule.getName() + message + " = " + message.answer);
                if (message.answer != null)
                    return message;
            }
        }
        return message;
    }
    //ключавое слово модуля - как обращаться к нему командами
    // например bcd module dummy disable
    public String getName(){
        return "BaseModule";
    }
    //описание модуля (Модуль отвечает на "который час?")
    public String getDescription(){
        return "Базовый модуль, не выполняющий никаких функций.";
    }
    @Override
    public String processCommand(Message message) {
        if(getName().equals("BaseModule")) //если имя не было переопределено, значит этот модуль нельзя ни включать ни выключать
            return super.processCommand(message);
        CommandParser commandParser = new CommandParser(message.getText());
        if(commandParser.getWord().toLowerCase().equals("module")) {                        //module
            if (commandParser.getWord().toLowerCase().equals(getName().toLowerCase())) {    //dummy
                if(commandParser.getWord().toLowerCase().equals("active")) {                //active
                    boolean active = commandParser.getBoolean();                            //enable
                    if(isEnabled() && active)
                        return "Модуль " + getName() + " уже был включён.\n" + getDescription();
                    if(!isEnabled() && !active)
                        return "Модуль " + getName() + " уже был выключён.\n" + getDescription();
                    setEnabled(active);
                    if(enabled)
                        return "Модуль " + getName() + " включён. Теперь он будет отаечать на сообщения.\n" + getDescription();
                    else
                        return "Модуль " + getName() + " выключён. Теперь он не будет отаечать на сообщения.\n" + getDescription();
                }
            }
        }
        return super.processCommand(message);
    }
    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = super.getHelp();
        if(!getName().equals("BaseModule")) { //если имя не было переопределено, значит этот модуль нельзя ни включать ни выключать
            result.add(new CommandDesc("Включить или выключить модуль " + getName(),
                    "Если модуль отключить, модуль перестанет отвечать.\n" +
                            "Включить его снова можно такой же командой.\n" +
                            getDescription(),
                    "botcmd module active <on//off>"));
        }
        return result;
    }
    @Override
    public String toString() {
        return isEnabled()?"":"(выключен) " + getName() + ", " + getDescription();
    }

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled){
        this.enabled = enabled;
        getStorage().put("enabled", enabled).commit();
    }

    protected void addChildModule(BotModule botModule){
        childModules.add(botModule);
        childCommands.add(botModule);
    }
    public ArrayList<BotModule> getChildModules() {
        return childModules;
    }
    public BotModule getChildModule(String name) {
        for (BotModule botModule:getChildModules())
            if(botModule.getName().toLowerCase().equals(name))
                return botModule;
        return null;
    }

    protected FileStorage getStorage(){
        return storage;
    }
    protected boolean hasTreatment(Message message){
        if(applicationManager == null)
            return false;
        BotBrain brain = applicationManager.getBrain();
        if(brain == null)
            return false;
        return brain.hasTreatment(message);
    }
    protected Message remTreatment(Message message){
        if(applicationManager == null)
            return null;
        BotBrain brain = applicationManager.getBrain();
        if(brain == null)
            return null;
        return brain.remTreatment(message);
    }
    private Message addTreatmentToAnswer(Message message){
        if(applicationManager == null)
            return null;
        BotBrain brain = applicationManager.getBrain();
        if(brain == null)
            return null;
        return brain.addTreatmentToAnswer(message);
    }
    protected Message prepare(Message message){
        message = addTreatmentToAnswer(message);
        return message;
    }
}
