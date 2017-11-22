package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.MessagePreparer;
import com.fsoft.vktest.ApplicationManager;

import java.util.Calendar;
import java.util.Random;

/**
 * отвечает тира "понедельник", "вторник". ...
 * Created by Dr. Failov on 16.09.2017.
 */

public class WhichDayOfWeek extends Function {
    public WhichDayOfWeek(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        //// TODO: 26.09.2017 не понижает ли подобная конструкция производительность драматически?
        String[] variants = {
                "По моим данным, сегодня %DAY%.",
                "Кажется, сегодня %DAY%.",
                "Сегодня %DAY%.",
                "Сегодняшний день недели: %DAY%.",
                "%DAY% сегодня.",
                "Лучшее время для приключений - %DAY%."
        };
        variants = applicationManager.getParameters().get(
                "dayofweek_variants",
                variants,
                "Список вариантов ответа на вопрос \"Какой сегодня день?\".",
                "На вопросы типа \"Какой сегодня день?\" бот может основываясь на имеющихся данных дать отсмысленный ответ.\n" +
                        "При ответе на этот вопрос бот использует один из этих шаблонов ответа.\n" +
                        "Фрагмент %DAY% в шаблоне заменится на текущий день недели.");

        String text = message.getText().toLowerCase().trim().replace("?", "").replace("!", "");
        int cnt = 0;
        if(text.contains("день"))
            cnt++;
        if(text.contains("какой"))
            cnt++;
        if(text.contains("недели"))
            cnt++;
        if(text.contains("сегодня"))
            cnt++;
        if(cnt >= 2){
            //суббота - 7
            Calendar calendar = Calendar.getInstance();
            String dayName = "Неизвестен";
            int dayNumber = calendar.get(Calendar.DAY_OF_WEEK);

            if(dayNumber == Calendar.MONDAY)
                dayName = "Понедельник";
            if(dayNumber == Calendar.TUESDAY)
                dayName = "Вторник";
            if(dayNumber == Calendar.WEDNESDAY)
                dayName = "Среда";
            if(dayNumber == Calendar.THURSDAY)
                dayName = "Четверг";
            if(dayNumber == Calendar.FRIDAY)
                dayName = "Пятница";
            if(dayNumber == Calendar.SATURDAY)
                dayName = "Суббота";
            if(dayNumber == Calendar.SUNDAY)
                dayName = "Воскресенье";

            Random random = new Random();
            String result = variants[random.nextInt(variants.length)].replace("%DAY%", dayName);

            message.setAnswer(new Answer(result));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    public String getName() {
        return "dayofweek";
    }
    @Override public String getDescription() {
        return "Вывод дня недели по запросу \"Какой сегодня день?\".";
    }
}
