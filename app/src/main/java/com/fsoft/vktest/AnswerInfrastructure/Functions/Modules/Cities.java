package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.BotModule;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.AnswerInfrastructure.MessageBase;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Communication.HttpServer;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.Parameters;
import com.fsoft.vktest.Utils.ResourceFileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * модуль отвечает за игру "города"
 * Created by Dr. Failov on 26.08.2017.
 */

public class Cities extends Function {
    private File citiesFile = null;
    // История игры с каждым пользователем.
    // история нужна для того чтобы не допускать повтора
    private HashMap<Long, ArrayList<String>> gameHistory = new HashMap<>();

    // Общее число городов в базе
    // при поиске по базе заполнять это число, чтобы можно было его использовать
    private int citiesCounter = -1;

    public Cities(ApplicationManager applicationManager) {
        super(applicationManager);
        citiesFile = new ResourceFileReader(applicationManager, R.raw.cities_database).getFile();

        childCommands.add(new Status(applicationManager));
        childCommands.add(new AddCity(applicationManager));
        childCommands.add(new RemCity(applicationManager));
        childCommands.add(new CityGames(applicationManager));

        addChildModule(new Game(applicationManager));
        addChildModule(new CityOnLetter(applicationManager));
        addChildModule(new IsCityExist(applicationManager));
    }

    @Override public Message processMessage(Message message){
        return super.processMessage(message);
    }



    private String prepareCityName(String in){
        return in
                .toLowerCase()
                .replace("!", "")
                .replace("?", "")
                .replace("-", "")
                .replace(",", "")
                .replace(".", "")
                .replace("_", "")
                .trim();
    }
    private boolean isCity(String in) throws Exception{
        if(in.length() < 3)
            return false;
        if(citiesFile.exists())
            throw new Exception("Файла с базой данных городов нет. " +
                    "Проверь, есть ли у программы права записи в память.");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(citiesFile));
        String line;
        in = prepareCityName(in);
        try {
            while ((line = bufferedReader.readLine()) != null) {
                if (prepareCityName(line).equals(in))
                    return true;
            }
        }
        finally {
            bufferedReader.close();
        }
        return false;
    }
    private void addCity(String in) throws Exception{
        if(in.length() < 3)
            throw new Exception("Город имеет слишком короткое название.");
        if(citiesFile.exists())
            throw new Exception("Файла с базой данных городов нет. " +
                    "Проверь, есть ли у программы права записи в память.");

        log(". Добавление города " + in + " базу городов...");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(citiesFile));
        String tmpCitiesFilePath = citiesFile.getPath() + "-tmp";
        File tmpCitiesFile = new File(tmpCitiesFilePath);
        PrintWriter fileTmpWriter = new PrintWriter(tmpCitiesFile);
        String line;
        in = prepareCityName(in);
        try {
            while ((line = bufferedReader.readLine()) != null) {
                if(in.equals(line))
                    throw new Exception("Город с таким названием уже есть в базе");
                fileTmpWriter.println(line);
            }
            fileTmpWriter.println(in);
        }
        finally {
            bufferedReader.close();
            fileTmpWriter.close();
        }


        //заменить файлы (ORIG -> BACKUP)
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault());
        String dateString = simpleDateFormat.format(new Date());
        File newCitiesFilePath = new File(citiesFile.getParent(), citiesFile.getName() + "_" +  dateString);
        if(!citiesFile.renameTo(newCitiesFilePath)) {
            log("! CANT MOVE ORIGINAL FILE TO BACKUP !");
            log("! CITIES FILE: " + citiesFile.getPath());
            log("! NEW CITIES FILE PLACE: " + newCitiesFilePath.getPath());
            throw new Exception("Не могу переместить файл базы городов на новое место");
        }


        //заменить файлы (TMP -> ORIG)
        if(!tmpCitiesFile.renameTo(citiesFile)) {
            log("! CANT MOVE TMP FILE TO ORIGINAL !");
            log("! TMP CITIES FILE: " + tmpCitiesFile.getPath());
            log("! CITIES FILE: " + citiesFile.getPath());
            throw new Exception("Не могу переместить временный файл на место базы городов");
        }

        citiesCounter ++;
        log(". Город " + in + " добавлен в базу городов.");
    }
    private int remCity(String in) throws Exception{
        if(citiesFile.exists())
            throw new Exception("Файла с базой данных городов нет. " +
                    "Проверь, есть ли у программы права записи в память.");

        log(". Удаление города " + in + " из базы городов...");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(citiesFile));
        String tmpCitiesFilePath = citiesFile.getPath() + "-tmp";
        File tmpCitiesFile = new File(tmpCitiesFilePath);
        PrintWriter fileTmpWriter = new PrintWriter(tmpCitiesFile);
        String line;
        in = prepareCityName(in);
        int removed = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                if(in.equals(line))
                    removed ++;
                else
                    fileTmpWriter.println(line);
            }
        }
        finally {
            bufferedReader.close();
            fileTmpWriter.close();
        }

        if(removed == 0)
            throw new Exception("Города с названием \"" + in + "\" нету в базе.");

        //заменить файлы (ORIG -> BACKUP)
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault());
        String dateString = simpleDateFormat.format(new Date());
        File newCitiesFilePath = new File(citiesFile.getParent(), citiesFile.getName() + "_" +  dateString);
        if(!citiesFile.renameTo(newCitiesFilePath)) {
            log("! CANT MOVE ORIGINAL FILE TO BACKUP !");
            log("! CITIES FILE: " + citiesFile.getPath());
            log("! NEW CITIES FILE PLACE: " + newCitiesFilePath.getPath());
            throw new Exception("Не могу переместить файл базы городов на новое место");
        }


        //заменить файлы (TMP -> ORIG)
        if(!tmpCitiesFile.renameTo(citiesFile)) {
            log("! CANT MOVE TMP FILE TO ORIGINAL !");
            log("! TMP CITIES FILE: " + tmpCitiesFile.getPath());
            log("! CITIES FILE: " + citiesFile.getPath());
            throw new Exception("Не могу переместить временный файл на место базы городов");
        }

        citiesCounter -= removed;
        log(". Из базы удалено " + removed + " городов.");
        return removed;
    }
    private boolean wasBefore(String in, long id){
        if(!gameHistory.containsKey(id))
            return false;
        ArrayList<String> history = gameHistory.get(id);
        in = prepareCityName(in);
        for (String c:history)
            if(prepareCityName(c).equals(in))
                return true;
        return false;
    }
    private String getLastCityFromHistory(long id){
        if(!gameHistory.containsKey(id))
            return null;
        ArrayList<String> history = gameHistory.get(id);
        if(history.size() == 0)
            return null;
        return history.get(history.size() - 1);
    }
    private char getLastLetter(String in){
        in = in.toLowerCase();
        for (int i = in.length()-1; i >= 0; i--) {
            if(in.charAt(i) != ' ' && in.charAt(i) != '.' && in.charAt(i) != 'ы' && in.charAt(i) != 'ь' && in.charAt(i) != 'ё' && in.charAt(i) != 'ъ')
                return in.charAt(i);
        }
        return 'a';
    }
    private char getFirstLetter(String in){
        in = in.toLowerCase().trim();
        for (int i = 0; i < in.length(); i++) {
            if(in.charAt(i) != ' ')
                return in.charAt(i);
        }
        return 'a';
    }
    private ArrayList<String> getCitiesOnLetter(char letter) throws Exception{
        ArrayList<String> result = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(citiesFile));
        String line;
        int lines = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                lines ++;
                if (getFirstLetter(line) == letter)
                    result.add(line);
            }
            citiesCounter = lines;
        }
        finally {
            bufferedReader.close();
        }
        return result;
    }
    private String getNextCityOnLetter(char letter, long sender) throws Exception{
        //загрузить говрода на букву
        ArrayList<String> correctCities = getCitiesOnLetter(letter);
        //удалить города которые были уже
        ArrayList<String> historyList = gameHistory.get(sender);
        for (int i = 0; i < historyList.size(); i++) {
            if(correctCities.contains(historyList.get(i)))
                correctCities.remove(historyList.get(i));
        }
        if(correctCities.size() == 0) {
            return "На эту букву больше городов нет. Ты выиграл!";
        }
        //выбрать случайным образом
        return correctCities.get(new Random().nextInt(correctCities.size()));
    }
    private int stopGame(long id){
        //returns number of cities played
        if(!gameHistory.containsKey(id))
            return 0;
        ArrayList<String> historyList = gameHistory.get(id);
        int played = historyList.size();
        gameHistory.remove(id);
        return played;
    }


    @Override
    public String getName() {
        return "cities";
    }
    @Override
    public String getDescription() {
        return "Игра в \"Города\".\n" +
                "Бот знает очень много городов и будет использовать их для игры в города. \n" +
                "Игра запустится если пользователь напишет боту \""+getInvoker()+"\".\n" +
                "Также этот модуль обеспечивает работу таких команд как: \"Города на букву ...\", \"Существует ли город ...?\".";
    }
    @Override
    public String defaultInvoker() {
        return "города";
    }

    private class Game extends BotModule{
        private String cities_answer_zero_cities_text = "";
        private String cities_answer_less5_cities_text = "";
        private String cities_answer_more5_cities_text = "";
        private String cities_answer_negative_stop_text = "";
        private String cities_answer_city_was_before = "";
        private String cities_answer_city_incorrect = "";
        private String cities_answer_city_dont_know = "";
        private String cities_answer_city_welcome = "";
        private String cities_error_text = "";
        private String[] cities_stop_markers = {};
        private String[] cities_negative_stop_markers = {};

        Game(ApplicationManager applicationManager) {
            super(applicationManager);

            Parameters parameters = applicationManager.getParameters();

            cities_answer_zero_cities_text = parameters.get(
                    "cities_answer_zero_cities_text",
                    "Игра закончена. Жаль, что мы так и не успели поиграть:(",
                    "Текст завершения игры города при 0 городов",
                    "Текст, который получит пользователь, если остановит игру в города не сыграв ни одного города.");

            cities_answer_less5_cities_text = parameters.get(
                    "cities_answer_less5_cities_text",
                    "Игра закончена. Мы сыграли всего %NUMBER% городов:( Давай потом поиграем ещё?",
                    "Текст завершения игры города при менее 5 городов",
                    "Текст, который получит пользователь, если остановит игру в города сыграв менее чем в 5 городов. " +
                            "Фрагмент %NUMBER% заменится на количество городов, сколько мы с ним сыграли.");

            cities_answer_more5_cities_text = parameters.get("cities_answer_more5_cities_text",
                    "Игра закончена. Мы сыграли %NUMBER% городов! С тобой круто было играть!",
                    "Текст завершения игры города при более 5 городов",
                    "Текст, который получит пользователь, если остановит игру в города сыграв более чем в 5 городов. " +
                            "Фрагмент %NUMBER% заменится на количество городов, сколько мы с ним сыграли.");

            cities_answer_negative_stop_text = parameters.get("cities_answer_negative_stop_text",
                    "Игра закончена. Ты пидор.",
                    "Текст завершения игры города если бота пошлют нахуй",
                    "Текст, который получит пользователь, если грубо остановит игру фразой \"иди нахуй\" или чем-то подобным. " +
                            "Фрагмент %NUMBER% заменится на количество городов, сколько мы с ним сыграли.");

            cities_error_text = parameters.get("cities_error_text",
                    "У меня проблема: я не могу прочитать базу данных городов, поэтому не могу тебе ответить.\n" +
                            "Из-за этой ошибки: %ERROR%\n" +
                            "Если эта ошибка появляется постоянно или очень часто, заверши игру одной из этих фраз: %STOPMARKERS%.",
                    "Текст ошибки во время игры в города",
                    "Текст, который получит пользователь, если во время игры в города бот не сможет дать ответ на фразу из-за ошибки. " +
                            "Такое возникает довольно редко. " +
                            "Фрагмент %ERROR% заменится на текст ошибки из системы. " +
                            "Фрагмент %STOPMARKERS% заменится список команд, которыми можно остановить игру.");

            cities_answer_city_was_before = parameters.get("cities_answer_city_was_before",
                    "Город %CITY% уже был раньше в нашей игре. Придумай другой город.",
                    "Текст, если город уже был в игре",
                    "Текст, который получит пользователь во время игры в города, если назовёт город, который уже был. " +
                            "Фрагмент %CITY% заменится на название города, которое прислал пользователь.");

            cities_answer_city_incorrect = parameters.get("cities_answer_city_was_before",
                    "Неправильно. Тебе сейчас нужно говорить город на букву \"%LETTER%\".",
                    "Текст, если город на неправильную букву",
                    "Текст, который получит пользователь во время игры в города, если назовёт город на неправильную букву. " +
                            "Фрагмент %LETTER% заменится на букву с которой должно начинаться название города.");

            cities_answer_city_dont_know = parameters.get("cities_answer_city_dont_know",
                    "Я не знаю такого города:( Попробуй какой-нибудь другой город, более известный.",
                    "Текст, если города нет в базе",
                    "Текст, который получит пользователь во время игры в города, если назовёт город которого нет в базе бота.");

            cities_answer_city_welcome = parameters.get("cities_answer_city_welcome",
                    "Ну давай сыграем в \"Города\"! Правила простые: нужно назвать город на последнюю букву предыдущего названого города." +
                            " Начинай ты! Называй город.\n" +
                            " Чтобы закончить игру, набери \"стоп игра\" или \"конец игры\" или \"закончить игру\".",
                    "Текст приветствия в игре Города",
                    "Текст, который получит пользователь в качестве приветствия, когда начнёт игру в города.");

            String cities_stop_markers_string = parameters.get("cities_stop_markers",
                    "Стоп игра, " +
                            "Закончить игру, " +
                            "Какая игра?, " +
                            "Мы не играем, " +
                            "Мы же не играем, " +
                            "Завершить игру, " +
                            "Завершить игру, " +
                            "Я не играю, " +
                            "Хватит играть!",
                    "Список стоп-фраз для игры Города",
                    "Варианты фраз (написанные через запятые), которыми можно остановить игру в города. " +
                            "Регистр фраз и пунктуация не учитыватся.");
            cities_stop_markers = cities_stop_markers_string
                    .toLowerCase()
                    .replace(" ,", ",")
                    .replace(", ", ",")
                    .replace("!", "")
                    .replace("?", "")
                    .replace("-", "")
                    .replace(".", "")
                    .replace("_", "")
                    .trim()
                    .split(",");
            cities_stop_markers = F.trimArray(cities_stop_markers);

            String cities_negative_stop_markers_string = parameters.get("cities_negative_stop_markers",
                    "Иди нахуй, " +
                            "Пошел нахуй, "+
                            "Пошёл нахуй, "+
                            "Иди в жопу, "+
                            "Пошел в жопу, "+
                            "Пошёл в жопу, "+
                            "Заткнись",
                    "Список грубых стоп-фраз для игры Города",
                    "Варианты фраз, написанные через запятые, которыми можно грубо остановить игру в города. " +
                            "После остановки игры одной из этих фраз бот ответит пользователю специальным текстом для грубой остановки игры. " +
                            "Регистр фраз и пунктуация не учитыватся.");
            cities_negative_stop_markers = cities_negative_stop_markers_string
                    .toLowerCase()
                    .replace(" ,", ",")
                    .replace(", ", ",")
                    .replace("!", "")
                    .replace("?", "")
                    .replace("-", "")
                    .replace(".", "")
                    .replace("_", "")
                    .trim()
                    .split(",");
            cities_negative_stop_markers = F.trimArray(cities_negative_stop_markers);
        }

        @Override
        public Message processMessage(Message messageOriginal) {
            if(!hasTreatment(messageOriginal))
                return super.processMessage(messageOriginal);
            Message message = remTreatment(messageOriginal);

            //С сервера нельзя играть в города, потому что там все пользователи как один
            if(message.getSource() == MessageBase.SOURCE_HTTP)
                return super.processMessage(messageOriginal);

            //если мы играем с этим пользователем - мы перехватываем все его сообщения
            if(gameHistory.containsKey(message.getAuthor())){
                String text = prepareCityName(message.getText());
                //================= Завершение игры?!
                if(F.isArrayContains(cities_negative_stop_markers, text)){//с матами
                    int played = stopGame(message.getAuthor());
                    message.setAnswer(new Answer(cities_answer_negative_stop_text.replace("%NUMBER%", String.valueOf(played))));
                    message = prepare(message);
                    return message;
                }
                if(F.isArrayContains(cities_stop_markers, text)){//завершение без матов
                    int played = stopGame(message.getAuthor());
                    if(played == 0){
                        message.setAnswer(new Answer(cities_answer_zero_cities_text));
                        message = prepare(message);
                        return message;
                    }
                    if(played < 5){
                        message.setAnswer(new Answer(cities_answer_less5_cities_text.replace("%NUMBER%", String.valueOf(played))));
                        message = prepare(message);
                        return message;
                    }
                    message.setAnswer(new Answer(cities_answer_more5_cities_text.replace("%NUMBER%", String.valueOf(played))));
                    message = prepare(message);
                    return message;
                }

                try {
                    //есть ли такой город?
                    if (isCity(text)) {
                        if (wasBefore(text, message.getAuthor())) {
                            message.setAnswer(new Answer(cities_answer_city_was_before.replace("%CITY%", message.getText())));
                            message = prepare(message);
                            return message;
                        }
                        else {
                            ArrayList<String> historyList = gameHistory.get(message.getAuthor());

                            if (historyList.size() > 0) {
                                char lastWordLastLetter = getLastLetter(historyList.get(historyList.size() - 1));
                                if (lastWordLastLetter != getFirstLetter(text)) {
                                    message.setAnswer(new Answer(cities_answer_city_incorrect.replace("%LETTER%", String.valueOf(lastWordLastLetter))));
                                    message = prepare(message);
                                    return message;
                                }
                            }
                            historyList.add(text);
                            char lastLetter = getLastLetter(text);
                            String result = getNextCityOnLetter(lastLetter, message.getAuthor());
                            historyList.add(result);
                            result = F.makeBeginWithUpper(result.trim());
                            message.setAnswer(new Answer(result));
                            message = prepare(message);
                            return message;
                        }
                    } else {
                        message.setAnswer(new Answer(cities_answer_city_dont_know));
                        message = prepare(message);
                        return message;
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                    message.setAnswer(
                            new Answer(
                                    cities_error_text
                                            .replace("%ERROR%", e.toString())
                                            .replace("%STOPMARKERS%", F.arrayToString(cities_stop_markers))
                            )
                    );
                    message = prepare(message);
                    return message;
                }
            }
            if(prepareCityName(message.getText()).equals(prepareCityName(getInvoker()))){
                gameHistory.put(message.getAuthor(), new ArrayList<String>());
                message.setAnswer(new Answer(cities_answer_city_welcome));
                message = prepare(message);
                return message;
            }
            return super.processMessage(messageOriginal);
        }
    }
    private class CityOnLetter extends BotModule{

        CityOnLetter(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public Message processMessage(Message message)
        {
            if(!hasTreatment(message))
                return super.processMessage(message);
            message = remTreatment(message);

            if(message.getText().toLowerCase().startsWith("города на букву ")
                    || message.getText().toLowerCase().startsWith("город на букву ")){
                String letterString =  message.getText().toLowerCase()
                        .replace("города на букву ", "")
                        .replace("город на букву ", "");

                letterString = prepareCityName(letterString).trim();
                if(letterString.length() < 1) {
                    message.setAnswer(new Answer("После этой фразы нужно написать букву." +
                            "\nНапример: Город на букву А?"));
                    message = prepare(message);
                    return message;
                }
                if(letterString.length() > 1) {
                    message.setAnswer(new Answer("Нужна только одна буква"));
                    message = prepare(message);
                    return message;
                }
                try {
                    char letterChar = letterString.charAt(0);
                    ArrayList<String> cities = getCitiesOnLetter(letterChar);
                    if(cities.size() == 0){
                        message.setAnswer(new Answer("Городов на букву \""+letterChar+"\" нет."));
                        message = prepare(message);
                        return message;
                    }
                    Random random = new Random();
                    String result = "10 случайных городов на букву \""+letterChar+"\":\n";
                    for (int i = 0; i < 10; i++)
                        result += cities.get(random.nextInt(cities.size())) + "\n";
                    result += "Всего городов на букву \""+letterChar+"\": " + cities.size();

                    message.setAnswer(new Answer(result));
                    message = prepare(message);
                    return message;
                }
                catch (Exception e){
                    message.setAnswer(new Answer("Не могу получить список городов. " + e.getMessage()));
                    message = prepare(message);
                    return message;
                }
            }
            return super.processMessage(message);
        }
    }
    private class IsCityExist extends BotModule{
        IsCityExist(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public Message processMessage(Message message) {
            if(!hasTreatment(message))
                return super.processMessage(message);
            message = remTreatment(message);

            if(message.getText().toLowerCase().startsWith("существует ли город ")){
                String cityString =  message.getText().toLowerCase()
                        .replace("существует ли город ", "");

                cityString = prepareCityName(cityString).trim();
                if(cityString.length() < 2) {
                    message.setAnswer(new Answer("После этой фразы нужно написать название города." +
                            "\nНапример: Существует ли город Киев?"));
                    message = prepare(message);
                    return message;
                }
                if(cityString.length() > 15) {
                    message.setAnswer(new Answer("Слишком длинное название как для города."));
                    message = prepare(message);
                    return message;
                }
                try {
                    //поискать для начала по первой букве
                    char letterChar = cityString.charAt(0);
                    ArrayList<String> cities = getCitiesOnLetter(letterChar);
                    if(cities.size() == 0){
                        message.setAnswer(new Answer("Городов на букву \""+letterChar+"\" нет."));
                        message = prepare(message);
                        return message;
                    }
                    if(cities.contains(cityString)){
                        message.setAnswer(new Answer("Город \""+cityString+"\" существует."));
                        message = prepare(message);
                        return message;
                    }

                    long time = System.currentTimeMillis();
                    while(System.currentTimeMillis() - time < 5000){
                        cityString = cityString.substring(0, cityString.length()-2);
                        if(cityString.length() < 3) {
                            message.setAnswer(new Answer("Такого города нет."));
                            message = prepare(message);
                            return message;
                        }
                        for(int i=0; i<cities.size(); i++) {
                            if (cities.get(i).startsWith(cityString)) {
                                message.setAnswer(new Answer("Такого города нет, но есть город \"" + cities.get(i) + "\"."));
                                message = prepare(message);
                                return message;
                            }
                        }
                    }
                    message.setAnswer(new Answer("Ошибка, программа поиска зависла."));
                    message = prepare(message);
                    return message;
                }
                catch (Exception e){
                    message.setAnswer(new Answer("Не могу получить список городов. " + e.getMessage()));
                    message = prepare(message);
                    return message;
                }
            }
            return super.processMessage(message);
        }
    }

    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().toLowerCase().equals("status") || message.getText().toLowerCase().equals("cities status")){
                String result = "";
                if(citiesCounter == -1)
                    result += "Обращений к модулю \"Города\" ещё не было. Размер базы неизвестен. " +
                            "(Размер базы выясняется в тот момент, когда кто-то обратится к любой из функций, которая потребует загрузки базы. " +
                            "Заранее база не загружается для экономии времени и памяти.)\n";
                else
                    result += "Количество городов в базе игры \"Города\": " + citiesCounter + "\n";
                result += "Количество активных игр в города: " + gameHistory.size() + "\n";
                return result;
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Статус модуля \"Города\"",
                    "Покажет количество городов в базе (если она загружена) и количество активных игр с пользователями.",
                    "botcmd cities status"));
            return result;
        }
    }
    private class AddCity extends CommandModule{
        public AddCity(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("addcity")){
                String newCity = commandParser.getWord();
                try{
                    addCity(newCity);
                    return "Город \"" + newCity + "\" добавлен в базу городов. Сейчас в базе " + citiesCounter + " городов.";
                }
                catch (Exception e){
                    e.printStackTrace();
                    return "Ошибка: " + e.getMessage();
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Добавить новый город в базу данных модуля \"Города\"",
                    "Во время игры в \"Города\" бот использует города из своей базы данных." +
                            " Если бот не знает какого-то города, его можно добавить в базу данных этой командой.\n" +
                            "Также он будет использовать этот город в модулях \"Есть ли город\" и \"Города на букву\".",
                    "botcmd AddCity <имя городв>"));
            return result;
        }
    }
    private class RemCity extends CommandModule{
        public RemCity(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("remcity")){
                String cityName = commandParser.getWord();
                try{
                    int cnt = remCity(cityName);
                    return "Из базы удален " + cnt + " город. Сейчас в базе " + citiesCounter + " городов.";
                }
                catch (Exception e){
                    e.printStackTrace();
                    return "Ошибка: " + e.getMessage();
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить город из базы данных модуля \"Города\"",
                    "Бот не будет больше использовать этот город в игре \"Города\".",
                    "botcmd RemCity <имя городв>"));
            return result;
        }
    }
    private class CityGames extends CommandModule{
        public CityGames(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("citygames")){
                try{
                    if(gameHistory.size() == 0)
                        return "Сейчас нет ни одной активной игры.";

                    String result = "Список активных игр в \"Города\":\n";
                    Set<Map.Entry<Long, ArrayList<String>>> set = gameHistory.entrySet();
                    Iterator<Map.Entry<Long, ArrayList<String>>> iterator = set.iterator();
                    while (iterator.hasNext()){
                        Map.Entry<Long, ArrayList<String>> entry = iterator.next();
                        Long id = entry.getKey();
                        ArrayList<String> history = entry.getValue();
                        String name = applicationManager.getCommunicator().getActiveAccount().getUserFullName(id);
                        result += name + " vk.com/id" + id + ", Городов: " + history.size() + "\n";
                    }
                    result += "----\n";
                    result += "Всего активно игр: " + gameHistory.size() + "\n";
                    return result;
                }
                catch (Exception e){
                    e.printStackTrace();
                    return "Ошибка: " + e.getMessage();
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Вывести список активных игр в \"Города\"",
                    "Команда покажет список кто и столько городов уже играет с ботом в \"Города\".",
                    "botcmd CityGames"));
            return result;
        }
    }
}
