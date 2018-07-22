package com.fsoft.vktest.Communication;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.FileStorage;
import com.fsoft.vktest.Modules.Commands.CommandDesc;

import com.fsoft.vktest.Utils.User;
import com.perm.kate.api.Comment;
import com.perm.kate.api.WallMessage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Класс, который занимается чтением стены автономно
 * Created by Dr. Failov on 11.03.2017.
 */

public class Wall extends CommandModule {
    private Communicator communicator = null;
    private FileStorage file = null;
    private long id;
    private Timer wallTimer = null;
    private String wallName = null;
    private boolean enabled = true;
    private int scanIntervalSec = 12;
    private int scanPosts = 50;

    private int postsDetected = 0;
    private int commentsDetected = 0;
    private int messagesReplied  = 0;
    private Date resetCountersDate = new Date();

    private int errorsCounter = 0;
    private int requestsCounter = 0;
    private String wallStatus = "Стена обрабатывается.";
    private HashMap<Long, Long> postCommentCounter = new HashMap<>();
    //// Как стена может узнать о возникновении ошибки. Например, бан? Использовать unsafe методы
        /*
        * ----------------- Как читать стену
        * Тезисы:
        * - необходимо хранить количество комментариев под каждой записью
        * - при инициализации заполнять массив с списком записей и количеством комментариев под каждой
        * - При сканировании всей стены, проверять есть ли обновления. Проверять по количеству записей.
        * - Если обновления есть, загружать комментарии
        * - при обнаружении новой записи вносить её в список записей для слежения
        * -
        *
        * Суть:
        * В начале делаем ОДИН запрос на на нужное количество записей(!) на стене и сохраняем количество комментариев под каждой записью.
        * Далее когда делаем новые запросы сравниваем количество комментариев дальше
        * а дальше загружаем ровно СТОЛЬКО комментариев, на сколько увеличилось число. Это и есть новые комментарии.
        *
        * */

    public Wall(long id, Communicator communicator) {
        super(communicator.getApplicationManager());
        this.id = id;
        file = new FileStorage("wall"+id, applicationManager);
        enabled = file.getBoolean("enabled", enabled);
        scanIntervalSec = file.getInt("scanIntervalSec", scanIntervalSec);
        scanPosts = file.getInt("scanPosts", scanPosts);
        postsDetected = file.getInt("postsDetected", postsDetected);
        commentsDetected = file.getInt("commentsDetected", commentsDetected);
        messagesReplied = file.getInt("messagesReplied", messagesReplied);
        resetCountersDate = file.getDate("resetCountersDate", resetCountersDate);

        childCommands.add(new Status(applicationManager));
        childCommands.add(new SetActive(applicationManager));
        childCommands.add(new SetEnabled(applicationManager));
        childCommands.add(new SetScanInterval(applicationManager));
        childCommands.add(new SetScanMessages(applicationManager));
        childCommands.add(new ResetCounters(applicationManager));
    }
    public @Override String toString(){
        return getWallName();
    }
    public @Override boolean equals(Object object){
        if(object == null)
            return false;
        if(object.getClass() == Long.class){
            long receivedId = (Long) object;
            return id == receivedId;
        }
        if(getClass() == object.getClass()){
            return id == ((Wall)object).id;
        }
        return object.equals(id);
    }
    public void startWall(){
        if(wallTimer == null && isEnabled()){
            log(". Запуск чтения стены " + getId() + " ...");
            loadName();
            wallTimer = new Timer("Wall reader for wall " + getId());
            wallTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    findNewPosts();
                }
            }, scanIntervalSec * 1000);
        }
    }
    public void stopWall(){
        if(wallTimer != null) {
            log(". Остановка чтения стены " + getId() + " ...");
            wallTimer.cancel();
            wallTimer = null;
        }
    }
    public long getId() {
        return id;
    }
    public boolean delete(){
        //удаляет файл который хранит данные про эту стену
        if(isRunning())
            stopWall();
        return file.delete();
    }
    public int getPostsDetected() {
        return postsDetected;
    }
    public int getCommentsDetected() {
        return commentsDetected;
    }
    public int getMessagesReplied() {
        return messagesReplied;
    }
    public String getWallStatus() {
        return wallStatus;
    }
    public String getWallName() {
        if(wallName == null || wallName.equals("")) {
            if(getId() < 0)
                return String.valueOf("Группа " + getId());
            else
                return String.valueOf("Стена " + getId());
        }
        else
            return wallName;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public boolean isRunning() {
        return wallTimer != null;
    }
    public void setWallStatus(String wallStatus) {
        this.wallStatus = wallStatus;
    }
    public void setCommentsDetected(int commentsDetected) {
        this.commentsDetected = commentsDetected;
        file.put("commentsDetected", commentsDetected).commit();
    }
    public void setPostsDetected(int postsDetected) {
        this.postsDetected = postsDetected;
        file.put("postsDetected", postsDetected).commit();
    }
    public void setMessagesReplied(int messagesReplied) {
        this.messagesReplied = messagesReplied;
        file.put("messagesReplied", messagesReplied).commit();
    }
    public void setResetCountersDate(Date resetCountersDate) {
        this.resetCountersDate = resetCountersDate;
        file.put("resetCountersDate", resetCountersDate).commit();
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        file.put("enabled", enabled).commit();
        if(enabled)
            startWall();
        else
            stopWall();
    }
    public void setScanPosts(int scanPosts) {
        this.scanPosts = scanPosts;
        file.put("scanPosts", scanPosts).commit();
    }
    public void setScanIntervalSec(int scanIntervalSec) {
        this.scanIntervalSec = scanIntervalSec;
        file.put("scanIntervalSec", scanIntervalSec).commit();
        stopWall();
        startWall();
    }

    private void findNewPosts(){
        if(enabled && wallTimer != null) {
            try {
                VkAccount accountToReadWall = communicator.getActiveVkAccount();
                ArrayList<WallMessage> messages = accountToReadWall.getWallMessagesUnsafe(id, scanPosts);
                requestsCounter ++;
                for(WallMessage wallMessage:messages){
                    if(postCommentCounter.containsKey(wallMessage.id)){
                        long commentsEarlier = postCommentCounter.get(wallMessage.id);
                        long commentsNow = wallMessage.comment_count;
                        if(commentsNow > commentsEarlier){
                            int newComments = (int)(commentsNow - commentsEarlier);
                            VkAccount accountToReadComments = communicator.getActiveVkAccount();
                            ArrayList<Comment> comments = accountToReadComments.getWallCommentsUnsafe(id, wallMessage.id, newComments);
                            requestsCounter ++;
                            for(Comment comment:comments){
                                //это новый комментарий под записью.
                                log(". COMM("+getWallName()+") " + comment.message);
                                setCommentsDetected(getCommentsDetected()+1);
                                User author = new User().vk(comment.from_id);
                                author.setName(communicator.getActiveVkAccount().getUserFullName(comment.from_id));
                                Message message = new Message(
                                        Message.SOURCE_COMMENT,
                                        comment.message,
                                        author,
                                        comment.attachments,
                                        accountToReadComments,
                                        new Message.OnAnswerReady() {
                                            @Override
                                            public void sendAnswer(Message answer) {
                                                replyMessage(answer);
                                            }
                                        }
                                );
                                message.setSource_id(getId());
                                message.setComment_wall_id(getId());
                                message.setComment_reply_comment_id(comment.reply_to_cid);
                                message.setComment_reply_user_id(comment.reply_to_uid);
                                message.setComment_post_id(wallMessage.id);
                                message.setMessage_id(comment.cid);
                                message.setComment_post_author(wallMessage.from_id);
                                message.setComment_post_text(wallMessage.text);

                                applicationManager.getBrain().processMessage(message);
                            }
                        }
                    }
                    else {
                        //это новый пост на стене
                        log(". POST("+getWallName()+") " + wallMessage.text);
                        setPostsDetected(getPostsDetected()+1);
                        User author = new User().vk(wallMessage.from_id);
                        author.setName(communicator.getActiveVkAccount().getUserFullName(wallMessage.from_id));
                        Message message = new Message(
                                Message.SOURCE_WALL,
                                wallMessage.text,
                                author,
                                wallMessage.attachments,
                                accountToReadWall,
                                new Message.OnAnswerReady() {
                                    @Override
                                    public void sendAnswer(Message answer) {
                                        replyMessage(answer);
                                    }
                                }
                        );
                        message.setSource_id(getId());
                        message.setComment_post_id(wallMessage.id);
                        message.setMessage_id(wallMessage.id);
                        message.setComment_post_author(wallMessage.from_id);
                        message.setComment_post_text(wallMessage.text);

                        applicationManager.getBrain().processMessage(message);
                    }
                    postCommentCounter.put(wallMessage.id, wallMessage.comment_count);
                }
                //clear HashMap from old posts для экономии памяти
                if(messages.size() > 0){
                    ArrayList<Long> toRemove = new ArrayList<>();
                    //сначала надо понять какие элементы устарели
                    //устаревшими  будем считать элементы которые не попали в выборку вот сейчас
                    Iterator<Map.Entry<Long, Long>> iterator = postCommentCounter.entrySet().iterator();
                    while (iterator.hasNext()){
                        long id = iterator.next().getKey();
                        boolean contains = false;
                        for (WallMessage message:messages)
                            if(message.id == id)
                                contains = true;
                        if(!contains)
                            toRemove.add(id);
                    }
                    //теперь можно спокойно удалить те элементы которые мы считаем устаревшими
                    for(Long id:toRemove)
                        postCommentCounter.remove(id);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                log("! Ошибка чтения стены "+getWallName()+": " + e.toString());
                reportError(e);
            }
        }
    }
    private void loadName(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    wallName = communicator.getActiveVkAccount().getUserFullName(getId());
                }
                catch (Exception e){
                    e.printStackTrace();
                    log("! Не могу загрузить имя стены " + getId() + " из-за ошибки: " + e.toString());
                }
            }
        }).start();
    }
    private void reportError(Throwable e){
        errorsCounter ++;
        if (e.toString().toLowerCase().contains("user was deleted or banned")) {
            deactivateFor10Minutes(log(". Пользователь "+ getWallName()+" заморожен. Стена дективирована."));//active = false;
            return;
        }
        if (e.toString().toLowerCase().contains("group is blocked")) {
            deactivateFor10Minutes(log(". Группа "+ getWallName()+" заморожена. Стена дективирована."));//active = false;
            return;
        }
        if (e.toString().toLowerCase().contains("you are in users blacklist")) {
            deactivateFor10Minutes(log(". Пользователь " + getWallName() + " вас забанил. Стена дективирована."));//active = false;
            return;
        }
        if (e.toString().toLowerCase().contains("access denied")) {
            deactivateFor10Minutes(log(". Пользователь " + getWallName() + " закрыл Вам доступ к стене. Стена дективирована."));//active = false;
            return;
        }
        if (e.toString().toLowerCase().contains("access to post comments denied")) {
            deactivateFor10Minutes(log(". Пользователь " + getWallName() + " закрыл Вам доступ к стене. Стена дективирована."));//active = false;
            return;
        }
//            if (e.toString().contains("Access to post comments denied")) { //Могут забанить не всех ботов, а лишь одного. Если забанят всех - сработает другая защита.
//                active = false;
//                log(". Стена пользователя "+getName()+" закрыта. Стена дективирована.");
//                return;
//            }

        //левая ошибка какая-то
        // которую надо показать пользователю но мы с ней ничего сделать
        // не сможем
        if(errorsCounter*3>requestsCounter && requestsCounter > 100)
            deactivateFor10Minutes("Слишком много ошибок. Из " + requestsCounter + " запросов " + errorsCounter + " завершились ошибкой. Последняя ошибка: " + e.toString());
    }
    private void deactivateFor10Minutes(final String reason){
        log("! Деактивация стены "+ getWallName() +" на 10 минут...");
        setWallStatus(reason);
        stopWall();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startWall();
                wallStatus = "Стена запущена после остановки на 10 минут.";
            }
        }, 600000);
    }
    private void replyMessage(Message message){
        if(message == null)
            return;
        try {
            if (message.getAnswer() != null && !message.getAnswer().text.equals("")) {
                log(". REPL (" + getWallName() + "): " + message.getAnswer().text);
                //send
                setMessagesReplied(getMessagesReplied() + 1);
                requestsCounter ++;
                if (message.getSource().equals(Message.SOURCE_COMMENT))
                    message.getBotAccount().createWallCommentUnsafe(getId(), message.getComment_post_id(), message.getAnswer(), message.getMessage_id());
                else if (message.getSource().equals(Message.SOURCE_WALL))
                    message.getBotAccount().createWallCommentUnsafe(getId(), message.getMessage_id(), message.getAnswer(), null);
            } else {

            }
        }
        catch (Throwable e){
            e.printStackTrace();
            log("! Ошибка отправки ответа на стену: " + e.toString());
            reportError(e);
        }
    }


    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().trim().toLowerCase().equals("status") || message.getText().trim().toLowerCase().equals("wall status"))
                return
                        "Стена " + getWallName() + " ID стены: "+getId()+"\n" +
                        "Стена " + getWallName() + " состояние: "+(isEnabled()?(isRunning()?"работает":"включена, но не работает"):"выключена")+"\n" +
                        "Стена " + getWallName() + " статус: "+getWallStatus()+"\n" +
                        "Стена " + getWallName() + " интервал сканирования постов: "+scanIntervalSec+" сек.\n" +
                        "Стена " + getWallName() + " количество сканируемых постов: "+scanPosts+"\n" +
                        "Стена " + getWallName() + " обнаружено новых постов с "+sdf.format(resetCountersDate)+": "+getPostsDetected()+"\n" +
                        "Стена " + getWallName() + " обнаружено новых комментариев с "+sdf.format(resetCountersDate)+": "+getCommentsDetected()+"\n" +
                        "Стена " + getWallName() + " отправлено ответов с "+sdf.format(resetCountersDate)+": "+getMessagesReplied()+"\n"+
                        "Стена " + getWallName() + " количество запросов с момента запуска: "+requestsCounter+"\n" +
                        "Стена " + getWallName() + " количество ошибок с момента запуска: "+errorsCounter+"\n";
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    private class SetActive extends CommandModule{
        public SetActive(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override public
        String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("active")) {
                        if(commandParser.getBoolean()) {
                            if(isRunning())
                                return "Стена " + getWallName() + " и так включена.";
                            startWall();
                            setWallStatus("Ты вручную включил эту стену " + sdf.format(new Date()) + " после \""+wallStatus+"\".");
                            return "Стена " + getWallName() + " теперь включена.\n" +
                                    "Если со стеной возникнут проблемы, она снова выключится.";
                        }
                        else {
                            if(!isRunning())
                                return "Стена " + getWallName() + " и так выключена.";
                            stopWall();
                            setWallStatus("Ты вручную выключил эту стену " + sdf.format(new Date()) + ".");
                            return "Стена " + getWallName() + " теперь выключена.\n" +
                                    "Обрати внимание, стена включится после перезапуска программы.";
                        }
                    }
            return "";
        }

        @Override public
        ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Временно деактивировать (активировать) стену "+ getWallName(),
                    "Включить или отключить стену до перезапуска может быть полезно " +
                            "если со стеной, например, возникает какая-либо проблема. Если программа " +
                            "автоматически отключила стену, этой командой можно её включить.\n" +
                            "После перезапуска программы стена снова включится.\n" +
                            "Если надо отключить стену надолго, используй команду wall ... enabled.",
                    "botcmd wall "+getId()+" active <on/off>"));
            return result;
        }
    }
    private class SetEnabled extends CommandModule{
        public SetEnabled(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override public
        String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().equals("enabled")) {
                        if(commandParser.getBoolean()) {
                            if(isEnabled())
                                return "Стена " + getWallName() + " и так включена.";
                            setEnabled(true);
                            return "Стена " + getWallName() + " теперь включена.";
                        }
                        else {
                            if(!isEnabled())
                                return "Стена " + getWallName() + " и так выключена.";
                            setEnabled(false);
                            return "Стена " + getWallName() + " теперь выключена.";
                        }
                    }
            return "";
        }

        @Override public
        ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Отключить или включить стену "+ getWallName(),
                    "Если стену выключить, она будет оставаться выключенной даже после перезапуска программы. " +
                            "При этом она не будет удаляться из списка стен.",
                    "botcmd wall "+getId()+" enabled <on/off>"));
            return result;
        }
    }
    private class ResetCounters extends CommandModule{
        public ResetCounters(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override public
        String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getLong() == id)
                    if(commandParser.getWord().trim().trim().contains("resetcounter")) {
                        setMessagesReplied(0);
                        setPostsDetected(0);
                        setCommentsDetected(0);
                        setResetCountersDate(new Date());
                        return "На стене " + getWallName() + " счётчики сброшены.";
                    }
            return "";
        }

        @Override public
        ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Сбросить счётчики статистики для стены "+ getWallName(),
                    "Позволяет сбросить на 0 счётчики постов, комментариев и ответов.",
                    "botcmd wall "+getId()+" ResetCounters"));
            return result;
        }
    }
    private class SetScanMessages extends CommandModule{
        public SetScanMessages(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall")) {
                if (commandParser.getLong() == id) {
                    if (commandParser.getWord().toLowerCase().trim().equals("setscanmessages")) {
                        int newValue = commandParser.getInt();
                        if (newValue < 1)
                            return "С числом явно что-то не так. Бот не может сканировать меньше одной записи.";
                        if (newValue > 200)
                            return "ВК не позволяет загружать более 200 записей одним запросом, " +
                                    "поэтому более 200 записей бот сканировать не может.";
                        setScanPosts(newValue);
                        return "Теперь бот сканирует " + scanPosts + " последних записей на стене " + getWallName() + ".";
                    }
                }
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Задать количество записей, сканируемых на стене " + getWallName(),
                    "Бот может отвечать только под несколькими последними записями на стене.\n" +
                            "Сейчас бот отвечает под " + scanPosts + " записями на стене.\n" +
                            "Эта команда позволяет изменить количество записей, под которыми может отвечать бот. " +
                            "Много сканируемых записей могут привести к понижению скорости и " +
                            "обязательно приведут к более быстрому использованию Интернет-трафика.",
                    "botcmd wall "+getId()+" SetScanMessages <новое число от 1 до 200)>"));
            return result;
        }
    }
    private class SetScanInterval extends CommandModule{
        public SetScanInterval(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall")) {
                if (commandParser.getLong() == id) {
                    if (commandParser.getWord().toLowerCase().trim().equals("setscaninterval")) {
                        int newValue = commandParser.getInt();
                        if (newValue > 5) {
                            setScanIntervalSec(newValue);
                            return "Теперь бот сканирует записи на стене "+getWallName()+" каждые " + scanIntervalSec + " секунд.";
                        }
                        else {
                            return newValue + " секунд - это слишком малый интервал, " +
                                    "из-за которого программа может работать нестабильно.";
                        }
                    }
                }
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Задать интервал сканирования стены "+ getWallName(),
            "Бот сканирует стены с определённой периодичностью. " +
                    "Эта команда позволяет задать, как часто бот будет сканировать стену. " +
                    "Если задать слишком малый интервал, бот может зависнуть или получить капчу.\n" +
                    "Сейчас стена сканируется каждые "+scanIntervalSec+" секунд.",
                    "botcmd wall "+getId()+" SetScanInterval <новый интервал в секундах>"));
            return result;
        }
    }
}
