package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.NumberToText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * Этот модуль генерирует ответы по типу
 * ВОСЕМЬНАДЦАТЬ ЁБАНЫХ ЧАСОВ СОРОК ТРИ БЛЯДСКИХ МИНУТЫ. САМОЕ ВРЕМЯ УБИВАТЬ.
 * Created by Dr. Failov on 19.09.2017.
 */

public class AngryClock extends Function{
    private String[] patterns = {
            "Уже %HOUR% блядских часов и %MINUTE% ёбаных минут длится этот хуёвый день!",
            "%HOUR% блядских часов и %MINUTE% ёбаных минут. Самое время убивать!",
            "На часах %HOUR% часов. Обычно в это время случается какая-то хуйня.",
            "На часах %HOUR% часов, %MINUTE% минут. Но какой в этом толк, если ты тратишь свою жизнь впустую?"
    };
    public AngryClock(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public String getName() {
        return "angryclock";
    }
    @Override
    public String getDescription() {
        return "Модуль отобрвжает время прописью согласно заданному шаблону.\n" +
                "Чтобы вызвать модуль, напиши боту \"" + getInvoker() + "\".\n" +
                "Пример: ВОСЕМЬНАДЦАТЬ ЁБАНЫХ ЧАСОВ СОРОК ТРИ БЛЯДСКИХ МИНУТЫ. САМОЕ ВРЕМЯ УБИВАТЬ.";
    }
    @Override
    protected String defaultInvoker() {
        return "злые часы";
    }
    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        //// TODO: 27.09.2017 А не будет ли драматического падения производительности из-за этого?
        patterns = applicationManager.getParameters().get(
                "angry_clock_patterns",
                patterns,
                "Список вариантов ответа \"злых часов\"",
                "Модуль \"Злые часы\" отвечает по запросу \"" + getInvoker() + "\". " +
                        "В ответах он использует один из заранее заданных вариантов ответа.\n");

        if(message.getText().toLowerCase().equals(getInvoker())){
            Random random = new Random();
            int index = random.nextInt(patterns.length);
            String pattern = patterns[index];
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(new Date());
            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);
            String hoursString = NumberToText.digits2Text((double)hours);
            String minutesString = NumberToText.digits2Text((double)minutes);
            pattern = F.replaceCaseInsensitive(pattern, "%HOUR%", hoursString);
            pattern = F.replaceCaseInsensitive(pattern, "%MINUTE%", minutesString);
            message.setAnswer(new Answer(pattern));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }
}
