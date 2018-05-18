package com.fsoft.vktest.AnswerInfrastructure;

import android.util.Log;

import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.AnswerElement;
import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.AnswerMicroElement;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.HttpServer;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.ResourceFileReader;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.Parameters;
import com.fsoft.vktest.Utils.User;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Этот класс фильтрует базар бота, чтобы он не сказал ничего запрещенного.
 *
 * Этот класс хранится в BotBrain, однако каждый BotModule использует свою функцию prepare обращается к этому
 * классу через ApplicationManager.
 *
 * Что этот класс делает:
 * - оставляет только разрешённые символы
 * - Находит запрещённые слова и заменяет их звёздочками
 * - Ломает сылки
 * - Высылает предупреждения
 * - Блокирует злобных нарушителей
 *
 *
 * как найти ссылку на заменённой строке и сломать на оригинальной?
 * Привет, напиши bot---cmd в ответе на моё сообщение!
 * оригинал: Привет, напиши bot---cmd
 * Изменено: приветнапишиbotcmd
 * Найдено: botcmd
 * Что надо заменить: bot---cmd
 * На что: b*t*-*c*d
 *
 * Идти по сообщению и накапливать буфер только текстовых символов без пробела
 * Когда в накопленном буфере можно найти ключевую фразу, значит она в конце
 * Передаём функции индекс последней буквы и найденное ключевое слово
 * функция идя с этого индекса в начало преобразовывает символ. если преобразованный символ соответствует символу из
 * найденного слова, заменяем символ в оригинальной строке на *
 * уменьшаем оба индекса (для оригинальной строки и для ключевого слова)
 * Created by Dr. Failov on 30.03.2017.
 */
public class Filter extends BotModule{
    private HashMap<User, Integer> warnings = new HashMap<>();
    private ArrayList<String> fuckingWords = null;
    private String allowedSymbols = null;
    private FileStorage storage = null;
    private boolean enabled = true;

    public Filter(ApplicationManager applicationManager) throws Exception {
        super(applicationManager);
        storage = new FileStorage("FilterSettings", applicationManager);
        enabled = storage.getBoolean("enabled", enabled);
        readBlacklist();
        //READ SYMBOLS
        File fileWithSymbols = new ResourceFileReader(applicationManager, R.raw.allowed_symbols).getFile();
        allowedSymbols = F.readFromFile(fileWithSymbols);
        log(". Разрешенные символы: загружено " + allowedSymbols.length() + " символов.");
        //READ WARNINGS
        try {
            JSONArray warningsJsonArray = storage.getJsonArray("warnings", new JSONArray());
            warnings = new HashMap<>();
            for (int i = 0; i < warningsJsonArray.length(); i++) {
                JSONObject jsonObject = warningsJsonArray.getJSONObject(i);
                User key = new User(jsonObject.getJSONObject("key"));
                int value = jsonObject.getInt("value");
                warnings.put(key, value);
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка загрузки списка предупреждений: " + e.getMessage());
        }

        childCommands.add(new Status(applicationManager));
        childCommands.add(new WarningReset(applicationManager));
        childCommands.add(new WarningGet(applicationManager));
        childCommands.add(new WarningSet(applicationManager));
        childCommands.add(new FilterEnable(applicationManager));
        childCommands.add(new FilterDisable(applicationManager));
        childCommands.add(new FilterTest(applicationManager));
        childCommands.add(new FilterDatabase(applicationManager));
        childCommands.add(new AddBlacklistWord(applicationManager));
        childCommands.add(new RemBlacklistWord(applicationManager));
        childCommands.add(new GetBlacklistWord(applicationManager));


    }
    @Override
    public Message processMessage(Message message) {
        if(!enabled)
            return message;
        if(applicationManager == null)
            return message;
        BotBrain brain = applicationManager.getBrain();
        if(brain == null)
            return message;
        if(message == null)
            return message;
        if(message.getAnswer() == null)
            return message;
        if(brain.getAllow().has(message.getAuthor()))
            return message;
        if(brain.getLearning().isAllowed(message.getAuthor()))
            return message;
        if(message.getSource() == MessageBase.SOURCE_HTTP)
            return message;
        if(message.getSource() == MessageBase.SOURCE_PROGRAM)
            return message;

        //оставить только разрещённые символы. Всякую псевдографику нахуй
        message.getAnswer().text = filterSymbols(message.getAnswer().text);

        //после каждой точки поставить по пробелу. Так убиваем ссылки
        message.getAnswer().text = filterLinks(message.getAnswer().text);


        String textError = applicationManager.getParameters().get(
                "text_filter_error",
                "(Ошибка фильтрации: %ERROR%)",
                "Текст оповещения о ошибке от фильтра",
                "Не всегда программы работают стабильно, иногда возникают ошибки. \n" +
                        "Не часто, но такое может быть. Именно на этот случай предусмотрено это оповещение.\n" +
                        "Фрагмент %ERROR% будет заменяться на текст ошибки.");
        try {
            String textWarning = applicationManager.getParameters().get(
                    "text_filter_warning",
                    "(Предупреждений: %WARNING%)",
                    "Текст предупреждения от фильтра",
                    "Существует много фраз, написав которые, страница пользователя блокируется автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Когда какой-то пользователь заставляет бота сказать что-то запрещённое, этот пользователь " +
                            "получает предупреждение.\n" +
                            "После нескольких предупреждений бот блокиует пользователя и больше ему не отвечает.\n" +
                            "Когда пользователь получает предупреждение, бот в конец своего сообщения " +
                            "добавляет фразу, чтобы сообщить об этом пользователю.\n" +
                            "Этот параметр позволяет настроить текст, который получит пользователь " +
                            "в конце сообщения, когда получит предупреждение.\n" +
                            "Фрагмент %WARNING% в предупреждении будет заменяться на число предупреждений пользователю.");
            String textIgnor = applicationManager.getParameters().get(
                    "text_filter_ignored",
                    "(Вы были заблокированы после %WARNING% предупреждений за попытку сломать бота)",
                    "Текст оповещения о блокировке от фильтра",
                    "Существует много фраз, написав которые, страница пользователя блокируется автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Когда какой-то пользователь заставляет бота сказать что-то запрещённое, этот пользователь " +
                            "получает предупреждение.\n" +
                            "После нескольких предупреждений бот блокиует пользователя и больше ему не отвечает.\n" +
                            "Когда пользователь блокируется, бот в конец своего сообщения " +
                            "добавляет фразу, чтобы сообщить об этом пользователю.\n" +
                            "Этот параметр позволяет настроить текст, который получит пользователь " +
                            "в конце сообщения, когда будет заблокирован.\n" +
                            "Фрагмент %WARNING% в предупреждении будет заменяться на число предупреждений пользователю.");

            //заменить запрещённые фразы звёздочками
            String before = new String(message.getAnswer().text);
            String after = filterForbidden(before);
            if (!before.equals(after)) {
                message.getAnswer().text = after;
                int warnings = addWarnings(message.getAuthor());
                if (warnings < 5)
                    message.getAnswer().text += " " + textWarning.replace("%WARNING%", String.valueOf(warnings));
                else {
                    applicationManager.getBrain().getIgnor().add(message.getAuthor(), "Попытка сломать бота.");
                    message.getAnswer().text += " " + textIgnor.replace("%WARNING%", String.valueOf(warnings));
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка обработки фильтра: " + e.getMessage());
            message.getAnswer().text += textError.replace("%ERROR%", e.getMessage());
        }

        return message;
    }
    public void setFilterEnabled(boolean enabled) {
        this.enabled = enabled;
        storage.put("enabled", enabled);
        storage.commit();
    }
    public boolean isFilterEnabled() {
        return enabled;
    }
    public boolean containsForbidden(String input){
        return input.equals(filterForbidden(input));
    }
    public String filterText(String input){
        return filterForbidden(filterLinks(filterSymbols(input)));
    }

    private void saveDataToStorage(){
        try {
            JSONArray warningsJsonArray = new JSONArray();
            Set<Map.Entry<User, Integer>> set = warnings.entrySet();
            Iterator<Map.Entry<User, Integer>> iterator = set.iterator();
            while (iterator.hasNext()){
                Map.Entry<User, Integer> entry = iterator.next();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("key", entry.getKey().toJson());
                jsonObject.put("value", entry.getValue());
                warningsJsonArray.put(jsonObject);
            }
            storage.put("warnings", warningsJsonArray);
            storage.commit();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка сохранения списка предупреждений фильтра: " +e.getMessage() +"!");
        }
    }
    private String filterSymbols(String input){
        //оставить только разрещённые символы. Всякую псевдографику нахуй
        return F.filterSymbols(input, allowedSymbols);
    }
    private String filterLinks(String input){
        //после каждой точки поставить по пробелу. Так убиваем ссылки
        input = input.replace(".", ". ");
        input = input.replaceAll(" +", " ");
        //восстанавливаем троеточие
        input = input.replace(". . .", "...");
        //ВК реагирует на спецграфику в кодах, типа &#228. Это её убивает
        input = input.replace("&#", " ");
        //восстанавливаем ссылки на ВК - они разрешены
        input = input.replace("vk. com", "vk.com");
        //убить упоминания
        input = input.replaceAll("\\*[a-zA-Zа-яА-Я0-9_]+", "...");
        input = input.replaceAll("\\@[a-zA-Zа-яА-Я0-9_]+", "...");
        //вернуть ответ
        return input;
    }
    private String filterForbidden(String input){
        /*
        Выполняет только замену запрещённых слов или фраз
        * -> botcmd, bro, nark, blue
        * -> The quick blue nark jumps over lazy bro.
        * --> The quick **** **** jumps over lazy ***.
        *
        * */
        //заменить запрещённые фразы звёздочками
        String buffer = "";
        for(int i=0; i<input.length(); i++){
            char c = prepareChar(input.charAt(i));
            if(c != 0) {
                buffer += c;
                String prohibited = containsProhibitedWord(buffer);
                if (prohibited != null) {
                    buffer = replaceProhibitedWord(buffer, prohibited, i);
                    input = replaceProhibitedWord(input, prohibited, i);
                }
            }
        }
        return input;
    }
    private void readBlacklist(){
        //READ BLACKLIST
        try {
            File blacklistFile = new ResourceFileReader(applicationManager, R.raw.blacklist).getFile();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(blacklistFile));
            String line;
            int lineNumber = 0;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber ++;
                fuckingWords.add(line);
            }
            bufferedReader.close();
            log(". Загружено " + fuckingWords.size() + " запрещённых слов.");
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка загрузки списка запрещённых слов: " + e.getMessage());
        }
    }
    private void saveBlacklist() throws Exception{
        //SAVE BLACKLIST
        File blacklistFile = new ResourceFileReader(applicationManager, R.raw.blacklist).getFile();
        PrintWriter fileWriter = new PrintWriter(blacklistFile);
        for (String word:fuckingWords)
            fileWriter.println(word);
        fileWriter.close();
        log(". Сохранено " + fuckingWords.size() + " запрещённых слов.");
    }
    private String prepareString(String input){
        String buffer = "";
        for(int i=0; i<input.length(); i++){
            char c = prepareChar(input.charAt(i));
            if(c != 0)
                buffer += c;
        }
        return buffer;
    }
    private char prepareChar(char in){
        in = toLowerCase(in);
        String allowed = "qwertyuiopasdfghjklzxcvbnmйцукенгшщзхъфывапролджэячсмитьбюіїєё1234567890";
        if(allowed.indexOf(in) >= 0)
            return in;
        else
            return 0;

    }
    private char toLowerCase(char in){
        switch (in){
            case 'Q': return'q';
            case 'W': return'w';
            case 'E': return'e';
            case 'R': return'r';
            case 'T': return't';
            case 'Y': return'y';
            case 'U': return'u';
            case 'I': return'i';
            case 'O': return'o';
            case 'P': return'p';
            case 'A': return'a';
            case 'S': return's';
            case 'D': return'd';
            case 'F': return'f';
            case 'G': return'g';
            case 'H': return'h';
            case 'J': return'j';
            case 'K': return'k';
            case 'L': return'l';
            case 'Z': return'z';
            case 'X': return'x';
            case 'C': return'c';
            case 'V': return'v';
            case 'B': return'b';
            case 'N': return'n';
            case 'M': return'm';

            case 'Й': return'й';
            case 'Ц': return'ц';
            case 'У': return'у';
            case 'К': return'к';
            case 'Е': return'е';
            case 'Н': return'н';
            case 'Г': return'г';
            case 'Ш': return'ш';
            case 'Щ': return'щ';
            case 'З': return'з';
            case 'Х': return'х';
            case 'Ъ': return'ъ';
            case 'Ф': return'ф';
            case 'Ы': return'ы';
            case 'В': return'в';
            case 'А': return'а';
            case 'П': return'п';
            case 'Р': return'р';
            case 'О': return'о';
            case 'Л': return'л';
            case 'Д': return'д';
            case 'Ж': return'ж';
            case 'Э': return'э';
            case 'Я': return'я';
            case 'Ч': return'ч';
            case 'С': return'с';
            case 'М': return'м';
            case 'И': return'и';
            case 'Т': return'т';
            case 'Ь': return'ь';
            case 'Б': return'б';
            case 'Ю': return'ю';
            case 'Ї': return'ї';
            case 'І': return'і';
            case 'Є': return'є';
            case 'Ё': return'ё';
            default: return in;
        }
    }
    private String containsProhibitedWord(String in){
        for(String word:fuckingWords){
            if(in.contains(word))
                return word;
        }
        return null;
    }
    private String replaceProhibitedWord(String in, String word, int endIndexInInputString){
        //принять фразу, слово которое в ней надо заменить звёздочками, последний индекс где буквы сходятся
        //идя с конца слова превращать каждую букву начальной строки
        //если превращення буева не 0, проверяем. и заменяем
        //
        // 0123456789012345678901234
        // Hello, Bro, how are you?!
        // bro
        // 9
        //result = Hello, ***, how are you?!

        int wordIndex = word.length()-1;
        for(int i=endIndexInInputString; i > 0; i--){
            char c = prepareChar(in.charAt(i));
            if(c != 0){
                if(c == word.charAt(wordIndex)){
                    in = replaceCharAt(in, '*', i);
                    wordIndex --;
                    if(wordIndex < 0)
                        break;
                }
                else
                    break;
            }
        }
        return in;
    }
    private String replaceCharAt(String in, char c, int index){
        return in.substring(0, Math.max(index-1, 0)) + c + in.substring(Math.min(index + 1, in.length()), in.length());
    }
    private int addWarnings(User userId){
        int cnt = 1;
        if(warnings.containsKey(userId)){
            cnt += warnings.get(userId);
        }
        warnings.put(userId, cnt);
        saveDataToStorage();
        return cnt;
    }
    private int resetWarnings(User userId){
        int result = 0;
        if(warnings.containsKey(userId)){
            result = warnings.remove(userId);
        }
        saveDataToStorage();
        return result;
    }
    private void setWarnings(User userId, int numberOfWarnings){
        warnings.put(userId, numberOfWarnings);
        saveDataToStorage();
    }
    private int getWarnings(long userId){
        int cnt = 0;
        if(warnings.containsKey(userId)){
            cnt = warnings.get(userId);
        }
        return cnt;
    }
    private void addFuckingWord(String word) throws Exception{
        fuckingWords.add(prepareString(word));
        saveBlacklist();
    }
    private void remFuckingWord(String word) throws Exception{
        String prepared = prepareString(word);
        if(!fuckingWords.remove(prepared))
            throw new Exception("Такого слова нет в списке запрещённых");
        saveBlacklist();
    }


    class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().toLowerCase().equals("status") || message.getText().toLowerCase().equals("filter status")){
                String result = "";
                result += "Фильтр сомнительного содержания включён: " + (enabled?"Да":"Нет") + "\n";
                result += "Шаблонов черного списка: "+(fuckingWords == null?"еще не загружено":fuckingWords.size())+"\n";
                result += "Разрешенных символов: "+(allowedSymbols == null?"еще не загружено":allowedSymbols.length())+"\n";
                result += "Пользователей получили предупреждения: "+ warnings.size() +"\n";

                return result;
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                "Просмотреть состояние фильтра.",
                    "Фильтр нужен для того, чтобы не позволить боту сказать чего-то такого, " +
                            "за что ВК может заблокировать аккаунт.",
                    "botcmd filter status"
            ));
            return result;
        }
    }
    class WarningReset extends CommandModule{
        public WarningReset(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("warning")
                    && commandParser.getWord().toLowerCase().equals("reset")){
                try {
                    User user = new User(commandParser.getWord());
                    int oldValue = resetWarnings(user);
                    String userName = applicationManager.getCommunicator().getActiveAccount().getUserName(user.getId());
                    String result = "Счетчик сброшен для пользователя " + userName + " (" + user + "). Было " + oldValue + " предупреждений.";
                    boolean isIgnored = applicationManager.getBrain().getIgnor().has(message.getAuthor());
                    if (isIgnored)
                        result += " Обрати внимание, пользователь всё ещё находится в игноре.";
                    return result;
                }
                catch (Exception e){
                    return "Ошибка выполнения команды: " + e.getMessage();
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Сбросить значение счетчика предупреждений фильтра для пользователя",
                    "Фильтр нужен для того, чтобы не позволить боту сказать чего-то такого, " +
                            "за что ВК может заблокировать аккаунт. Когда пользователь заставляет бота " +
                            "написать что-то запрещённое, пользователь получает предупреждение.\n" +
                            "После нескольких предупреждений страница пользователя блокируется.\n" +
                            "Эта команда позволяет сбросить предупреждения для пользователя.",
                    "botcmd warning reset <ID пользователя>"
            ));
            return result;
        }
    }
    class WarningGet extends CommandModule{
        public WarningGet(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("warning")
                    && commandParser.getWord().toLowerCase().equals("get")){
                String result = "Счетчик предупреждений:\n";
                Iterator<Map.Entry<User, Integer>> iterator = warnings.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<User, Integer> cur = iterator.next();
                    result += "- Пользователь " + cur.getKey() + " получил " + cur.getValue() + " предупреждений.\n";
                }
                return result;
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Показать значения счетчиков предупреждений пользователей",
                    "Фильтр нужен для того, чтобы не позволить боту сказать чего-то такого, " +
                            "за что ВК может заблокировать аккаунт. Когда пользователь заставляет бота " +
                            "написать что-то запрещённое, пользователь получает предупреждение.\n" +
                            "После нескольких предупреждений страница пользователя блокируется.\n" +
                            "Эта команда позволяет узнать, кто сколько предупреждений получил.",
                    "botcmd warning get"
            ));
            return result;
        }
    }
    class WarningSet extends CommandModule{
        public WarningSet(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("warning")
                    && commandParser.getWord().toLowerCase().equals("set")){
                //todo в справке написан формат screen-name. Надо его реализовать.
                try {
                    User user = new User(commandParser.getWord());
                    int warnings = commandParser.getInt();
                    setWarnings(user, warnings);
                    //String userName = applicationManager.getCommunicator().getActiveAccount().getUserName(id);
                    String result = "Счетчик задан для пользователя " + user + " (" + user.getGlobalId() + "). ";
                    boolean isIgnored = applicationManager.getBrain().getIgnor().has(message.getAuthor());
                    if (isIgnored)
                        result += " Обрати внимание, пользователь находится в игноре.";
                    return result;
                }
                catch (Exception e){
                    return "Ошибка выполнения команды: " + e.getMessage();
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Задать значение счетчика предупреждений фильтра для пользователя",
                    "Фильтр нужен для того, чтобы не позволить боту сказать чего-то такого, " +
                            "за что ВК может заблокировать аккаунт. Когда пользователь заставляет бота " +
                            "написать что-то запрещённое, пользователь получает предупреждение.\n" +
                            "После нескольких предупреждений страница пользователя блокируется.\n" +
                            "Эта команда позволяет задать счётчик предупреждений для пользователя.",
                    "botcmd warning set <ID пользователя> <Новое значение счётчика>"
            ));
            return result;
        }
    }
    class FilterEnable extends CommandModule{
        public FilterEnable(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("filter")
                    && commandParser.getWord().toLowerCase().equals("enable")){
                if(isEnabled())
                    return "Фильтр отправляемых сообщений уже включён.\n" +
                            "Это поможет предотвратить блокировку аккаунта из-за отправки запрещённого текста.\n" +
                            "Рекоментую оставить этот фильтр включённым.";
                setEnabled(true);
                return "Фильтр отправляемых сообщений включён. Теперь бот будет проверять " +
                        "отправляемые сообщения и цензурить запрещённые фразы. \n" +
                        "Это поможет предотвратить блокировку аккаунта из-за отправки запрещённого текста.\n" +
                        "Рекоментую оставить этот фильтр включённым.";
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Включить фильтр отправляемых сообщений",
                    "Существует много фраз, написав которые, страница пользователя блокируется автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Эта команда включает этот фильтр. Рекомендуетсы не выключать фильтр.",
                    "botcmd filter enable"
            ));
            return result;
        }
    }
    class FilterDisable extends CommandModule{
        public FilterDisable(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("filter")
                    && commandParser.getWord().toLowerCase().equals("disable")){
                if(!isEnabled())
                    return "Фильтр отправляемых сообщений уже выключён.\n" +
                            "Когда фильтр выключен, бот находится в опасности. Если кто-то захочет навредить боту, он " +
                            "может заставить его сказать что-то запрещённое и аккаунт могут заморозить.\n" +
                            "Рекоментую оставить фильтр включённым.";
                setEnabled(false);
                return "Фильтр отправляемых сообщений выключен. Теперь бот НЕ будет проверять " +
                        "отправляемые сообщения и цензурить запрещённые фразы. \n" +
                        "Это привести к заморозке аккаунта из-за отправки запрещённого текста.\n" +
                        "Рекоментую включить фильтр.";
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Выключить фильтр отправляемых сообщений",
                    "Существует много фраз, написав которые, страница пользователя замораживается автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Эта команда выключает этот фильтр. Рекомендуетсы не выключать фильтр.",
                    "botcmd filter enable"
            ));
            return result;
        }
    }
    class FilterTest extends CommandModule{
        public FilterTest(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("filter")
                    && commandParser.getWord().toLowerCase().equals("test")){


                String outputSymbols = filterSymbols(input);
                boolean hasSymbols = outputSymbols.equals(input);
                String outputLinks = filterLinks(input);
                boolean hasLinks = outputLinks.equals(input);
                String outputForbidden = filterForbidden(input);
                boolean hasForbidden = outputForbidden.equals(input);
                String finalResult = filterText(input);



                String result = "Результат тестирования фильтра:\n";
                if(hasSymbols)
                    result += "Строка содержит подозрительные символы.\n";
                if(hasLinks)
                    result += "Строка содержит ссылки.\n";
                if(hasForbidden)
                    result += "Строка содержит запрещённые фразы.\n";

                if(!hasForbidden && !hasLinks && !hasSymbols)
                    result += "В строке не найдено ничего запрещённого.\n";


                result += ".\n";
                result += "Результат этапов тестированя:\n";
                result += "------------------------\n";
                if(message.getSource() != MessageBase.SOURCE_PROGRAM)
                    result += "Оригинал фразы: [Отображается только если вызвать команду из интерфейса программы]\n";
                else
                    result += "Оригинал фразы: "+input+"\n";


                result += "------------------------\n";
                if(message.getSource() != MessageBase.SOURCE_PROGRAM)
                    result += "После фильтрации символов: [Отображается только если вызвать команду из интерфейса программы]\n";
                else
                    result += "После фильтрации символов: "+outputSymbols+"\n";


                result += "------------------------\n";
                if(message.getSource() != MessageBase.SOURCE_PROGRAM)
                    result += "После фильтрации ссылок: [Отображается только если вызвать команду из интерфейса программы]\n";
                else
                    result += "После фильтрации ссылок: "+outputLinks+"\n";


                result += "------------------------\n";
                if(message.getSource() != MessageBase.SOURCE_PROGRAM)
                    result += "После фильтрации запрещённых фраз: [Отображается только если вызвать команду из интерфейса программы]\n";
                else
                    result += "После фильтрации запрещённых фраз: "+outputLinks+"\n";

                result += "------------------------\n";
                result += "Конечный результат: " + finalResult + "\n";
                return result;
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Выполнить тестовую фильтрацию",
                    "Существует много фраз, написав которые, страница пользователя замораживается автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Эта команда позволяет проверить работу фильтра на тестовой фразе.\n" +
                            "Команда выводит результат работы отдельных этапов фильтрации сообщения перед отправкой.\n" +
                            "Будь осторожен вызывая эту команду из соцсети! Если ты напишешь в ней " +
                            "запрещённый текст, твой аккаунт могут заморозить!\n" +
                            "Будучи вызвана из соцвети команда сработает только частично, в целях безопасности.\n",
                    "botcmd filter test <Текст для проверки>"
            ));
            return result;
        }
    }
    class FilterDatabase extends CommandModule{
        public FilterDatabase(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("filter")
                    && commandParser.getWord().toLowerCase().equals("database")){
                //INIT
                String deleteString = commandParser.getWord();
                boolean isDelete = deleteString.trim().toLowerCase().equals("delete");
                ArrayList<AnswerMicroElement> strangeAnswers = new ArrayList<>();
                ArrayList<AnswerMicroElement> allAnswers = applicationManager.getBrain().getAnswerDatabase().getAnswers();

                //SEARCH FOR STRANGE ANSWERS
                for (AnswerMicroElement answer:allAnswers){
                    String before = answer.getAnswerText();
                    String after = filterForbidden(before);
                    if(!after.equals(before)){
                        strangeAnswers.add(answer);
                    }
                }

                //Make report
                String result = "Список вопросов, которые содержат запрещённый текст:\n";
                ArrayList<Long> toRemoveId = new ArrayList<>();
                for (AnswerMicroElement answer:strangeAnswers){
                    //// 228) привид как дила (1 фотографий) -> Привет, отлично! (+photo, photo)
                    result += answer.toString() + "\n";
                    toRemoveId.add(answer.getId());
                }
                if(!isDelete) {
                    result += "Ответы из базы не были удалены.\n";
                    return result;
                }
                result += "Удаление вопросов из базы включено.\n";
                try {
                    int count = applicationManager.getBrain().getAnswerDatabase().removeAnswer(toRemoveId);
                    result += "Из базы было удалено "+count+" ответов.\n";
                }
                catch (Exception e){
                    result += "Во время удаления вопросов из базы произошла ошибка: "+e.getMessage()+"\n";
                }
                return result;
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Выполнить фильтрацию базы ответов",
                    "Существует много фраз, написав которые, страница пользователя замораживается автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Эта команда позволяет проверить базу данных ответов на запрешённые фразы.\n" +
                            "Если команда вызвана без слова delete, из базы ничего не будет удалено, а сомнительные ответы " +
                            "только будут показаны тебе.\n" +
                            "Если вызвать команду со словом delete, ответы из базы удаляются.\n" +
                            "Эта команда не работает из соцсети, т.к. выводит запрещённые слова, которые могут " +
                            "стать причиной заморозки аккаунта.",
                    "botcmd filter database <delete?>"
            ));
            return result;
        }
    }
    class AddBlacklistWord extends CommandModule{
        public AddBlacklistWord(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("filter")
                    && commandParser.getWord().toLowerCase().equals("word")
                    && commandParser.getWord().toLowerCase().equals("add")){
                String word = commandParser.getText();
                if(word.length() == 0)
                    return "После команды через пробел надо написать слово, которое хочешь добавить как запрещённое.";
                if(word.length() < 3)
                    return "Слишком короткое слово. Такое слово будет вызывать частые ложные срабатывания.";
                try {
                    addFuckingWord(word);
                    String result = "Запрещённое слово добавлено. Теперь бот будет заменять это слово звёздочками в своих ответах, " +
                            "а человек, который заставил бота это написать, получит предупреждение.\n" +
                            "Сейчас в базе " + fuckingWords.size() + " запрещённых слов.\n" +
                            "Чтобы убедиться, что это слово не вызывает конфликтов в базе данных, воспользуйся командой " +
                            "botcmd filter AnalyzeDatabase, чтобы проверить, есть ли в твоей базе такие запрещённые слова.\n";
                    if(message.getSource() != MessageBase.SOURCE_PROGRAM)
                        result += "Будь очень осторожен работая с запрещёнными словами в соцсети! " +
                                "Твой аккаунт могут заморозить за то, что ты их пишешь.\n" +
                                "Работать с базой запрещённых рекомендуется только в интерфейсе программы.";
                    return result;
                }
                catch (Exception e){
                    log("! Ошибка добавления запрещённого слова: " + e.getMessage());
                    e.printStackTrace();
                    return "Ошибка добавления запрещённого слова: " + e.getMessage();
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Добавить слово в реестр запрещённых слов",
                    "Существует много фраз, написав которые, страница пользователя замораживается автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Эта команда позволяет добавить слово в список запрещённых фраз." +
                            "ВНИМАНИЕ!!! Будь осторожен выполняя эту команду из соцсети! " +
                            "Тебя, как отправителя запрещённого слова, тоже могут заморозить!",
                    "botcmd filter word add <Запрещённое слово или текст>"
            ));
            return result;
        }
    }
    class RemBlacklistWord extends CommandModule{
        public RemBlacklistWord(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("filter")
                    && commandParser.getWord().toLowerCase().equals("word")
                    && commandParser.getWord().toLowerCase().equals("rem")){
                String word = commandParser.getText();
                if(word.length() == 0)
                    return "После команды через пробел надо написать слово, которое хочешь удалить из списка запрещённых.";
                try {
                    remFuckingWord(word);
                    String result = "Запрещённое слово удалено. Теперь бот не будет заменять это слово звёздочками в своих ответах, " +
                            "и человек, который заставил бота это написать, не получит предупреждение.\n" +
                            "Сейчас в базе " + fuckingWords.size() + " запрещённых слов.\n";
                    if(message.getSource() != MessageBase.SOURCE_PROGRAM)
                        result += "Будь очень осторожен работая с запрещёнными словами в соцсети! " +
                                "Твой аккаунт могут заморозить за то, что ты их пишешь.\n" +
                                "Работать с базой запрещённых рекомендуется только в интерфейсе программы.";
                    return result;
                }
                catch (Exception e){
                    log("! Ошибка удаления запрещённого слова: " + e.getMessage());
                    e.printStackTrace();
                    return "Ошибка удаления запрещённого слова: " + e.getMessage();
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Удалить слово из реестра запрещённых слов",
                    "Существует много фраз, написав которые, страница пользователя замораживается автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Эта команда позволяет удалить слово из списка запрещённых фраз." +
                            "ВНИМАНИЕ!!! Будь осторожен выполняя эту команду из соцсети! " +
                            "Тебя, как отправителя запрещённого слова, тоже могут заморозить!",
                    "botcmd filter word rem <Запрещённое слово или текст>"
            ));
            return result;
        }
    }
    class GetBlacklistWord extends CommandModule{
        public GetBlacklistWord(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().toLowerCase().equals("filter")
                    && commandParser.getWord().toLowerCase().equals("word")
                    && commandParser.getWord().toLowerCase().equals("get")){
                if(message.getSource() != MessageBase.SOURCE_PROGRAM)
                    return "Эту команду нельзя выполнить из соцсети, поскольку она списком выводит слова, " +
                            "за которые блокируется аккаунт. И если её выполнить, аккаунт будет заблокирован.\n" +
                            "Эту команду можно выполнить только в интерфейсе программы.";

                String range = commandParser.getText();
                try {
                    ArrayList<Integer> list = F.parseRange(range, fuckingWords.size(), fuckingWords.size());
                    String result = "Список запрещённых слов: \n";

                    Iterator<Integer> iterator = list.iterator();
                    while (iterator.hasNext()){
                        int index = iterator.next();
                        String word = fuckingWords.get(index);
                        result += index + ") " + word + "\n";
                    }
                    return result;
                }
                catch (Exception e){
                    log("! Ошибка просмотра списка: " + e.getMessage());
                    e.printStackTrace();
                    return "Ошибка просмотра списка: " + e.getMessage();
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Просмотр списка запрещённых слов",
                    "Существует много фраз, написав которые, страница пользователя замораживается автоматически. " +
                            "Чтобы защититься от такой блокировки бота, предусмотрен фильтр, который цензурит " +
                            "опасные участки сообщений отправляемых ботом.\n" +
                            "Эта команда позволяет просмотреть список запрещённых фраз.\n" +
                            "Если диапазон не указан, будут выведены все слова.\n" +
                            "ВНИМАНИЕ!!! Эту команду НЕЛЬЗЯ вызывать из соцсети, её можно использовать только " +
                            "в интерфейсе программы, в целях безопасности.",
                    "botcmd filter word get <Перечень или диапазон индексов>"
            ));
            return result;
        }
    }
}
