package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.ResourceFileReader;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;

/**
 * будет давать стандартные ответы на стандартные фразы
 * Created by Dr. Failov on 10.08.2014.
 */
public class PatternProcessor implements Command {
    ApplicationManager applicationManager = null;
    String name;
    ArrayList<Pattern> patterns;
    ResourceFileReader resourceFileReader;
    ArrayList<Command> commands;

    public PatternProcessor(ApplicationManager applicationManager, String name, int defaultPatterns) {
        this.applicationManager = applicationManager;
        this.name = name;
        this.applicationManager = applicationManager;
        resourceFileReader = new ResourceFileReader(applicationManager.activity.getResources(), defaultPatterns, name);
        patterns = new ArrayList<>();
        commands = new ArrayList<>();
        commands.add(new Status());
        commands.add(new Save());
        commands.add(new GetPatternizator());
        commands.add(new AddPatternizator());
        commands.add(new RemPatternizator());
        commands.add(new TestPatternizator());
        commands.add(new WhatIsPatternizator());
    }
    public String processMessage(String text, Long senderId) {
        ArrayList<String> variants = new ArrayList<>();
        for (int i = 0; i < patterns.size(); i++) {
            String reply = patterns.get(i).process(text);
            if(reply != null)
                variants.add( reply);
        }
        if(variants.size() == 0)
            return null;
        return variants.get(new Random().nextInt(variants.size()));
    }
    public void load() {
        log(". Загрузка шаблонизатора...");
        String fileText = resourceFileReader.readFile();
        if(fileText == null) {
            log("! Ошибка прочтения файла шаблонизатора: " + resourceFileReader.getFilePath());
            return;
        }
        String[] lines = fileText.split("\\\n");
        for (int i = 0; i < lines.length; i++) {
            if(lines[i] != null && !lines[i].equals(""))
                patterns.add(new Pattern(lines[i]));
        }
        log(". Данные шаблонизатора загружены: " + patterns.size() + " шаблонов.");
    }
    public void close() {
        //save();
    }
    String save(){
        log(". Сохранение шаблонизатора...");
        if(patterns.size() == 0) {
            log( "! Сохранение шаблонизатора невозможно: база пустая.");
            return "Сохранение шаблонизатора невозможно: база пустая.\n";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < patterns.size(); i++) {
            stringBuilder.append(patterns.get(i).serialize());
            if(i<patterns.size()-1)
                stringBuilder.append("\n");
        }
        boolean result = resourceFileReader.writeFile(stringBuilder.toString());
        if(result) {
            log(". Сохранение данных шаблонизатора (" + patterns.size() + " шаблонов) выполнено в " + resourceFileReader.getFilePath());
            return ". Сохранение данных шаблонизатора (" + patterns.size() + " шаблонов) выполнено в " + resourceFileReader.getFilePath() + "\n";
        }
        else{
            log(". Ошибка сохранения данных шаблонизатора в файл " + resourceFileReader.getFilePath());
            return "! Ошибка сохранения данных шаблонизатора в файл " + resourceFileReader.getFilePath() + "\n";
        }
    }
    public @Override String getHelp() {
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).getHelp();
        }
        return result;
    }
    public @Override String process(String text, Long senderId) {
        String result =  "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).processCommand(text, senderId);
        }
        return result;
    }
    private void log(String text){
        ApplicationManager.log(text);
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
        String answer = "";
        String pattern = "";
        String preparedPattern = null;
        Pattern (String pattern, String answer){
            this.answer = answer;
            this.pattern = pattern;
        }
        Pattern (String serializable){
            try {
                JSONObject jsonObject = new JSONObject(serializable);
                pattern = jsonObject.getString("pattern");
                answer = jsonObject.getString("answer");
            }
            catch (Exception e){
                if(!serializable.contains(".*")) {
                    String[] part = serializable.split("\\*");
                    if (part.length >= 2) {
                        pattern = ".*" + part[0] + ".*";
                        answer = part[1];
                    }
                }
            }
        }
        String process(String mes){
            try {
                if (isIt(mes)) {
                    String toReturn = answer;

                    java.util.regex.Pattern pat = java.util.regex.Pattern.compile(pattern);
                    Matcher matcher = pat.matcher(mes);
                    if (matcher.find()) {
                        for (int i = 0; i < 50; i++) {
                            try {
                                String part = matcher.group(i);
                                toReturn = toReturn.replace("[" + i + "]", (part == null ? "" : part));
                            } catch (Exception e) {
                                break;
                            }
                        }
                    }

                    return toReturn;
                }
            }
            catch (Exception e){
                applicationManager.activity.messageBox(e.toString() + "\nПредположительно, Вы допустили ошибку в регулярном выражении шаблонизатора: \n" + pattern + " --> " + answer + " \nУдалите шаблон и исправьте ошибку.");
            }
            return null;
        }
        boolean isIt(String mes){
            if(pattern == null || pattern.equals(""))
                return false;
            return mes.toLowerCase().matches(pattern.toLowerCase());
        }
        String serialize(){
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("pattern", pattern);
                jsonObject.put("answer", answer);
                return jsonObject.toString();
            }
            catch (Exception e){
                return "";
            }
        }
    }
}
