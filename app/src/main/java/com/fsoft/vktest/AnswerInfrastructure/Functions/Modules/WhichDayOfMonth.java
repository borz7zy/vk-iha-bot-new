package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.MessagePreparer;
import com.fsoft.vktest.ApplicationManager;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * модуль отвечает на "какое сегодня число?"
 * Created by Dr. Failov on 29.09.2017.
 */

public class WhichDayOfMonth extends Function{
    public WhichDayOfMonth(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        if(message.getText().equalsIgnoreCase(getInvoker())){
            DateFormatSymbols myDateFormatSymbols = new DateFormatSymbols(){
                @Override
                public String[] getMonths() {
                    return new String[]{"января", "февраля", "марта", "апреля", "мая", "июня",
                            "июля", "августа", "сентября", "октября", "ноября", "декабря"};
                }
            };
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy", myDateFormatSymbols);
            String result = "Сейчас " + simpleDateFormat.format(new Date()) + " года.";
            message.setAnswer(new Answer(result));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    protected String defaultInvoker() {
        return "Какое сегодня число?";
    }

    @Override
    public String getName() {
        return "dayofmonth";
    }

    @Override
    public String getDescription() {
        return "Вывод дня даты по запросу \""+getInvoker()+"\".";
    }
}
