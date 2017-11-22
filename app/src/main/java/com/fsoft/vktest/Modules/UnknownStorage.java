package com.fsoft.vktest.Modules;

import com.fsoft.vktest.ApplicationManager;

import java.io.File;
import java.util.ArrayList;

/**
 * этот класс занимается хранением неизвестных.
 * позволяет:
 * - положить фразу в неизвестные
 * - изъять фразу из неизвестных
 *
 * Неизвестные должны храниться в формате 1 фраза на строку.
 * Переходы на новую строку удалять из текста неизвестных.
 * Created by Dr. Failov on 13.04.2017.
 */
public class UnknownStorage extends CommandModule{
    private ArrayList<String> unknown = new ArrayList<>();
    private File fileUnknown = null;

    public UnknownStorage(ApplicationManager applicationManager) {
        super(applicationManager);
    }
    public int put(String text){
        //возвращает количество
        return 0;
    }
    public String pop(){
        return "";
    }
    public int count(){
        //возвращает размер базы
        return  0;
    }
}
