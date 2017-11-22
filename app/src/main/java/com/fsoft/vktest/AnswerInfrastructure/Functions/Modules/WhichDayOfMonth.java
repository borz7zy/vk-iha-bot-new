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
 * ������ �������� �� "����� ������� �����?"
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
                    return new String[]{"������", "�������", "�����", "������", "���", "����",
                            "����", "�������", "��������", "�������", "������", "�������"};
                }
            };
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy", myDateFormatSymbols);
            String result = "������ " + simpleDateFormat.format(new Date()) + " ����.";
            message.setAnswer(new Answer(result));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    protected String defaultInvoker() {
        return "����� ������� �����?";
    }

    @Override
    public String getName() {
        return "dayofmonth";
    }

    @Override
    public String getDescription() {
        return "����� ��� ���� �� ������� \""+getInvoker()+"\".";
    }
}
