package com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.JaroWinkler;
import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.MessagePreparer;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.Parameters;
import com.perm.kate.api.Attachment;
import com.perm.kate.api.Document;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * этот класс будет заниматься хранением неизвестных ответов
 *
 * Что надо делать с неизвестными:
 * + Добавлять
 *      - Проверять сколько их сейчас
 *      - Проверять нету ли в базе чего-то очень похожего
 * + Обновлять статус имеющихся неизвестных, менять коэффициенты
 * - Доставать по одному
 * - Отсортировать, получить топ неизвестных
 * - Очищать базу
 * Created by Dr. Failov on 28.04.2017.
 */
public class UnknownMessagesDatabase extends CommandModule {
    private File file = null;
    private ArrayList<UnknownMessage> unknownMessages = new ArrayList<>();
    private int limit = 5000;
    private MessagePreparer preparer = null;
    private JaroWinkler jaroWinkler = null;

    public UnknownMessagesDatabase(ApplicationManager applicationManager) {
        super(applicationManager);
        preparer = new MessagePreparer(applicationManager);
        jaroWinkler = new JaroWinkler();
        childCommands.add(new Status(applicationManager));
        childCommands.add(new TopUnknown(applicationManager));
        childCommands.add(new ClearUnknown(applicationManager));
        childCommands.add(new DumpUnknownMessages(applicationManager));
        try {
            limit = applicationManager.getParameters().get("unknown_limit",
                    limit,
                    "Лимит неизвестных",
                    "Максимальное количество неизвестных сообщений, которые будут сохраняться. " +
                            "Больше этого лимита неизвестные добавляться не будут. " +
                            "Если установить слишком большой лимит, со временем может потребоваться больше оперативной памяти, " +
                            "что может привести к ошибкам, если память устройства закончится.");
            file = new File(applicationManager.getHomeFolder(), "Unknown.txt");
            if(!file.exists())
                return;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber = 0;
            int errors = 0;
            while ((line = bufferedReader.readLine()) != null) {
                lineNumber ++;
                try {
                    UnknownMessage unknownMessage = new UnknownMessage(new JSONObject(line));
                    unknownMessage.setPreparedText(preparer.prepare(unknownMessage.getText()));
                    unknownMessages.add(unknownMessage);
                }
                catch (Exception e){
                    e.printStackTrace();
                    errors ++;
                    log("! Ошибка разбора строки " + lineNumber + " как неизвестного. " + e.getMessage());
                }
            }
            bufferedReader.close();
            log(". Загружено " + unknownMessages.size() + " неизвестных сообщений.");
            if(errors != 0)
                log("! При загрузке базы неизвестных возникло ошибок: " + errors + ".");
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка загрузки модуля работы с неизвестными!\n" + e.toString());
        }
    }
    public void add(final UnknownMessage unknownMessage){
        //// В этом месте нужно проверять есть ли в базе неизвестных уже такой ответ и если такого нету, добавлять.
        //делать это надо асинхронно!
        new Thread(new Runnable() {
            @Override
            public void run() {
                UnknownMessage theSame = null;
                //если частота больше 0, значит, это не новое неизвестное, а какое-то вернулось с обучения
                if(unknownMessage.getFrequency() == 0)
                    theSame = findTheSame(unknownMessage.getText());
                if(theSame == null) {
                    if(unknownMessages.size() < limit){
                        log(". Найдено новое неизвестное, добавление в базу...");
                        unknownMessages.add(unknownMessage);
                    }
                    else {
                        log(". Найдено новое неизвестное, но лимит не позволяет его добавить.");
                        return;
                    }
                }
                else {
                    log(". Найдено повтор неизвестного, обновление счётчика повторов...");
                    theSame.setFrequency(theSame.getFrequency() + 1);
                }
                writeToFile();
            }
        }).start();
    }
    public UnknownMessage[] getTopQuestions(int howMuch){
        UnknownMessage[] topElements = new UnknownMessage[howMuch];

        for (UnknownMessage unknownMessage:unknownMessages){
            if(topElements[topElements.length-1] == null ||
                    topElements[topElements.length-1].getFrequency() < unknownMessage.getFrequency()){
                //добавить в топ
                for (int i = 0; i < topElements.length; i++) {
                    if(topElements[i].getFrequency() < unknownMessage.getFrequency()){
                        //выполнить сдвиг
                        for (int j = topElements.length-1; j > i; j--)
                            topElements[j] = topElements[j-1];
                        //записать элемент в топ
                        topElements[i] = unknownMessage;
                        //выйти из цикла
                        break;
                    }
                }
            }
        }

        return topElements;
    }
    public UnknownMessage popTop(){
        //получить самый актуальный неизвестный и удалить его из базы
        // Полезно будет при обучении
        UnknownMessage[] top = getTopQuestions(5);
        unknownMessages.remove(top[0]);
        writeToFile();
        return top[0];
    }
    public UnknownMessage[] popTops(){
        //получить самые актуальные неизвестные и удалить их из базы
        // Полезно будет при обучении
        UnknownMessage[] top = getTopQuestions(5);
        for(UnknownMessage unknownMessage:top)
            unknownMessages.remove(unknownMessage);
        writeToFile();
        return top;
    }


    private UnknownMessage findTheSame(String text){
        //если вернул null , значит такого нету
        if(unknownMessages.size() == 0)
            return null;
        double threshold = applicationManager.getParameters().get(
                "unknown_duplicate_threshold",
                0.9,
                "Порог схожести сообщений базы неизвестных",
                "Это число в диапазоне от 0 до 1 определяет, насколько похожей должна быть фраза, чтобы считаться дубликатом " +
                        "неизвестного вопроса из базы неизвестных. " +
                        "Чем больше это число, тем реже новые неизвестные будут считаться повторами имеющихся неизвестных.\n" +
                        "Например, если это число 1, то каждая неизвестная фраза будет добавляться в список неизвестных как новая,\n" +
                        "а если это число 0, то каждая неизвестная фраза будет считаться повтором перового неизвестного.\n" +
                        "Когда обнаруживаются повторы неизвестных фраз, их счётчик повторов будет увеличиваться, но новые неизвестные " +
                        "добавляться не будут.\n" +
                        "Если ты не понял этого описания, лучше не трогай!");
        if(threshold == 1)
            return null;
        if(threshold == 0)
            return unknownMessages.get(0);
        String preparedText = preparer.prepare(text);
        for (UnknownMessage unknownMessage:unknownMessages)
            if(jaroWinkler.similarity(preparedText, unknownMessage.getPreparedText()) > threshold)
                return unknownMessage;
        return null;
    }
    private void writeToFile(){
        log(". Сохранение базы неизвестных...");
        if(unknownMessages.size() == 0) {
            log( "! Сохранение неизвестных невозможно: база пустая.");
            return;
        }
        try {
            PrintWriter fileWriter = new PrintWriter(file);
            for (UnknownMessage unknownMessage:unknownMessages)
                fileWriter.println(unknownMessage.toJson().toString());
            fileWriter.close();
            log(". База неизвестных сохранена: " + unknownMessages.size() + " сообщений.");
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка сохранения "+unknownMessages.size()+" неизвестных в " + file + ": " + e.toString());
        }
    }

    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().toLowerCase().equals("status")){
                return "Количество неизвестных ответов в базе: " + unknownMessages.size()
                        + (unknownMessages.size() >= limit - 2?" (количество ограничено лимитом)\n":"\n");
            }
            return super.processCommand(message);
        }
    }
    private class TopUnknown extends CommandModule{
        public TopUnknown(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<CommandDesc>();
            result.add(new CommandDesc("Получить топ неизвестных сообщений",
                    "Если бот не знает какого-то вопроса, который ему задали - этот вопрос попадает в список неизвестных фраз.\n" +
                            "Чем чаще боту задают такой, или очень похожий вопрос, тем выше ставится рейтинг этого неизвестного.\n" +
                            "Эта команда покажет самые популярные вопросы к боту.\n" +
                            "Топ неизвестных образовывается не сразу. Чтобы он образовался, бот должен меного общаться.\n",
                    "bcd TopUnknown <количество элементов топа>"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("topunknown")){
                int count = commandParser.getInt();
                if(count < 1)
                    count = 20;
                if(count > 50){
                    message.sendAnswer("Максимум можно получить топ 50 элементов. Я выдам тебе 50 неизвестных.");
                    count = 50;
                }
                UnknownMessage[] top = getTopQuestions(count);
                String result = "Топ "+count+" неизвестных фраз: \n";
                for (int i=0; i<top.length; i++){
                    UnknownMessage unknownMessage = top[i];
                    if(unknownMessage == null){
                        result += "--- конец ---";
                        break;
                    }
                    result += unknownMessage.toString() + "\n";
                }
                return result;
            }
            return super.processCommand(message);
        }
    }
    private class ClearUnknown extends CommandModule{
        public ClearUnknown(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<CommandDesc>();
            result.add(new CommandDesc("Очистить список неизвестных сообщений",
                    "Удалить все сохранённые неизвестные сообщения. После этого они начнут накапливаться заново.",
                    "bcd ClearUnknown"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("clearunknown")){
                unknownMessages.clear();
                writeToFile();
                return "Неизвестные сообщения удалены.";
            }
            return super.processCommand(message);
        }
    }
    private class DumpUnknownMessages extends CommandModule {
        public DumpUnknownMessages(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Выгрузить дамп базы неизвестных слов",
                    "Получить базу данных неизвестных слов накопленную этим ботом в виде документа.",
                    "botcmd DumpUnknown"));
            return result;
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("dumpunknown")) {
                Document document = message.getBotAccount().uploadDocument(file);
                if (document == null)
                    return "Не удалось выгрузить документ на сервер.\n";
                else {
                    message.sendAnswer(new Answer("База неизвестных фраз: ", new Attachment(document)));
                    return "База неизвестных отправлена.\n";
                }
            }
            return "";
        }
    }
}
