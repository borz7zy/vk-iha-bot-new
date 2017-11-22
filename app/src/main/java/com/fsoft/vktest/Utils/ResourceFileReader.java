package com.fsoft.vktest.Utils;

import android.content.res.Resources;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;

import java.io.*;

/**
 * класс для работы с файлами которые имеют свои стандартные
 * Created by Dr. Failov on 21.09.2014.
 * Edited by Dr. Failov on 29.09.2017.
 */
public class ResourceFileReader extends CommandModule {
    private File file = null;

    public ResourceFileReader(ApplicationManager applicationManager, int resourceId) {
        super(applicationManager);

        Resources resources = null;
        if(applicationManager != null && applicationManager.getContext() != null)
            resources = applicationManager.getContext().getResources();
        if(resources != null){
            String fileName = resources.getResourceEntryName(resourceId);
            file = new File(applicationManager.getHomeFolder(), fileName);
        }
        if(file != null && !file.exists()){
            log(". Загрузка файла " + file.getName() + " из ресурсов...");
            F.copyFile(resourceId, resources, file);
        }
    }
    public File getFile(){
        return file;
    }
    public String readFile(){
        try {
            if(file == null){
                log("! Ошибка: Инициализация ResourceFileReader не была выполнена, " +
                        "поэтому я не могу проочитать файл.");
                return "";
            }
            if (!file.exists()) {
                log("! Ошибка: Файла "+file.getName()+" нет.");
                return "";
            }
            return F.readFromFile(file);
        }
        catch (Exception e){
            log("! Ошибка чтения файла в классе ResourceFileReader: " + e.toString());
            e.printStackTrace();
            return "";
        }
    }
    public boolean writeFile(String toWrite){
        try{
            if(file != null){
                log(". Запись "+file.getName()+"...");
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(toWrite);
                fileWriter.close();
                return true;
            }
            else {
                log("! Из-за некорректной инициализации ResourceFileReader, запись в файл невозможна.");
                return false;
            }
        }
        catch (Exception e){
            log(". Ошибка записи файла в классе ResourceFileReader: "+e.toString());
            e.printStackTrace();
            return false;
        }
    }
}
