package com.fsoft.vktest.AnswerInfrastructure.MessageComparison;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.FileStorage;

import java.util.ArrayList;

/**
 * Этот класс занимается подготовкой сообщения для сравнения
 * Created by Dr. Failov on 25.08.2014.
 */

/**
 * Это всё полная хуйня.
 * Как сравнивать сообщения:
 * - сообщение разделяется на слова
 * - слова разделяются на пары букв
 * - сравнивается наличие соответствующих пар букв каждого слова с кажным
 * - результат слова - максимальное соотношение в другой фразе
 * - результат соотношения множится на соотношение количества букв между словами
 * - Результат фразы - сумма соотношения всех букв.
 *
 * Перед сравнением сначала каждая строчка проходит подготовку.
 * То есть:
 * float compare(PreparedString text1, PreparedString text2);
 * Created by Dr. Failov on 13.03.2017.
 */
/**
 * Этот класс занимается тем, что подготавливает текст для сравнения алгоритмом Джаро-Винклера
 * В его задачи входит:
 * - привести всё к нижнему реристру
 * - выполнить транслитерацию латиницы на кириллицу
 * - заменить все слова их синонимами (если есть)
 * - избавиться от всего кроме кириллицы цифр и знака вопроса (в том числе и от пробелов)
 * - заменить некоторые символы их эквивалентами
 * - удалить повторы любых символов
 *
 * На выходе будет получено что-то типа такого:
 * Вход: Здравствуй, как дела?
 * Вызод: привидкокдило?
 *
 * Created by Dr. Failov on 30.04.2017.
 */
public class MessagePreparer extends CommandModule{
    private SynonimousProvider synonimousProvider = null;

    public MessagePreparer(ApplicationManager applicationManager) {
        super(applicationManager);
        synonimousProvider = new SynonimousProvider(applicationManager);
        childCommands.add(synonimousProvider);
        childCommands.add(new Prepare(applicationManager));
    }
    public String prepare(String in){
        in = in.toLowerCase();
        in = removeTextInBrackets(in);
        in = in.replace('!', ' ')
                .replace(',', ' ')
                .replace('-', ' ')
                .replace('@', ' ')
                .replace('\\', ' ')
                .replace('/', ' ')
                .replace('_', ' ')
                .replace('(', ' ')
                .replace(')', ' ');
        in = F.filterSymbols(in, ".? qwertyuiopasdfghjklzxcvbnm їіёйцукенгшщзхъфывапролджэячсмитьбю 1234567890");
        in = removeRepeatingSymbols(in);
        in = in.trim();
        in = passOnlyLastSentence(in);
        in = in.trim();
        in = synonimousProvider.process(in);
        in = replacePhoneticallySimilarLetters(in);
        return in;
    }

    private String removeTextInBrackets(String in){
        // Hello [Anatoly|228] how are you? -> Hello  how are you?
        if(!in.contains("[") || !in.contains("[") || !in.contains("|"))
            return in;
        int cnt = 0;
        StringBuilder stringBuffer = new StringBuilder("");
        for(int i=0; i<in.length(); i++){
            char c = in.charAt(i);
            if(c == '[') cnt++;
            if(cnt == 0)
                stringBuffer.append(c);
            if(c == ']') cnt --;
        }
        return stringBuffer.toString();
    }
    private String passOnlyLastSentence(String in){
        if(!in.contains("."))
            return in;
        String[] sentences = in.split("\\.");
        for(int i=sentences.length-1; i>=0; i--)
            if(sentences[i].length() > 3)
                return sentences[i];
        //если не было найдено ни одного нормального предложения
        return in;
    }
    private String replacePhoneticallySimilarLetters(String in) {
        String result = in;
        result = result.replace('о', 'а');
        result = result.replace('й', 'и');
        result = result.replace('е', 'и');
        result = result.replace('ё', 'и');
        result = result.replace('ы', 'и');
        result = result.replace('і', 'и');
        result = result.replace('ї', 'и');
        result = result.replace('э', 'и');
        result = result.replace('т', 'д');
        result = result.replace('з', 'с');
        result = result.replace('ц', 'с');
        result = result.replace('ф', 'в');
        result = result.replace('щ', 'ш');
        result = result.replace('б', 'п');
        result = result.replace('г', 'х');
        result = result.replace('ъ', 'ь');

        result = result.replace('b', 'p');
        result = result.replace('d', 't');
        result = result.replace('i', 'e');
        result = result.replace('f', 'h');
        result = result.replace('g', 'j');
        result = result.replace('u', 'o');
        result = result.replace('w', 'v');
        return result;
    }
    private String removeRepeatingSymbols(String in){
        Character last = null;
        String result = "";
        for (Character c : in.toCharArray()) {
            if (c.equals(last)) {
                continue;
            }
            result = result.concat(c.toString());
            last = c;
        }
        return result;
    }

    private class Prepare extends CommandModule{
        public Prepare(ApplicationManager applicationManager) {
            super(applicationManager);
        }
        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("prepare")){
                String text = commandParser.getText();
                if(text.equals(""))
                    return "! Не получено сообщение для подготовки. Напишите текст сообщения после команды prepare.";
                long start = System.nanoTime();
                String prepared = prepare(text);
                long dt = System.nanoTime() - start;
                return "Текст до подготовки: "+text+"\n" +
                        "Текст после подготовки: " + prepared + "\n" +
                        "Время выполнения: " + (0.001d * dt) + " мс.\n";
            }
            return "";
        }
        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Подготовить сообщение к сравнению",
                    "Внутри программы перед сравнением сообщения с образцами из базы каждое сообщение " +
                            "проходит этам подготовки, в котором заменяются синонимы, удаляются лишние " +
                            "(в большинстве случаев) фрагменты, слова преобразуются фонетически.\n" +
                            "Такая подготовка нужна для того, чтобы бот умел более точно подбирать ответ на " +
                            "вопросы написанные с незначительными ошибками, опечатками, и т.д.\n" +
                            "Эта команда позволяет посмотреть, как преобразуется текст внутри программы " +
                            "и поможет понять насколько похожими являются фразы внутри программы.\n" +
                            "Также эта команда может помочь оценить время выполнения команды " +
                            "подготовки на твоем устройстве.",
                    "botcmd prepare <сообщение>"));
            return result;
        }
    }
}