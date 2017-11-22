package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.HttpServer;

import java.util.ArrayList;

/**
 *
 * Created by Dr. Failov on 28.09.2017.
 */

public class BanMe extends Function {
    private ArrayList<Long> warned = new ArrayList<>();


    public BanMe(ApplicationManager applicationManager) {
        super(applicationManager);
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return super.processMessage(messageOriginal);
        Message message = remTreatment(messageOriginal);

        if(message.getAuthor() == HttpServer.USER_ID)
            return super.processMessage(messageOriginal);

        if(message.getText().equalsIgnoreCase(getInvoker())){
            try {
                if (warned.contains(message.getAuthor())) {
                    applicationManager.getIgnorUsersList().add(message.getAuthor(), "���������� �� ������������ �������");
                    message.setAnswer(new Answer("����������, �� ��� �������� � ������ ������������ �������������! " +
                            "������ � ������� �� ���� ���� ��������."));
                    message = prepare(message);
                    warned.remove(message.getAuthor());
                    return message;
                }
                else {
                    warned.add(message.getAuthor());
                    message.setAnswer(new Answer("�� ������ ������, ����� � ���� ������������?\n" +
                            "� ���� ����� ����� ������� �� ���� ��������, � ��� ������� ����� �� ����.\n" +
                            "�� ������? ���� ��, ������� ��� ��� ���."));
                    message = prepare(message);
                    return message;
                }
            }
            catch (Exception  e){
                e.printStackTrace();
                message.setAnswer(new Answer("������, � �� ���� �������� ���� � ���: " + e.getMessage()));
                message = prepare(message);
                return message;
            }
        }
        return super.processMessage(messageOriginal);
    }

    @Override
    public String defaultInvoker() {
        return "������ ����";
    }

    @Override
    public String getName() {
        return "banhammer";
    }
    @Override
    public String getDescription() {
        return "��� ������������ �� ������� \""+getStorage()+"\".\n" +
                "������������ �������� � ������ ������ ����� ������� ��������������.";
    }
}
