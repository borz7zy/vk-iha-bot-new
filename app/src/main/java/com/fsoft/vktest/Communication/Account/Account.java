package com.fsoft.vktest.Communication.Account;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Utils.CommandParser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Общая хуйня для всех аккаунтов в том числе и ВК. Токен ведь нужен всегда.
 * Я уже просто заебался копаться в горах говна основного аккаунта, поэтому
 * ищу что можно было бы вынести нахуй
 *
 * constructor() -> login() -> startAccount()
 * constructor() -> startAccount()
 * Created by Dr. Failov on 22.02.2017.
 */
public class Account extends CommandModule implements AccountBase {
    private long id = 0L;
    private String token = null;
    //место для хранения данных этого аккаунта и любого наследованного аккаунта
    private FileStorage fileStorage = null;
    private String fileName = null;
    //эту переменную делать true если получилось успешно
    // войти и делать false если возникли серьезные проблемы с аккаунтом
    private boolean token_ok = false;
    //это настройка для пользователя чтобы выключить аккаунт
    private boolean enabled = true;
    //устанавливается true во время запуска аккаунта
    private boolean running = false;
    //это - что-то наподобие временного комментария описывающего, что с этим аккаунтом происходит.
    private String state = "";
    private Runnable onStateChangedListener = null;


    public Account(ApplicationManager applicationManager, String fileName) {
        super(applicationManager);
        this.fileName = fileName;
        fileStorage = new FileStorage(fileName, applicationManager);
        log(". Создан аккаунт: " + fileName);

        enabled = getFileStorage().getBoolean("enabled", enabled);
        id = getFileStorage().getLong("id", id);
        token = getFileStorage().getString("token", token);

        childCommands.add(new Status(applicationManager));
        childCommands.add(new Enabled(applicationManager));
        childCommands.add(new GetToken(applicationManager));
        childCommands.add(new SetToken(applicationManager));
    }
    public boolean remove(){
        //удалить файл аккаунта
        return new File(fileStorage.getFilePath()).delete();
    }
    public void login(){
        //открывает процедуру (пере)логина.
        // По итогу задает значение для token
    }
    public void startAccount(){
        //если токен есть, эта функция его проверяет. Если токен валидный,
        //эта функция запускает работу во всех службах аккаунта, если isEnabled.
        running = true;
        log(". Аккаунт " + toString() + " запускается...");
        setState("Запускается...");
        //token_ok = true; //кажется, это не всегда связано
    }
    public void stopAccount(){
        //эта функция останавливает работу во всех службах аккаунта
        running = false;
        log(". Аккаунт " + toString() + " остановлен.");
        setState("Остановлен.");
        // token_ok = false; ////кажется, это не всегда связано
    }
    protected interface OnTokenValidityCheckedListener{
        void onTokenPass();
        void onTokenFail();
    }
    protected void checkTokenValidity(OnTokenValidityCheckedListener listener){
        //запускать проверку и вызывать лисенер...
        log(". Аккаунт " + toString() + " проверяется...");
        setState("Проверяется...");
    }
    public boolean isMine(String commandTreatment){
        //Эта функция должна отвечать за то, чтобы при обращении в команде
        // можно было понять что обращение именно к этому аккаунту
        //например bcd acc 098309832 enable
        try{
            return Long.parseLong(commandTreatment.trim()) == id;
        }catch (Exception e){
            return false;
        }
    }

    public boolean isToken_ok() {
        return token_ok;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public boolean isRunning() {
        return running;
    }
    public String getState() {
        return state;
    }
    public FileStorage getFileStorage() {
        return fileStorage;
    }
    public long getId() {
        return id;
    }
    public String getToken() {
        return token;
    }
    public String getFileName() {
        //именно это имя аккаунт получает при создании
        //эта функция нужна для того чтобы список аккаунтов можно было сохранить, не только загрузить
        return fileName;
    }

    //------------
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        getFileStorage().put("enabled", enabled).commit();
        if(isRunning() && !isEnabled())
            stopAccount();
        if(!isRunning() && isEnabled())
            startAccount();
    }
    public String state(String state) {
        setState(state);
        return state;
    }
    public void setState(String state) {
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        this.state = time + " " + state;
        if(onStateChangedListener != null)
            onStateChangedListener.run();
    }
    public void setId(long id) {
        this.id = id;
        getFileStorage().put("id", id).commit();
    }
    public void setToken(String token) {
        this.token = token;
        getFileStorage().put("token", token).commit();
    }
    public void setToken_ok(boolean token_ok) {
        this.token_ok = token_ok;
    }
    public void setOnStateChangedListener(Runnable onStateChangedListener) {
        this.onStateChangedListener = onStateChangedListener;
    }

    @Override public String toString() {
        return "Аккаунт " + id;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        if (getId() != account.getId()) return false;

        return true;
    }
    @Override public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    class Enabled extends CommandModule {
        public Enabled(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("enabled")) {
                        Boolean ena = commandParser.getBoolean();
                        String reason = commandParser.getText();
                        setEnabled(ena);
                        if (!reason.equals(""))
                            setState(reason);
                        if(isEnabled())
                            return "Аккаунт "+Account.this+" теперь включен. Через пару минут все функции заработают.";
                        else
                            return "Аккаунт "+Account.this+" теперь выключен. Бот не будет его использовать, пока ты снова его не включишь.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Включить или выключить аккаунт "+Account.this,
                    "Если ты не хочешь, чтобы бот использовал этот аккаунт, ты можешь выключить его," +
                            "и бот не будет его использовать пока ты его не включишь.\n" +
                            "Ты можешь также дописать комментарий, чтобы описать почему " +
                            "аккаунт отключён или включён.",
                    "botcmd acc " + id + " enabled <on/off> <комментарий>"));
            return result;
        }
    }
    class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }
        public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            if(message.getText().equals("status") || message.getText().equals("acc status"))
                return  "Аккаунт " + Account.this + " id: "+getId() + "\n" +
                        "Аккаунт " + Account.this + " файл: "+new File(fileStorage.getFilePath()).getName() + "\n" +
                        "Аккаунт " + Account.this + " включён: "+(isEnabled()?"ВКЛ":"ВЫКЛ")+ "\n" +
                        "Аккаунт " + Account.this + " состояние: "+getState()+ "\n" +
                        "Аккаунт " + Account.this + " токен в норме: "+(isToken_ok()?"да":"нет") + "\n";
            return "";
        }
        public @Override ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    class GetToken extends CommandModule{
        public GetToken(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(isMine(commandParser.getWord()))
                    if(commandParser.getWord().equals("gettoken"))
                        return "("+Account.this+") id = " + id +
                                "\n("+Account.this+") token = " + token +
                                "\nБудь осторожен с токеном! Не кидай его незнакомым людям. " +
                                "Если какой-то нехороший человек получил твой токен, сделай следующее:" +
                                "\nОткрой Вконтакте -> Нажми на аватарку в верхнем правом углу -> " +
                                "Настройки -> Безопасность -> Завершить все сеансы." +
                                "\nТак ты отключишь все токены, в том числе и тот " +
                                "токен, который у тебя украли." +
                                "\nЕсли ты хочешь добавить этот аккаунт в другого бота, " +
                                "вот тебе готовая команда:" +
                                "\n" +
                                "\nbotcmd accs add "+token+" "+id+" " +
                                "\n" +
                                "\nОбрати внимание: если другой бот находится в другом городе, \n" +
                                "токен может не заработать, либо спросить номер телефона.\n" ;
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc(
                    "Получить токен  для аккаунта "+Account.this,
                    "Токен - это секретный код, который позволяет получать доступ к аккаунту" +
                            " и выполнять от его имени разные действия. Это не пароль. " +
                            "Пароль как раз и нужен для того, чтобы получить этот токен. " +
                            "Не кидай этот код незнакомым людям, если не доверяешь им. " +
                            "Токен - это временный код. Он меняется каждый раз, когда ты " +
                            "заходишь во Вконтакте. Иногда токены перестают работать сами по себе, " +
                            "если контакту почему-то кажется, что страницу взломали. " +
                            "Также токен будет работать только в том же городе где он был получен. " +
                            "Если ты попытаешься скинуть свой токен другу из другого города, он " +
                            "может не сработать.\n" +
                            "Если токен перестанет работать, надо заново воходить во Вконтакте.",
                    "botcmd acc " + id + " gettoken"));
            return result;
        }
    }
    class SetToken extends CommandModule{
        public SetToken(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(isMine(commandParser.getWord()))
                    if(commandParser.getWord().equals("settoken")) {
                        setToken(commandParser.getText());
                        startAccount();
                        return "(" + Account.this + ") Задан новый токен: " + token + "\n";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Задать токен для аккаунта "+Account.this,
                    "Токен - это секретный код, который позволяет получать доступ к аккаунту" +
                            " и выполнять от его имени разные действия. Это не пароль. " +
                            "Пароль как раз и нужен для того, чтобы получить этот токен. " +
                            "Не кидай этот код незнакомым людям, если не доверяешь им. " +
                            "Токен - это временный код. Он меняется каждый раз, когда ты " +
                            "заходишь во Вконтакте. Иногда токены перестают работать сами по себе, " +
                            "если контакту почему-то кажется, что страницу взломали. " +
                            "Также токен будет работать только в том же городе где он был получен. " +
                            "Если ты попытаешься скинуть свой токен другу из другого города, он " +
                            "может не сработать.\n" +
                            "Если токен перестанет работать, надо заново воходить во Вконтакте.",
                    "botcmd acc " + id + " settoken"));
            return result;
        }
    }
}
