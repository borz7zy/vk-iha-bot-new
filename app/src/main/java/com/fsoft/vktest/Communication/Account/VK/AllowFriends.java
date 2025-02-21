package com.fsoft.vktest.Communication.Account.VK;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.Parameters;
import com.perm.kate.api.User;
import com.fsoft.vktest.Modules.Commands.CommandDesc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Этот модуль занимается тем, что проверяет наличие новых заявок в друзья и принимает их,
 * Также он ищет друзей, который удалились (это те, на которых мы подписаны) и отклоняет от них завяки
 * Если включено, то при удалении друзей, он добавляет их в ЧС.
 * Created by Dr. Failov on 16.02.2017.
 */
public class AllowFriends extends CommandModule {
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy", Locale.US);

    private VkAccount vkAccount = null;
    //если таймеры null, значит функция не запущена
    private Timer acceptFriendRequestsTimer = null;
    private Timer rejectDeletedFriendsTimer= null;
    //опция для пользователя, чтобы отключить функции
    private boolean acceptFriendRequestsEnabled = false;
    private boolean rejectDeletedFriendsEnabled = false;
    private boolean blacklistDeletedFriendsEnabled = false;
    //счётчики для отчётности
    private int acceptedFriendRequestsCounter = 0;
    private int rejectedDeletedFriendsCounter = 0;
    private int blacklistedDeletedFriendsCounter = 0;
    private Date countersResetDate = new Date();

    public AllowFriends(ApplicationManager applicationManager, VkAccount vkAccount) {
        super(applicationManager);
        this.vkAccount = vkAccount;

        acceptFriendRequestsEnabled = vkAccount.getFileStorage().getBoolean("acceptFriendRequestsEnabled", acceptFriendRequestsEnabled);
        rejectDeletedFriendsEnabled = vkAccount.getFileStorage().getBoolean("rejectDeletedFriendsEnabled", rejectDeletedFriendsEnabled);
        blacklistDeletedFriendsEnabled = vkAccount.getFileStorage().getBoolean("blacklistDeletedFriendsEnabled", blacklistDeletedFriendsEnabled);

        acceptedFriendRequestsCounter = vkAccount.getFileStorage().getInt("acceptedFriendRequestsCounter", acceptedFriendRequestsCounter);
        rejectedDeletedFriendsCounter = vkAccount.getFileStorage().getInt("rejectedDeletedFriendsCounter", rejectedDeletedFriendsCounter);
        blacklistedDeletedFriendsCounter = vkAccount.getFileStorage().getInt("blacklistedDeletedFriendsCounter", blacklistedDeletedFriendsCounter);
        countersResetDate = vkAccount.getFileStorage().getDate("countersResetDate", countersResetDate);

        childCommands.add(new Status(applicationManager));
        childCommands.add(new AcceptAnyRequest(applicationManager));
        childCommands.add(new RejectFollowers(applicationManager));
        childCommands.add(new BlacklistFollowers(applicationManager));
        childCommands.add(new ResetFriendsCounter(applicationManager));
        childCommands.add(new ClearBlacklist(applicationManager));
    }
    @Override public void stop() {
        stopModule();
        super.stop();
    }
    public void setAcceptFriendRequestsEnabled(boolean in){
        log(". Принятие заявок в друзья для аккаунта "+vkAccount+"  " + (in?"запускатеся...":"отключается..."));
        acceptFriendRequestsEnabled = in;
        vkAccount.getFileStorage().put("acceptFriendRequestsEnabled", acceptFriendRequestsEnabled).commit();

        if(acceptFriendRequestsEnabled)
            startAcceptFriendRequests();
        if(!acceptFriendRequestsEnabled)
            stopAcceptFriendsRequest();
    }
    public void setRejectDeletedFriendsEnabled(boolean in){
        log(". Отписка от удалившихся друзей для аккаунта "+vkAccount+"  " + (in?"запускатеся...":"отключается..."));
        rejectDeletedFriendsEnabled = in;
        vkAccount.getFileStorage().put("rejectDeletedFriendsEnabled", rejectDeletedFriendsEnabled).commit();
        if(rejectDeletedFriendsEnabled)
            startRejectDeletedFriends();
        if(!rejectDeletedFriendsEnabled)
            stopRejectDeletedFriends();
    }
    public void setBlacklistDeletedFriendsEnabled(boolean blacklistDeletedFriends) {
        this.blacklistDeletedFriendsEnabled = blacklistDeletedFriends;
        vkAccount.getFileStorage().put("blacklistDeletedFriendsEnabled", blacklistDeletedFriendsEnabled).commit();
        if(!rejectDeletedFriendsEnabled)
            messageBox(log("! Внимание! Ты включил отправку удалившихся друзей в ЧС, " +
                    "но у тебя не включена отписка от них! \n" +
                    "Отправка удалившихся друзей в ЧС будет работать только в том случае, " +
                    "если включена отписка от удалившихся друзей!"));
//        if(!applicationManager.isDonated())
//            messageBox(log("! Внимание! Ты включил отправку удалившихся друзей в ЧС, " +
//                    "но у тебя не куплена донатка. \n" +
//                    "Отправка удалившихся друзей в ЧС будет работать только в том случае, " +
//                    "если куплена донатка для бота."));
    }
    public void startModule(){
        if(acceptFriendRequestsEnabled)
            startAcceptFriendRequests();
        if(rejectDeletedFriendsEnabled)
            startRejectDeletedFriends();
    }
    public void stopModule(){
        stopAcceptFriendsRequest();
        stopRejectDeletedFriends();
    }


    private void startAcceptFriendRequests(){
        if(acceptFriendRequestsTimer != null)
            return;
        if (!acceptFriendRequestsEnabled) {
            log("! Принятие заявок в друзья отключено. Модуль запущен не будет.");
            return;
        }
        log(". Запуск принятия заявок в друзья на аккаунте "+vkAccount+"...");
        int periodMin = applicationManager.getParameters().get("acceptFriendRequestsPeriod", 10,
                "Период принятия заявок в друзья в минутах.",
                "Определяет как часто бот будет проверять наличие заявок в друзья. Оптимальное время - это 10 минут.");
        int periodMs = periodMin * 60 * 1000;
        acceptFriendRequestsTimer = new Timer("AcceptFriends timer for " + vkAccount);
        acceptFriendRequestsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                acceptFriendRequests();
            }
        }, 1000, periodMs);
    }
    private void stopAcceptFriendsRequest(){
        if(acceptFriendRequestsTimer == null)
            return;
        log(". Остановка принятия заявок в друзья на аккаунте "+vkAccount+"...");
        acceptFriendRequestsTimer.cancel();
        acceptFriendRequestsTimer = null;
    }
    private void startRejectDeletedFriends(){
        if(rejectDeletedFriendsTimer != null)
            return;
        if (!rejectDeletedFriendsEnabled) {
            log("! Отклонение подписок на удалившихся друзей отключено. Модуль запущен не будет.");
            return;
        }
        log(". Запуск отклонения подписок на удалившихся друзей на аккаунте "+vkAccount+"...");
        int periodMin = applicationManager.getParameters().get("rejectDeletedFriendsPeriod", 10,
                "Период отклонения подписок на удалившихся друзей в минутах.",
                "Определяет как часто бот будет проверять подписки и отписываться. Оптимальное время - это 10 минут.");
        int periodMs = periodMin * 60 * 1000;
        rejectDeletedFriendsTimer = new Timer("rejectDeletedFriends timer for " + vkAccount);
        rejectDeletedFriendsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                rejectDeletedFriends();
            }
        }, 1000, periodMs);
    }
    private void stopRejectDeletedFriends(){
        if(rejectDeletedFriendsTimer == null)
            return;
        log(". Остановка отклонения подписок на удалившихся друзей на аккаунте "+vkAccount+"...");
        rejectDeletedFriendsTimer.cancel();
        rejectDeletedFriendsTimer = null;
    }
    private void acceptFriendRequests(){
        try {
            log(". ("+vkAccount+") Проверка числа друзей...");
            int cnt = vkAccount.getFriendsCount();
            if(cnt > 9990) {
                log(". ("+vkAccount+") Слишком много друзей. Больше не принимать.");
                return;
            }
            log(". (" + vkAccount + ") Принятие новых заявок в друзья...");
            ArrayList<Long> users = vkAccount.getInFriendRequests();

            log(". ("+vkAccount+") Обнаружено "+users.size()+" заявок в друзья.");
            for (int i = 0; i < users.size(); i++) {
                long id = users.get(i);
                long result = vkAccount.addFriend(id);
                if(result == 2) {
                    log(". ("+vkAccount+") " + i + "/" + users.size() + " Заявка одобрена: " + id);
                    increment_acceptedFriendRequestsCounter();
                }
                else if(result == 1) {
                    log(". ("+vkAccount+") " + i + "/" + users.size() + " Заявка отправлена: " + id);
                    increment_acceptedFriendRequestsCounter();
                }
                else if(result == 4) {
                    log(". ("+vkAccount+") " + i + "/" + users.size() + " Повторная отправка заявки: " + id);
                    increment_acceptedFriendRequestsCounter();
                }
                else {
                    log(". ("+vkAccount+") " + i + "/" + users.size() + " Ошибка принятия заявки от " + id + " : " + result);
                }
            }
            log(". ("+vkAccount+") Принято "+users.size()+" заявок в друзья.");

        }
        catch (Throwable e){
            e.printStackTrace();
            log("! ("+vkAccount+") Ошибка принятия заявок в друзья: " + e.toString());
        }
    }
    private void rejectDeletedFriends(){
        try {
            if(rejectDeletedFriendsEnabled){
                log(". ("+vkAccount+") Отклонение подписок...");
                ArrayList<Long> users = vkAccount.getOutFriendRequests();

                log(". ("+vkAccount+") Обнаружено "+users.size()+" подписок.");
                for (int i = 0; i < users.size(); i++) {
                    long id = users.get(i);
                    long result = vkAccount.deleteFriend(id);

                    if(result == 1) {
                        log(". (" + vkAccount + ") " + i + "/" + users.size() + " пользователь удален из списка друзей: " + id);
                        increment_rejectedDeletedFriendsCounter();
                    }
                    else if(result == 2) {
                        log(". (" + vkAccount + ") " + i + "/" + users.size() + " заявка на добавление в друзья от данного пользователя отклонена: " + id);
                        increment_rejectedDeletedFriendsCounter();
                    }
                    else if(result == 3) {
                        log(". (" + vkAccount + ") " + i + "/" + users.size() + " рекомендация добавить в друзья данного пользователя удалена: " + id);
                        increment_rejectedDeletedFriendsCounter();
                    }
                    else
                        log(". ("+vkAccount+") " + i + "/" + users.size() + " oшибка принятия заявки от " + id + " : " + result);
                    if(blacklistDeletedFriendsEnabled) {
                        log(". Внесение пользователя " + id + " в чёрный список...");
                        vkAccount.addToBlacklist(id);
                        increment_blacklistedDeletedFriendsCounter();
                    }
                }
                log(". ("+vkAccount+") Отклонено "+users.size()+" подписок.");
            }
        }
        catch (Throwable e){
            e.printStackTrace();
            log("! ("+vkAccount+") Ошибка отклонения заявок от удалившихся друзей: " + e.toString());
        }
    }

    private void increment_acceptedFriendRequestsCounter(){
        acceptedFriendRequestsCounter ++;
        vkAccount.getFileStorage().put("acceptedFriendRequestsCounter", acceptedFriendRequestsCounter).commit();
    }
    private void increment_rejectedDeletedFriendsCounter(){
        rejectedDeletedFriendsCounter ++;
        vkAccount.getFileStorage().put("rejectedDeletedFriendsCounter", rejectedDeletedFriendsCounter).commit();
    }
    private void increment_blacklistedDeletedFriendsCounter(){
        blacklistedDeletedFriendsCounter ++;
        vkAccount.getFileStorage().put("blacklistedDeletedFriendsCounter", blacklistedDeletedFriendsCounter).commit();
    }
    private void resetCounters(){
        acceptedFriendRequestsCounter = 0;
        vkAccount.getFileStorage().put("acceptedFriendRequestsCounter", acceptedFriendRequestsCounter).commit();
        blacklistedDeletedFriendsCounter = 0;
        vkAccount.getFileStorage().put("blacklistedDeletedFriendsCounter", blacklistedDeletedFriendsCounter).commit();
        rejectedDeletedFriendsCounter = 0;
        vkAccount.getFileStorage().put("rejectedDeletedFriendsCounter", rejectedDeletedFriendsCounter).commit();
        countersResetDate = new Date();
        vkAccount.getFileStorage().put("countersResetDate", countersResetDate).commit();
    }


    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }
        public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            if(message.getText().equals("status") || message.getText().equals("acc status"))
                return  "Аккаунт " + vkAccount + " принятие всех заявок в друзья: " +
                            (acceptFriendRequestsEnabled?"включено":"выключено") + "\n" +
                        "Аккаунт " + vkAccount + " отписка от удалившихся друзей: " +
                            (rejectDeletedFriendsEnabled?"включено":"выключено") + "\n" +
                        "Аккаунт " + vkAccount + " добавление в ЧС удалившихся друзей: "+
                            (blacklistDeletedFriendsEnabled?(rejectDeletedFriendsEnabled?"включено":"не работает: включи отклонение исходящих заявок"):"выключено") + "\n" +
                        "Аккаунт " + vkAccount + " принято заявок в друзья c "+DATE_FORMAT.format(countersResetDate) + ": " +
                            acceptedFriendRequestsCounter + "\n" +
                        "Аккаунт " + vkAccount + " отписано от удалившихся друзей с "+DATE_FORMAT.format(countersResetDate) + ": " +
                            rejectedDeletedFriendsCounter + "\n" +
                        "Аккаунт " + vkAccount + " заблокировано удалившихся друзей с "+DATE_FORMAT.format(countersResetDate) + ": " +
                            blacklistedDeletedFriendsCounter + "\n";
            return "";
        }
        public @Override ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    private class AcceptAnyRequest extends CommandModule {
        public AcceptAnyRequest(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("acceptanyrequest")) {
                        setAcceptFriendRequestsEnabled(commandParser.getBoolean());
                        if(acceptFriendRequestsEnabled)
                            return "("+vkAccount+") Теперь "+vkAccount+" будет автоматически принимать всех друзей.";
                        else
                            return "("+vkAccount+") Больше "+vkAccount+" не будет автоматически принимать всех друзей.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Включить или выключить принятие всех заявок в друзья для аккаунта "+vkAccount,
                    "Бот может автоматически проверять наличие новых заявок в друзья и принимать их. " +
                            "Также бот примет в друзья всех (кого сможет) подписчиков твоей страницы.\n" +
                            "Обрати внимание, что во Вконтакте есть ограничение в 10 000 друзей. " +
                            "Больше этого лимита бот не будет принимать друзей.",
                    "botcmd acc " + vkAccount.getId() + " acceptanyrequest <on/off>"));
            return result;
        }
    }
    private class RejectFollowers extends CommandModule{
        public RejectFollowers(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("rejectfollowers")) {
                        setRejectDeletedFriendsEnabled(commandParser.getBoolean());
                        if (rejectDeletedFriendsEnabled)
                            return "(" + vkAccount + ") Теперь " + vkAccount + " будет автоматически отписываться от людей, " +
                                    "которые удалили его из друзей.";
                        else
                            return "(" + vkAccount + ") Больше " + vkAccount + " не будет автоматически отписываться от людей, " +
                                    "которые удалили его из друзей.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Включить или выключить отписку от удалившихся из друзей людей для аккаунта "+vkAccount,
                    "Каждый раз, когда какой-то мудак удаляется из друзей, ты оказываешься у него в подписчиках.\n" +
                            "Бот может следить за твоими подписками и отписываться от всех.\n" +
                            "Обрати внимание, когда ты кому-то ВРУЧНУЮ отправляешь заявку в друзья, " +
                            "бот будет отменять её, потому что подумает что он удалился.",
                    "botcmd acc " + vkAccount.getId() + " rejectfollowers <on/off>"));
            return result;
        }
    }
    private class BlacklistFollowers extends CommandModule{
        public BlacklistFollowers(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("blacklistfollowers")) {
                        setBlacklistDeletedFriendsEnabled(commandParser.getBoolean());
                        if(blacklistDeletedFriendsEnabled) {
                            if(!rejectDeletedFriendsEnabled)
                                return "(" + vkAccount + ") Внесение удалившихся друзей в чёрный список включено, " +
                                        "но оно будет работать только если ты включишь отписку от удалившихся друзей.";
                            return "(" + vkAccount + ") Внесение удалившихся друзей в чёрный список включено.";
                        }
                        else
                            return "(" + vkAccount + ") Те, кто удалился из друзей "+vkAccount+" больше не будут заноситься в чёрный список.";
                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Включить или выключить внесение удалившихся друзей в чёрный список для аккаунта "+vkAccount,
                    "Все, кто удалил тебя из друзей, могут быть автоматически заблокированы!\n" +
                            "Каждый раз, когда какой-то мудак удаляется из друзей, ты оказываешься у него в подписчиках.\n" +
                            "Бот будет следить за твоими подписками и блокировать всех, кто там появится.\n" +
                            "Обрати внимание, когда ты кому-то ВРУЧНУЮ отправляешь заявку в друзья, " +
                            "бот будет блокировать его, потому что подумает что он удалился.\n"+
                            "Эта возможность работает только если включено отклонение исходящих заявок в друзья.\n"+
                            "Эта возможность работает только если куплена донатка.",
                    "botcmd acc " + vkAccount.getId() + " blacklistfollowers <on/off>"));
            return result;
        }
    }
    private class ResetFriendsCounter extends CommandModule{
        ResetFriendsCounter(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("resetfriendscounter")) {
                        String oldData =
                                "Было принято заявок в друзья c "+DATE_FORMAT.format(countersResetDate) + ": " +
                                        acceptedFriendRequestsCounter + ".\n" +
                                "Было отписано от удалившихся друзей с "+DATE_FORMAT.format(countersResetDate) + ": " +
                                        rejectedDeletedFriendsCounter + ".\n" +
                                "Было заблокировано удалившихся друзей с "+DATE_FORMAT.format(countersResetDate) + ": " +
                                        blacklistedDeletedFriendsCounter + "."
                                ;
                        resetCounters();
                        return "Счётчики сброшены. До сброса на счётчиках были следующие значения:\n" + oldData;

                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Сбросить счётчики автоматически принятых и удалённых друзей для аккаунта "+vkAccount,
                    "Бот ведёт счётчики автоматически принятых друзей, " +
                            "автоматических отписок от удалившихся друзей и " +
                            "автоматических блокировок удалившихся друзей.\n" +
                            "Эти счётчики отображаются командой status.\n" +
                            "Эти счётчики сохраняются после перезапуска программы. " +
                            "Чтобы их сбросить и нужна эта команда.",
                    "botcmd acc " + vkAccount.getId() + " resetfriendscounter"));
            return result;
        }
    }
    private class ClearBlacklist extends CommandModule{
        private Thread clearThread = null;

        public ClearBlacklist(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(final Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("acc"))
                if(vkAccount.isMine(commandParser.getWord()))
                    if(commandParser.getWord().toLowerCase().equals("clearblacklist")) {
                        if(clearThread == null) {
                            clearThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        clearBlacklist(message);
                                    }
                                    catch (Exception e){
                                        message.sendAnswer("Не получилось почистить ЧС " + vkAccount +
                                                "  из-за ошибки:\n" + e.toString());
                                    }
                                }
                            });
                            clearThread.start();
                            return "Процедура очистки ЧС " + vkAccount + " запустилась. " +
                                    "Пока она будет выполняться, тебе будут приходить отчёты.";
                        }
                        else {
                            clearThread.interrupt();
                            clearThread = null;
                            return "Процедура очистки ЧС " + vkAccount + " останавливается... " +
                                    "Когда она остановится, я тебе сообщу.";
                        }

                    }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Очистить чёрный список аккаунта "+vkAccount,
                    "Эта команда очистит чёрный список(ЧС) страницы Вконтакте. \n" +
                            "Это длительная процедура, которая выполняется на сервере. Скорость " +
                            "удаления людей из чёрного списка составляет примерно по 1 человеку " +
                            "за 10 секунд. Если людей очень много, команда может не успеть удалить " +
                            "всех до выключения бота, тогда после запуска понадобится снова " +
                            "запустить команду.\n" +
                            "После того как команда запустится, ты получишь отчёт о начале работы, " +
                            "а потом будешь получать отчёты каждый раз после того, как бот " +
                            "удалит из ЧС ещё 100 человек, либо закончит чистить ЧС." +
                            "Чтобы остановить очистку ЧС, напиши эту команду ещё раз.",
                    "botcmd acc " + vkAccount.getId() + " resetfriendscounter"));
            return result;
        }

        private void clearBlacklist(Message message) throws Exception{
            int cleared = 0;
            long started = System.currentTimeMillis();
            while(true){
                ArrayList<User> users = vkAccount.getBlacklist100();
                if(users.size() == 0) {
                    if(cleared == 0)
                        message.sendAnswer("У пользователя " + vkAccount + " в ЧС никого нет.");
                    else {
                        long now = System.currentTimeMillis();
                        long min = (now - started)/(1000*60);
                        message.sendAnswer(
                                "Очистка ЧС " + vkAccount + " завершена.\n" +
                                "Удалено " + cleared + " пользователей.\n" +
                                "Очистка заняла "+min+" минут.");
                    }
                    return;
                }
                for (User user : users) {
                    vkAccount.removeFromBlacklist(user.uid);
                    cleared++;

                    if(clearThread == null){
                        long now = System.currentTimeMillis();
                        long min = (now - started)/(1000*60);
                        message.sendAnswer("Очистка ЧС " + vkAccount + " остановлена.\n" +
                                        "Удалено " + cleared + " пользователей.\n" +
                                        "Очистка длилась "+min+" минут.");
                        return;
                    }
                }
                message.sendAnswer(
                        "Очистка ЧС " + vkAccount + " продолжается... \n" +
                        "Уже удалено "+cleared+" пользователей.");
            }

        }
    }

}
