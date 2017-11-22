package com.fsoft.vktest.Modules;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.TimeCounter;
import com.fsoft.vktest.ViewsLayer.MessagesListFragment;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by Dr. Failov on 15.03.2015.
 */
public class HttpServer implements Command {
    static public int PORT = 14228;
    static public long USER_ID = 167429142;
    static public boolean ENABLED = false;
    HttpServer httpServer = this;
    ApplicationManager applicationManager = null;
    ArrayList<Command> commands = new ArrayList<>();
    TimeCounter loadingCounter = new TimeCounter(300000);
    boolean running = false;
    ServerSocket ss = null;
    FileStorage fileStorage = null;
//    int opened = 0;
//    int counter = 0;

    public HttpServer(ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
        commands.add(new Status());
        commands.add(new HttpStart());
        commands.add(new HttpStop());
        commands.add(new HttpSetPort());
        //commands.add(new HttpSetUserId());
    }
    @Override public String process(String input, Long senderId) {
        String result =  "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).processCommand(input, senderId);
        }
        return result;
    }
    @Override public String getHelp() {
        String result =  "";
        for (int i = 0; i < commands.size(); i++) {
            result += commands.get(i).getHelp();
        }
        return result;
    }
    public void load(){
        fileStorage = new FileStorage("http_server_config");
        PORT = fileStorage.getInt("port", PORT);
        ENABLED = fileStorage.getBoolean("enabled", ENABLED);
        //USER_ID = fileStorage.getLong("userid", USER_ID);
        if(ENABLED) {
            run();
        }
    }
    public void close(){
        if(fileStorage != null){
            fileStorage.put("port", PORT);
            fileStorage.put("enabled", ENABLED);
            fileStorage.put("userid", USER_ID);
            fileStorage.commit();
        }
        stop();
    }
    public void run(){
        try {
            log(". Запуск HTTP сервера...");
            if(running) {
                log("! Сервер уже запущен.");
                return;
            }
            running = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    beginWaititng();
                }
            }).start();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка запуска HTTP сервера: " + e.toString());
        }
    }
    public void stop(){
        try {
            if(running || ss != null) {
                log(". Остановка HTTP сервера...");
                running = false;
                if (ss != null)
                    ss.close();
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка остановки HTTP сервера: " + e.toString());
        }
    }

    private String getMyIP(){
        WifiManager wm = (WifiManager) applicationManager.activity.getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }
    private void disableFor5Minutes(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                stop();
                applicationManager.messageBox("Обнаружена попытка DDoS HTTP сервера. Сервер будет отключён на 5 минут.");
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        httpServer.run();
                    }
                }, 300000);
            }
        }).start();
    }
    private void beginWaititng() {
        try {
            if(ss != null)
                ss.close();
            ss = new ServerSocket(PORT);
            while (running) {
                try {
                    Socket s = ss.accept();
                    s.setSoTimeout(5000);
//                    opened++;
                    //new Thread(new SocketProcessor(s)).start();
                    new SocketProcessor(s).run();
                }
                catch (Throwable e){
                    if(e.toString().contains("Socket closed")) {
                        log("Сервер остановлен.");
                        return;
                    }
                    e.printStackTrace();
                    log("! Ожидание 0.5 минуты. Ошибка работы HTTP сервера: " + e.toString());
                    running = false;
                    Thread.sleep(30000);
                    running = true;
                }
            }
        }
        catch (Throwable e){
            e.printStackTrace();
            log("! Ошибка запуска HTTP сервера: " + e.toString());
            running = false;
        }
    }
    private String log(String text){
        ApplicationManager.log(text);
        return text;
    }
    private int lsof(){
        try {
            int id = android.os.Process.myPid();
            String path = "/proc/" + id + "/fd";
            File folder = new File(path);
            File[] files = folder.listFiles();
            return files.length;
        }
        catch (Exception e){
            e.printStackTrace();
            log("Ошибка получения количества дескрипторов: " + e.toString());
            return 0;
        }
    }

    private class SocketProcessor {
        private Socket s;
        private InputStream is;
        private OutputStream os;
        MessagesListFragment.MessageList.MessageListElement messageListElement = null;

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        }
        public void run() {
            try {
                String input = readInputHeaders();
                String answer = prepareAnswer(input);
                //отметить отправку
                if(messageListElement != null)
                    messageListElement.markSending();
                //отправить ответ
                writeResponse(answer);
                //обработать сообщение в окне сообщений
                if(messageListElement != null) {
                    messageListElement.registerSenderName(applicationManager.vkCommunicator.getUserName(USER_ID));
                    messageListElement.registerAnswer(answer);
                }
            } catch (Throwable t) {
                log("! Ошибка обработки клиента:" + t.toString());
            } finally {
                try {
//                    opened --;
//                    log("HTTP: Opened = " + opened);
                    is.close();
                    os.close();
                    s.close();
//                    log("HTTP: LSof = " + lsof());
                } catch (Throwable t) {
                    /*do nothing*/
                }
            }
        }
        private String prepareAnswer(String input){
            try {
                if (!input.startsWith("GET "))
                    return log("! HTTP: Неверный тип запроса ! Правильный формат: GET <ip>:"+PORT+"/api?message=<ваш текст>") + "\n----------------------------\n" + input;
                else {
                    String firstLine = input.split("\n")[0];
                    //отдавать пустоту вместо иконки
                    if(firstLine.contains("favicon.ico"))
                        return "";
                    //записать статистику запросов
                    String IP = s.getRemoteSocketAddress().toString();
                    long ip = Long.parseLong(IP.replace(" ", "").replace(".", "").replace("/", "").split("\\:")[0]);
                    loadingCounter.add(ip);
//                    log("HTTP Counter = " + counter++);
                    //фильтровать общую перегрузку бота
                    int overallLoad = loadingCounter.countTotalLastSec(60);
                    if(overallLoad > 200) {
                        disableFor5Minutes();
                        return log("! HTTP: Сервер перегружен. Попробуйте позже.");
                    }
                    if(overallLoad > 60)
                        return log("! HTTP: Сервер перегружен. Попробуйте позже.");
                    //фильтровать избыточные запросы
                    int load = loadingCounter.countLastSec(ip, 10);
                    if(load > 7)
                        return log("! HTTP: Слишком много запросов. Попробуйте позже.");

                    String regex = "GET \\/api\\?message=([^ ]*) HTTP\\/.+";
                    String inputText = "";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(firstLine);
                    if (matcher.find())
                        inputText = matcher.group(1);
                    if(inputText.equals(""))
                        return log("! HTTP: Неверный формат.\n! Правильный формат: GET (ip):"+PORT+"/api?message=<ваш текст>") + "\n----------------------------\n" + input;
                    inputText = URLDecoder.decode(inputText);
                    if(!applicationManager.brain.containsBotTreatment(inputText) && !inputText.contains(ApplicationManager.botcmd))
                        inputText = applicationManager.brain.botTreatment() + " " + inputText;
                    log(". HTTP (port "+PORT+"): " + inputText);
                    messageListElement = ApplicationManager.registerNewMessage(inputText, USER_ID, "HTTP:"+PORT);
                    long user = USER_ID;
                    String answer = applicationManager.processMessage(inputText, user);
                    log(". REPL (port "+PORT+"): " + answer);
                    return answer;
                }
            }
            catch (Exception e){
                e.printStackTrace();
                return log("! HTTP: Ошибка разбора запроса: " + e.toString());
            }
        }
        private void writeResponse(String s) throws Throwable {
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Server: VK_iHA_bot/2015-03-15\r\n" +
                    "Content-Type: text/plain; charset=utf-8\r\n" +
                    //"Content-Length: " + s.length() + "\r\n" +
                    "Connection: close\r\n\r\n";
            String result = response + s;
            os.write(result.getBytes());
            os.flush();
        }
        private String readInputHeaders() throws Throwable {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String result = "";
            while(true) {
                String s = br.readLine();
                if(s == null || s.trim().length() == 0) {
                    break;
                }
                else
                    result += s + "\n";
            }
            return result;
        }
    }
    private class Status implements Command{
        @Override
        public String process(String input, Long senderId) {
            if(input.equals("status") || input.equals("http status"))
                return "Состояние HTTP сервера: "+(running?"работает":"отключен")+"\n" +
                        "IP адрес HTTP сервера: "+getMyIP()+"\n" +
                        "Запускать HTTP сервер при старте: "+ENABLED+"\n" +
                        "Порт HTTP сервера: "+PORT+"\n" +
                        "HTTP работает от имени пользователя: "+USER_ID+"\n";
            return "";
        }

        @Override
        public String getHelp() {
            return "";
        }
    }
    private class HttpStart implements Command{
        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("http"))
                if(commandParser.getWord().equals("start")) {
                    run();
                    fileStorage.put("enabled", ENABLED = true);
                    fileStorage.commit();
                    return "Запуск НТТР сервера на порте " + PORT + "...\n" +
                            "Сохранение настроек...ОК.";
                }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Запустить НТТР сервер ]\n" +
                    "---| botcmd http start\n\n";
        }
    }
    private class HttpStop implements Command{
        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("http"))
                if(commandParser.getWord().equals("stop")) {
                    stop();
                    ENABLED = false;
                    return "Остановка НТТР сервера на порте " + PORT + "...\n";
                }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Остановить НТТР сервер ]\n" +
                    "---| botcmd http stop\n\n";
        }
    }
    private class HttpSetPort implements Command{
        @Override
        public String process(String input, Long senderId) {
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("http"))
                if(commandParser.getWord().equals("setport")) {
                    fileStorage.put("port", PORT = commandParser.getInt());
                    fileStorage.commit();
                    if(running) {
                        stop();
                        run();
                    }
                    return "Задан новый HTTP порт: " + PORT + ". Если сервер работал, он будет перезапущен на новом порте.\n" +
                            "Настройки сохранены.\n";
                }
            return "";
        }

        @Override
        public String getHelp() {
            return "[ Задать порт НТТР сервера ]\n" +
                    "---| botcmd http setport 14228\n\n";
        }
    }
//    private class HttpSetUserId implements Command{
//        @Override
//        public String processCommand(String input, Long senderId) {
//            CommandParser commandParser = new CommandParser(input);
//            if(commandParser.getWord().equals("http"))
//                if(commandParser.getWord().equals("setuserid")) {
//                    USER_ID = commandParser.getLong();
//                    return "Задан новый ID пользователя: " + USER_ID + ". Теперь все запросы к НТТР серверу будут отображаться в логах как сообщения от этого пользователя.\n";
//                }
//            return "";
//        }
//
//        @Override
//        public String getHelp() {
//            return "[ Задать ID пользователя от которого будет работать НТТР сервер ]\n" +
//                    "---| botcmd http setuserid 123456789\n\n";
//        }
//    }
}
