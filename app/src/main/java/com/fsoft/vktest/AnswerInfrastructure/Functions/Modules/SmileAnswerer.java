package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;

import java.util.Random;

/**
 * модуль, который отвечает случайным смайликом на смайлики
 * Created by Dr. Failov on 01.10.2017.
 */

public class SmileAnswerer extends Function {
    public SmileAnswerer(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        //Один смайл = пара символов. Каждый символ по отдельности смайлом не будет.
        // Они состоят из одного символа, а второй обычно этот: 55357.
        String text = message.getText();
        int[] codes = new int [] {56470,56836,56835,56832,56842,56841,56845,56856,56858,56855,56860,56857,56861,
                56859,56883,56833,56852,56844,56850,56862,56867,56866,56834,56877,56874,56869,56880,56837,56851,
                56873,56875,56872,56881,56864,56865,56868,56854,56838,56843,56887,56846,56884,56885,56882,56863,
                56870,56871,56840,56447,56878,56876,56848,56853,56879,56886,56839,56847,56849,56890,56888,56891,
                56893,56892,56896,56895,56889,56894,56397,56398,56396,56394,56395,56400,56390,56391,56393,56392,
                56908,56475,56476,56474,56473,56471};
        //если какого то из символов нет - выйти из функции
        for (int i = 0; i < text.length(); i++) {
            int c = (int)text.charAt(i);
            boolean exists = false;
            if(c == (int)' ' || c == (int)'\n' || c == 55357)
                exists = true;
            else
                for (int j = 0; j < codes.length; j++) {
                    int s = codes[j];
                    if (c == s ) {
                        exists = true;
                        break;
                    }
                }
            //log("Char " + c + " exists " + exists);
            if (!exists)
                return super.processMessage(messageOriginal);
        }
        //Если мы дошли сюда - значит в сообщении только смайлики
        Random random = new Random();
        String result = "Случайный смайлик: ";
        int index = random.nextInt((codes.length));
        result += (char)55357;
        result += (char)codes[index];

        message.setAnswer(new Answer(result));
        message = prepare(message);
        return message;
    }

    @Override
    public String getName() {
        return "smile";
    }
    @Override
    public String getDescription() {
        return "Модуль отвечает случайным смайликом, когда в вопросе есть только смайлики.";
    }
}
