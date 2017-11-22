package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Utils.F;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * отвечает случайными датами из будушего
 * Created by Dr. Failov on 17.09.2017.
 */


public class When extends Function {
    public When(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        String text = message.getText();
        String[] words = text.split("\\ ");
        text = text.replace("?", "").replace("!", "");

        if(words.length > 1 && words[0].toLowerCase().equals(getInvoker())) {
            text = text.replace(getInvoker() + " ", "");
            if (text.length() > 200)
                return super.processMessage(messageOriginal);
            Random random = new Random();
            int variant = random.nextInt(6);
            if (variant == 0){
                String result = "Примерно через " + (random.nextInt(30) + 1) + " дней.";
                message.setAnswer(new Answer(result));
                message = prepare(message);
                return message;
            }
            if (variant == 1){
                String result = "Где-то через " + (random.nextInt(10)+1) +" месяцев.";
                message.setAnswer(new Answer(result));
                message = prepare(message);
                return message;
            }
            if (variant == 2){
                String result = "Походу, никогда.";
                message.setAnswer(new Answer(result));
                message = prepare(message);
                return message;
            }

            Date dateNow = new Date();
            Calendar c = Calendar.getInstance();
            c.setTime(dateNow);
            c.add(Calendar.YEAR, 10);
            Date date10years = c.getTime();
            Date randomDate = new Date(F.randomLong(dateNow.getTime(), date10years.getTime()));
            DateFormatSymbols myDateFormatSymbols = new DateFormatSymbols(){
                @Override
                public String[] getMonths() {
                    return new String[]{"января", "февраля", "марта", "апреля", "мая", "июня",
                            "июля", "августа", "сентября", "октября", "ноября", "декабря"};
                }
            };
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy", myDateFormatSymbols);
            String result = "Примерно " + simpleDateFormat.format(randomDate) + " года.";
            message.setAnswer(new Answer(result));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    public String defaultInvoker() {
        return "когда";
    }

    @Override
    public String getName() {
        return "when";
    }
    @Override
    public String getDescription() {
        return "Вывод случайной даты для конструкции \"" + applicationManager.getBrain().getTreatment() + " " +getInvoker()+" ...?\".";
    }
}
