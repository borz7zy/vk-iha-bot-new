package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.AnswerDatabase;
import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.UnknownMessagesDatabase;
import com.fsoft.vktest.AnswerInfrastructure.Functions.FunctionProcessor;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Learning;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Communication.HttpServer;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.Parameters;
import com.fsoft.vktest.Utils.TimeCounter;
import com.fsoft.vktest.Utils.User;
import com.fsoft.vktest.Utils.UserList;

import java.text.SimpleDateFormat;
import java.util.*;

/*
 * новый (05.08.2014) модуль интеллекта
 * Created by Dr. Failov on 05.08.2014.
 */
/*
 * Итак, логика такая: поступают на обработку сообщения с обращением.
 * Кто занимается отбором сообщенией с обращением?
 * Никто. BotBrain передаёт все сообщения модулям. Вне зависимости от того есть так обращение или нет.
 *
 * А с командами?
 * BotBrain проверяет наличие команды, если она там есть, он удаляет из неё botcmd и передаёт
 * в обработку ApplicationManager`y.
 *
 *
 * Сообщение получает Account
 * ВСЕ сообщения отправляются на обработку на форму и сразу в BotBrain
 * каждый модуль сам решает отвечать или нет
 * если после всего ответ был получен - BotBrain должен отправить на activity сообщение о том что ответ есть
 * если ответа нет, BotBrain отправляет на activity сигнал что ответа нет (если activity null значит программа работает в фоне)
 *
 *
 *
 * Ответ подбирает AnswerDatabase. На вход он получает полный текст сообщения "бот, привет"
 * Обращение модуль сам должен убрать при помощи функции в BotBrain
 *
 *
 *
 *
 * На каждый модуль. На каждый модуль поступают вообщения в их начальном виде
 * Чтобы удалить обращение, обращаемся к функциям BotBrain который убирает обращение
 * Он же проверяет факт наличия обращения
 *
 *
 * BotBrain должен:
 * - проверять наличие обращения
 * - удалять обращение
 * - проверять наличие команд
 * - удалять ключевое слово botcmd
 * - содержать команды связанные с обращением
 * - добавлять метку
 * - отправлять сообщение используя встроенные функции Message
 * Изменено by Dr. Failov on 05.08.2014.
 */
public class BotBrain extends CommandModule {
    private AnswerDatabase answerDatabase;
    private UnknownMessagesDatabase unknownMessages;
    private PatternProcessor patternProcessor;
    private FunctionProcessor functionAnswerer;
    private String[] botTreatments = {"бот,"};
    private FileStorage fileStorage = null;
    private ArrayList<Long> answeredAboutOwnerOnly = new ArrayList<>();
    private UserList allow = null;
    private UserList ignor = null;
    private Learning learning = null;
    private Filter filter = null;

    public BotBrain(ApplicationManager applicationManager) throws Exception {
        super(applicationManager);
        fileStorage = new FileStorage("iHA_Brain", applicationManager);
        answerDatabase = new AnswerDatabase(applicationManager);
        learning = new Learning(applicationManager);
        unknownMessages = new UnknownMessagesDatabase(applicationManager);
        patternProcessor = new PatternProcessor(applicationManager);
        functionAnswerer = new FunctionProcessor(applicationManager);
        filter = new Filter(applicationManager);
        botTreatments = fileStorage.getStringArray("botTreatments", botTreatments);

        ignor = new UserList("ignor",
                "Список игнорируемых пользователей",
                "Список пользователей, которым бот не будет отвечать на сообщения.\n" +
                        "В игнор пользователя можно добавить вручную, также нарушители правил бота попадают в игнор автоматически.",
                //// TODO: 27.09.2017 дополнить комментариями когда будет понятно что к чему
                applicationManager);
        allow = new UserList("allow",
                "Список доверенных пользователей",
                "Список пользователей, которые имеют право давать боту команды.\n" +
                        "Команды позволяют настраивать бота, редактировать базы, получать служебную информацию...\n" +
                        "Список всех команд можно увидеть по команде botcmd help, или на экране \"Команды\".\n" +
                        "Команды боту можно писать там же, где обычные сообщения.\n" +
                        "Все команды начинаются со слова botcmd.\n" +
                        "Для всех остальных пользователей (не доверенных) при попытке отправить боту команду будет выдана ошибка.",
                applicationManager);
        allow.addHardcodeDefined(new User().vk(10299185L));
        allow.addHardcodeDefined(new User().tg(248067313L));

        childCommands.add(answerDatabase);
        childCommands.add(learning);
        childCommands.add(unknownMessages);
        childCommands.add(patternProcessor);
        childCommands.add(functionAnswerer);
        childCommands.add(filter);
        childCommands.add(ignor);
        childCommands.add(allow);
        childCommands.add(new Help(applicationManager));
    }
    public Message processMessage(Message message){
        //ВСЕ ССЫЛКИ ВЕДУТ СЮДА. ВСЕ ЗАЩИТЫ РЕАЛИЗОВЫВАТЬ ЗДЕСЬ.
        //// TODO: 18.03.2017 пропускать сообщения с меткой другого бота
        // TODO: 14.08.2017 вызывать отсюда отрисовку на экране сообщения
        //// TODO: 18.03.2017 вызывать отправку сообщения (onAnswerReady) даже если ничего не нужно отправлять. Для этого исползовать ""
        //эта функция вызывается непосредственно модулями от которых послупают сообщения.
        //сделовательно, отправлять сообщение надо здесь же

        try {
            //команда?
            if(hasCommandMark(message) && allow.has(message.getAuthor())){
                log(". В сообщении обнаружена команда: " + message.getText());
                String reply = applicationManager.processCommand(remCommandMark(message));
                message.setAnswer(reply);
            }
            if(ignor.has(message.getAuthor())){
                log(". Пользователь " + message.getAuthor() + " находится в игноре. Пропуск сообщения...");
                return message;
            }
            //подготовить ответ
            if (message.getAnswer() == null && patternProcessor != null)
                message = patternProcessor.processMessage(message);
            log(patternProcessor.getName() + message + " = " + message.answer);
            if (message.getAnswer() == null && functionAnswerer != null)
                message = functionAnswerer.processMessage(message);
            log(functionAnswerer.getName() + message + " = " + message.answer);
            if (message.getAnswer() == null && learning != null)
                message = learning.processMessage(message);
            log(learning.getName() + message + " = " + message.answer);
            if (message.getAnswer() == null && answerDatabase != null)
                message = answerDatabase.processMessage(message);
            log(answerDatabase.getName() + message + " = " + message.answer);

            //профильтровать
            if (message.getAnswer() == null && filter != null)
                message = filter.processMessage(message);
        }
        catch (Exception e){
            e.printStackTrace();
            message.setAnswer("Произошла ошибка при обработке сообщения: " + e.getMessage());
        }
        finally {
            //отправить
            if(message.getOnAnswerReady() != null && message.getAnswer() != null)
                message.getOnAnswerReady().sendAnswer(message);
        }
        return message;
    }
    /*ОБРАЩЕНИЯ
    * Они хранятся в том виде в котором будут использоваться. если надо чтобы было с запятой - будет с запятой.
    * если вписать без запятой, то бот будет реагировать без запятой.
    * Обращения хранятся в памяти без пробела
    * в сообщении между обращением и текстом пробел обязателен (если нет другого знака)*/
    public boolean hasTreatment(Message message){
        //Эта функция должна принимать на вход сообщение и определять есть ли в нём обращение к боту
        //то есть, надо ли на это сообщение отвечать в разговоре
        for(String treatment:botTreatments)
            if(message.getText().toLowerCase().startsWith(treatment.toLowerCase()))
                return true;
        return false;
    }
    //убирает из сообщения пользователя обращение к боту
    public Message remTreatment(Message message){
        for(String treatment:botTreatments) {
            if (message.getText().toLowerCase().startsWith(treatment.toLowerCase())) {
                String newText = "";
                if(message.getText().length() > treatment.length())
                    newText = message.getText().substring(treatment.length() + 1);
                Message copyMessage =  new Message(message);
                copyMessage.setText(newText);
                return copyMessage;
            }
        }
        return message;
    }
    public void addTreatment(String text) throws Exception{
        //добавить обращение к списку обращений
        if(text == null)
            throw new Exception("Новое обращение не было получено.");
        text = text.trim();
        if(text.trim().equals(""))
            throw new Exception("Обращение должно быть обязательно не пустым. " +
                    "Чтобы убрать обращение - есть отдельная команда!");
        String letters = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM" +
                "йцукенгшщзфывапролджэхъячсмитьбю1234567890ЙЦУКЕНГШЩЗХЪХЭЖДЛОРПАВЫФЯЧСМИТЬББЮ";
        boolean containsLetters = false;
        for(int i=0; i<text.length(); i++)
            if(letters.indexOf(letters.charAt(i)) != -1)
                containsLetters = true;
        if(!containsLetters)
            throw new Exception("Обращение должно содержать хотя бы одну букву!");
        //если последний символ обращения - буква (не символ)
        //добавить пробел после обращения ЕСЛИ там нет какого нибудь знака
        if(letters.indexOf(text.charAt(text.length()-1)) != -1)
            text = text + " ";
        for (int i = 0; i < botTreatments.length; i++) {
            if(botTreatments[i].toLowerCase().equals(text.toLowerCase()))
                throw new Exception("Такое обращение уже есть!");
        }
        String[] result = new String[botTreatments.length + 1];
        for (int i = 0; i < botTreatments.length; i++)
            result[i] = botTreatments[i];
        result[botTreatments.length] = text;
        botTreatments = result;
        fileStorage.put("botTreatments", botTreatments);
        log(". Обращения сохранены.");
    }
    public String getTreatment(){
        if(botTreatments.length == 0)
            return "";
        return botTreatments[0];
    }
    public UserList getAllow() {
        return allow;
    }
    public UserList getIgnor() {
        return ignor;
    }
    public Learning getLearning() {
        return learning;
    }
    public boolean hasCommandMark(Message message){
        //Эта функция должна принимать на вход текст и определять есть
        // ли в нём команда к боту
        //то есть, надо ли на это сообщение отвечать в разговоре
        return message.getText().toLowerCase().startsWith("botcmd ") || message.getText().toLowerCase().startsWith("bcd ");
    }
    public Message remCommandMark(Message message){
        if (message.getText().toLowerCase().startsWith("botcmd ")) {
            String newText = message.getText().substring(7);
            message.setText(newText);
            return message;
        }
        if (message.getText().toLowerCase().startsWith("bcd ")) {
            String newText = message.getText().substring(4);
            message.setText(newText);
            return message;
        }
        return message;
    }
    public boolean hasBotMark(Message message){
        //(met ka) yexylknjk
        //Эта функция должна принимать на вход текст и определять есть ли в нём метка какного нибудь другого бота
        //то есть, надо ли на это сообщение отвечать в разговоре
        //// TODO: 14.08.2017 проверить в tpt ботом
        return message.getText().matches("\\([^\\)]+\\) .+");
    }
    public Message addTreatmentToAnswer(Message message){
        if(message == null)
            return null;
        Answer answer = message.getAnswer();
        if(answer == null || answer.isEmpty())
            return message;
        //добавляеь обращение к пользователю (если включено) и заменить константы на данные
        boolean treatmentEnabled = applicationManager.getParameters().get(
                "includeUserTreatment",
                true,
                "Обращаться ли к пользователю",
                "Нужно ли включать ли обращение к пользователю по имени в ответе.");
        //нужно будет загружать инфу о пользователе только в том случае, если либо включено
        // добавление обращения, либо если в ответе есть какая-то константа, которую надо заменить на инфу о пользователе
        if(treatmentEnabled || answer.text.contains("%")) {
            //если нигде в тексте не упоминается имя, добавить его в начале
            if(message.getAuthor().getName() != null &&
                    !answer.text.toLowerCase().contains("%username%") &&
                    !answer.text.toLowerCase().contains("%usersurname%"))
                answer.text = message.getAuthor().getName() + ", " + answer.text;
        }
        message.setAnswer(answer);
        return message;
    }
    public Message replacePlaceholdersByData(Message message){
        //заменяет констатны типа %USERNAME% данными
        if(message == null)
            return null;
        Answer answer = message.getAnswer();
        if(answer == null || answer.isEmpty())
            return message;
        //нужно будет загружать инфу о пользователе только в том случае, если либо включено
        // добавление обращения, либо если в ответе есть какая-то константа, которую надо заменить на инфу о пользователе
        if(answer.text.contains("%")) {
            User user = message.getAuthor();
            /*
            Hello, deAr %username%
            Hello, deAr %usernamE%
            Hello, deAr %UsernamE% and %UsernamE%
            Hello, deAr %USERNAME%
            * "bdate,first_name,last_name,about,interests,home_town,screen_name,is_friend,books,photo_id"*/

            //прописать в ответ константы, которые могут быть в ответе
//            if(user.first_name != null)
//                answer.text = F.replaceCaseInsensitive(answer.text, "%USERNAME%", user.first_name);
//            if(user.last_name != null)
//                answer.text = F.replaceCaseInsensitive(answer.text, "%USERSURNAME%", user.last_name);
//            if(user.domain != null)
//                answer.text = F.replaceCaseInsensitive(answer.text, "%USERDOMAIN%", user.domain);
//            if(user.status != null)
//                answer.text = F.replaceCaseInsensitive(answer.text, "%USERSTATUS%", user.status);
            //// TODO: 23.09.2017 Я хуй его знает в каком блять виде приходит аватарка пользователя! Нужно реализовать вставку аватара
            //log("BotBrain.java: user.photo_id = " + user.photo_id);
            //if(answer.text.toLowerCase().contains("%userphoto%"))
//            if(user.photo_id != null)
//                answer.text = F.replaceCaseInsensitive(answer.text, "%USERNAME%", user.first_name);

            answer.text = F.replaceCaseInsensitive(answer.text, "%DATE%", new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date()));
            answer.text = F.replaceCaseInsensitive(answer.text, "%TIME%", new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        }
        message.setAnswer(answer);
        return message;
    }
    public Message addBotMarkToAnswer(Message message){
        if(message == null)
            return null;
        Answer answer = message.getAnswer();
        if(answer == null || answer.isEmpty())
            return message;

        boolean markEnabled = applicationManager.getParameters().get(
                "includeBotMark",
                true,
                "Отправлять метку бота",
                "Если включено, в ответах бота перед текстом будет метка бота в скобках. Например, такая: (бот).");

        //// TODO: 23.11.2017 когда не куплен, игнорировать
        if(markEnabled) {
            String botMark = applicationManager.getParameters().get(
                    "bot_mark",
                    "бот",
                    "Метка бота",
                    "Если метка бота включена, она отображается перед ответом бота в скобках.");

            answer.text = "(" + botMark + ") " + answer.text;
        }
        message.setAnswer(answer);
        return message;
    }
    public AnswerDatabase getAnswerDatabase() {
        return answerDatabase;
    }
    public UnknownMessagesDatabase getUnknownMessages() {
        return unknownMessages;
    }
    public PatternProcessor getPatternProcessor() {
        return patternProcessor;
    }
    public Filter getFilter() {
        return filter;
    }

    private class Help extends CommandModule{
        public Help(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("help")) {
                String query = commandParser.getText().toLowerCase();
                ArrayList<CommandDesc> commandDescs = applicationManager.getHelp();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Список команд " + ApplicationManager.getVisibleName()+"\n");
                stringBuilder.append("Всего команд:"+commandDescs.size()+"\n");
                stringBuilder.append("================================\n");
                for (CommandDesc commandDesc:commandDescs){
                    if(query.isEmpty()) {
                        stringBuilder.append(commandDesc.getName() + "\n");
                        //stringBuilder.append(commandDesc.getHelpText()+"\n");
                        stringBuilder.append("" + commandDesc.getExample() + "\n");
                        stringBuilder.append(".\n");
                        //stringBuilder.append(".\n");
                    }
                    else {
                        if(commandDesc.getName().toLowerCase().contains(query) || commandDesc.getHelpText().toLowerCase().contains(query)){
                            stringBuilder.append("--=== " + commandDesc.getName() + " ===-- \n");
                            stringBuilder.append(commandDesc.getHelpText()+"\n");
                            stringBuilder.append("" + commandDesc.getExample() + "\n");
                            stringBuilder.append(".\n");
                            stringBuilder.append(".\n");
                        }
                    }
                }
                return stringBuilder.toString();
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
}
