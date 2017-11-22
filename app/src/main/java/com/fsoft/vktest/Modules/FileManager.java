package com.fsoft.vktest.Modules;

import android.os.Environment;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.ResourceFileReader;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Dr. Failov on 12.02.2017.
 */
public class FileManager extends CommandModule {
    private HashMap<Long, FileSession> current = new HashMap<>();

    public FileManager(ApplicationManager applicationManager) {
        super(applicationManager);
    }
    @Override public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<CommandDesc>();
        result.add(new CommandDesc("Начать управление файлами",
                "[ list - список файлов]\n" +
                        "[ up - подняться на уровень вверх ]\n" +
                        "[ cd ... - перейти в папку ]\n" +
                        "[ get ... - получить файл ]\n" +
                        "[ put (1) (2) - загрузить прикрепленный файл(2) в папку под именем (1) ]\n" +
                        "[ del ... - удалить файл или папку ]\n" +
                        "[ read ... - вывести содержимое текстового файла ]\n" +
                        "[ write ... ... - Записать текст (2) в файл (1)]\n" +
                        "[ end - закончить сеанс управления файлами ]\n",
                "botcmd filemanager"
                ));
        return result;
    }
    @Override public String processCommand(Message message) {
        long senderId = message.getAuthor();
        String input = message.getText();
        if(isOpened(senderId)){
            if(input.equals("end")){
                if(endSession(senderId))
                    return "Сессия для "+applicationManager.vkCommunicator.getUserName(senderId)+" закрыта.";
                else
                    return "Сессия для "+applicationManager.vkCommunicator.getUserName(senderId)+" не закрыта. А она вообще была открыта?";
            }
            FileSession fileSession = getSession(senderId);
            if(fileSession != null)
                return fileSession.processCommand(message);
        }
        else if(input.equals("filemanager")){
            FileSession fileSession = openSession(senderId);
            if(fileSession != null)
                return "Сессия файл-менеджера для пользователя "+applicationManager.vkCommunicator.getUserName(senderId)+" открыта. Вот краткая инструкция:\n (каждую команду писать начиная с botcmd)\n"+getHelp()+"\n" + fileSession.list();
            else
                return "Сессия для "+applicationManager.vkCommunicator.getUserName(senderId)+" не открыта. Не знаю почему.";
        }
        return "";
    }

    private boolean isOpened(Long user){
        return current.containsKey(user);
    }
    private FileSession openSession(long userId){
        if(!isOpened(userId)){
            FileSession fileSession = new FileSession(userId);
            current.put(userId, fileSession);
            return fileSession;
        }
        else
            return getSession(userId);
    }
    private boolean endSession(long userId){
        if(isOpened(userId)){
            current.remove(userId);
            return true;
        }
        else
            return false;
    }
    private FileSession getSession(long userId){
        if(isOpened(userId))
            return current.get(userId);
        return null;
    }

    private class FileSession implements Command{
        File currentFile = getRoot();
        Long userId = 0L;

        public FileSession(Long userId) {
            this.userId = userId;
        }
        @Override public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            switch(commandParser.getWord()){
                case "list":{
                    return list();
                }
                case "up":{
                    return up();
                }
                case "cd":{
                    return cd(commandParser.getText());
                }
                case "get":{
                    return get(commandParser.getText());
                }
                case "put":{
                    return put(commandParser.getWord(), commandParser.getText());
                }
                case "del":{
                    return del(commandParser.getText());
                }
                case "read":{
                    return read(commandParser.getText());
                }
                case "write":{
                    return write(commandParser.getWord(), commandParser.getText());
                }
            }
            return "";
        }
        @Override public ArrayList<CommandDesc> getHelp() {
            return new ArrayList<CommandDesc>();
        }

        private String write(String file, String text){
            String result = "Запись в файл " + file + " текста  " + text + "... \n";
            try{
                result += "Открытие файла...\n";
                FileWriter fileWriter = new FileWriter(new File(currentFile + File.separator + file));
                result += "Запись...\n";
                fileWriter.write(text);
                fileWriter.close();
                result += "Готово.\n";
            }
            catch (Exception e){
                e.printStackTrace();
                result += "Ошибка записи: " + e.toString();
            }
            return result;
        }
        private String read(String file){
            File next = new File(currentFile + File.separator + file);
            if(next.isFile()){
                if(next.length() < 4000) {
                    String result = ResourceFileReader.readFromFile(next.getPath());
                    return "Содержимое файла: " + result;
                }
                else
                    return "Файл "+file+" слишком большой";
            }
            else
                return "Файла  "+file+" нет";
        }
        private String put(String dest, String src){
            File dloaded = applicationManager.vkCommunicator.downloadFileFromVk(src);
            if(dloaded != null && dloaded.isFile()){
                File destination = new File(currentFile + File.separator + dest);
                boolean copied = ResourceFileReader.copyFile(dloaded.getPath(), destination.getPath());
                return "Загрузка файла из " + src + " в " + destination + " результат: " + copied;
            }
            else
                return "Файла "+dloaded+" нет в загрузках";
        }
        private String del(String file){
            File next = new File(currentFile + File.separator + file);
            if(next.isFile() || next.isDirectory()){
                String result = deleteFile(next.getPath());
                return "Удаление файла: " + result;
            }
            else
                return "Файла или папки "+file+" нет";
        }
        private String get(String file){
            File next = new File(currentFile + File.separator + file);
            if(next.isFile()){
                String result = applicationManager.vkCommunicator.uploadDocument(next);
                return "Документ загружен: " + result;
            }
            else
                return "Файла "+file+" нет";
        }
        private String getList(File path){
            String result = "Папка " + path.getPath() + ":\n";
            if(path.isDirectory()){
                File[] list = path.listFiles();
                if(list != null)
                    for (File file:list)
                        result += (file.isDirectory()?"+":"-")+" "+file.getName()+"\n";
                else
                    result += "Папка пуста.";
            }
            else
                result += "Это не папка.";
            return result;
        }
        private String list(){
            return getList(getCurrentFolder());
        }
        private String up(){
            File upper = currentFile.getParentFile();
            if(upper != null)
                currentFile = upper;
            else
                return "Выше некуда.";
            return list();
        }
        private String cd(String folder){
            File next = null;
            if(folder.startsWith("/"))
                next = new File(folder);
            else
                next = new File(currentFile + File.separator + folder);
            if(next.isDirectory()){
                currentFile = next;
                return list();
            }
            else
                return "Папки "+folder+" нет";
        }
        private File getCurrentFolder(){
            return currentFile;
        }
        private File getRoot(){
            return Environment.getExternalStorageDirectory();
        }
        private String deleteFile(String in){
            String result = "Удаление обьекта...\n";
            File file = new File(in);
            if(file.isDirectory()){
                result += "Тип обьекта: папка\n";
                result += "Удаление: " + deleteDirectory(file);
            }
            else if(file.isFile()){
                result += "Тип обьекта: файл\n";
                result += "Удаление: " + file.delete();
            }
            else {
                result += "Тип обьекта определить не удалось.\n";
            }
            return result;
        }
        private boolean deleteDirectory(File directory) {
            if(directory.exists()){
                File[] files = directory.listFiles();
                if(null!=files){
                    for(int i=0; i<files.length; i++) {
                        if(files[i].isDirectory()) {
                            deleteDirectory(files[i]);
                        }
                        else {
                            files[i].delete();
                        }
                    }
                }
            }
            return(directory.delete());
        }
    }
}
