package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.ResourceFileReader;

import java.util.ArrayList;

/**
 * Этот модуль рисует красивые тексты псевдографикой
 * Created by Dr. Failov on 29.09.2017.
 */

public class PseudoGraphic extends Function {
    private ArrayList<Symbol> symbols = null;

    public PseudoGraphic(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);


        CommandParser commandParser = new CommandParser(message.getText());
        if(commandParser.getWord().equalsIgnoreCase(getInvoker())){
            String text = commandParser.getText().toLowerCase();
            if(text.length() > 70) {
                message.setAnswer(new Answer("Сообщение не может быть нарисовано, потому что оно слишком длинное."));
                message = prepare(message);
                return message;
            }
//           проверку перед написание аски не обязательно проводить, т.к. VK не сможет это никак проконтролировать
//            if(!applicationManager.brain.filter.isAllowedSymbol(text, true))
//                return "Сообщение не может быть показано.";
            if(text.length() == 0) {
                message.setAnswer(new Answer("Вы забыли написать сообщение после слова \"" + getInvoker() + "\"."));
                message = prepare(message);
                return message;
            }
            //да похуй.
//            if(isNegative(text))
//                return "Я не буду этого писать!";
            String[] line = text.split("\\n");
            String result = "Результат: \n";
            for (int i = 0; i < line.length; i++) {
                result +=getText(line[i]) + "\n";
            }
            message.setAnswer(new Answer(result));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }
    @Override
    public String defaultInvoker() {
        return "напиши";
    }
    @Override
    public String getName() {
        return "asciiart";
    }
    @Override
    public String getDescription() {
        //// TODO: 29.09.2017 слишком мало информацици. Описание необходимо дополнить
        return "Рисование слов по запросу \""+getInvoker()+"...\".\n" +
                "";
    }



    private String[] getLetter(char c){
        if(symbols == null)
            loadLetters();

        for (int i = 0; i < symbols.size(); i++)
            if(symbols.get(i).isIt(c))
                return symbols.get(i).getLines();

        return new String[]{
                "     ",
                "     ",
                "     "
        };
    }
    private String getText(String text){
        //init mas
        String[] fullSizeText = new String[3];
        for (int j = 0; j < fullSizeText.length; j++)
            fullSizeText[j] = " ";

        //build 3-height text
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String[] letter = getLetter(c);
            for (int j = 0; j < fullSizeText.length; j++)
                fullSizeText[j] += letter[j];
        }
        for (int j = 0; j < fullSizeText.length; j++)
            fullSizeText[j] += " ";


        for (int j = 0; j < fullSizeText.length; j++)
            fullSizeText[j] = fullSizeText[j];//.replace(' ', empty).replace('#', filled);
        String out = repeat(" ", fullSizeText[0].length()) + "\n";
        for (int i = 0; i < fullSizeText.length; i++) {
            out += fullSizeText[i] + "\n";
        }
        out += repeat(" ", fullSizeText[0].length());
        return out;
    }
    private String repeat(String data, int times){
        String res = "";
        for(int i=0; i<times; i++)
            res += data;
        return res;
    }
    private void loadLetters(){
        symbols = new ArrayList<>();
        ResourceFileReader resourceFileReader = new ResourceFileReader(applicationManager, R.raw.symbols_database);
        String text = resourceFileReader.readFile();
        String[] letters = text.split("NS:");
        for (int i = 0; i < letters.length; i++) {
            String[] lines = letters[i].split("\n");
            if(lines.length >= 4){
                symbols.add(new Symbol(lines[0], new String[]{
                        lines[1],
                        lines[2],
                        lines[3]
                }));
            }
        }
    }

    private class Symbol{
        private String letters = "";
        private String[] lines = new String[3];

        public Symbol(String letters, String[] lines){
            this.lines = normalize(lines);
            this.letters = letters;
        }
        private String[] normalize(String[] lines){
            int max = 0;
            for (int i = 0; i < lines.length; i++) {
                max = Math.max(max, lines[i].length());
            }
            for (int i = 0; i < lines.length; i++) {
                while(lines[i].length() < max)
                    lines[i] = lines[i] + " ";
            }
            return lines;
        }
        public int getHeight(){
            return lines.length;
        }
        public boolean isIt(char c){
            for (int i = 0; i < letters.length(); i++) {
                if(letters.charAt(i) == c)
                    return true;
            }
            return false;
        }
        public String[] getLines(){
            return lines;
        }

    }
}
