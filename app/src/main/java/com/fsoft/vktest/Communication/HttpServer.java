package com.fsoft.vktest.Communication;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.widget.ImageView;

import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.Attachment;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.AnswerInfrastructure.MessageBase;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.AccountBase;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.Command;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.R;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.TimeCounter;
import com.fsoft.vktest.Utils.User;

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
public class HttpServer extends CommandModule implements AccountBase {
    //порт на котором сервер слушает
    private int port = 14228;
    //ID пользователя которым притворяется сервер (ЭТО КОСТЫЛЬ!)
    private long userId = 167429142;
    //Включён ли сервер
    private boolean enabled = false;
    //Счётчик времени для предотвращения перегрузки сервера
    private TimeCounter loadingCounter = new TimeCounter(300000);
    private FileStorage fileStorage = null;
    private Thread serverThread = null;
    private ServerSocket ss = null;
    private Context context; // Add Context

    public HttpServer(ApplicationManager applicationManager) {
        super(applicationManager);
        fileStorage = new FileStorage("http_server_config", applicationManager);
        port = fileStorage.getInt("port", port);
        enabled = fileStorage.getBoolean("enabled", enabled);

        childCommands.add(new Status(applicationManager));
        childCommands.add(new HttpStart(applicationManager));
        childCommands.add(new HttpStop(applicationManager));
        childCommands.add(new HttpSetPort(applicationManager));

        if(enabled) {
            startServer();
        }
    }
    @Override
    public boolean remove() {
        return false;
    }
    @Override
    public void login() {
        log("Была вызвана функция login() для HTTP сервера. HTTP серверу не требуется логин.");
    }
    @Override
    public void startAccount() {
        setEnabled(true);
    }
    @Override
    public void stopAccount() {
        setEnabled(false);
    }
    @Override
    public boolean isMine(String commandTreatment) {
        return false;
    }
    @Override
    public boolean isToken_ok() {
        return false;
    }
    @Override
    public String getState() {
        return "HTTP сервер " + (serverThread == null?"работает":"остановлен");
    }
    @Override
    public FileStorage getFileStorage() {
        return null;
    }
    @Override
    public long getId() {
        return 0;
    }
    @Override
    public String getToken() {
        return null;
    }
    @Override
    public String getFileName() {
        return null;
    }
    @Override
    public String state(String state) {
        return null;
    }
    @Override
    public void setState(String state) {

    }
    @Override
    public void setId(long id) {

    }
    @Override
    public void setToken(String token) {

    }
    @Override
    public void setToken_ok(boolean token_ok) {

    }
    public String getScreenName() {
        return "HTTP сервер:"+port;
    }
    public void setScreenName(String screenName) {
    }

    @Override
    public void fillAvatar(ImageView imageView) {
        imageView.setImageResource(R.drawable.bot);
    }


    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
        fileStorage.put("port", port);
        fileStorage.commit();
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if(isRunning() && !enabled)
            stopServer();
        if(!isRunning() && enabled)
            startServer();

        fileStorage.put("enabled", enabled);
        fileStorage.commit();
    }
    public boolean isRunning(){
        return serverThread != null;
    }

    private void startServer(){
        if(serverThread != null)
            return;
        log(". Запуск HTTP сервера...");
        try {
            serverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    beginWaititng();
                }
            });
            serverThread.start();
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка запуска HTTP сервера: " + e.toString());
        }
    }
    private void stopServer(){
        try {
            if(serverThread != null || ss != null) {
                log(". Остановка HTTP сервера...");
                serverThread = null;
                if (ss != null)
                    ss.close();
                ss = null;
            }
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка остановки HTTP сервера: " + e.toString());
        }
    }
    private String getMyIP(){
        WifiManager wm = (WifiManager) applicationManager.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }
    private void disableFor5Minutes(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                stop();
                messageBox("Обнаружена попытка DDoS HTTP сервера. Сервер будет отключён на 5 минут.");
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startServer();
                    }
                }, 300000);
            }
        }).start();
    }
    private void beginWaititng() {
        try {
            if(ss != null)
                ss.close();
            ss = new ServerSocket(port);
            while (serverThread != null) {
                try {
                    Socket s = ss.accept();
                    s.setSoTimeout(5000);
                    new SocketProcessor(s).run();
                }
                catch (Throwable e){
                    if(e.toString().contains("Socket closed")) {
                        log("Сервер остановлен.");
                        return;
                    }
                    e.printStackTrace();
                    log("! Ожидание 0.5 минуты. Ошибка работы HTTP сервера: " + e.toString());
                    Thread.sleep(30000);
                }
            }
        }
        catch (Throwable e){
            e.printStackTrace();
            log("! Ошибка запуска HTTP сервера: " + e.toString());
            serverThread = null;
        }
    }


    private class SocketProcessor {
        private Socket s;
        private InputStream is;
        private OutputStream os;

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        }
        public void run() {
            try {
                String input = readInputHeaders();
                String answer = prepareAnswer(input);
                //отправить ответ
                writeResponse(answer);
            } catch (Throwable t) {
                log("! Ошибка обработки клиента:" + t.toString());
            } finally {
                try {
                    is.close();
                    os.close();
                    s.close();
                } catch (Throwable t) {
                    /*do nothing*/
                }
            }
        }
        private String prepareAnswer(String input){
            try {
                if (!input.startsWith("GET "))
                    return log("! HTTP: Неверный тип запроса ! Правильный формат: GET <ip>:"+port+"/api?message=<ваш текст>") + "\n----------------------------\n" + input;
                else {
                    String firstLine = input.split("\n")[0];
                    //отдавать пустоту вместо иконки
                    if(firstLine.contains("favicon.ico"))
                        return "";
                    //записать статистику запросов
                    String IP = s.getRemoteSocketAddress().toString();
                    long ip = Long.parseLong(IP.replace(" ", "").replace(".", "").replace("/", "").split(":")[0]);
                    loadingCounter.add(ip);
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

                    String regex = "GET /api\\?message=([^ ]*) HTTP/.+";
                    String inputText = "";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(firstLine);
                    if (matcher.find())
                        inputText = matcher.group(1);
                    if(inputText.equals(""))
                        return log("! HTTP: Неверный формат.\n! Правильный формат: GET (ip):"+port+"/api?message=<ваш текст>") + "\n----------------------------\n" + input;
                    inputText = URLDecoder.decode(inputText);

                    log(". HTTP (port "+port+"): " + inputText);

                    User user = new User().vk(userId);
                    Message messageWithAnswer = applicationManager.getBrain().processMessage(new Message(
                            MessageBase.SOURCE_HTTP,
                            inputText,
                            user,
                            new ArrayList<Attachment>(),
                            HttpServer.this,
                            null
                    ));
                    String answer = "";
                    if(messageWithAnswer.getAnswer() != null)
                        answer = messageWithAnswer.getAnswer().text;
                    log(". REPL (port "+port+"): " + answer);
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
    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            if(input.equals("status") || input.equals("http status"))
                return "Состояние HTTP сервера: "+(serverThread != null?"работает":"отключен")+"\n" +
                        "IP адрес HTTP сервера: "+getMyIP()+"\n" +
                        "Запускать HTTP сервер при старте: "+isEnabled()+"\n" +
                        "Порт HTTP сервера: "+getPort()+"\n\n" +
                        "Адрес и формат запроса на примере фразы \"Привет!\" в локальной сети:\n" +
                        "http://"+ getMyIP() + ":" + getPort() + "/api?message=Привет!\n";
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Отобразить состояние HTTP сервера",
                    "HTTP сервер позволяет отправлять на бота GET запросы и получать на них ответы" +
                            " так же как в соцсети.\n" +
                            "Эта команда отображает статус сервера и адрес по которому можно обращаться.",
                    "botcmd http status"));
            return result;
        }
    }
    private class HttpStart extends CommandModule{
        public HttpStart(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("http"))
                if(commandParser.getWord().equals("start")) {
                    setEnabled(true);
                    return "Запуск НТТР сервера на порте " + getPort() + "...\n" +
                            "Автозапуск сервера после перезапуска бота включён.\n\n" +
                            "Адрес и формат запроса на примере фразы \"Привет!\" в локальной сети:\n" +
                            "http://"+ getMyIP() + ":" + getPort() + "/api?message=Привет!\n";
                }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Запустить НТТР сервер",
                    "Бот начнёт отвечать на " + getPort() + " порте на GET запросы, " +
                            "откуда ему можно будет задать вопрос запросом определённого формата.\n" +
                            "Отвечать он будет так же, как и в социальной сети.\n" +
                            "Отправлять команды через этот HTTP сервер нельзя.\n" +
                            "Если сервер включён, так так же запустится после перезапуска программы VK iHA bot.",
                    "botcmd http start"));
            return result;
        }
    }
    private class HttpStop extends CommandModule{
        public HttpStop(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("http"))
                if(commandParser.getWord().equals("stop")) {
                    setEnabled(false);
                    return "Остановка НТТР сервера на порте " + getPort() + "...\n" +
                            "Автозапуск сервера отключён.";
                }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Остановить НТТР сервер",
                    "Бот перестанет отвечать на HTTP GET запросы и HTTP сервер больше не " +
                            "будет запускаться при перезапуске программы.",
                    "http stop"));
            return result;
        }
    }
    private class HttpSetPort extends CommandModule{
        public HttpSetPort(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            String input = message.getText();
            CommandParser commandParser = new CommandParser(input);
            if(commandParser.getWord().equals("http"))
                if(commandParser.getWord().equals("setport")){
                    int newport = commandParser.getInt();
                    if(newport < 8000 || newport > 65535 )
                        return "Номера портов могут быть от 8000 до 65535\n" +
                                "Всё что ниже - плотно зарезервировано системой\n" +
                                "Всё что выше - в природе не существует.\n";
                    setPort(newport);

                    if(isRunning()) {
                        stopServer();
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                startServer();
                            }
                        }, 5000);
                        return "Задан новый HTTP порт: " + getPort() + ".\n" +
                                "Настройки сохранены.\n" +
                                "В течение 5 секунд сервер будет перезапущен на новом порте.";
                    }
                    return "Задан новый HTTP порт: " + getPort() + ".\n" +
                            "Настройки сохранены.\n" +
                            "В данный момент сервер выключен";
                }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Задать порт НТТР сервера",
                    "К любому сетевому серверу привязан его порт на устройстве.\n" +
                            "Всего таких портов в устройстве 65536, порты до 8000 обычно зарезервированы операционной системой.\n" +
                            "Изначально бот использует 14228 порт, но его можно сменить.",
                    "botcmd http setport <Новый номер порта. Стандартный: 14228>"));
            return result;
        }
    }
}
