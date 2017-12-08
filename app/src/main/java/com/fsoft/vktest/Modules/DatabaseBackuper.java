package com.fsoft.vktest.Modules;

import android.os.Environment;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.F;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * модуль который отвечает за создание резервных копий и работу с ними
 * Created by Dr. Failov on 12.02.2017.
 */
public class DatabaseBackuper extends CommandModule {
    Timer backupTimer = null;
    final int backupInterval = (1000*60*60*12)/*12 часов*/ + (1000 * 60 * 11)/*11 минут*/;
    String backupsFolder = Environment.getExternalStorageDirectory() + File.separator + "backups";

    public DatabaseBackuper(ApplicationManager applicationManager) {
        super(applicationManager);
        backupTimer = new Timer();
        backupTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(isSomethingChanged())
                    backup("Автоматическое резервирование");
                else
                    log("Автоматическое резервирование отменено - изменений в базах не обнаружено.");
            }
        }, backupInterval, backupInterval);
    }
    @Override
    public String processCommand(Message message) {
        CommandParser commandParser = new CommandParser(message.getText());
        switch (commandParser.getWord()){
            case "makebackup":
                String name = commandParser.getText();
                if(name == null || name.equals(""))
                    name = "Резервная копия создана вручную";
                return backup(name);
            case "getbackuplist":
                return getBackupList();
            case "restorebackup":
                return restoreBackup(commandParser.getInt());
            case "removebackup":
                return removeBackup(commandParser.getInt());
            case "renamebackup":
                return renameBackup(commandParser.getInt(), commandParser.getText());
        }
        return "";
    }
    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<>();
        result.add(new CommandDesc(
                "Создать резервную копию(бекап) папки с настройками.",
                "Это позволит тебе в будущем восстановить все настройки бота так, " +
                        "как они установлены сейчас.\n" +
                        "Бот иногда создаёт резервные копии автоматически.\n" +
                        "Резервные копии хранятся в папке backup внутренней памяти.",
                "botcmd makebackup <имя создаваемого бекапа>"));


        result.add(new CommandDesc(
                "Получить список всех имеющихся резервных копий(бекапов).",
                "Резервные копии позволяют восстановить все настройки бота " +
                        "в тот момент в который эта резервная копия создавалась." +
                        "Бот иногда создаёт резервные копии автоматически.\n" +
                        "Резервные копии хранятся в папке backup внутренней памяти.",
                "botcmd getbackuplist"));


        result.add(new CommandDesc(
                "Восстановить резервную копию(бекап) состояния бота.",
                "Это заменит все текущие настройки бота на те, что были " +
                        "когда создавалась резервная копия.\n" +
                        "Порядковый номер бекапа - это тот номер, который " +
                        "написан возле имени и даты в выводе команды botcmd getbackuplist\n" +
                        "Во время восстановления бот также создаст новую резервную копию, " +
                        "чтобы иметь возможность восстановиться если что-то пойдёт не так.",
                "botcmd restorebackup <порядновый номер бекапа>"));


        result.add(new CommandDesc(
                "Удалить резервную копию(бекап) настроек бота.",
                "Порядковый номер бекапа - это тот номер, который " +
                        "написан возле имени и даты в выводе команды botcmd getbackuplist\n" +
                        "Резервные копии хранятся в папке backup внутренней памяти.",
                "botcmd removebackup <порядновый номер бекапа>"));


        result.add(new CommandDesc(
                "Переименовать резервную копию(бекап) настроек бота.",
                "Порядковый номер бекапа - это тот номер, который " +
                        "написан возле имени и даты в выводе команды botcmd getbackuplist\n" +
                        "Резервные копии хранятся в папке backup внутренней памяти.",
                "botcmd renamebackup <порядновый номер бекапа> <новое имя бекапа>"));

        return result;
    }
    @Override
    public void stop() {
        if(backupTimer != null){
            backupTimer.cancel();
            backupTimer = null;
        }
        super.stop();
    }

    private String backup(String name){
        String result = "";

        log(". Waiting while saving finishes...");
        F.sleep(1000);

        try{
            File originalFolder = new File(applicationManager.getHomeFolder());
            result += log(". Оригинальная папка: " + originalFolder.getPath() + "\n");
            File copyFolder;
            if(name == null || name.equals(""))
                copyFolder = new File(backupsFolder + File.separator + sdf.format(Calendar.getInstance().getTime()) + " " + applicationManager.programName+"_backup");
            else
                copyFolder = new File(backupsFolder + File.separator + sdf.format(Calendar.getInstance().getTime()) + " " + name);
            result += log(". Целевая папка: " + copyFolder.getPath() + "\n");
            result += copyDirectory(originalFolder, copyFolder);
            result += clearOldBackups();
        }
        catch (Throwable e){
            e.printStackTrace();
            result += log("! Ошибка создания бекапа: " + e.toString() + "\n");
        }
        finally {

        }
        result += log(". Завершено.\n");
        return result;
    }
    private File getLastBackup(){
        try{
            File folder = new File(backupsFolder);
            log(". Место хранения бекапов: " + folder.getPath());
            if(!folder.isDirectory()) {
                log("! Ошибка: это не папка. Продолжение операции невозможно.");
                return null;
            }
            File[] backups = folder.listFiles();
            if(backups == null || backups.length == 0) {
                log(". Папка пустая");
                return null;
            }
            File newest = null;
            for (int i = 0; i < backups.length; i++) {
                File database = new File(backups[i] + File.separator + "bot" + File.separator + "answer_databse.bin");
                if(database.isFile()){
                    if(newest == null)
                        newest = backups[i];
                    if(backups[i].lastModified() > newest.lastModified())
                        newest = backups[i];
                }
            }
            log(". Последний бекап: " + newest);
            return newest;

        } catch (Exception e){
            e.printStackTrace();
            log("! Ошибка получения последнего бекапа: " + e.toString());
            return null;
        }
    }
    private boolean isSomethingChanged(){
        File lastBackup = getLastBackup();
        if(lastBackup == null)
            return true;
        File database = new File(lastBackup + File.separator + "bot" + File.separator + "answer_databse.bin");
        int lastBackupLines = countFileLines(database);
        if(lastBackupLines < 0)
            return true;

        File currentDatabase = new File(applicationManager.getHomeFolder() + File.separator + "bot" + File.separator + "answer_databse.bin");
        log(". Текущая база: " + currentDatabase);
        int currentDBLines = countFileLines(currentDatabase);
        if(currentDBLines < 0)
            return true;

        log("Ответов в текущей базе: " + currentDBLines + "; Ответов в базе бекапа: " + lastBackupLines + ";");
        return Math.abs(lastBackupLines - currentDBLines) > 5;
    }
    private String clearOldBackups(){
        String result = "";
        File folderWithBackups = new File(backupsFolder);
        try{
            File[] files = folderWithBackups.listFiles();
            if(files != null && files.length > 20){
                result += log(". Поиск автоматически сгенерированных бекапов...\n");
                ArrayList<File> automaticGeneratedBackups = new ArrayList<>();
                for (int i=files.length-1; i>= 0; i--)
                    if(files[i].getName().matches("....-..-..-..-.. Автоматическое резервирование"))
                        automaticGeneratedBackups.add(files[i]);
                files = new File[automaticGeneratedBackups.size()];
                for (int i = 0; i < automaticGeneratedBackups.size(); i++)
                    files[i] = automaticGeneratedBackups.get(i);

                result += log(". Очистка старых бекапов...\n");
                //сортировка. В начало списка - более новые.
                for (int i = 0; i < files.length; i++) {
                    for (int j = 1; j < files.length; j++) {
                        if(files[j-1].lastModified() < files[j].lastModified()){
                            File tmp = files[j-1];
                            files[j-1] = files[j];
                            files[j] = tmp;
                        }
                    }
                }
                for (int i=20; i<files.length; i++){
                    result += log("Удаление " + files[i] + ": " + deleteFile(files[i].getPath()) + "\n");
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
            result += log("! Ошибка очистки старых бекапов: " + e.toString() + "\n");
        }
        return result;
    }
    private int countFileLines(File input){
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(input), Charset.forName("UTF-8")));
            int c;
            int cnt = 0;
            while((c = reader.read()) != -1) {
                char character = (char) c;
                if(character == '\n')
                    cnt ++;
            }
            return cnt;
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Error counling lines in file: " + e.toString());
            return -1;
        }
    }
    private String getBackupList(){
        try{
            File folder = new File(backupsFolder);
            String result = "Место хранения бекапов: " + folder.getPath() + "\n";
            if(!folder.isDirectory())
                return result + "Ошибка: это не папка. Продолжение операции невозможно.";
            File[] backups = folder.listFiles();
            result += "Файлов в папке: " + backups.length + "\n";
            result += "Найденные бекапы: \n";
            int backups_cnt = 0;
            for (int i = 0; i < backups.length; i++) {
                if(backups[i].isDirectory()){
                    File database = new File(backups[i] + File.separator + "bot" + File.separator + "answer_databse.bin");
                    if(database.isFile()){
                        int lines = countFileLines(database);
                        if(lines > 0) {
                            result += i + ") " + backups[i].getName() + " [" + lines + "]\n";
                            backups_cnt ++;
                        }
                    }
                }
            }
            if(backups_cnt == 0)
                result += "Не найдено ни одного валидного бекапа.\n";
            return result;
        } catch (Exception e){
            e.printStackTrace();
            return log("Ошибка чтения списка бекапов: " + e.toString());
        }
    }
    private String removeBackup(int number){
        try{
            File folder = new File(backupsFolder);
            String result = "Место хранения бекапов: " + folder.getPath() + "\n";
            if(!folder.isDirectory())
                return result + "Ошибка: это не папка. Продолжение операции невозможно.";
            File[] backups = folder.listFiles();
            result += "Файлов в папке: " + backups.length + "\n";
            result += "Найденные бекапы: \n";
            int backups_cnt = 0;
            for (int i = 0; i < backups.length; i++) {
                if(backups[i].isDirectory()){
                    File database = new File(backups[i] + File.separator + "bot" + File.separator + "answer_databse.bin");
                    if(database.isFile()){
                        int lines = countFileLines(database);
                        if(lines > 0) {
                            result += i + ") " + backups[i].getName() + " [" + lines + "]\n";
                            if(i == number){
                                result += "Да! Найден нужный бекап. Удаление...\n";
                                result += deleteFile(backups[i].getPath()) + "\n";
                                return result;
                            }
                            backups_cnt ++;
                        }
                    }
                }
            }
            if(backups_cnt == 0)
                result += "Не найдено ни одного валидного бекапа.\n";
            return result;
        } catch (Exception e){
            e.printStackTrace();
            return log("! Ошибка чтения списка бекапов: " + e.toString());
        }
    }
    private String restoreBackup(int number){
        try{
            File folder = new File(backupsFolder);
            String result = "Место хранения бекапов: " + folder.getPath() + "\n";
            if(!folder.isDirectory())
                return result + "Ошибка: это не папка. Продолжение операции невозможно.";
            File[] backups = folder.listFiles();
            result += "Файлов в папке: " + backups.length + "\n";
            result += "Найденные бекапы: \n";
            int backups_cnt = 0;
            for (int i = 0; i < backups.length; i++) {
                if(backups[i].isDirectory()){
                    File database = new File(backups[i] + File.separator + "bot" + File.separator + "answer_databse.bin");
                    if(database.isFile()){
                        int lines = countFileLines(database);
                        if(lines > 0) {
                            result += i + ") " + backups[i].getName() + " [" + lines + "]\n";
                            if(i == number){
                                result += "Да! Найден нужный бекап. Восстановление...\n";
                                result += "Перед восстановлением создаю бекап...\n";
                                result += backup("Резервная копия перед восстановлением") + "\n";
                                result += "Удаляю текущие данные...\n";
                                result += deleteFile(applicationManager.getHomeFolder()) + "\n";
                                result += "Копирую выбранный бекап на место сохранений...\n";
                                result += copyDirectory(backups[i], new File(applicationManager.getHomeFolder())) + "\n";
                                result += "Устанавливаю метку для пропуска сохранения...\n";
                                result += "Выполнено! Сейчас программа будет перезагружена.";
                                new Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        //// TODO: 08.12.2017 applicationManager.activity.restart();
                                    }
                                }, 5000);
                                return result;
                            }
                            backups_cnt ++;
                        }
                    }
                }
            }
            if(backups_cnt == 0)
                result += "Не найдено ни одного валидного бекапа.\n";
            return result;
        } catch (Exception e){
            e.printStackTrace();
            return log("Ошибка чтения списка бекапов: " + e.toString());
        }
    }
    private String renameBackup(int number, String name){
        try{
            File folder = new File(backupsFolder);
            String result = "Место хранения бекапов: " + folder.getPath() + "\n";
            if(!folder.isDirectory())
                return result + "Ошибка: это не папка. Продолжение операции невозможно.";
            File[] backups = folder.listFiles();
            result += "Файлов в папке: " + backups.length + "\n";
            result += "Найденные бекапы: \n";
            int backups_cnt = 0;
            for (int i = 0; i < backups.length; i++) {
                if(backups[i].isDirectory()){
                    File database = new File(backups[i] + File.separator + "bot" + File.separator + "answer_databse.bin");
                    if(database.isFile()){
                        int lines = countFileLines(database);
                        if(lines > 0) {
                            result += i + ") " + backups[i].getName() + " [" + lines + "]\n";
                            if(i == number){
                                result += "Да! Найден нужный бекап. Переименование...\n";
                                File newname = new File(backups[i].getParent() + File.separator + name);
                                result += "Новое имя папки: " + newname + "\n";
                                boolean r = backups[i].renameTo(newname);
                                result += "Результат переименования: " + r;
                                return result;
                            }
                            backups_cnt ++;
                        }
                    }
                }
            }
            if(backups_cnt == 0)
                result += "Не найдено ни одного валидного бекапа.\n";
            return result;
        } catch (Exception e){
            e.printStackTrace();
            return log("! Ошибка чтения списка бекапов: " + e.toString());
        }
    }

    String deleteFile(String in){
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
    boolean deleteDirectory(File directory) {
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

    private String copyDirectory(File sourceLocation , File targetLocation) throws IOException {
        String result = "";
        try {
            if (sourceLocation.isDirectory()) {
                if (!targetLocation.exists()) {
                    result += log(". Создание папки " + targetLocation.getName() + "...\n");
                    targetLocation.mkdirs();
                }

                String[] children = sourceLocation.list();
                for (int i = 0; i < children.length; i++)
                    result += copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            } else {
                result += log(". Копирование " + sourceLocation.getName() + " -> " + targetLocation.getName() + "...\n");
                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
                in.close();
                out.close();
            }
        }
        catch (Throwable e){
            e.printStackTrace();
            result += log("! Ошибка копирования папки: " + e.toString() + "\n");
        }
        return result;
    }
}
