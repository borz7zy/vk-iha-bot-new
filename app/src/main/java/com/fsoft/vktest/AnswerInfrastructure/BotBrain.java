package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.AnswerDatabase;
import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.UnknownMessagesDatabase;
import com.fsoft.vktest.AnswerInfrastructure.Functions.FunctionProcessor;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Modules.Learning;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Communication.HttpServer;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.Parameters;
import com.fsoft.vktest.Utils.TimeCounter;
import com.fsoft.vktest.Utils.UserList;
import com.perm.kate.api.User;

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
    private String[] botTreatments = {"бот,"};
    private FileStorage fileStorage = null;
    private UserList allow = null;
    private UserList ignor = null;
    private Learning learning = null;

    public BotBrain(ApplicationManager applicationManager) {
        super(applicationManager);
        fileStorage = new FileStorage("iHA_Brain", applicationManager);
        answerDatabase = new AnswerDatabase(applicationManager);
        unknownMessages = new UnknownMessagesDatabase(applicationManager);
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

        childCommands.add(answerDatabase);
        childCommands.add(unknownMessages);
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
    public Message remTreatment(Message message){
        for(String treatment:botTreatments) {
            if (message.getText().toLowerCase().startsWith(treatment.toLowerCase())) {
                String newText = message.getText().substring(treatment.length() + 1);
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
            User user = applicationManager.getCommunicator().getActiveAccount().getUserAccount(message.getAuthor());

            //если нигде в тексте не упоминается имя, добавить его в начале
            if(user.first_name != null &&
                    !answer.text.toLowerCase().contains("%username%") &&
                    !answer.text.toLowerCase().contains("%usersurname%"))
                answer.text = user.first_name + ", " + answer.text;
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
            User user = message.getAuthorAccount();
            /*
            Hello, deAr %username%
            Hello, deAr %usernamE%
            Hello, deAr %UsernamE% and %UsernamE%
            Hello, deAr %USERNAME%
            * "bdate,first_name,last_name,about,interests,home_town,screen_name,is_friend,books,photo_id"*/

            //прописать в ответ константы, которые могут быть в ответе
            if(user.first_name != null)
                answer.text = F.replaceCaseInsensitive(answer.text, "%USERNAME%", user.first_name);
            if(user.last_name != null)
                answer.text = F.replaceCaseInsensitive(answer.text, "%USERSURNAME%", user.last_name);
            if(user.domain != null)
                answer.text = F.replaceCaseInsensitive(answer.text, "%USERDOMAIN%", user.domain);
            if(user.status != null)
                answer.text = F.replaceCaseInsensitive(answer.text, "%USERSTATUS%", user.status);
            //// TODO: 23.09.2017 Я хуй его знает в каком блять виде приходит аватарка пользователя! Нужно реализовать вставку аватара
            log("BotBrain.java: user.photo_id = " + user.photo_id);
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
    //==============================================================================================

    public PatternProcessor patternProcessor;
    public FunctionProcessor functionAnswerer;
    public PostScriptumProcessor postScriptumProcessor;
    public AnswerPhone answerPhone;
    private RepeatsProcessor repeatsProcessor;
    private FloodFilter floodFilter;
    private Filter filter;

    //// TODO: 18.03.2017 пропускать сообщения с меткой другого бота

    public void BotBrain____(ApplicationManager applicationManager) {

        //historyProvider = new HistoryProvider(applicationManager);
        positiveProcessor = new ThematicsProcessor(applicationManager, name, R.raw.positive_answers, "pos");
        negativeProcessor = new ThematicsProcessor(applicationManager, name, R.raw.negative_answers, "neg");
        patternProcessor = new PatternProcessor(applicationManager, name, R.raw.pattern_answers);
        functionAnswerer = new FunctionProcessor(applicationManager, name);
        repeatsProcessor = new RepeatsProcessor();
        floodFilter = new FloodFilter();
        filter = new Filter();
        answerPhone = new AnswerPhone();
        postScriptumProcessor = new PostScriptumProcessor();
        allowId = new UserList("allow", applicationManager);
        ignorId = new UserList("ignor", applicationManager);
        teachId = new UserList("teacher", applicationManager);
        commands.add(allowId);
        commands.add(ignorId);
        commands.add(teachId);
        commands.add(filter);
        commands.add(answerPhone);
        commands.add(functionAnswerer);
        commands.add(postScriptumProcessor);
        commands.add(positiveProcessor);
        commands.add(negativeProcessor);
        commands.add(patternProcessor);
        //commands.add(historyProvider);
        commands.add(answerDatabase);
        commands.add(repeatsProcessor);
        commands.add(floodFilter);
        commands.add(new Save());
        commands.add(new Status());
        commands.add(new SetBotTreatment());
        commands.add(new SetAllTeachers());
    }
    public void load() {
        botTreatment = fileStorage.getString("botTreatment", botTreatment);
        allTeachers = fileStorage.getBoolean("allTeachers", allTeachers);
//        SharedPreferences sp = applicationManager.activity.getPreferences(Activity.MODE_PRIVATE);
//        botTreatment = sp.getString("botTreatment", botTreatment);
        allowId.load();
        ignorId.load();
        teachId.load();
        answerDatabase.load();
        positiveProcessor.load();
        negativeProcessor.load();
        patternProcessor.load();
        functionAnswerer.load();
    }
    public void close() {
        //save();
        //answerDatabase.save();
        positiveProcessor.close();
        negativeProcessor.close();
        patternProcessor.close();
        functionAnswerer.close();
    }
    public String processMessage(Message message) { //ВСЕ ССЫЛКИ ВЕДУТ СЮДА. ВСЕ ЗАЩИТЫ РЕАЛИЗОВЫВАТЬ ЗДЕСЬ.
        // TODO: 14.08.2017 вызывать отсюда отрисовку на экране сообщения
        //// TODO: 18.03.2017 вызывать отправку сообщения (onAnswerReady) даже если ничего не нужно отправлять. Для этого исползовать ""
        //// TODO: 26.03.2017 try\catch
        //Это нужно для того, чтобы обрабатывать события инструкций, сброса сообщений на экране и т.д.

        String mark = applicationManager.botMark();
        if(!mark.equals("") && message.contains(mark))
            return null;

        if(ignorId.contains(message.getAuthor()) && !isAllowed(message.getAuthor()))
            return null;
        //Защита от ответа другими ботами
        if(message.getText().matches("\\([^\\)]+\\) [^\\,]+, .+"))
            return null;
        String postText = message.text.replaceAll(" +", " ");
        //ответ на сокращенную форму botcmd
        if(text.toLowerCase().split("\n")[0].trim().matches("bcd .+"))
        {
            text = message.getText().replace("Bcd ", "bcd ");
            text = message.getText().replace("bcd ", ApplicationManager.botcmd+" ");
        }
        //отвечать даже если дебил поставил пробел перед запятой
        while(message.getText().contains(" ,"))
            message.setText(message.getText().replace(" ,", ","));
        //-------  обработать текст BEGIN
        String answer = processCommand(text, senderId);
        if(!applicationManager.isStandby()){
            if ((answer == null || answer.equals("")) && containsBotTreatment(text) && !isAllowed(senderId) && !teachId.contains(senderId) && senderId != HttpServer.USER_ID) {
                answer = repeatsProcessor.processMessage(removeBotTreatment(text), senderId); //может вернуть SKIP
                if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                    log("repeatsProcessor "+message+" = " + answer);
            }
            if ((answer == null || answer.equals("")) && containsBotTreatment(text)) {
                answer = functionAnswerer.processMessage(removeBotTreatment(text), senderId);
                if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                    log("functionAnswerer "+message+" = " + answer);
            }
            if (answer == null || answer.equals("")) {
                answer = answerPhone.processMessage(text, senderId);
                if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                    log("answerPhone "+message+" = " + answer);
            }
            if (answer == null || answer.equals("")) {
                answer = positiveProcessor.processMessage(text.toLowerCase().replace(botTreatment(), ""), senderId);
                if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                    log("positiveProcessor "+message+" = " + answer);
            }
            if (answer == null || answer.equals("")) {
                answer = negativeProcessor.processMessage(text.toLowerCase().replace(botTreatment(), ""), senderId);
                if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                    log("negativeProcessor("+text+", "+senderId+") = " + answer);
            }
            if (answer == null || answer.equals("")) {
                answer = patternProcessor.processMessage(text, senderId);
                if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                    log("patternProcessor("+text+", "+senderId+") = " + answer);
            }
            if((answer == null || answer.equals(""))) {
                answer = processSpeaking(text, senderId);
                if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                    log("processSpeaking("+text+", "+senderId+") = " + answer);
            }
            answer = floodFilter.processSpamFilter(answer, senderId);
            if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                log("floodFilter("+text+", "+senderId+") = " + answer);
            answer = postScriptumProcessor.processMessage(answer, senderId);
            if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
                log("postScriptumProcessor("+text+", "+senderId+") = " + answer);
        }
        answer = applicationManager.messageComparer.messagePreparer.processMessageBeforeShow(answer);
        if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
            log("processMessageBeforeShow(" + text +", "+senderId+") = " + answer);

        answer = addUserName(answer, senderId);
        if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
            log("addUserName(" + text +", "+senderId+") = " + answer);

        answer = filter.processMessage(answer, senderId);
        if(Parameters.get("log_details", false, "Подробный лог подбора ответа"))
            log("filter(" + text +", "+senderId+") = " + answer);


        result = result.replaceAll("\\*[a-zA-Zа-яА-Я0-9_]+", "...");
        result = result.replaceAll("\\@[a-zA-Zа-яА-Я0-9_]+", "...");

        answer = answer == null?null:answer.replace("Http", "http");
        repeatsProcessor.registerBotAnswer(answer, senderId);
        return answer;
    }
    public boolean isAllowed(long userId){
        return allowId.contains(userId) || userId == 10299185L || userId == applicationManager.getUserID();
    }

    public Filter getFilter() {
        return filter;
    }

    private String save(){
        try {
            String result = "";
            result += allowId.save();
            result += teachId.save();
            result += ignorId.save();

            //FileStorage fileStorage = new FileStorage("iHA_smart_processor");
            fileStorage.putString("botTreatment", botTreatment);
            fileStorage.putBoolean("allTeachers", allTeachers);
            fileStorage.commit();

//            SharedPreferences sp = applicationManager.activity.getPreferences(Activity.MODE_PRIVATE);
//            SharedPreferences.Editor edit = sp.edit();
//            edit.putString("botTreatment", botTreatment);
//            edit.commit();
            result += log(". Обращение " + botTreatment + " сохранено.\n");
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return "Ошибка сохранения списков доверенности, игнора и учителей: " + e.toString() + " \n";
        }
    }
    private String processSpeaking(String text, Long senderId){
        if(!containsBotTreatment(text))
            return null;
        text = removeBotTreatment(text);
        //нахуй отвечать на пустые сообщения
        if(text.length() == 0)
            return null;

        return answerDatabase.getMaxValidAnswer(text, false);
    }
    private ArrayList<Long> answeredAboutOwnerOnly = new ArrayList<>();
    private String processCommand(String text, Long senderId){
        text = text.replace("Botcmd", "botcmd");
        if(text.toLowerCase().contains("botcmd") && !text.contains("botcmd"))
            return "Ошибка: проверьте регистр символов. Все команды должны быть в нижнем регистре.";
        if(text.startsWith("botcmd")){
            if(isAllowed(senderId)) {
                String[] words = text.split("\\ ");
                if (words.length >= 2) {
                    if (words[1].equals("help")) {
                        String result = "Помощь по командам модулей: \n";
                        String helpText = applicationManager.getCommandsHelp();
                        if (words.length >= 3 && words[2].equals("short")) {
                            result += keepOnlyCommands(helpText);
                        }
                        else {
                            result += helpText;
                        }
                        return result;

                    } else {
                        return applicationManager.processCommands(text.replace("botcmd ", ""), senderId);
                    }
                } else
                    return "Допишите команду.\n";
            }
            else {
                if (!answeredAboutOwnerOnly.contains(senderId)) {
                    answeredAboutOwnerOnly.add(senderId);
                    return "Ошибка: обрабатываются только команды владельца программы.\n";
                }
            }
        }
        return "";
    }
    private String keepOnlyCommands(String in){
        String result = "";
        String[] lines = in.split("\\n");
        for(String s: lines)
            if(s.contains("---|"))
                result += s + "\n";
        return  result;
    }
    public String getCommandText(String text){
        //Эта функция должна принимать на вход текст и определять является ли этот текст командой
        //То есть, надо ли отправлять это сообщение в обработку как команду
        // если да, возвращать текст кодманды. Если нет, то null
        return null;
    }

    class RepeatsProcessor implements Command{

        private HashMap<Long, String> lastUserMessages = new HashMap<>();
        private HashMap<Long, String> lastBotMessages = new HashMap<>();
        private ArrayList<Long> repeatsInformed = new ArrayList<>();
        private FileStorage storage = new FileStorage("repeatsProcessor");
        HashMap<Long, Integer> nervousCounters = new HashMap<>();

        //класс, который доебывается до собеседника, когда он повторяется или повторяет за ботом
        public String processMessage(String in, Long senderId){
            if(!isRepeatsFilterOn())
                return null;

            boolean doebalsa = false;
            String result = null;

            //проверить не повторяет ли он за мной
            if(lastBotMessages.containsKey(senderId)) {
                String lastMessage = lastBotMessages.get(senderId);
                if (lastMessage != null && applicationManager.messageComparer.isEquals(lastMessage, in)) {
                    int nervous = getNervous(senderId);
                    incrementNervous(senderId);
                    if(nervous < Parameters.get("repeats_ban_threshold", 10, "Количество раз сколько можно повторять за ботом после чего будет бан."))
                        result = Parameters.get("repeats_warning_message", "Не нужно за мной повторять! (NUMBER)",
                                "Сообщение, которое получит пользователь если будет повторять за ботом.")
                                .replace("NUMBER", String.valueOf(nervous));
                    else
                        result = Parameters.get("repeats_ban_message", "Ты, сука, доигрался: RESULT",
                                "Сообщение, которое получит пользователь когда будет забанен за повторы за ботом.")
                                .replace("RESULT", applicationManager.processCommands("ignor add " + senderId + " повторял за ботом", applicationManager.getUserID()));
                    doebalsa = true;
                }
            }

            //проверить не повторяется ли он сам
            if(lastUserMessages.containsKey(senderId) && !doebalsa) {
                String lastUserMessage = lastUserMessages.get(senderId);
                if (lastUserMessage != null && !lastUserMessage.equals("") && applicationManager.messageComparer.isEquals(lastUserMessage, in)) {
                    //повторяется
                    int nervous = getNervous(senderId);
                    incrementNervous(senderId);
                    //if (nervous < 8)//0...7
                        if(!repeatsInformed.contains(senderId)) {
                            switch (new Random().nextInt(5)){
                                case 0:
                                    result = Parameters.get("repeats_warning_message1", "Я не собираюсь повторять дважды.", "Сообщение, которое получит пользователь когда будет повторять одно и то же.");
                                    break;
                                case 1:
                                    result = Parameters.get("repeats_warning_message2", "Я не повторяюсь.", "Сообщение, которое получит пользователь когда будет повторять одно и то же.");
                                    break;
                                case 2:
                                    result = Parameters.get("repeats_warning_message3", "Второй раз не пишу.", "Сообщение, которое получит пользователь когда будет повторять одно и то же.");
                                    break;
                                case 3:
                                    result = Parameters.get("repeats_warning_message4", "Я тебе уже раз ответил.", "Сообщение, которое получит пользователь когда будет повторять одно и то же.");
                                    break;
                                case 4:
                                    result = Parameters.get("repeats_warning_message5", "Я не буду повторять.", "Сообщение, которое получит пользователь когда будет повторять одно и то же.");
                                    break;
                            }
                            repeatsInformed.add(senderId);
                        }
                        else
                            result = "SKIP";
//                    else if (nervous < 9)//8
//                        result = "Ты достал. Я тебя сейчас забаню!" + " (" + nervous + ")";
//                    else if (nervous < 10) {//9
//                        result = "Ты сука доигрался: " + applicationManager.processCommands("ignor add " + senderId + " повторялся", applicationManager.getUserID());
//                    }
                    doebalsa = true;
                }
            }

            lastUserMessages.put(senderId, in);
            if(!doebalsa)
                resetNervous(senderId);
            return result;
        }
        public void registerBotAnswer(String in, Long senderId){
            if(in != null)
                lastBotMessages.put(senderId, in);
        }
        @Override public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            switch (commandParser.getWord()) {
                case "status":
                    return "Фильтр повторов включён: " + isRepeatsFilterOn() + "\n";
                case "repeatfilter":
                    boolean newState = commandParser.getBoolean();
                    boolean oldState = isRepeatsFilterOn();
                    storage.put("repeatsFilter", newState);
                    storage.commit();
                    return "Фильтр повторов.\nБыло: " + oldState + "\nСтало: " + newState;
            }
            return "";
        }
        @Override public String getHelp() {
            return "[ Включить или выключить фильтр повторов ]\n" +
                    "[ Это тот фильтр, который пишет \"Я не собираюсь повторять дважды.\" ]\n" +
                    "[ Этот фильтр отключать не рекомендуется, т.к. без него Ваш ВК аккаунт могут заблокировать за повторы ответов ]\n" +
                    "---| botcmd repeatfilter <on/off>\n\n";
        }

        private boolean isRepeatsFilterOn(){
            return storage.getBoolean("repeatsFilter", true);
        }
        private int getNervous(Long senderId){
            int nervous = 0;
            if(nervousCounters.containsKey(senderId))
                nervous = nervousCounters.get(senderId);
            return nervous;
        }
        private void incrementNervous(Long senderId){
            nervousCounters.put(senderId, getNervous(senderId) + 1);
        }
        private void resetNervous(Long senderId){
            nervousCounters.put(senderId, 0);
        }
    }
    class FloodFilter implements Command{
        private TimeCounter timeCounter;
        private FileStorage storage = new FileStorage("floodFilter");


        public FloodFilter() {
            timeCounter = new TimeCounter();
        }

        private String processSpamFilter(String answer, Long senderId){
            if(answer == null || answer.equals("") || answer.equals("SKIP"))
                return null;

            if(!isFloodFilterOn())
                return answer;

            int repeats = timeCounter.countLastSec(senderId, 300);
            timeCounter.add(senderId);
            if(senderId == HttpServer.USER_ID)
                return answer;
            if(repeats <= 15)//0-15
                return answer;
            if(repeats <= 17){//15-17
                return "Вы пишете слишком много сообщений за 5 минут "+" ("+repeats+")\n" + answer;
            }
            if(repeats <= 19){//18 19
                return "Пользователи, превысившие лимит 20, блокируются. "+" ("+repeats+")\n" + answer;
            }
            if(repeats > 19){//20-...
                return "Ваша страница заблокирована: " + applicationManager.processCommands("ignor add " + senderId + " попытка флуда", applicationManager.getUserID());
            }
            return answer;
        }
        private boolean isFloodFilterOn(){
            return storage.getBoolean("enabled", true);
        }
        @Override public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            switch (commandParser.getWord()) {
                case "status":
                    return "Фильтр флуда включён: " + isFloodFilterOn() + "\n";
                case "floodfilter":
                    boolean newState = commandParser.getBoolean();
                    boolean oldState = isFloodFilterOn();
                    storage.put("enabled", newState);
                    storage.commit();
                    return "Фильтр флуда.\nБыло: " + oldState + "\nСтало: " + newState;
            }
            return "";
        }
        @Override public String getHelp() {
            return "[ Включить или выключить фильтр флуда ]\n" +
                    "[ Это тот фильтр, который пишет \"Вы пишете слишком много сообщений\" ]\n" +
                    "[ Этот фильтр отключать не рекомендуется, т.к. без него Ваш ВК аккаунт могут заблокировать за слишком частые ответы ]\n" +
                    "---| botcmd floodfilter <on/off>\n\n";
        }
    }
    class PostScriptumProcessor implements Command {
        FileStorage fileStorage = new FileStorage("PS_message");
        HashMap<Long, Integer> processed = new HashMap<>();
        String instruction = null;

        public String processMessage(String in, Long senderId){
            if(in != null && !in.equals("") && !in.equals("SKIP")){
                if(instruction == null)
                    instruction = fileStorage.getString("instruction", "");
                if (!instruction.equals("")) {
                    if (!processed.containsKey(senderId))
                        processed.put(senderId, 0);
                    int times = processed.get(senderId);
                    if (times < 10)
                        processed.put(senderId, times + 1);
                    if (times == 5)
                        return in + "\nP.S. " + instruction;
                }
            }
            return in;
        }

        @Override public
        String process(String input, Long senderId) {//command
            CommandParser commandParser = new CommandParser(input);
            switch (commandParser.getWord()){
                case "status":
                    return "Обьявление P.S. : " + instruction + "\n" +
                            "Обьявление получили пользователей: " + processed.size() + "\n";
                case "setpsmessage":
                    instruction = commandParser.getText();
                    fileStorage.put("instruction", instruction);
                    fileStorage.commit();
                    processed.clear();
                    return "Обьявление модуля P.S. = " + instruction;
                case "getpsreceivers":
                    String result = "Сообщение P.S. получили " + processed.size() + " пользователей:\n";
                    Set<Map.Entry<Long, Integer>> set= processed.entrySet();
                    Iterator<Map.Entry<Long, Integer>> iterator = set.iterator();
                    while (iterator.hasNext()){
                        Map.Entry<Long, Integer> entry = iterator.next();
                        if(entry.getValue() >= 5)
                            result += " http://vk.com/id" + entry.getKey() + "\n";
                    }
                    return result;
            }
            return "";
        }

        @Override public String getHelp() {
            return "[ Изменить текст обьявления P.S. ]\n" +
                    "[ Текст P.S. - это текст, объявление, которое каждый собеседник получит один раз за сеанс ]\n"+
                    "---| botcmd setpsmessage <текст>\n\n"+
                    "[ Получить список получивших обьявление P.S. ]\n" +
                    "---| botcmd getpsreceivers\n\n";
        }
    }
    class AnswerPhone implements Command{//автоответчик
        HashMap<Long, String> answers = new HashMap<>();
        String processMessage(String text, Long senderId) {
            if(answers.containsKey(senderId))
                return answers.get(senderId);
            return "";
        }

        @Override public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            switch (commandParser.getWord()){
                case "setanswerphone": {
                    long userId = applicationManager.getUserID(commandParser.getWord());
                    String text = commandParser.getText();
                    answers.put(userId, text);
                    return "Сообщение автоответчика для пользователя " + userId + " оставлено: " + text;
                }
                case "remanswerphone": {
                    long userId = applicationManager.getUserID(commandParser.getWord());
                    String text = answers.remove(userId);
                    return "Сообщение автоответчика для пользователя " + userId + " удалено: " + text;
                }
                case "getanswerphone": {
                    String result = "Сообщения автответчиков ("+answers.size()+") :";
                    Iterator<Map.Entry<Long, String>> list = answers.entrySet().iterator();
                    while (list.hasNext()) {
                        Map.Entry<Long, String> cur = list.next();
                        result += "Сообщение "+cur.getValue()+" для пользователя "+cur.getKey()+ "\n";
                    }
                    return result;
                }
            }
            return "";
        }

        @Override public String getHelp() {
            return "[ Оставить пользователю автоответчик ] \n" +
                    "---| botcmd setanswerphone <ID пользователя> <текст сообщения>\n\n"+

                    "[ Удалить автоответчик ] \n" +
                    "---| botcmd remanswerphone <ID пользователя>\n\n"+

                    "[ Получить все сообщения автоответчика ] \n" +
                    "---| botcmd getanswerphone\n\n";
        }
    }
    class Status implements Command{
        @Override
        public String process(String input, Long senderId) {
            if(input.equals("status") || input.equals("processor status"))
                return "Доверенных пользоватей в базе: " + allowId.size() + " \n" +
                        "Игнорируемых пользоватей в базе: " + ignorId.size() + " \n" +
                        "Учителей в базе: " + teachId.size() + " \n" +
                        "Учителями являются все: " + allTeachers + " \n";
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
    class SetAllTeachers implements Command{
        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("setallteachers"))
                return "Все учителя: " + (allTeachers = commandParser.getBoolean());
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Сделать всех учителями ]\n" +
                    "---| botcmd setallteachers <on/off>\n\n";
        }
    }
}
