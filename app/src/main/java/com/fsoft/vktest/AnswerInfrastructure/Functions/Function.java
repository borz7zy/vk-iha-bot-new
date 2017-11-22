package com.fsoft.vktest.AnswerInfrastructure.Functions;

import com.fsoft.vktest.AnswerInfrastructure.BotModule;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;

import java.util.ArrayList;

/**
 * ������� ��� ���� ������� � FunctionProcessor
 *
 * ������� �������:
 * - ������ �������� ��������� � ����� ����. �� ����, ���, ��� ��� ������� ������������. � ���������� ����.
 * - ������ ���������� ��������� � ������� ��� �������� ����. � ������, ����������, � �.�.
 * ����� ����� ��������� ����� ������ ���� ��������� �� ����������� �������, ����� ���� ����������.
 *
 * ��� ��������
 * - ���� ������ �� ����� �������� �� ��� ��������� - ���������� message � answer NULL
 * - ���� ������ ������� ��� �� ��� ��������� �� ����� �������� - ���������� message � answer ""
 * - ���� ������ ����� �������� - ���������� message � answer ����� ������ ������� � ��������
 *
 * ����� ���������� Message? ����� ������ ��� ������������� ��������� �� ������ �������.
 *
 * Edited by Dr. Failov on 14.08.2017.
 * Created by Dr. Failov on 12.02.2017.
 */
public class Function extends BotModule {
    //����������� �������� ����� ������ ��� ������������� ������. ������ ��� ����, �����
    // ��������� �� ������������� �� ������ ���� �������� �����. ���� ������������� - ������
    // ����� ���������� ��� �������
    private String defaultInvoker = "�������� ����� � ���� ������ �� ��������������.";
    private String invoker = defaultInvoker();

    public Function(ApplicationManager applicationManager) {
        super(applicationManager);
        invoker = getStorage().getString("invoker", invoker);
    }

    //�������� ������� �� �������� ���� �������. ���� ������ ���, ������� "".
    @Override public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = super.getHelp();
        if (!defaultInvoker().equals(defaultInvoker)) {
            //���������� ��� ������� ������ ���� ��� ����� ������ ��� ����� ��������� �������
            result.add(new CommandDesc("������ �������� ����� ��� ������ " + getName(),
                    "������ ���� ������ ��� ������ ���������� ����� " + defaultInvoker() + ". " +
                            "���� �������� ��� ����� ����� �������� �� ����� ������.",
                    "botcmd module " + getName() + " SetInvoker <����� �������� �����>"));
        }
        return result;
    }

    //���������� �������. �� ���� ��������� ����� �������� ��� botcmd, �������: "wall add drfailov".
    @Override public String processCommand(Message messageOriginal) {
        CommandParser commandParser = new CommandParser(messageOriginal.getText());             //botcmd
        if (commandParser.getWord().toLowerCase().equals("module")) {                   //module
            if (!defaultInvoker().equals(defaultInvoker)) {
                if (commandParser.getWord().toLowerCase().equals(getName().toLowerCase())) {//dummy
                    if (commandParser.getWord().toLowerCase().equals("setinvoker")) {   //setinvoker
                        String newInv = commandParser.getText();                        //������
                        if (newInv.equals(""))
                            return "�� ���� �������� ����� �������� ����� ��� ������ " + getName() + "\n" +
                                    "������ �������:\n" +
                                    F.commandDescToText(getHelp().get(0));
                        setInvoker(newInv);
                        return "������ ����� �������� ����� ��� ������ " + getName() + ": " + getInvoker();
                    }
                }
            }
        }
        return super.processCommand(messageOriginal);
    }

    @Override
    public String toString() {
        return super.toString() + " ���������� ������ " + getInvoker();
    }

    //� ���������������� ������� �������������� ����� � ��������� ����������� ��������� ��� �������
    protected String defaultInvoker(){
        return defaultInvoker;
    }
    protected String getInvoker(){
        //������� ����� ������� ������ ������ ��� ����� ������. ���� ���� "������" ��� "������������"
        return invoker;
    }
    private void setInvoker(String invoker){
        this.invoker = invoker;
        getStorage().put("invoker", invoker).commit();
    }
}
