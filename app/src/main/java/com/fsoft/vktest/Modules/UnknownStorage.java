package com.fsoft.vktest.Modules;

import com.fsoft.vktest.ApplicationManager;

import java.io.File;
import java.util.ArrayList;

/**
 * ���� ����� ���������� ��������� �����������.
 * ���������:
 * - �������� ����� � �����������
 * - ������ ����� �� �����������
 *
 * ����������� ������ ��������� � ������� 1 ����� �� ������.
 * �������� �� ����� ������ ������� �� ������ �����������.
 * Created by Dr. Failov on 13.04.2017.
 */
public class UnknownStorage extends CommandModule{
    private ArrayList<String> unknown = new ArrayList<>();
    private File fileUnknown = null;

    public UnknownStorage(ApplicationManager applicationManager) {
        super(applicationManager);
    }
    public int put(String text){
        //���������� ����������
        return 0;
    }
    public String pop(){
        return "";
    }
    public int count(){
        //���������� ������ ����
        return  0;
    }
}
