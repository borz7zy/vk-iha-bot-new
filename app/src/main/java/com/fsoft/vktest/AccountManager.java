package com.fsoft.vktest;

import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Communication.Account.VK.VkAccountCore;
import com.fsoft.vktest.Utils.CommandParser;

import java.io.File;
import java.util.*;

/**
 * класс для управления аккаунтами
 * Created by Dr. Failov on 13.11.2014.
 */


public class AccountManager extends ArrayList<VkAccountCore> implements Command {
    private String homeFolder = "";
    private ApplicationManager applicationManager;
    private final Object getActiveSync = new Object();
    private ArrayList<Command> commands = new ArrayList<>();

    public AccountManager(ApplicationManager applicationManager){
        this.applicationManager = applicationManager;
        homeFolder = ApplicationManager.getHomeFolder() + File.separator + "accounts";
        commands.add(new Status());
        commands.add(new RemAccount());
        commands.add(new AddAccount());
        commands.add(new Accs());
    }
    public void addAccount(final String token, final long id){
        if(get(id) == null) {
            VkAccountCore account = new VkAccountCore(applicationManager, getNextAccountFileName(), token, id);
            //account.waitUntilActive();
            add(account);
//            if (applicationManager.vkCommunicator.walls.size() == 0)
//                log(applicationManager.vkCommunicator.addOwnerID(account.id));
        }
        else
            log("! Повтор аккаунта "+id+" не добавлен.");
    }
    public void addAccount(){
        VkAccountCore account = new VkAccountCore(applicationManager, getNextAccountFileName());
        //account.waitUntilActive();
        add(account);
//        if(applicationManager.vkCommunicator.walls.size() == 0)
//            log(applicationManager.vkCommunicator.addOwnerID(account.id));
    }
    public void addAccount(final VkAccountCore account){
        //account.waitUntilActive();
        add(account);
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if (applicationManager.vkCommunicator.walls.size() == 0)
//                    log(applicationManager.vkCommunicator.addOwnerID(account.id));
//            }
//        }, 2000);
    }
    public void removeAccount(final VkAccountCore account){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    account.setActive(false, "Аккаунт удаляется");
                    account.removeFile();
                    remove(account);
                }
                catch (Exception e){
                    e.printStackTrace();
                    log("Error removeAccount: " + e.toString());
                }
            }
        }).start();
    }
    public void load(){
        File folder = new File(homeFolder);
        log(". Поиск аккаунтов в " + folder.getPath() + " ...");
        File[] files = folder.listFiles();
        if(files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File file, File file2) {
                    return file.getPath().compareTo(file2.getPath());
                }
            });
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if(file.isFile()){
                    log(". Добавление аккаунта " + file.getPath());
                    addAccount(new VkAccountCore(applicationManager, file.getPath()));
                    sleep(1000);
                }
            }
        }
        else {
            String help = "Для работы бота нужно добавить аккаунт.\n" +
                    "Перейди на вкладку Аккаунты и нажми кнопку Добавить аккаунт.\n" +
                    "Войди в свой или фейковый аккаунт для бота. Жди дальнейших инструкций.";
            log(help);
            applicationManager.messageBox(help);
        }
    }
    public void close(){
        for (int i = 0; i < size(); i++) {
            get(i).close();
        }
        applicationManager = null;
    }
    public @Override String getHelp() {
        String result = "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).getHelp();
        }
        for (int i = 0; i < size(); i++) {
            result += get(i).getHelp();
        }
        return result;
    }
    public @Override String process(String text, Long senderId) {
        String result = "";
        for (int i = 0; i < size(); i++) {
            result += get(i).processCommand(text, senderId);
        }
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).processCommand(text, senderId);
        }
        return result;
    }
    public String getNextAccountFileName(){
        log(". Поиск нового имени файла ...");
        for (int i = 0; true; i++) {
            String path = homeFolder + File.separator + "account" + i;
            File file = new File(path);
            if(!file.exists()){
                log(". Найдено имя: " + path);
                return path;
            }
        }
    }
    public VkAccountCore getActive(){
        synchronized (getActiveSync) {
            Random random = new Random();
            while (true) {
                ArrayList<VkAccountCore> activeAccounts = new ArrayList<>();
                int active = 0;
                for (int i = 0; i < size(); i++) {
                    VkAccountCore account = get(i);
                    if (account.isReady() && account.id > 0)
                        activeAccounts.add(account);
                    if(account.isActive())
                        active++;
                }
                if (activeAccounts.size() > 0) {
                    return activeAccounts.get(random.nextInt(activeAccounts.size()));
                }
//                if(active == 0)
//                    return null;
                sleep(500);
            }
        }
    }
    public VkAccountCore get(long userID){
        synchronized (getActiveSync) {
            for (int i = 0; i < size(); i++) {
                VkAccountCore account = get(i);
                if (account.id == userID)
                    return account;
            }
            return null;
        }
    }
    public int getActiveCount() {
        int cnt=0;
        for (VkAccountCore vkAccount:this)
            if(vkAccount.isActive())
                cnt ++;
        return cnt;
    }
    public int getTotalReceivedMessagesCount() {
        int cnt=0;
        for (VkAccountCore vkAccount:this)
            cnt += vkAccount.getMessageCounter();
        return cnt;
    }
    public int getTotalApiCount() {
        int cnt=0;
        for (VkAccountCore vkAccount:this)
            cnt += vkAccount.getApiCounter();
        return cnt;
    }
    public boolean containsAccount(long id){
        for(VkAccountCore account:this) {
            if (account.id == id){
                return true;
            }
        }
        return false;
    }

    private void sleep(int mili){
        try{
            Thread.sleep(mili);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private void log(String text){
        ApplicationManager.log(text);
    }

    private class Status implements Command{
        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("status")){
                return "Количество аккаунтов: " + size() + "\n";
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
    private class AddAccount implements Command{
        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("accs")){
                if(commandParser.getWord().equals("add")){
                    String token = commandParser.getWord();
                    long id = commandParser.getLong();
                    addAccount(token, id);
                    return "В список добавлен аккаунт с id = " + id +  ", token = " + token;
                }
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Добавить аккаунт в список аккаунтов ]\n" +
                    "---| botcmd accs add <токен> <ID пользователя>\n\n";
        }
    }
    private class RemAccount implements Command{
        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("accs")){
                if(commandParser.getWord().equals("rem")){
                    long id = commandParser.getLong();
                    VkAccountCore acc = get(id);
                    if(acc == null)
                        return "Аккаунта с id = " + id +  " нет.";
                    else{
                        removeAccount(acc);
                        return "Удаление аккаунта "+acc.userName+" ("+acc.id+") успешно.";
                    }
                }
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Удалить аккаунт из списка аккаунтов ]\n" +
                    "---| botcmd accs rem <ID пользователя>\n\n";
        }
    }
    private class Accs implements Command{
        @Override
        public String process(String input, Long senderId) {
            if(input.equals("accs")){
                String result = "Аккаунты в программе: \n";
                for (VkAccountCore account:applicationManager.vkAccounts){
                    result += account.userName + " - " + account.id + " (http://vk.com/id"+account.id+")" + (!account.isActive()?(" - отключен ("+account.getState()+")"):"") + " \n";
                }
                return result;
            }
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
}