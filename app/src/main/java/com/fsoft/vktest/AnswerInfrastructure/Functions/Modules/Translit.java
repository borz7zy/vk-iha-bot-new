package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;

/**
 * Перевод текста из неправильной раскладки
 * Created by Dr. Failov on 29.09.2017.
 */

public class Translit  extends Function {
    public Translit(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        if(message.getText().startsWith(getInvoker() + " ")){
            String text = message.getText().replace(getInvoker() + " ", "").trim();
            if(text.length() == 0) {
                String result = "Не получен текст для конвертации раскладки. Напишите текст после команды \"" + getInvoker() + "\".";
                message.setAnswer(new Answer(result));
                message = prepare(message);
                return message;
            }
            if(text.length() > 4000) {
                String result = "Текст слишком длинный.";
                message.setAnswer(new Answer(result));
                message = prepare(message);
                return message;
            }

            int cyr = countCyrillic(text);
            int lat = countLatin(text);
            if(cyr == 0 && lat == 0){
                String result = "Не удалось определить раскладку текста, текст переведён с английской на русскую:\n" +
                        latinToCyrillic(text);
                message.setAnswer(new Answer(result));
                message = prepare(message);
                return message;
            }
            if(cyr > lat){
                String result = "Текст переведён с русской на английскую:\n" +
                        cyrillicToLatin(text);
                message.setAnswer(new Answer(result));
                message = prepare(message);
                return message;
            }
            else {
                String result = "Текст переведён с английской на русскую:\n" +
                        latinToCyrillic(text);
                message.setAnswer(new Answer(result));
                message = prepare(message);
                return message;
            }
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    public String getName() {
        return "translit";
    }

    @Override
    protected String defaultInvoker() {
        return "транслит";
    }

    @Override
    public String getDescription() {
        return "Модуль перевода текста набранного на неправильной раскладке. Вызывается по запросу \""+getInvoker()+"\".";
    }



    private boolean isCyrillic(char c){
        String cyr = "йцукенгшщзхъфывапролджэячсмитьбю";
        return cyr.toLowerCase().indexOf(c) != -1
                || cyr.toUpperCase().indexOf(c) != -1;
    }
    private boolean isLatin(char c){
        String lat = "qwertyuiopasdfghjklzxcvbnm";
        return lat.toLowerCase().indexOf(c) != -1
                || lat.toUpperCase().indexOf(c) != -1;
    }
    private int countCyrillic(String s){
        int cntCyr = 0;
        int cntTotal = s.length();
        if(cntTotal == 0)
            return 0;

        for (int i = 0; i < cntTotal; i++)
            if(isCyrillic(s.charAt(i)))
                cntCyr ++;

        //это кириллица если больше половины кириллица
        return cntCyr;
    }
    private int countLatin(String s){
        int cntLat = 0;
        int cntTotal = s.length();
        if(cntTotal == 0)
            return 0;

        for (int i = 0; i < cntTotal; i++)
            if(isLatin(s.charAt(i)))
                cntLat ++;

        //это латиница если больше половины кириллица
        return cntLat;
    }
    private char latinToCyrillic(char c){
        switch (c){
            case 'q': return 'й';
            case 'w': return 'ц';
            case 'e': return 'у';
            case 'r': return 'к';
            case 't': return 'е';
            case 'y': return 'н';
            case 'u': return 'г';
            case 'i': return 'ш';
            case 'o': return 'щ';
            case 'p': return 'з';
            case 'a': return 'ф';
            case 's': return 'ы';
            case 'd': return 'в';
            case 'f': return 'а';
            case 'g': return 'п';
            case 'h': return 'р';
            case 'j': return 'о';
            case 'k': return 'л';
            case 'l': return 'д';
            case ';': return 'ж';
            case '\'': return 'э';
            case 'z': return 'я';
            case 'x': return 'ч';
            case 'c': return 'с';
            case 'v': return 'м';
            case 'b': return 'и';
            case 'n': return 'т';
            case 'm': return 'ь';
            case ',': return 'б';
            case '.': return 'ю';
            case '/': return '.';
            case '[': return 'х';
            case ']': return 'ъ';

            case 'Q': return 'Й';
            case 'W': return 'Ц';
            case 'E': return 'У';
            case 'R': return 'К';
            case 'T': return 'Е';
            case 'Y': return 'Н';
            case 'U': return 'Г';
            case 'I': return 'Ш';
            case 'O': return 'Щ';
            case 'P': return 'З';
            case '{': return 'Х';
            case '}': return 'Ъ';
            case 'A': return 'Ф';
            case 'S': return 'Ы';
            case 'D': return 'В';
            case 'F': return 'А';
            case 'G': return 'П';
            case 'H': return 'Р';
            case 'J': return 'О';
            case 'K': return 'Л';
            case 'L': return 'Д';
            case ':': return 'Ж';
            case '\"': return 'Э';
            case 'Z': return 'Я';
            case 'X': return 'Ч';
            case 'C': return 'С';
            case 'V': return 'М';
            case 'B': return 'И';
            case 'N': return 'Т';
            case 'M': return 'Ь';
            case '<': return 'Б';
            case '>': return 'Ю';
            case '?': return '.';
            case '`': return 'ё';
            case '~': return 'Ё';
            default: return c;
        }
    }
    private char cyrillicToLatin(char c){
        switch (c){
            case 'ё': return '`';
            case 'Ё': return '~';

            case 'й': return 'q';
            case 'ц': return 'w';
            case 'у': return 'e';
            case 'к': return 'r';
            case 'е': return 't';
            case 'н': return 'y';
            case 'г': return 'u';
            case 'ш': return 'i';
            case 'щ': return 'o';
            case 'з': return 'p';
            case 'х': return '[';
            case 'ъ': return ']';
            case 'Й': return 'Q';
            case 'Ц': return 'W';
            case 'У': return 'E';
            case 'К': return 'R';
            case 'Е': return 'T';
            case 'Н': return 'Y';
            case 'Г': return 'U';
            case 'Ш': return 'I';
            case 'Щ': return 'O';
            case 'З': return 'P';
            case 'Х': return '{';
            case 'Ъ': return '}';



            case 'ф': return 'a';
            case 'ы': return 's';
            case 'в': return 'd';
            case 'а': return 'f';
            case 'п': return 'g';
            case 'р': return 'h';
            case 'о': return 'j';
            case 'л': return 'k';
            case 'д': return 'l';
            case 'ж': return ';';
            case 'э': return '\'';
            case 'Ф': return 'A';
            case 'Ы': return 'S';
            case 'В': return 'D';
            case 'А': return 'F';
            case 'П': return 'G';
            case 'Р': return 'H';
            case 'О': return 'J';
            case 'Л': return 'K';
            case 'Д': return 'L';
            case 'Ж': return ':';
            case 'Э': return '\"';

            case 'я': return 'z';
            case 'ч': return 'x';
            case 'с': return 'c';
            case 'м': return 'v';
            case 'и': return 'b';
            case 'т': return 'n';
            case 'ь': return 'm';
            case 'б': return ',';
            case 'ю': return '.';
            case '.': return '/';
            case 'Я': return 'Z';
            case 'Ч': return 'X';
            case 'С': return 'C';
            case 'М': return 'V';
            case 'И': return 'B';
            case 'Т': return 'N';
            case 'Ь': return 'M';
            case 'Б': return '<';
            case 'Ю': return '>';
            case ',': return '?';

            default: return c;
        }
    }
    private String latinToCyrillic(String s){
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < s.length(); i++)
            builder.append(latinToCyrillic(s.charAt(i)));

        return builder.toString();
    }
    private String cyrillicToLatin(String s){
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < s.length(); i++)
            builder.append(cyrillicToLatin(s.charAt(i)));

        return builder.toString();
    }
}
