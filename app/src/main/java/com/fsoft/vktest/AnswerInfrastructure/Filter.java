package com.fsoft.vktest.AnswerInfrastructure;

import android.util.Log;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private HashMap<Long, Integer> warnings = new HashMap<>();
    private ArrayList<String> fuckingWords = null;
    private String allowedSymbols = null;
    private FileStorage storage = null;
    private boolean enabled = true;
    //// TODO: 01.12.2017 проверка наличия запрещённого слова
    //// TODO: 01.12.2017 добавление запрещённых
    //// TODO: 01.12.2017 удаление запрещённых

    public Filter(ApplicationManager applicationManager) {
        super(applicationManager);
        storage = new FileStorage("FilterSettings", applicationManager);
        enabled = storage.getBoolean("enabled", enabled);
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
        //READ SYMBOLS
        File fileWithSymbols = new ResourceFileReader(applicationManager, R.raw.allowed_symbols).getFile();
        allowedSymbols = F.readFromFile(fileWithSymbols);
        log(". Разрешенные символы: загружено " + allowedSymbols.length() + " символов.");
        //READ WARNINGS
        try {
            JSONArray warningsJsonArray = storage.getJsonArray("warnings", new JSONArray());
            warnings = F.hashMapFromJsonArray(warningsJsonArray);
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка загрузки списка предупреждений: " + e.getMessage());
        }


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
        message.getAnswer().text = F.filterSymbols(message.getAnswer().text, allowedSymbols);



        //после каждой точки поставить по пробелу. Так убиваем ссылки
        message.getAnswer().text = message.getAnswer().text.replace(".", ". ");
        message.getAnswer().text = message.getAnswer().text.replaceAll(" +", " ");
        //восстанавливаем троеточие
        message.getAnswer().text = message.getAnswer().text.replace(". . .", "...");
        //ВК реагирует на спецграфику в кодах, типа &#228. Это её убивает
        message.getAnswer().text = message.getAnswer().text.replace("&#", " ");
        //восстанавливаем ссылки на ВК - они разрешены
        message.getAnswer().text = message.getAnswer().text.replace("vk. com", "vk.com");


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


    public String filterForbidden(String input){
        /*
        * -> botcmd, bro, nark, blue
        * -> The quick blue nark jumps over lazy bro.
        * --> The quick **** **** jumps over lazy ***.
        *
        * */
        //заменить запрещённые фразы звёздочками
        String buffer = "";
        for(int i=0; i<input.length(); i++){
            char c = prepareChar(input.charAt(i));
            buffer += c;
            String prohibited = containsProhibitedWord(buffer);
            if(prohibited != null){
                buffer = replaceProhibitedWord(buffer, prohibited, i);
                input = replaceProhibitedWord(input,   prohibited, i);
            }
        }
        return input;
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
    private int addWarnings(long userId){
        int cnt = 1;
        if(warnings.containsKey(userId)){
            cnt += warnings.get(userId);
        }
        warnings.put(userId, cnt);
        try {
            JSONArray warningsJsonArray = F.hashMapToJsonArray(warnings);
            storage.put("warnings", warningsJsonArray);
            storage.commit();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка сохранения предупреждения для пользователя: " + userId + "     " +e.getMessage() +"!");
        }
        return cnt;
    }
    private int resetWarnings(long userId){
        int result = 0;
        if(warnings.containsKey(userId)){
            result = warnings.remove(userId);
        }
        try {
            JSONArray warningsJsonArray = F.hashMapToJsonArray(warnings);
            storage.put("warnings", warningsJsonArray);
            storage.commit();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка сохранения предупреждения для пользователя: " + userId + "     " +e.getMessage() +"!");
        }
        return result;
    }
    private int getWarnings(long userId){
        int cnt = 0;
        if(warnings.containsKey(userId)){
            cnt = warnings.get(userId);
        }
        return cnt;
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
            return super.getHelp();
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
                Long id = applicationManager.getCommunicator().getActiveAccount().resolveScreenName(commandParser.getWord());
                int oldValue = resetWarnings(id);
                String userName =  applicationManager.getCommunicator().getActiveAccount().getUserName(id);
                String result = "Счетчик сброшен для пользователя " + userName + " ("+id+"). Было " + oldValue + " предупреждений.";
                boolean isIgnored = applicationManager.getBrain().getIgnor().has(message.getAuthor());
                if(isIgnored)
                    result += " Обрати внимание, пользователь всё ещё находится в игноре.";
                return result;
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
                    "botcmd filter status"
            ));
            return super.getHelp();
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
            return super.getHelp();
        }
    }

    //=============================================================================================



    public @Override String process(String input, Long senderId) {
        CommandParser commandParser = new CommandParser(input);
        switch (commandParser.getWord()) {
            case "warning":
                switch (commandParser.getWord()){
                    case "get":
                        String result = "Счетчик предупреждений:\n";
                        Iterator<Map.Entry<Long, Integer>> iterator = warnings.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<Long, Integer> cur = iterator.next();
                            result += "- Пользователь vk.com/id" + cur.getKey() + " получил " + cur.getValue() + " предупреждений.\n";
                        }
                        return result;
                    case "set": {
                        Long id = applicationManager.getUserID(commandParser.getWord());
                        int num = commandParser.getInt();
                        warnings.put(id, num);
                        return "Счетчик для пользователя " + id + " : " + num;
                    }
                }
            case "addblacklistword":
                String word = commandParser.getText();
                word = (word).toLowerCase().replace("|", "");
                word = replaceTheSameSymbols(word);
                fuckingWords.add(word);
                return "Добавлено слово в черный список слов: "+word+" \n" + save();
        }
        return "";
    }
    public @Override String getHelp() {
        return "[ Сбросить значение счетчика предупреждений для пользователя ]\n" +
                "---| botcmd warning reset <id пользователя>\n\n"+
                "[ Получить значения счетчика предупреждений ]\n" +
                "---| botcmd warning get\n\n"+
                "[ Задать значение счетчика предупреждений для пользователя ]\n" +
                "---| botcmd warning set <id пользователя> <новое значение счетчика>\n\n"+
                "[ Включить или выключить фильтр сомнительного содержания ]\n" +
                "[ Это тот фильтр, который пишет \"ваше поведение сомнительно.\" ]\n" +
                "[ Этот фильтр отключать не рекомендуется, т.к. без него Ваш ВК аккаунт могут заблокировать за рассылку подозрительных сообщений ]\n" +
                "---| botcmd enablefilter <on/off>\n\n"+
                "[ Добавить слово в реестр запрещённых слов ]\n" +
                "---| botcmd addblacklistword <|слово которое добавить|>\n\n";
    }

}
