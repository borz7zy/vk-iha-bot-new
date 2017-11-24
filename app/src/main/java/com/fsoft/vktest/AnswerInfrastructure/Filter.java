package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.SynonimousProvider;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Modules.HttpServer;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.ResourceFileReader;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.Parameters;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
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
 * классу через ApplicationManager
 * Created by Dr. Failov on 30.03.2017.
 */
public class Filter extends BotModule{
    private HashMap<Long, Integer> warnings = new HashMap<>();
    private ArrayList<String> fuckingWords = null;
    private String allowedSymbols = null;
    private String[] allowedWords = {
            "vk.com",
            "com.fsoft",
            "perm"
    };
    private FileStorage storage = null;

    public Filter(ApplicationManager applicationManager) {
        super(applicationManager);
        //READ BLACKLIST
        try {
            File blacklistFile = new ResourceFileReader(applicationManager, R.raw.synonimous).getFile();
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

    @Override
    public Message processMessage(Message message) {
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
        if(applicationManager.)
        return super.processMessage(message);
    }

    public String processMessag(Message message){

        if(in != null && isFilterOn() && !isAllowed(sender) && sender != HttpServer.USER_ID && !teachId.contains(sender)) { //надо ли фильтровать
            String out = in;
            out = passOnlyAllowedSymbols(out);
            out = out.replace(".", ". ");
            out = out.replace("&#", " ");
            out = out.replace(". . . ", "...");
            out = out.replace("vk. com", "vk.com");
            if(!isAllowedSymbol(out, true)){
                //log("! Система защиты: Исходное сообщение: " + out);
                applicationManager.messageBox("Сообщение для " + sender + " ("+applicationManager.vkCommunicator.getUserName(sender)+") опасно.\n" +
                        "--------------\n" +
                        out + "\n" +
                        "--------------\n" +
                        securityReport);
                String warningMessage = Parameters.get("security_warning_message","\nСистема защиты: ваше поведение сомнительно. \n" +
                        "Если это не так, сообщите подробности разработчику.\n" +
                        "Вы получаете предупреждение:", "Текст который получит пользователь если в ответ на его сообщение бот отправит опасную фразу");
                if(warnings.containsKey(sender)){
                    int currentWarnings = warnings.get(sender);
                    currentWarnings ++;
                    warnings.put(sender, currentWarnings);
                    if(currentWarnings >= Parameters.get("security_warning_count", 3, "Количество предупреждений о попытке сломать бота до момента автоматического бана.")){
                        String result = applicationManager.processCommands("ignor add " + sender + " подозрительное поведение", applicationManager.getUserID());
                        out = Parameters.get("security_banned_message", "Ваша страница заблокирована: RESULT",
                                "Сообщение, которое получит пользователь когда он будет заблокирован за попытку сломать бота.").replace("RESULT", result);
                    }
                    else {
                        out = warningMessage + currentWarnings + ".";
                    }
                }
                else {
                    warnings.put(sender, 1);
                    out = warningMessage + "1.";
                }
            }
            out = out.trim();
            return out;
        }
        return in;
    }
    public boolean isAllowedSymbol(String out, boolean deep){
        String tmp = prepareToFilter(out);
        loadWords();
        securityReport = "";
        boolean warning = false;
        for (int i = 0; i < fuckingWords.size(); i++) {
            if(tmp.contains(fuckingWords.get(i))) {
                securityReport += log("! Система защиты: обнаружен подозрительный фрагмент: " + fuckingWords.get(i)) + "\n";
                warning = true;
            }
        }
        if(!warning)
            securityReport = ". Угроз не обнаружено.";
        return !warning;
    }
    public @Override String process(String input, Long senderId) {
        CommandParser commandParser = new CommandParser(input);
        switch (commandParser.getWord()) {
            case "status":
                return "Фильтр сомнительного содержания включён: " + isFilterOn() + "\n"+
                        "Шаблонов черного списка: "+(fuckingWords == null?"еще не загружено":fuckingWords.size())+"\n"+
                        "Разрешенных символов: "+(allowedSymbols == null?"еще не загружено":allowedSymbols.length())+"\n"+
                        "Пользователей получили предупреждения: "+ warnings.size() +"\n";
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
                    case "reset": {
                        Long id = applicationManager.getUserID(commandParser.getWord());
                        return "Счетчик сброшен для пользователя " + id + " : " + warnings.remove(id);
                    }
                    case "set": {
                        Long id = applicationManager.getUserID(commandParser.getWord());
                        int num = commandParser.getInt();
                        warnings.put(id, num);
                        return "Счетчик для пользователя " + id + " : " + num;
                    }
                }
            case "enablefilter":
                boolean newState = commandParser.getBoolean();
                boolean oldState = isFilterOn();
                storage.put("enableFilter", newState);
                storage.commit();
                return "Фильтр симнительного содержания.\nБыло: " + oldState + "\nСтало: " + newState;
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

    private boolean isFilterOn(){
        return storage.getBoolean("enableFilter", true);
    }
    private String prepareToFilter(String in){
        String tmp = in.toLowerCase();
        for (int i = 0; i < allowedWords.length; i++)
            tmp = tmp.replace(allowedWords[i], "");
        tmp = replaceTheSameSymbols(tmp);
        //составить список разрешенных символов а все остальные удалить нах
        String allowed = "qwertyuiopasdfghjklzxcvbnmйцукенгшщзхъфывапролджэячсмитьбюієё1234567890";
        for (int i = 0; i < tmp.length(); i++) {
            char c = tmp.charAt(i);
            //проверить есть ли этот символ в списке разрешенных
            boolean isAllowed = false;
            for (int j = 0; j < allowed.length(); j++) {
                char ca = allowed.charAt(j);
                if(c == ca)
                    isAllowed = true;
            }
            if(!isAllowed)
                tmp = tmp.replace(c, ' ');
        }
        //заменить пробелы
        tmp = tmp.replace(" ", "");
        return tmp;
    }
    private String replaceTheSameSymbols(String in){
        String out = in;
        out = out.replace("й", "i");
        out = out.replace("ц", "c");
        out = out.replace("у", "y");
        out = out.replace("к", "k");
        out = out.replace("е", "e");
        out = out.replace("г", "g");
        out = out.replace("ш", "sh");
        out = out.replace("щ", "sch");
        out = out.replace("ъ", "");
        out = out.replace("ф", "f");
        out = out.replace("ы", "y");
        out = out.replace("в", "v");
        out = out.replace("а", "a");
        out = out.replace("п", "p");
        out = out.replace("л", "l");
        out = out.replace("д", "d");
        out = out.replace("ж", "z");
        out = out.replace("э", "e");
        out = out.replace("я", "ya");
        out = out.replace("ч", "ch");
        out = out.replace("т", "t");
        out = out.replace("ь", "i");
        out = out.replace("ю", "y");

        out = out.replace("у", "y");
        out = out.replace("к", "k");
        out = out.replace("е", "e");
        out = out.replace("н", "h");
        out = out.replace("з", "3");
        out = out.replace("х", "x");
        out = out.replace("в", "v");
        out = out.replace("б", "b");
        out = out.replace("а", "a");
        out = out.replace("р", "r");
        out = out.replace("о", "o");
        out = out.replace("с", "c");
        out = out.replace("м", "m");
        out = out.replace("и", "n");
        out = out.replace("т", "t");
        out = out.replace("і", "i");
        //out = out.replace("я", "r");
        return out;
    }
    private void loadWords(){
        if(fuckingWords == null){
        }
    }
    private String save(){
        String result = "";
        if(fuckingWords.size() > 1){
            result += log(". Сохранение Черный спискок в " + blacklistResourceFileReader.getFilePath() + "...\n");
            try {
                FileWriter fileWriter = new FileWriter(blacklistResourceFileReader.getFile());
                for (int i = 0; i < fuckingWords.size(); i++) {
                    fileWriter.append("|");
                    fileWriter.append(fuckingWords.get(i));
                    fileWriter.append("|");
                    if(i < fuckingWords.size() -1)
                        fileWriter.append("\n");
                }
                fileWriter.close();
                result += log(". Черный спискок Сохранено " + fuckingWords.size() + " слов.\n");
            }
            catch (Exception e){
                result += log("! Сохранить базу Черный спискок в " + blacklistResourceFileReader.getFilePath() + " не удалось, и вот почему: "+e.toString()+"\n");
            }
        }
        else
            result += log("! База Черный спискок пуста. Не сохранять.\n");
        return result;
    }
    private void loadSymbols(){
        if(allowedSymbols == null){
            ResourceFileReader resourceFileReader = new ResourceFileReader(applicationManager.activity.getResources(), R.raw.allowed_symbols, name);
            allowedSymbols = resourceFileReader.readFile();
            log(". Разрешенные символы: загружено " + allowedSymbols.length() + " символов.");
        }
    }
    private boolean isAllowedByServer(String text){
        if(text == null)
            return true;
        String encodedText = encodeURIcomponent(text).toUpperCase();
        if(encodedText.replace("%20", "").equals(""))
            return true;
        String address = "http://filyus.ru/verbal.hasBadLinks?q=" + encodedText;
        log("ADDRESS = " + address);
        HttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpGet = new HttpGet(address);
            HttpResponse response = httpclient.execute(httpGet);
            String result = EntityUtils.toString(response.getEntity());
            log("RESULT = " + result);
            JSONObject jsonObject = new JSONObject(result);
            int bad = jsonObject.getInt("response");
            return !(bad == 1);
        }
        catch (Throwable e){
            e.printStackTrace();
            log("! Error while filtering: " + e.toString());
        }
        return true;
    }
    private String encodeURIcomponent(String s){
        /** Converts a string into something you can safely insert into a URL. */
        StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (isUnsafe(ch)) {
                if(ch == ' ') {
                    o.append('%');
                    o.append(toHex(ch / 16));
                    o.append(toHex(ch % 16));
                }
            }
            else o.append(ch);
        }
        return o.toString();
    }
    private char toHex(int ch) {
        return (char)(ch < 10 ? '0' + ch : 'A' + ch - 10);
    }
    private boolean isUnsafe(char ch) {
        return " qwertyuiopasdfghjklzxcvbnm1234567890QWERTYUIOPASDFGHJKLZXCVBNM".indexOf(ch) < 0;
    }
}
