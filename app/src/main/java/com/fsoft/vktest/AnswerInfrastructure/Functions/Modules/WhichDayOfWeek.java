package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.MessagePreparer;
import com.fsoft.vktest.ApplicationManager;

import java.util.Calendar;
import java.util.Random;

/**
 * �������� ���� "�����������", "�������". ...
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

        //// TODO: 26.09.2017 �� �������� �� �������� ����������� ������������������ ������������?
        String[] variants = {
                "�� ���� ������, ������� %DAY%.",
                "�������, ������� %DAY%.",
                "������� %DAY%.",
                "����������� ���� ������: %DAY%.",
                "%DAY% �������.",
                "������ ����� ��� ����������� - %DAY%."
        };
        variants = applicationManager.getParameters().get(
                "dayofweek_variants",
                variants,
                "������ ��������� ������ �� ������ \"����� ������� ����?\".",
                "�� ������� ���� \"����� ������� ����?\" ��� ����� ����������� �� ��������� ������ ���� ������������ �����.\n" +
                        "��� ������ �� ���� ������ ��� ���������� ���� �� ���� �������� ������.\n" +
                        "�������� %DAY% � ������� ��������� �� ������� ���� ������.");

        String text = message.getText().toLowerCase().trim().replace("?", "").replace("!", "");
        int cnt = 0;
        if(text.contains("����"))
            cnt++;
        if(text.contains("�����"))
            cnt++;
        if(text.contains("������"))
            cnt++;
        if(text.contains("�������"))
            cnt++;
        if(cnt >= 2){
            //������� - 7
            Calendar calendar = Calendar.getInstance();
            String dayName = "����������";
            int dayNumber = calendar.get(Calendar.DAY_OF_WEEK);

            if(dayNumber == Calendar.MONDAY)
                dayName = "�����������";
            if(dayNumber == Calendar.TUESDAY)
                dayName = "�������";
            if(dayNumber == Calendar.WEDNESDAY)
                dayName = "�����";
            if(dayNumber == Calendar.THURSDAY)
                dayName = "�������";
            if(dayNumber == Calendar.FRIDAY)
                dayName = "�������";
            if(dayNumber == Calendar.SATURDAY)
                dayName = "�������";
            if(dayNumber == Calendar.SUNDAY)
                dayName = "�����������";

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
        return "����� ��� ������ �� ������� \"����� ������� ����?\".";
    }
}
