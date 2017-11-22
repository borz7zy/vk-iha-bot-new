package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Utils.CommandParser;

/**
 * ��������� ��������� �����
 * Created by Dr. Failov on 16.09.2017.
 */

public class Rendom extends Function {
    public Rendom(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        CommandParser commandParser = new CommandParser(message.getText());
        if(commandParser.getWord().toLowerCase().equals(getInvoker())){
            int min=0;
            int max = 2000000;
            java.util.Random random = new java.util.Random();
            switch (commandParser.wordsRemaining()){
                case 1:
                    max = commandParser.getInt();
                    break;
                case 2:
                    min = commandParser.getInt();
                    max = commandParser.getInt();
                    break;
            }
            if(max < min) {
                int tmp = max;
                max = min;
                min = tmp;
            }
            if(max == min){
                max = min + 10;
            }
            int num = Math.min(min, max) + (random.nextInt(Math.abs(max - min)));

            String result = "��������� �����: "+num+".";
            message.setAnswer(new Answer(result));
            message = prepare(message);
            return message;
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    public String defaultInvoker() {
        return "������";
    }

    @Override
    public String getName() {
        return "random";
    }
    @Override
    public String getDescription() {
        return "��������� ��������� ����� �� ������� \""+getInvoker()+"...\"." +
                "\n����� ����� ����� ������ ������� ������������ ����� (��������: \"" + applicationManager.getBrain().getTreatment() + " " + getInvoker() + " 255\")." +
                "\n��� ����� ������ ���������� ����� (��������: \"" + applicationManager.getBrain().getTreatment() + " " + getInvoker() + " 10 20\").";
    }
}
