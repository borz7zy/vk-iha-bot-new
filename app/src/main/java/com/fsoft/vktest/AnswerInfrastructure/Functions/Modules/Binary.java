package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;

import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * перевод текста в двоичку
 * Created by Dr. Failov on 12.02.2017.
 */
public class Binary extends Function {
    public Binary(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        String text = message.getText();
        if(text.trim().toLowerCase().startsWith(getInvoker() + " ")){
            text = text.toLowerCase().replace(getInvoker() + " ", "");
            if(text.matches("[10 ]+") && text.length() < 2400){//расшифровать
                text = text.replace(" ", "");

                //сформировать массив байтов
                ArrayList<Byte> bytesArray = new ArrayList<>();
                for(int i=0; i<text.length(); i+=8){
                    String block = text.substring(i, Math.min(text.length(), i+8));
                    if(!block.equals("")) {
                        int code = Integer.parseInt(block, 2);
                        bytesArray.add((byte)code);
                    }
                }

                //конвертировать массив байтов
                byte[] bytes = new byte[bytesArray.size()];
                for (int i = 0; i < bytesArray.size(); i++) {
                    bytes[i] = bytesArray.get(i);
                }

                //Вывести отчёты
                try{
                    String result = "Расшифровано в кодировке UTF-8: \n" + new String(bytes, "UTF-8");
                    result += "\n---------------------------\n";
                    result += "Расшифровано в кодировке UTF-16: \n" + new String(bytes, "UTF-16");
                    result += "\n---------------------------\n";
                    result += "Расшифровано в кодировке Windows-1251 (Cp1251): \n" + new String(bytes, "Cp1251");

                    message.setAnswer(new Answer(result));
                    message = prepare(message);
                    return message;
                }
                catch (Exception e){
                    e.printStackTrace();
                    message.setAnswer(new Answer("Ошибка расшифровки: система не поддерживает нужных кодировок."));
                    message = prepare(message);
                    return message;
                }
            }
            else if(text.length() < 400){//зашифромать
                try {
                    String firstWord = text.split(" ")[0];
                    String charset = "UTF-8";
                    if(isCharsetSupported(firstWord)){
                        charset = firstWord;
                        text = text.substring(charset.length()+1).trim();
                    }
                    byte[] bytes = text.getBytes(charset);
                    String result = "";
                    for (int i = 0; i < bytes.length; i++) {
                        String block = Integer.toBinaryString(bytes[i] & 0xFF);
                        while (block.length() < 8)
                            block = "0"+block;
                        result += block + " ";
                    }
                    message.setAnswer(new Answer("Зашифровано в кодировке "+charset+": \n" + result));
                    message = prepare(message);
                    return message;
                }
                catch (Exception e){
                    e.printStackTrace();
                    message.setAnswer(new Answer("Ошибка шифровки: система не поддерживает нужных кодировок."));
                    message = prepare(message);
                    return message;
                }
            }
        }
        return super.processMessage(messageOriginal);
    }
    private boolean isCharsetSupported(String name) {
        try {
            Charset.forName(name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public String defaultInvoker() {
        return "бинар";
    }
    @Override public String getName() {
        return "binary";
    }
    @Override public String getDescription() {
        return "Создание или расшифровка двоичных данных по запросу  \""+getInvoker()+" ...\" .";
    }
}
