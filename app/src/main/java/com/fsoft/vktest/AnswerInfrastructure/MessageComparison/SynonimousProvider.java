package com.fsoft.vktest.AnswerInfrastructure.MessageComparison;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.ResourceFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Провайдер синонимов содержит скопы. Скопы содержат слова.
 * Один скоп - это последовательность слов которые являются синонимами.
 *
 * Как заменить всё на синонимы:
 * - Входную фразу приводим в нижний регистр
 * - Во входной фразе удаляем запятые, смайлики. Оставляем только кириллицу,латиницу, цифры, знаки вопроса и пробелы
 * - Входную фразу делим на слова
 * - каждое слово сравниваем с скопом.
 * - Внутри скопа сравниваем с каждым словом. Если такое слово есть - заменяем его на синоним.
 * - Собираем все слова вместе снова в строку
 * - Делаем замену " ?" на "?"
 *
 * Что должен уметь провайдер синонимов:
 * - Работать с огромными обьемами синонимов
 * - Вывести количество рядов и слов в рядах
 * - показать все ряды (только первые слова из ряда) (просмотреть базу синонимов)
 * - показать содержимое ряда
 * - добавить пару синонимов (если он уже где-то содержится то дополнить ряд)
 * Created by Dr. Failov on 30.04.2017.
 */


//// TODO: 11.05.2017 использовать новый словарь, когда всё будет готово

public class SynonimousProvider extends CommandModule {
    private ArrayList<Scope> scopes = new ArrayList<>();
    private File file = null;

    public SynonimousProvider(ApplicationManager applicationManager) {
        super(applicationManager);
        try {
            file = new ResourceFileReader(applicationManager, R.raw.synonimous).getFile();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber = 0;
            int errors = 0;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber ++;
                try {
                    scopes.add(new Scope(line));
                }
                catch (Exception e){
                    e.printStackTrace();
                    errors ++;
                    log("! Ошибка разбора строки " + lineNumber + " как синонима. " + e.getMessage());
                }
            }
            bufferedReader.close();
            log(". Загружено " + scopes.size() + " синонимических рядов.");
            if(errors != 0)
                log("! При загрузке базы синонимов возникло ошибок: " + errors + ".");
            childCommands.add(new Status(applicationManager));
            childCommands.add(new AddSyn(applicationManager));
            childCommands.add(new GetSyn(applicationManager));
            childCommands.add(new RemScope(applicationManager));
            childCommands.add(new RemSyn(applicationManager));
            childCommands.add(new GetScope(applicationManager));
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка загрузки модуля работы с синонимами: " + e.getMessage());
        }
    }
    public String process(String text){
        //эта функция должна заменить все слова в предложении их синонимами если они есть
        text = prepareText(text);
        String[] words = text.split(" ");
        for (int i = 0; i < words.length; i++)
            for(Scope scope:scopes)
                words[i] = scope.processWord(words[i]);
        text = F.arrayToString(words);
        text = text.replace(" ?", "?").trim();
        return text;
    }

    private Scope addSyn(String baseSyn, String newSyn) throws Exception{
        //эта функция возвращает тот скоп в который она добавила синоним
        if(baseSyn == null || newSyn == null || baseSyn.equals("") || newSyn.equals("")){
            log("! Ошибка добавления синонима: аргументы пусты.");
            throw new Exception("Агументы пусты.");
        }
        if(baseSyn.contains(" ") || newSyn.contains(" ")){
            log("! Ошибка добавления синонима: Синонимы - это слова, и они не должны содержать пробелов.");
            throw new Exception("Синонимы - это слова, и они не должны содержать пробелов.");
        }
        newSyn = prepareText(newSyn);
        baseSyn = prepareText(baseSyn);
        if(baseSyn.equals("") || newSyn.equals("")){
            log("! Ошибка добавления синонима: аргументы состоят из недопустимых символов.");
            throw new Exception("Агументы состоят из недопустимых символов.");
        }
        if(baseSyn.equals(newSyn)){
            log("! Ошибка добавления синонима: Аргументы одинаковые.");
            throw new Exception("Аргументы одинаковые.");
        }
        for (Scope scope:scopes) {
            boolean containsBase = scope.getWords().contains(baseSyn);
            boolean containsNew = scope.getWords().contains(newSyn);
            if(containsBase && containsNew) {
                log("! Ошибка добавления синонима: Эти слова уже являются синонимами.");
                throw new Exception("Эти слова уже являются синонимами.");
            }
            String synToAdd = null;
            if(containsBase)
                synToAdd = newSyn;
            if(containsNew)
                synToAdd = baseSyn;
            if(synToAdd != null){
                scope.words.add(synToAdd);
                writeToFile();
                return scope;
            }
        }
        Scope scope = new Scope(baseSyn + " " + newSyn);
        scopes.add(scope);
        writeToFile();
        return scope;
    }
    private String prepareText(String text){
        //Эта функция вызывается перед сравнением синонимов
        text = text.toLowerCase();
        String allowedSymbols = "qwertyuiopasdfghjklzxcvbnmйцукенгшщзхъфывапролджэячсмитьбюії12345667890 ?";
        text = F.filterSymbols(text, allowedSymbols);
        text = text.replace("?", " ?").replaceAll(" +", " ").trim();
        return text;
    }
    private void writeToFile(){
        log(". Сохранение базы синонимов...");
        if(scopes.size() == 0) {
            log( "! Сохранение синонимов невозможно: база пустая.");
            return;
        }
        try {
            PrintWriter fileWriter = new PrintWriter(file);
            for (Scope scope:scopes)
                fileWriter.println(scope.toLine());
            fileWriter.close();
            log(". База синонимов сохранена: " + scopes.size() + " рядов.");
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка сохранения "+scopes.size()+" рядов синонимов в " + file + ": " + e.toString());
        }
    }
    private int countSyns(){
        int cnt = 0;
        for (Scope scope:scopes)
            cnt += scope.getWords().size();
        return cnt;
    }

    class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().equals("status") || message.getText().equals("syn status"))
                return "Всего синонимических рядов: " + scopes.size() + "\n" +
                        "Всего слов-синонимов: " + countSyns() + "\n" ;
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    class AddSyn extends CommandModule{
        public AddSyn(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("addsyn")){
                if(commandParser.wordsRemaining() != 2)
                    return log("! Для добавления синонима требуется два слова: базовое слово и его синоним. Писать их надо через пробел.");
                String base = commandParser.getWord();
                String nw = commandParser.getWord();
                try {
                    Scope scope = addSyn(base, nw);
                    return "Синоним добавлен в ряд к " + scope.getMainSyn() + ".";
                }
                catch (Exception e) {
                    return log("! Ошибка добавления синонима: " + e.getMessage());
                }
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Добавить слово как синоним",
                    "Синонимы - слова, похожие по смыслу.\n" +
                            "Во время общения по базе бот будет отвечать на синонимы в вопросах одинаково. " +
                            "Вместо того, чтобы дублировать ответы в базе, можно просто добавить синонимы.",
                    "botcmd AddSyn <Слово>*<Синоним>"));
            return result;
        }
    }
    class GetSyn extends CommandModule{
        public GetSyn(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("getsyn")){
                int total = scopes.size();
                if(total == 0)
                    return "! В базе нет синонимов.";
                String range = commandParser.getText();
                ArrayList<Integer> list;
                try {
                     list = F.parseRange(range, total, 50);
                }
                catch (Exception e){
                    return "! Неправильный диапазон: " + e.getMessage();
                }

                String result = "";
                result += "Список рядов синонимов: \n";
                result += "номер | главный синоним\n";
                result += "------------------------\n";
                for (Integer number:list) {
                    result += number + " | " + scopes.get(number).getMainSyn() + "\n";
                }
                result += "------------------------\n";
                result += "Всего синонимов: " + total + "\n";
                return result;
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Получить список синонимических рядов из базы\n",
                    "Синонимы - слова, похожие по смыслу.\n" +
                    "Во время общения по базе бот будет отвечать на синонимы в вопросах одинаково. " +
                    "Вместо того, чтобы дублировать ответы в базе, можно просто добавить синонимы. \n\n" +
                    "Для просмотра можно использовать любые диапазоны чисел. Например:\n" +
                    "1 ::: Только первый объект;\n" +
                    "12-20 ::: Объекты с 12-го по 20-й;\n" +
                    "15- ::: С 20-го и до конца;\n" +
                    "-22 ::: От начала до 22-го;\n" +
                    "1,5,7 ::: первый, пятый и седьмой;\n" +
                    "1,5,12-15 ::: первый, пятый и с 12-го по 15-й;\n" +
                    "-10,86,90 ::: первые 10, 86-й и 90-й;",
                    "botcmd GetSyn <диапазон>"));
            return result;
        }
    }
    class RemScope extends CommandModule{
        public RemScope(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("remscope")){
                int total = scopes.size();
                if(total == 0)
                    return "! В базе нет синонимов.";
                String range = commandParser.getText();
                ArrayList<Integer> list;
                try {
                    list = F.parseRange(range, total, 50);
                }
                catch (Exception e){
                    return "! Неправильный диапазон: " + e.getMessage();
                }

                ArrayList<Scope> objects = new ArrayList<>();
                for (Integer number:list)
                    objects.add(scopes.get(number));

                int success = 0;
                for(Scope scope:objects)
                    if(scopes.remove(scope))
                        success++;

                writeToFile();
                return "Готово, удалено синонимических рядов: " + success + ".";
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить синонимические ряды из базы",
                    "Для удаления можно использовать любые диапазоны чисел. Например:\n" +
                    "1 ::: Только первый объект;\n" +
                    "12-20 ::: Объекты с 12-го по 20-й;\n" +
                    "15- ::: С 20-го и до конца;\n" +
                    "-22 ::: От начала до 22-го;\n" +
                    "1,5,7 ::: первый, пятый и седьмой;\n" +
                    "1,5,12-15 ::: первый, пятый и с 12-го по 15-й;\n" +
                    "-10,86,90 ::: первые 10, 86-й и 90-й;",
                    "botcmd RemScope <диапазон>"));
            return result;
        }
    }
    class RemSyn extends CommandModule{
        public RemSyn(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("remsyn")){
                int total = scopes.size();
                if(total == 0)
                    return "! В базе нет синонимов.";
                int scopeNumber = commandParser.getInt();
                if(scopeNumber < 0)
                    return "! Номер ряда не может быть отрицательный.";
                if(scopeNumber >= total)
                    return "! В базе всего "+total+" рядов.";
                Scope scope = scopes.get(scopeNumber);
                if(scope == null)
                    return "! Не могу получить ряд под номером " + scopeNumber + ".";
                int wordNumber = commandParser.getInt();
                if(wordNumber < 0)
                    return "! Номер слова не может быть отрицательный.";
                int wordsTotal = scope.getWords().size();
                if(wordNumber >= wordsTotal)
                    return "! В ряду "+scopeNumber+" всего "+wordsTotal+" слов.";
                String removedWord = scope.getWords().remove(wordNumber);
                if(removedWord != null){
                    writeToFile();
                    return "Слово " + removedWord + " успешно удалено из ряда " + scope.toLine();
                }
                else {
                    return "!Не могу удалить слово " + wordNumber + " из ряда " + scopeNumber + ".";
                }
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить слово из синонимического ряда",
                    "Если в ряду более 2х синонимов, то один из них можно убрать, оставив все остальные.\n" +
                            "Номер ряда можно получить командой GetSyn, номер слова можно получить командой GetScope.\n" +
                            "Нумерация слов в ряду начинается с нуля.",
                    "botcmd RemSyn <номер ряда> <номер слова>"));
            return result;
        }
    }
    class GetScope extends CommandModule{
        public GetScope(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("getscope")){
                int total = scopes.size();
                if(total == 0)
                    return "! В базе нет синонимов.";
                int number = commandParser.getInt();
                if(number < 0)
                    return "! Отрицательного номера нет.";
                if(number >= total)
                    return "! В базе всего "+total+" рядов.";

                Scope scope = scopes.get(number);
                return "Ряд с номером " + number + ":\n" + scope.toLine();
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Получить список синонимов в ряду\n",
                    "Номер по порядку можно получить командой GetSyn.",
                    "botcmd GetScope <номер по порядку>"));
            return result;
        }
    }

    private class Scope{
        private ArrayList<String> words;
        private String separator = " ";

        public Scope(String scope) throws Exception{
            if(!scope.contains(separator))
                throw new Exception("Строка должна содержать минимум 2 синонима разделенных пробелами: \n\""+scope+"\"");
            words = new ArrayList<>();
            String[] splitted = scope.split(separator);
            for(int i=0; i<splitted.length; i++)
                words.add(splitted[i]);
        }
        public String processWord(String in){
            //если слово содержится как синоним, вернуть главный
            for (int i = 1; i < words.size(); i++)
                if(words.get(i).equals(in))
                    return getMainSyn();
            return in;
        }
        public String toLine(){
            String result = "";
            for (int i = 0; i < words.size(); i++) {
                result += words.get(i);
                if(i<words.size()-1)
                    result += separator;
            }
            return result;
        }
        public String getMainSyn(){
            if(words.isEmpty())
                return "";
            return words.get(0);
        }
        public ArrayList<String> getWords() {
            return words;
        }
    }
}
