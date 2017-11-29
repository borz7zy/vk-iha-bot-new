package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
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

    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().toLowerCase().equals("status") || message.getText().toLowerCase().equals("patternizator status"))
                return "Количество шаблонов шаблонизатора: "+patterns.size()+"\n";
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            //// TODO: 28.11.2017 маловато тут всего
            result.add(new CommandDesc(
                    "Состояние шаблонизатора",
                    "Отображает состояние шаблонизатора",
                    "botcmd patternizator status"));
            return result;
        }
    }
    private class WhatIsPatternizator extends CommandModule{
        public WhatIsPatternizator(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Что такое \"Шаблонизатор\"?",
                    "Шаблонизатор отвечает по шаблонам. " +
                            "Но не всегда понятно что это значит. " +
                            "Эта команда отобразит подробную инструкцию модуля, " +
                            "чтобы было проще разобраться.",
                    "botcmd whatispatternizator"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().toLowerCase().equals("whatispatternizator"))
                return "Шаблонизатор - один из мощнейших модулей бота. Это модуль бота, занимающийся ответами на сообщения, соответствующими регулярному выражению вне зависимости от наличия обращения. " +
                        "Может быть полезен для того, чтобы внести ответы на конкретные фразы, которые не содержат обращения.\n" +
                        "Шаблонизатор может хранить неограниченное количество пар выражение - ответ.\n" +
                        "По умолчанию в бота уже добавлены некоторые ответы, список которых можно увидеть командой getpatternizator." +
                        "Задача этих шаблонов состоит в том, чтобы их можно было использовать в качестве примера" +
                        "Можете проверить: на эти фразы бот ответит, даже если кто-либо напишет их без обращения.\n" +
                        "Добавить шаблон ответа можно командой addpatternizator(apt). Для добавления ответа Вам понадобится регулярное выражение и текст ответа, который должен написать бот.\n" +
                        "Регулярные выражения(Регулярки, RegExp) - это широко используемый стандарт, " +
                        "позволяющий формулировать сложные фильтры поиска. " +
                        "Про него можно прочитать много где. Есть даже специальные сайты для их создания.\n" +
                        "Задача регулярного выражения в паре выражение-ответ: определить, НА ЧТО бот должен ответить. " +
                        "Т.е., если принятое сообщение соответствует (matches) регурярному выражению - бот отправляет ответ.\n" +
                        "Если в базе бота имеется несколько ответов которым соответствует вопрос, ответ будет выбран " +
                        "случайным образом среди тех ответов, которые подходят.\n" +
                        "Регулярные выражения могут содержать в себе подгруппы, которые при подборе ответа могут " +
                        "использоваться в частях ответа. Такие группы в регулярке прячутся в круглые скобки. " +
                        "Пример регулярки: \"Сегодня было ([0-9]) клиентов\" -> $1 = число в [0-9].\n" +
                        "Если ответ на фразу сожержит фрагменты $1, $2, $3 ...и т.д., они будут заменены на соответствующие группы из выражения. " +
                        "Группы Поддерживается до 10 групп. Нумерация групп начинается с 1.\n" +
                        "Про группы также можно почитать во многих информационных ресурсах, т.к. они тоже являются частью стандарта.\n" +
                        "Стоит обратить внимание, что в Java есть некоторые особенности в обработке регулярных выражений, поэтому для их тестирования предусмотрена функция TestPatternizator(tpt), " +
                        "принимающая сообщение и регулярное выражение и говорящая о том, соответствует ли сообщение регулярному выражению и какие группы оно содержит.\n\n" +
                        ".\n" +
                        "ПРИМЕР 1:\n" +
                        "Регулярка: Где наход[ия]тся ([а-яА-Я]+)\\?    \n" +
                        "Ответ: Скорее всего, $1 где-то недалеко.   \n" +
                        "> Фрагмент [ия] означает, что на этом месте может быть либо буква И либо буква Я. " +
                        "Таким образом бот будет отвечать и на \"находится\" и на \"находятся\".\n" +
                        "> Фрагмент ([а-яА-Я]+) находится в круглых скобках для того, чтобы слово, которое будет там находится " +
                        "можно было использовать в ответе по ключу $1.\n" +
                        "> Фрагмент [а-яА-Я]+ означает несколько (знак +) букв от а до я либо от А до Я. " +
                        "Без знака + был бы только один символ.\n" +
                        ".\n" +
                        "> Реакция бота: \"Где находится Мавзолей?\" -> \"Скорее всего, Мавзолей где-то недалеко.\"\n" +
                        "> Реакция бота: \"Где находится Мавзолей\" -> не ответит. Знак вопроса прописан в регулярке, " +
                        "без него она уже не срабатывает.\n" +
                        "> Реакция бота: \"Бот, Где находится Мавзолей?\" -> не ответит. Обращение в регулярке не прописано.\n" +
                        "> Реакция бота: \"где находится Мавзолей?\" -> не ответит. Регулярки регистрочувствительны.\n" +
                        "> Реакция бота: \"Где находится мавзолей?\" -> Скорее всего, мавзолей где-то недалеко.\n" +
                        "> Реакция бота: \"Где Мавзолей?\" -> не ответит.\n" +
                        "> Реакция бота: \"Где находится завод?\" -> Скорее всего, завод где-то недалеко.\n" +
                        "> Реакция бота: \"Где находятся негры?\" -> Скорее всего, негры где-то недалеко.\n" +
                        ".\n" +
                        "ПРИМЕР 2:\n" +
                        "Регулярка: Купи ([А-Я][а-я]+) (новый )?телефон[\\?\\!]\n" +
                        "Ответ: Пускай $1 сам себе покупает.\n" +
                        "> Фрагмент ([А-Я][а-я]+) находится в скобках, чтобы использовать слово в ответе как $1.\n" +
                        "> Фрагмент [А-Я] означает одну любую большую букву кириллицы от А до Я. \n" +
                        "> Фрагмент [а-я] означает одну маленькую букву кириллицы от а до я. \n" +
                        "> Фрагмент [а-я]+ означает несколько маленьких букв кириллицы от а до я. \n" +
                        "> Фрагмент [А-Я][а-я]+ означает слово кириллицы с большой буквы. \n" +
                        "> Фрагмент (новый )? означает что слово \"новый\" может в быть а может и не быть " +
                        "(за это отвечает знак вопроса). " +
                        "Скобки нужны для того, чтобы задать условие \"может и не быть\" для всего слова целиком. " +
                        "Эти скобки, конечно, можно использовать в ответе как $2 (вторые по порядку). " +
                        "Однако когда этого слова не будет, тогда и $2 не будет. Надо быть с этим осторожным.\n" +
                        "> Фрагмент [\\?\\!] означает либо знак вопроса либо знак восклицания. \n" +
                        ".\n" +
                        "> Реакция бота: \"Купи Вите новый телефон!\" -> \"Пускай Вите сам себе покупает.\"\n" +
                        "> Реакция бота: \"Купи саше новый телефон!\" -> не ответит. Слово должно быть с большой буквы.\n" +
                        "> Реакция бота: \"Купи мне новый телефон!\" -> не ответит. Слово должно быть с большой буквы.\n" +
                        "> Реакция бота: \"Купи Мне новый телефон!\" -> \"Пускай Мне сам себе покупает.\"\n" +
                        "> Реакция бота: \"Купи Вите новый телефон?\" -> \"Пускай Вите сам себе покупает.\"\n" +
                        "> Реакция бота: \"Купи Вите телефон!\" -> \"Пускай Вите сам себе покупает.\"\n" +
                        "> Реакция бота: \"купи Вите новый телефон!\" -> не ответит. В регулярке слово написано с большой буквы.\n" +
                        "> Реакция бота: \"Купи Вите новый телефон\" -> не ответит. В конце знак вопроса или восклицания должны быть.\n" +
                        "> Реакция бота: \"Купи Вите новый телефон!!\" -> не ответит. Сообще не целиком подходит под регулярку.\n" +
                        "> Реакция бота: \"Бот, Купи Вите новый телефон!\" -> не ответит. Обращение в регулярке не указано.\n" +
                        "> Реакция бота: \"Купи телефон!\" -> не ответит. Фраза совсем не соответствует регулярке.\n" +
                        "> Реакция бота: \"Купи А телефон!\" -> не ответит. Нет малых букв.\n" +
                        "> Реакция бота: \"Купи Аа телефон!\" -> \"Пускай Аа сам себе покупает.\".\n" +
                        "> Реакция бота: \"Купи ААа телефон!\" -> не ответит. Согласно регулярке, большая буква только одна.\n" +
                        "> Реакция бота: \"Купи Вите телефон новый!\" -> не ответит. Последовательность слов не та.\n" +
                        ".\n" +
                        "Надеюсь, эта инструкция была хотя бы немного полезна. " +
                        "Помни, эта инструкция - лишь моя попытка описать сложную тему просто. " +
                        "Если надо больше информации, всегда есть Google. Тема регулярных выражений (RegExp) там широко представлена " +
                        "в виде мануалов, ответов пользователей, советов, статей.";
            return "";
        }
    }
    private class GetPatternizator extends CommandModule{
        public GetPatternizator(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().toLowerCase().equals("getpatternizator") || message.getText().toLowerCase().equals("gpt")) {
                String result = "Список шаблонов шаблонизатора: \n";
                for (int i = 0; i < patterns.size(); i++) {
                    result += i + ") " +patterns.get(i) + "\n" +
                            ".\n";
                }
                return result;
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Получить список шаблонов шаблонизатора",
                    "Шаблонизатор отвечает фразой из ответа, если регулярное выражение выполняется.\n" +
                            "Шаблоны шаблонизатора ты можешь добавить командой AddPatternizator.\n" +
                            "Изначально в программу добавлено несколько шаблонов для примера, " +
                            "чтобы было проще разобраться как они работают.\n" +
                            "У этой команды есть сокращённый вариант вызова: bcd gpt.",
                    "botcmd GetPatternizator"));
            return result;
        }
    }
    private class AddPatternizator extends CommandModule{
        public AddPatternizator(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String word = commandParser.getWord();
            if(word.toLowerCase().equals("addpatternizator") || word.toLowerCase().equals("apt")) {
                String lastText = commandParser.getText();
                if(lastText.equals(""))
                    return "Ошибка внесения шаблона: не получен текст шаблона.\n"
                            + F.commandDescsToText(getHelp());

                java.util.regex.Pattern pat = java.util.regex.Pattern.compile("([^*]+)\\*(.+)");
                Matcher matcher = pat.matcher(lastText);
                String answer = null, pattern = null;
                try {
                    if (matcher.find()) {
                        answer = matcher.group(1);
                        pattern = matcher.group(2);
                    }
                }
                catch (Exception e){
                    return "Ошибка внесения шаблона: недостаточно аргументов. Вы не забыли звездочку?!\n"
                            + F.commandDescsToText(getHelp());
                }
                if(pattern != null && answer != null) {
                    Pattern newPat = new Pattern(pattern, answer);
                    patterns.add(newPat);
                    return "Шаблон добавлен: " + newPat.toString();
                }
                else {
                    return "Ошибка внесения шаблона: не получен текст шаблона.\n"
                            + F.commandDescsToText(getHelp());
                }
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Добавить шаблон шаблонизатора",
                    "Шаблонизатор отвечает фразой из ответа, если регулярное выражение выполняется.\n" +
                            "Подробнее о том что такое регулярное выражение ты можешь почитать " +
                            "в команде WhatIsPatternizator.\n" +
                            "Текст ответа не может содержать звёздочку, т.к. она является разделителем аргументов.\n" +
                            "У этой команды есть сокращённый вариант вызова: bcd apt.",
                    "botcmd AddPatternizator <Текст ответа>*<Регулярное выражение>"));
            return result;
        }
    }
    private class RemPatternizator extends CommandModule{
        public RemPatternizator(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String word = commandParser.getWord();
            if(word.toLowerCase().equals("rempatternizator") || word.toLowerCase().equals("rpt")) {
                int id = commandParser.getInt();
                if(id < 0 || id >= patterns.size())
                    return "Ошибка удаления шаблона "+id+": такого нет.\n"
                            + F.commandDescsToText(getHelp());
                Pattern deleted = patterns.remove(id);
                return "Шаблон удален: " + deleted.toString() + "\n";
            }
            return "";
        }


        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить шаблон шаблонизатора",
                            "Порядковый номер можно узнать командой GetPatternizator.\n" +
                                    "У этой команды есть сокращённый вариант вызова: bcd rpt.",
                    "botcmd RemPatternizator <Порядковый номер элемента>"));
            return result;
        }
    }
    private class TestPatternizator extends CommandModule{
        public TestPatternizator(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            String word = commandParser.getWord();
            if(word.toLowerCase().equals("testpatternizator") || word.toLowerCase().equals("tpt")) {
                String lastText = commandParser.getText();
                if(lastText.equals(""))
                    return "Ошибка теста шаблона: не получен текст шаблона.\n"
                            + F.commandDescsToText(getHelp());

                String text = null, pattern = null;
                {
                    java.util.regex.Pattern pat = java.util.regex.Pattern.compile("([^*]+)\\*(.+)");
                    Matcher matcher = pat.matcher(lastText);
                    try {
                        if (matcher.find()) {
                            text = matcher.group(1);
                            pattern = matcher.group(2);
                        }
                    } catch (Exception e) {
                        return "Ошибка теста шаблона: недостаточно аргументов. Вы не забыли звездочку?!\n"
                                + F.commandDescsToText(getHelp());
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
                                result += "$"+i+" = " + matcher.group(i) + "\n";
                            }
                            catch (Exception e){
                                break;
                            }
                        }
                    }
                    //result += "\n- Конец поиска.\n";

                    return result;
                }
                else {
                    return "Ошибка теста шаблона: не получен текст.\n"
                            + F.commandDescsToText(getHelp());
                }
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Проверить регулярное выражение на соответствие тексту",
                    "Поскольку в Java механизм регулярных выражений отличается от того, который применяется в других языках, " +
                            "может потребоваться проверить, правильно ли была написана регулярка.\n" +
                            "Эта команда позволяет проверить соотвествует ли текст регулярному выражению " +
                            "и какие аргументы достаются из этой регулярки.\n" +
                            "У этой команды есть сокращённый вариант вызова: bcd tpt.",
                    "botcmd TestPatternizator <текст>*<регулярное выражение>"));
            return result;
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
