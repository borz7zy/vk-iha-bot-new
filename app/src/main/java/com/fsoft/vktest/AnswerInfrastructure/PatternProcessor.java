package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.ResourceFileReader;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;

/*
 * Полностью переписываем шаблонизатор.
 * Теперь данные хранятся в JSON.
 * Стандартные примеры содержат много конструкций для примера.
 *
 * Created by Dr. Failov on 27.11.2017.
 */

/*
 * будет давать стандартные ответы на стандартные фразы
 * Created by Dr. Failov on 10.08.2014.
 */
public class PatternProcessor extends BotModule {
    private ArrayList<Pattern> patterns = new ArrayList<>();
    private File fileToSave = null;

    public PatternProcessor(ApplicationManager applicationManager) {
        super(applicationManager);
        this.applicationManager = applicationManager;
        fileToSave = new ResourceFileReader(applicationManager, R.raw.pattern_answers).getFile();

        readFromFile();

        childCommands.add(new Status(applicationManager));
        childCommands.add(new GetPatternizator(applicationManager));
        childCommands.add(new AddPatternizator(applicationManager));
        childCommands.add(new RemPatternizator(applicationManager));
        childCommands.add(new TestPatternizator(applicationManager));
        childCommands.add(new WhatIsPatternizator(applicationManager));
    }

    @Override
    public Message processMessage(Message message) {
        ArrayList<String> variants = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            String reply = patterns.get(i).processMessage(message.getText());
            if(reply != null)
                variants.add(reply);
        }
        if(variants.size() == 0)
            return super.processMessage(message);

        Random random = new Random();
        int index = random.nextInt(variants.size());
        message.setAnswer(new Answer(variants.get(index)));

        return message;
    }
    public void addPattern(Pattern pattern){
        patterns.add(pattern);
        writeToFile();
    }
    public boolean remPattern(Pattern pattern){
        boolean result = patterns.remove(pattern);
        writeToFile();
        return result;
    }
    public ArrayList<Pattern> getPatterns() {
        return patterns;
    }

    private void readFromFile(){
        log(". Загрузка шаблонизатора...");
        String fileText = F.readFromFile(fileToSave).trim();
        if(fileText == null) {
            log("! Ошибка прочтения файла шаблонизатора: " + fileToSave);
            return;
        }
        String[] lines = fileText.split("\n");
        for (String line:lines) {
            try {
                if (line == null || line.equals("")) {
                    log("! Пропуск некорректной строки шаблонизатора");
                    continue;
                }
                JSONObject jsonObject = new JSONObject(line);
                patterns.add(new Pattern(jsonObject));
            }
            catch (Exception e){
                e.printStackTrace();
                log("! Ошибка загрузки шаблона шаблонизатора: " + line);
            }
        }
        log(". Данные шаблонизатора загружены: " + patterns.size() + " шаблонов.");
    }
    private void writeToFile(){
        log(". Сохранение шаблонизатора...");
        try {
            PrintWriter fileTmpWriter = new PrintWriter(fileToSave);

            for (Pattern pattern:patterns)
                fileTmpWriter.println(pattern.toJson().toString());

            fileTmpWriter.close();
            log(". Сохранение данных шаблонизатора (" + patterns.size() + " шаблонов) выполнено в " + fileToSave);
        }
        catch (Exception e){
            e.printStackTrace();
            log(". Ошибка сохранения данных шаблонизатора в файл " + fileToSave + "\n" + e.getMessage());
        }
    }

    private class Status implements Command{
        @Override
        public String getHelp() {
            return "";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status"))
                return "Количество шаблонов шаблонизатора: "+patterns.size()+"\n" +
                        "Расположение файла базы данных шаблонизатора: "+ resourceFileReader.getFilePath()+"\n";
            return "";
        }
    }
    private class Save implements Command{
        @Override
        public String getHelp() {
            return "";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("save"))
                return save();
            return "";
        }
    }
    private class WhatIsPatternizator implements Command{
        @Override
        public String getHelp() {
            return "[ Что такое \"Шаблонизатор\"? ]\n" +
                    "---| botcmd whatispatternizator\n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("whatispatternizator"))
                return "Шаблонизатор - один из мощнейших модулей бота. Это модуль бота, занимающийся ответами на сообщения, соответствующими регулярному выражению вне зависимости от наличия обращения. " +
                        "Может быть полезен для того, чтобы внести ответы на конкретные фразы, которые не содержат обращения.\n" +
                        "Шаблонизатор может хранить неограниченное количество пар выражение - ответ.\n" +
                        "По умолчанию в бота уже добавлены некоторые ответы, список которых можно увидеть командой getpatternizator." +
                        "Можете проверить: на эти фразы бот ответит, даже если кто-либо напишет их без обращения.\n" +
                        "Добавить шаблон ответа можно командой addpatternizator. Для добавления ответа Вам понадобится регулярное выражение и текст ответа, который должен написать бот.\n" +
                        "Регулярные выражения(regexp) - это стандарт, позволяющий формулировать сложные фильтры поиска. Про него можно прочитать много где. Есть даже специальные сайты для их создания.\n" +
                        "Задача регулярного выражения в паре выражение-ответ: определить, НА ЧТО бот должен ответить. Т.е., если принятое сообщение соответствует (matches) регурярному выражению - бот отправляет ответ.\n" +
                        "Если в базе бота имеется несколько ответов на одну фразу, ответ будет подобран случайным образом.\n" +
                        "Если ответ на фразу сожержит фрагменты [0], [1], [2] ...и т.д., они будут заменены на соответствующие группы из выражения. Поддерживается до 50 групп. " +
                        "Про группы также можно почитать во многих информационных ресурсах, т.к. они тоже являются частью стандарта.\n" +
                        "Стоит обратить внимание, что в Java есть некоторые особенности в обработке регулярных выражений, поэтому для их тестирования предусмотрена функция TestPatternizator, " +
                        "принимающая сообщение и регулярное выражение и говорящая о том, соответствует ли сообщение регулярному выражению и какие группы оно содержит.\n\n" +
                        "\n" +
                        "ПРИМЕР 1:\n" +
                        "Ответ: Это на красной площади.\n" +
                        "Регулярка: Где находится Мавзолей\\?\n" +
                        "> Реакция бота: \"Где находится Мавзолей?\" -> \"Это на красной площади.\"\n" +
                        "> Реакция бота: \"Где находится Мавзолей\" -> не ответит\n" +
                        "> Реакция бота: \"Бот, Где находится Мавзолей?\" -> не ответит\n" +
                        "> Реакция бота: \"где находится Мавзолей?\" -> не ответит\n" +
                        "> Реакция бота: \"Где находится мавзолей?\" -> не ответит\n" +
                        "> Реакция бота: \"Где Мавзолей?\" -> не ответит\n" +
                        "> Реакция бота: \"Где находится завод?\" -> не ответит\n" +
                        "\n" +
                        "ПРИМЕР 2:\n" +
                        "Ответ: Пускай [1] сам себе покупает.\n" +
                        "Регулярка: Купи (.+)(новый)? телефон[\\?\\!]\n" +
                        "> Реакция бота: \"Купи Вите новый телефон!\" -> \"Пускай Вите сам себе покупает.\"\n" +
                        "> Реакция бота: \"Купи саше новый телефон!\" -> \"Пускай саше сам себе покупает.\"\n" +
                        "> Реакция бота: \"Купи мне новый телефон!\" -> \"Пускай мне сам себе покупает.\"\n" +
                        "> Реакция бота: \"Купи Вите новый телефон?\" -> \"Пускай Вите сам себе покупает.\"\n" +
                        "> Реакция бота: \"Купи Вите телефон!\" -> \"Пускай Вите сам себе покупает.\"\n" +
                        "> Реакция бота: \"купи Вите новый телефон!\" -> не ответит\n" +
                        "> Реакция бота: \"Купи Вите новый ТЕЛЕФОН!\" -> не ответит\n" +
                        "> Реакция бота: \"Купи Вите новый телефон\" -> не ответит\n" +
                        "> Реакция бота: \"Купи Вите новый телефон!!\" -> не ответит\n" +
                        "> Реакция бота: \"Бот, Купи Вите новый телефон!\" -> не ответит\n" +
                        "> Реакция бота: \"Купи телефон!\" -> не ответит\n" +
                        "> Реакция бота: \"Купи Вите телефон новый!\" -> не ответит\n";
            return "";
        }
    }
    private class GetPatternizator implements Command{
        @Override
        public String getHelp() {
            return "[ Получить список шаблонов шаблонизатора ]\n" +
                    "[ так же сработает gpt ]\n" +
                    "---| botcmd getpatternizator\n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("getpatternizator") || word.equals("gpt")) {
                String result = "Список шаблонов шаблонизатора: \n\n";
                for (int i = 0; i < patterns.size(); i++) {
                    result += i + ") " +patterns.get(i).serialize() + "\n\n";
                }
                return result;
            }
            return "";
        }
    }
    private class AddPatternizator implements Command{
        @Override
        public String getHelp() {
            return "[ Добавить шаблон шаблонизатора ]\n" +
                    "[ так же сработает apt ]\n" +
                    "---| botcmd addpatternizator <текст ответа>*<регулярное выражение>\n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("addpatternizator") || word.equals("apt")) {
                String lastText = commandParser.getText();
                if(lastText.equals(""))
                    return "Ошибка внесения шаблона: не получен текст шаблона.\n" + getHelp();

                java.util.regex.Pattern pat = java.util.regex.Pattern.compile("([^\\*]+)\\*(.+)");
                Matcher matcher = pat.matcher(lastText);
                String answer = null, pattern = null;
                try {
                    if (matcher.find()) {
                        answer = matcher.group(1);
                        pattern = matcher.group(2);
                    }
                }
                catch (Exception e){
                    return "Ошибка внесения шаблона: недостаточно аргументов. Вы не забыли звездочку?!\n" + getHelp();
                }
                if(pattern != null && answer != null) {
                    Pattern newPat = new Pattern(pattern, answer);
                    patterns.add(newPat);
                    return "Шаблон добавлен: " + newPat.serialize();
                }
                else {
                    return "Ошибка внесения шаблона: не получен текст шаблона.\n" + getHelp();
                }
            }
            return "";
        }
    }
    private class TestPatternizator implements Command{
        @Override
        public String getHelp() {
            return "[ Проверить регулярное выражение на соответствие тексту ]\n" +
                    "[ так же сработает tpt ]\n" +
                    "---| botcmd testpatternizator <текст для теста>*<регулярное выражение>\n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("testpatternizator") || word.equals("tpt")) {
                String lastText = commandParser.getText();
                if(lastText.equals(""))
                    return "Ошибка теста шаблона: не получен текст шаблона.\n";

                String text = null, pattern = null;
                {
                    java.util.regex.Pattern pat = java.util.regex.Pattern.compile("([^\\*]+)\\*(.+)");
                    Matcher matcher = pat.matcher(lastText);
                    try {
                        if (matcher.find()) {
                            text = matcher.group(1);
                            pattern = matcher.group(2);
                        }
                    } catch (Exception e) {
                        return "Ошибка теста шаблона: недостаточно аргументов. Вы не забыли звездочку?!\n" + getHelp();
                    }
                }
                if(pattern != null && text != null) {
                    java.util.regex.Pattern pat = java.util.regex.Pattern.compile(pattern);
                    Matcher matcher = pat.matcher(text);
                    String result = "--- Результат: \n";
                    result += "- Текст: " + text + "\n";
                    result += "- Регулярка: " + pattern + "\n";
                    result += "- Результат matches(): " + text.matches(pattern) + "\n";
                    result += "--- Найденные группы: \n";
                    int search = 0;
                    while (matcher.find()){
                        result += "\n- find() - " + search + ":\n";
                        search ++;
                        for (int i=0; i<100; i++){
                            try{
                                result += "group["+i+"] " + matcher.group(i) + "\n";
                            }
                            catch (Exception e){
                                break;
                            }
                        }
                    }
                    result += "\n- Конец поиска.\n";

                    return result;
                }
                else {
                    return "Ошибка теста шаблона: не получен текст.\n" + getHelp();
                }
            }
            return "";
        }
    }
    private class RemPatternizator implements Command{
        @Override
        public String getHelp() {
            return "[ Удалить шаблон шаблонизатора ]\n" +
                    "[ так же сработает rpt ]\n" +
                    "---| botcmd rempatternizator <порядковый номер элемента>\n\n";
        }

        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            String word = commandParser.getWord();
            if(word.equals("rempatternizator") || word.equals("rpt")) {
                int id = commandParser.getInt();
                if(id < 0 || id >= patterns.size())
                    return "Ошибка удаления шаблона "+id+": такого нет.\n" + getHelp();
                Pattern deleted = patterns.remove(id);
                return "Шаблон удален: " + deleted.serialize() + "\n";
            }
            return "";
        }
    }

    private class Pattern{
        private String answer = null;
        private String pattern = null;

        Pattern (String pattern, String answer){
            this.answer = answer;
            this.pattern = pattern;
        }
        Pattern (JSONObject jsonObject) throws Exception{
            fromJson(jsonObject);
        }
        public void fromJson(JSONObject jsonObject) throws Exception {
            pattern = jsonObject.getString("pattern");
            answer = jsonObject.getString("answer");
        }
        public JSONObject toJson() throws Exception{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("pattern", pattern);
            jsonObject.put("answer", answer);
            return jsonObject;
        }

        @Override
        public String toString() {
            return pattern + " -> " + answer;
        }

        //null = не подходит
        //пустота - ответить пустотой
        //ответ - ответить
        public String processMessage(String mes){
            try {
                if(pattern == null || pattern.equals("") || answer == null)
                    return null;
                if(!mes.toLowerCase().matches(pattern.toLowerCase()))
                    return null;

                String toReturn = answer;
                java.util.regex.Pattern pat = java.util.regex.Pattern.compile(pattern);
                Matcher matcher = pat.matcher(mes);
                if (matcher.find()) {
                    for (int i = 0; i < 50; i++) {
                        try {
                            String part = matcher.group(i);
                            toReturn = toReturn.replace("$"+(i+1), (part == null ? "" : part));
                        } catch (Exception e) {
                            break;
                        }
                    }
                }

                return toReturn;
            }
            catch (Exception e){
                e.printStackTrace();
                log(e.toString() + "\n" +
                        "Предположительно, Вы допустили ошибку в регулярном выражении шаблонизатора: \n" +
                        pattern + " --> " + answer + " \n" +
                        "Удалите шаблон и исправьте ошибку.");
                if(applicationManager.getActivity() != null)
                    applicationManager.getActivity().showMessage(e.toString() + "\n" +
                            "Предположительно, Вы допустили ошибку в регулярном выражении шаблонизатора: \n" +
                            pattern + " --> " + answer + " \n" +
                            "Удалите шаблон и исправьте ошибку.");
            }
            return null;
        }
    }
}
