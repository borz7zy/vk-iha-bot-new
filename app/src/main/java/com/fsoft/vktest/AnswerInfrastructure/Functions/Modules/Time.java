package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.MessagePreparer;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.Parameters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Этот модуль отвечает на вопрос "который час?"
 * Created by Dr. Failov on 19.09.2017.
 */

public class Time extends Function {

    public Time(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        String[] invokers = getInvoker().split(",");

        if(F.isArrayContains(invokers, message.getText().replace("?", ""))){
            Random random = new Random();
            String[] variants = new String[]{
                    "По моим данным, текущее время: TIME.",
                    "Текущее время: TIME.",
                    "Сейчас TIME.",
                    "У меня TIME. А у тебя?",
                    "TIME. Самое время покушать.",
                    "Время приключений, TIME.",
                    "На моем смарте сейчас TIME."
            };
            variants = applicationManager.getParameters().get("time_patterns",
                    variants,
                    "Список вариантов ответа модуля Time",
                    "Модуль \"Часы\" вызывается фразой \"" + getInvoker() + "\" " +
                            "и отвечает одним из вариантов ответа, который выбирает случайным образом.");
            String result = variants[random.nextInt(variants.length)]
                    .replace("TIME", new SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(new Date()));
            message.setAnswer(new Answer(result));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    protected String defaultInvoker() {
        return "который час, " +
                "сколько времени, " +
                "время, " +
                "точное время, " +
                "сколько сейчас времени";
    }

    @Override
    public String getName() {
        return "time";
    }
    @Override
    public String getDescription() {
        return "Вывод времени по одному из запросов: \""+getInvoker()+"\".\n" +
                "Варианты вызова команды писать через запятую без знака вопроса. При подборе ответа знак вопроса добавится автоматически.";
    }
}
