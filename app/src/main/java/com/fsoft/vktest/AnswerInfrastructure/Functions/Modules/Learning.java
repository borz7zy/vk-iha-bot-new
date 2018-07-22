package com.fsoft.vktest.AnswerInfrastructure.Functions.Modules;

import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.AnswerElement;
import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.UnknownMessage;
import com.fsoft.vktest.AnswerInfrastructure.Functions.Function;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.User;
import com.perm.kate.api.Attachment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Сложный модуль, обеспечивающий работу систем обучения бота
 *
 *
// * Модуль содержит списки (это всё хуйня):
// * - обычные учителя
// *      Ответы от них попадают в песочницу, где ожидают подтверждения доверенных учителей
// * - доверенные учителя
// *      Могут проводить валидацию ответов из песочницы, а их ответы добавляются в базу автоматически
// * - плохие учителя
// *      Список, люди из которого к обучению не допускаются
// * - счётчик активности пользователей.
// *      Особо активным пользователям 100+ ответов будет периодически (2 раза в день по умолчанию) присылаться неизвестные из базы.
// *      Пользователь может отказаться от этой "подписки", тогда в его запись в вносится notifyFrequency = 0; // def = 2
// *      отсчёт уведомлений начинается в 10:00 каждого дня и заканчивается в 21:00 каждого дня, по умолчанию.
// *      Настраивается.
 *
 *
 *
 *
 *
 * Модуль содержит список всех пользователей, котовые писали боту за последний месяц!
 * Эта информация имеет большое значение, поскольку обучение - это очень важно для бота!
 * Этот список содержит поля:
 * + long ID пользователя (по умолчанию -1)
 * + int количество сообщений в диалогах с ботом (по умолчанию 0)
 * + long bot id - ID аккаунта бота, с которым этот пользователь общается
 * + date дата первого личного сообщения от пользователя (по умолчанию текущая)
 * + date дата последнего личного сообщения от пользователя (по умолчанию текущая)
 * + bool Является ли он доверенным учителем (по умолчанию нет)
 * + bool Является ли он плохим учителем (по умолчанию нет)
 * + bool открыта ли сессия обучения. если сессия открыта, бот будет присылать новые вопросы после записанного ответа (по умолчанию нет)
 * + bool был ли отказ от подписки
 * + int количество добавленных в базу ответов (прошли модерацию) (по умолчанию 0)
 * + int количество выполненных модераций (по умолчанию 0)
 * + int количество ответов которые НЕ прошли модерацию (по умолчанию 0)
 * + int количество добавленных в песочницу ответов (по умолчанию 0)
 * + int Частота присылания пользователю вопросов за день (по умолчанию 0, если отключено)
 * + int время начала дня, присылания вопросов (по умолчанию 10)
 * + int время конца дня, присылания вопросов (по умолчанию 21)
 * + int количество вопросов присланных пользователю сегодня (по умолчанию 0)
 * + int failsCounter количество неудачных попыток прислать вопрос (проигнорированных)
 * + UnknownMessage pending question последние вопросы заданный пользователю (по умолчанию null)
 * + AnswerElement[] pending moderation последние вопросы отправленные пользовалю на модерацию
 * + date время присылания пользователю последнего вопроса (по умолчанию 01.01.1991)
 * + String comment комментарий к пользователю в контексте обучения
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 * Когда приходит новое сообщение:
 * - проверить источник сообщения. Если не DIALOG - пропускаем.
 * - проверить нет ли там обращения. Если нет, пропускаем.
 * - проверить не плохой ли это учитель. Если плохой, пропускаем.
 * - Проверить не команда ли это.
 *      обучение стоп
 *          Если сесися обучения не открыта, написать "Сессия не открыта"
 *          Если сессия обучения открыта, сессия = false. если в ожидании модерация, вынести ответ
 *          обратно в список модерации. Eсли в ожидании вопрос, вернуть вопрос в список неизвестных.
 *      обучение начать
 *          если сессия уже открыта, написать что сессия уже открыта.
 *          Открыта сессия - true
 *          Если есть ожидающие вопросы вынести их юзеру
 *          Если ожидающих нету, то если это доверенный, то если есть на модерацию - запросить модерацию.
 *          вывести список неизвестных
 *      обучение чаще
 *          Оформлена ли подписка? если да, увеличить число.
 *      обучение реже
 *          Оформлена ли подписка? если да, уменьшить число.
 *      обучение подписка
 *          Оформлена ли подписка? если нет, оформить.
 *      обучение отписка
 *          Оформлена ли подписка? если да, отписать.
 *      обучение вопрос*ответ
 *          если юзер доверенный, добавить в базу
 *          если не доверенный, добавить в песочницу
 *      обучение пропустить
 *          если это был запрос на обучение, вернуть неизвестное назад в базу неизвестных
 *      обучение начинать в ...
 *          если подписка оформлена, изменить время
 *      обучение заканчивать в ...
 *          если подписка не оформлена, изменить время
 *      не команда
 *          если есть ожидающая модерация, проверить формат. Должно быть 0987654321
 *          Если есть ожидающий вопрос: если юзер доверенный, добавить в базу
 *          если не доверенный, добавить в песочницу
 *
 * - зарегистрировать сообщение от юзера в базе (дата первого обращения, дата последнего обращения, количество)
 *
 *
 *
 *                       ПРИМЕРЫ СООБЩЕНИЙ БОТА
 *
 * +++++++++++++ Что бот пишет пользователю который много и давно с ним общается, но не подписан на рассылку: =================================================================
 *
 * Аркадий, привет! Мы с тобой давно общаемся, поэтому я хотел бы предложить тебе принять участие в моём обучении!
 * Мне пишут много разных вопросов, на многие из которых я не знаю ответа. Я собираю такие вопросы для того, чтобы учителя помогли мне подобрать на них ответы.
 * Если ты хочешь добавлять в мою базу ответы:
 * .
 * Напиши мне "Бот, обучение начать" если ты хочешь подряд ответить на несколько вопросов.
 * Напиши мне "Бот, обучение вопрос*ответ" если ты хочешь добавить в базу ответ на какой-то вопрос.
 * Напиши мне "Бот, обучение подписка" если ты хочешь чтобы я сам периодически присылал тебе новые вопросы.
 *
 * ++++++++++++ Что бот пишет пользователю который оформил подписку: =================================================================
 *
 * Аркадий, поздравляю! Теперь я буду периодически присылать тебе новые вопросы, чтобы ты мог меня обучать.
 * По умолчанию я буду присылать тебе вопросы 2 раза в день.
 * .
 * Напиши мне "Бот, обучение чаще" если хочешь получать от меня получать сообщения с вопросами чаще.
 * Напиши мне "Бот, обучение реже" если хочешь получать от меня получать сообщения с вопросами реже.
 * Напиши мне "Бот, обучение начинать в 10" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое начинать тебе присылать сообщения с вопросами.
 * Напиши мне "Бот, обучение заканчивать в 22" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое заканчивать тебе присылать сообщения с вопросами.
 * Напиши мне "Бот, обучение отписка" если ты не хочешь от меня получать сообщений с вопросами.
 *
 * +++++++++++ Что бот пишет пользователю периодически: (ВОПРОС) ====================================================================================================================
 *
 * Аркадий, привет! Мне нужна твоя помощь. Я не знаю как ответить на эту фразу:
 * .
 * А жаренных гвоздей не хочешь?
 * .
 * Напиши мне, как правильно ответить на это сообщение.
 * Твоё следующее сообщение будет сохранено как ответ на эту фразу.
 * Напиши мне "Бот, обучение отмена" если ты не можешь сейчас отвечать на сообщения. Тогда я напишу тебе позже.
 * Напиши мне "Бот, обучение пропустить" если ты не хочешь отвечать на это сообщение. Тогда я напишу тебе другое.
 * Напиши мне "Бот, обучение чаще" если хочешь получать от меня получать сообщения с вопросами чаще.
 * Напиши мне "Бот, обучение реже" если хочешь получать от меня получать сообщения с вопросами реже.
 * Напиши мне "Бот, обучение начинать в 10" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое начинать тебе присылать сообщения с вопросами.
 * Напиши мне "Бот, обучение заканчивать в 22" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое заканчивать тебе присылать сообщения с вопросами.
 * Напиши мне "Бот, обучение отписка" если ты не хочешь от меня получать сообщений с вопросами.
 *
 * ++++++++++++ Что бот отвечает на добавленный ответ если сессия не открыта: (периодическая рассылка) ======================================================================
 *
 * Аркадий, спасибо! Я запомнил, что на "А жаренных гвоздей не хочешь?" надо отвечать "Сам их жри!".
 * .
 * Ты добавил мне уже 10 ответов, это мне очень помогает!
 * Напиши мне "Бот, обучение начать" если ты хочешь добавить ещё несколько ответов.
 * Напиши мне "Бот, обучение вопрос*ответ" если ты хочешь добавить в базу ответ на какой-то вопрос.
 *
 * +++++++++++++ Что бот отвечает на добавленный ответ если сессия открыта: =================================================================================================
 *
 * Аркадий, спасибо! Я запомнил, что на "А жаренных гвоздей не хочешь?" надо отвечать "Сам их жри!".
 * Ответь на следующий вопрос:
 * .
 * "Кто проживает на дне океана?"
 * .
 * Напиши мне, как правильно ответить на это сообщение.
 * Твоё следующее сообщение будет сохранено как ответ на эту фразу.
 * Ты добавил мне уже 10 ответов, это мне очень помогает!
 * Напиши мне "Бот, обучение пропустить" если ты не хочешь отвечать на это сообщение. Тогда я напишу тебе другое.
 * Напиши мне "Бот, обучение стоп" чтобы остановить сессию обучения.
 *
 * ============== Что бот отвечает если сессия закрыта: ======================================================================================================================
 *
 * Аркадий, сессия обучения завершена. За всё время ты добавил мне 10 ответов. Спасибо!
 * .
 * Напиши мне "Бот, обучение начать" если ты хочешь добавить ещё несколько ответов.
 * Напиши мне "Бот, обучение вопрос*ответ" если ты хочешь добавить в базу ответ на какой-то вопрос.
 * Напиши мне "Бот, обучение отписка" если ты не хочешь от меня получать сообщений с вопросами.
 *
 * ++++++++++++++ Как бот предлагает промодерировать ответы: =================================================================================================================
 *
 * Аркадий, привет! У меня накопилось немного ответов, качество которых я не могу оценить.
 * Мне нужна твоя помощь.
 * Вот ответы:
 * .
 * 0) .... -> ...
 * .
 * 1) .... -> ...
 * .
 * 2) .... -> ...
 * .
 * 3) .... -> ...
 * .
 * 4) .... -> ...
 * .
 * 5) .... -> ...
 * .
 * 6) .... -> ...
 * .
 * 7) .... -> ...
 * .
 * 8) .... -> ...
 * .
 * 9) .... -> ...
 * .
 * Напиши мне в ответе номера хороших ответов.
 * Например: бот, 3845.
 * Напиши мне "Бот, обучение отмена" если ты не можешь сейчас отвечать на сообщения. Тогда я напишу тебе позже.
 * Напиши мне "Бот, обучение чаще" если хочешь получать от меня получать сообщения с вопросами чаще.
 * Напиши мне "Бот, обучение реже" если хочешь получать от меня получать сообщения с вопросами реже.
 * Напиши мне "Бот, обучение начинать в 10" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое начинать тебе присылать сообщения с вопросами.
 * Напиши мне "Бот, обучение заканчивать в 22" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое заканчивать тебе присылать сообщения с вопросами.
 * Напиши мне "Бот, обучение отписка" если ты не хочешь от меня получать сообщений с вопросами.
 *
 *
 *
 *=== есть смысл эти все сущности собрать в общий список пользователей. Но тогда это же будет дохрена? Нет.
 * Там ещё надо вписать дату, и чистить старых пользователей, у которых уже более месяца нет активности.
 * Список хранить в оперативке. Юзеров вроде не так уж и много.
 *
 *
 *
 * В обязанности данного модуля входит:
 *
 * - предоставление возможности обучения
 * - предоставление возможности модерации
 * - учёт списка учителей
 * - учёт списка доверенных учителей
 * - учёт песочницы
 * - периодическая рассылка учителям неизвестных сообщений
 * - Предложение активным пользователям принять участие в обучении
 * - предоставление учителям доступа к пополнению базы вне зависивости от их прав
 * - предоставление всем права пополнять базуновыми фразами с последующей модерацией
 * - сохранение новых ответов в базу с сохранением максимального количества информации
 *
 *
 *
 *
 *
 * Начало обучения: Бот, обучение
 *
 *
 * ОПИСАНИЕ:
 * Модуль предоставляет возможности для обучения.
 * Модуль работает ТОЛЬКО в личных сообщениях
 * Модуль хранить список доверенных учителей и плохих учителей.
 * Плохие учителя этим модулем игнорируются
 * Ответы от доверенных учителей попадают сразу в базу ответов,
 * а ответы обычных пользователей попадают в песочницу.
 * Ответы из песочницы должны пройти модерацию доверенными учителями.
 * Любой пользователь может подписаться на рассылку вопросов командой "обучение подписка"
 * Любой пользователь может добавить ответ в песочницу командой обучение вопрос*ответ
 * Любой пользователь может отвечать на неизвестные фразы командой "обучение начать"
 *
 *
 *
 * ПИЗДЕЦ, КАКОЙ ЖЕ СЛОЖНЫЙ МОДУЛЬ
 * Created by Dr. Failov on 01.10.2017.
 */

public class Learning extends Function {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault());
    private ArrayList<UserData> users = new ArrayList<>();
    private ArrayList<AnswerElement> sandbox = new ArrayList<>();
    private Timer timer = null;



    public Learning(ApplicationManager applicationManager) {
        super(applicationManager);
        load();
    }

    @Override
    public Message processMessage(Message messageOriginal) {
        if(!hasTreatment(messageOriginal))
            return messageOriginal;
        Message message = remTreatment(messageOriginal);
        //если это плохой учитель - игнорировать его
        if(isBad(message.getAuthor().getGlobalId()))
            return message;
        //// TODO: 09.11.2017 какое-то это дерьмо. Чувствую будет ломать систему

        UserData user = getByGlobalId(message.getAuthor().getGlobalId());
        if(user == null){
            user = new UserData(message.getAuthor(), message.getBotAccount().getId());
            users.add(user);
        }
        message = user.processMessage(message);
        if(message.getAnswer() != null)
            return message;
        return messageOriginal;
    }

    @Override
    public String getName() {
        return "learning";
    }

    @Override
    public String getDescription() {
        //// TODO: 09.10.2017
        return "Модуль занимается обслуживанием функций обучения" +
                "..." +
                "Важное замечание: модуль сохраняет базы данных раз в час, поэтому ";
    }

    @Override
    public void stop() {
        super.stop();
        stopTimer();
    }

    public boolean isBad(String id){
        UserData userData = getByGlobalId(id);
        if(userData == null)
            return false;
        return userData.isBadTeacher();
    }
    public boolean isAllowed(User user){
        UserData userData = getByUser(user);
        if(userData == null)
            return false;
        return userData.isAllowedTeacher();
    }


    private void load(){
        {
            log(". Загрузка базы данных пользователей модуля обучение...");
            String[] rows = getStorage().getStringArray("users", new String[]{});
            for (String row : rows) {
                try {
                    users.add(new UserData(new JSONObject(row)));
                } catch (Exception e) {
                    log("! Ошибка разбора строки как элемента базы пользователей модуля обучения: " + row);
                    e.printStackTrace();
                }
            }
            log(". База данных пользователей модуля обучение загружена.");
        }
        {
            log(". Загрузка базы данных песочницы модуля обучение...");
            String[] rows = getStorage().getStringArray("sandbox", new String[]{});
            for (String row : rows) {
                try {
                    sandbox.add(new AnswerElement(new JSONObject(row)));
                } catch (Exception e) {
                    log("! Ошибка разбора строки как элемента базы песочницы модуля обучения: " + row);
                    e.printStackTrace();
                }
            }
            log(". База данных песочницы модуля обучение загружена.");
        }
        startTimer();
    }
    private void save(){
        {
            log(". Сохранение базы данных пользователей модуля обучение...");
            String[] rows = new String[users.size()];
            for (int i = 0; i < users.size(); i++) {
                try {
                    rows[i] = users.get(i).toJson().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            getStorage().put("users", rows).commit();
            log(". База данных пользователей модуля обучение сохранена.");
        }

        {
            log(". Сохранение базы данных песочницы модуля обучение...");
            String[] rows = new String[sandbox.size()];
            for (int i = 0; i < sandbox.size(); i++) {
                try {
                    rows[i] = sandbox.get(i).toJson().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            getStorage().put("sandbox", rows).commit();
            log(". База данных песочницы модуля обучение сохранена.");
        }
    }
    private void startTimer(){
        if(timer == null){
            timer = new Timer("Главный таймер модуля обучения.");
            long min10 = 1000L*60L*10L;
            long hr1 = 1000L*60L*60L;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    tick();
                }
            }, min10, hr1);
        }
    }
    private void stopTimer(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }
    private void tick(){
        /*

 * периодически (раз в час):
 * - проверить работает ли ещё программа. если не работает - ничего не делать.
 * - удалить старых пользователей (которые писали больше чем месяц назад)
 * - проверить у кого просрочен вопрос (более 5 часов). Для тех отменить вопрос и сделать типа он был задан сейчас.
 * - если последний вопрос был задан вчера, а в счётчике отправленных сегодня не ноль, сбросить счётчик отправленных сегодня
 * - проверить кому пора отправлять новый вопрос, разослать вопросы, обновить время последнего отправленного сообщения
 * - для доверенный учителей слать вопросы из песочницы на подтверждение
 * - сохранить базу. Писать во временный файл, и только когда всё готово, переименовать текущий в _BKP_yyyy-MM-dd а новый на его место
        * */
        log(". Выполнение периодических задач модуля обучения...");
        if(!applicationManager.isRunning()){
            log(". Программа была остановлена. Пропуск таймера.");
            return;
        }
        log(". Очистка старых юзеров...");
        clearOldUsers();
        log(". Отзыв вопросов, на которые не был получен ответ...");
        processQuestionsTimeout();
        updateTodayQuestionCounters();
        broadcastQuestions();
        save();
        log(". Завершено выполнение периодических задач модуля обучения...");
    }
    private boolean hasModeration(){
        //показывает ли в очереди хоть один ответ требующий модерации
        return !sandbox.isEmpty();
    }
    private ArrayList<AnswerElement> popModeration(){
        //возвращает 10 ответов которые требуют модерации, при этом удаляя их из массива модерации
        ArrayList<AnswerElement> result = new ArrayList<>();
        for (int i=0; i< Math.min(sandbox.size(), 8); i++)
            result.add(sandbox.get(i));
        for(AnswerElement answerElement:result)
            sandbox.remove(answerElement);
        return result;
    }
    private UnknownMessage popUnknown(){
        try {
            return getApplicationManager().getBrain().getUnknownMessages().popTop();
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    private UserData getByGlobalId(String id){
        //// TODO: 16.10.2017 Как вариант для оптимизации, использовать HashMap в качестве кэша. Он имеет ряд полезных оптимизаций.
        for (UserData user:users)
            if(user.user.getGlobalId().equals(id))
                return user;
        return null;
    }
    private UserData getByUser(User inUser){
        for (UserData user:users)
            if(user.user.equals(inUser))
                return user;
        return null;
    }
    private void clearOldUsers(){
        //очистить старых пользователей. Это те, кто не писал боту более месяца
        log(". Очистка старых пользователей из истории обучения...");
        ArrayList<UserData> toDelete = new ArrayList<>();
        for(UserData user:users){
            if(user.howMuchDaysSinceLastMessage() > 30)
                toDelete.add(user);
        }
        log(". Будет удалено "+toDelete.size()+" пользователей из истории обучения.");
        users.removeAll(toDelete);
        log(". Удалено "+toDelete.size()+" пользователей из истории обучения. Осталось " + users.size() + " пользователей.");
    }
    private void processQuestionsTimeout(){
        //очистить старых пользователей. Это те, кто не писал боту более месяца
        log(". Отзыв вопросов от пользователей, которые на вопросы не ответили вовремя.");
        int cnt = 0;
        for(UserData user:users){
            if(user.processQuestionsTimeout())
                cnt++;
        }
        log(". Отозвано "+cnt+" вопросов к пользователям.");
    }
    private void updateTodayQuestionCounters(){
        //когда наступил следующий день, счётчик сообщений надо сбросить в ноль
        log(". Обновление данных о счётчике сообщений.");
        for(UserData user:users){
            user.updateTodayQuestionCounter();
        }
        log(". Обработано "+users.size()+" пользователей.");
    }
    private void broadcastQuestions(){
        //Раз в час рассылать пользователям уведомления, вопросы и т.д.
        log(". Рассылка вопросов пользователям...");
        for(UserData user:users){
            user.broadcastQuestions();
        }
        log(". Обработано "+users.size()+" пользователей.");
    }





    private class UserData{
        //класс хранит информацию о пользователе.
        //ID пользователя в VK
        private User user = null;
        //Счётчик сообщений с этим пользователем
        private long lsCounter = 0;
        //ID аккаунта бота, с которым юзер общается
        private long botId = 0;
        //Дата первого и последнего сообщения от юзера
        private Date firstMessageDate = new Date();
        private Date lastMessageDate = new Date();
        //дата когда пользователю был последний раз задан вопрос
        private Date lastQuestionDate = new Date();
        //Является ли пользователь доверенным учителем.
        //Ответы от доверенных добавляются сразу в базу данных
        private boolean allowedTeacher = false;
        //является ли пользователь плохим учителем. Их этот модуль игнорирует
        private boolean badTeacher = false;
        //Открыта ли сессия обучения. Надо ли присылать новый вопрос после сохранения предыдущего
        private boolean learningSessionOpened = false;
        //Предлагалась ли пользователю подписка
        private boolean subscriptionSuggested = false;
        //Количество ответов которые прошли модерацию и были добавлены в БД
        private int databaseAddCounter = 0;
        //Количество ответов которые не прошли модерацию
        private int answersRejectedCounter = 0;
        //количество выполненных модераций
        private int reviewCounter = 0;
        //Количество сообщений которые были добавлены в песочницу
        private int sandboxAddCounter = 0;
        //Количество задаваемых по подписке вопросов в день
        private int questionFrequency = 0;
        //С которого часа задавать пользователю вопросы
        private int dayStartHour = 10;
        //До которого часа задавать пользователю вопросы
        private int dayFinishHour = 22;
        //сколько вопросов было задано пользователю сегодня
        private int todayQuestionsCounter = 0;
        //Количество раз ПОДРЯД, когда вопросы по подписке пользователь проигнорировал
        private int subscriptionFailsCounter = 0;
        //Заданный пользователю вопрос, ожидающий ответа
        private UnknownMessage pendingQuestion = null;
        //Ответы других пользователей, ожидающие модерации
        private ArrayList<AnswerElement> pendingModeration = null;
        //комментарий к пользователю в контексте обучения
        private String comment = "";


        public UserData(User user, long botId) {
            this.user = user;
            this.botId = botId;
        }
        public UserData(JSONObject jsonObject) throws Exception{
            fromJson(jsonObject);
        }

        public JSONObject toJson() throws Exception{
            JSONObject result = new JSONObject();

            result.put("user", user.toJson());

            result.put("lsCounter", lsCounter);

            result.put("botId", botId);

            result.put("firstMessageDate", sdf.format(firstMessageDate));

            result.put("lastMessageDate", sdf.format(lastMessageDate));

            result.put("lastQuestionDate", sdf.format(lastQuestionDate));

            result.put("allowedTeacher", allowedTeacher);

            result.put("badTeacher", badTeacher);

            result.put("learningSessionOpened", learningSessionOpened);

            result.put("subscriptionSuggested", subscriptionSuggested);

            result.put("databaseAddCounter", databaseAddCounter);

            result.put("answersRejectedCounter", answersRejectedCounter);

            result.put("reviewCounter", reviewCounter);

            result.put("sandboxAddCounter", sandboxAddCounter);

            result.put("questionFrequency", questionFrequency);

            result.put("dayStartHour", dayStartHour);

            result.put("dayFinishHour", dayFinishHour);

            result.put("todayQuestionsCounter", todayQuestionsCounter);

            result.put("subscriptionFailsCounter", subscriptionFailsCounter);

            result.put("pendingQuestion", pendingQuestion.toJson());

            result.put("comment", comment);

            {
                JSONArray jsonArray = new JSONArray();
                for (AnswerElement answerElement:pendingModeration)
                    jsonArray.put(answerElement.toJson());
                result.put("pendingModeration", jsonArray);
            }

            return result;
        }
        private void fromJson(JSONObject jsonObject) throws Exception{
            if(jsonObject.has("user") && !jsonObject.isNull("user"))
                user = new User(jsonObject.getJSONObject("user"));

            if(jsonObject.has("lsCounter") && !jsonObject.isNull("lsCounter"))
                lsCounter = jsonObject.getLong("lsCounter");

            if(jsonObject.has("botId") && !jsonObject.isNull("botId"))
                botId = jsonObject.getLong("botId");

            if(jsonObject.has("botId") && !jsonObject.isNull("botId"))
                botId = jsonObject.getLong("botId");

            if(jsonObject.has("firstMessageDate") && !jsonObject.isNull("firstMessageDate"))
                firstMessageDate = sdf.parse(jsonObject.getString("firstMessageDate"));

            if(jsonObject.has("lastMessageDate") && !jsonObject.isNull("lastMessageDate"))
                lastMessageDate = sdf.parse(jsonObject.getString("lastMessageDate"));

            if(jsonObject.has("lastQuestionDate") && !jsonObject.isNull("lastQuestionDate"))
                lastQuestionDate = sdf.parse(jsonObject.getString("lastQuestionDate"));

            if(jsonObject.has("allowedTeacher") && !jsonObject.isNull("allowedTeacher"))
                allowedTeacher = jsonObject.getBoolean("allowedTeacher");

            if(jsonObject.has("badTeacher") && !jsonObject.isNull("badTeacher"))
                badTeacher = jsonObject.getBoolean("badTeacher");

            if(jsonObject.has("learningSessionOpened") && !jsonObject.isNull("learningSessionOpened"))
                learningSessionOpened = jsonObject.getBoolean("learningSessionOpened");

            if(jsonObject.has("subscriptionSuggested") && !jsonObject.isNull("subscriptionSuggested"))
                subscriptionSuggested = jsonObject.getBoolean("subscriptionSuggested");

            if(jsonObject.has("databaseAddCounter") && !jsonObject.isNull("databaseAddCounter"))
                databaseAddCounter = jsonObject.getInt("databaseAddCounter");

            if(jsonObject.has("answersRejectedCounter") && !jsonObject.isNull("answersRejectedCounter"))
                answersRejectedCounter = jsonObject.getInt("answersRejectedCounter");

            if(jsonObject.has("reviewCounter") && !jsonObject.isNull("reviewCounter"))
                reviewCounter = jsonObject.getInt("reviewCounter");

            if(jsonObject.has("sandboxAddCounter") && !jsonObject.isNull("sandboxAddCounter"))
                sandboxAddCounter = jsonObject.getInt("sandboxAddCounter");

            if(jsonObject.has("questionFrequency") && !jsonObject.isNull("questionFrequency"))
                questionFrequency = jsonObject.getInt("questionFrequency");

            if(jsonObject.has("dayStartHour") && !jsonObject.isNull("dayStartHour"))
                dayStartHour = jsonObject.getInt("dayStartHour");

            if(jsonObject.has("dayFinishHour") && !jsonObject.isNull("dayFinishHour"))
                dayFinishHour = jsonObject.getInt("dayFinishHour");

            if(jsonObject.has("todayQuestionsCounter") && !jsonObject.isNull("todayQuestionsCounter"))
                todayQuestionsCounter = jsonObject.getInt("todayQuestionsCounter");

            if(jsonObject.has("subscriptionFailsCounter") && !jsonObject.isNull("subscriptionFailsCounter"))
                subscriptionFailsCounter = jsonObject.getInt("subscriptionFailsCounter");

            if(jsonObject.has("comment") && !jsonObject.isNull("comment"))
                comment = jsonObject.getString("comment");

            if(jsonObject.has("pendingQuestion") && !jsonObject.isNull("pendingQuestion"))
                pendingQuestion = new UnknownMessage(jsonObject.getJSONObject("pendingQuestion"));

            if(jsonObject.has("pendingModeration") && !jsonObject.isNull("pendingModeration")) {
                JSONArray jsonArray = jsonObject.getJSONArray("pendingModeration");
                for (int i=0; i<jsonArray.length(); i++){
                    pendingModeration.add(new AnswerElement(jsonArray.getJSONObject(i)));
                }
            }
        }
        public Message processMessage(Message message) {
            //сюда приходит сообщение уже без обращения и только от этого пользователя

            //когда этот пользователь что-то написал, можно начать с того, что
            // + зарегистрировать новое сообщение от него
            // Если оно содержит команду, обрабатывать её
            // если нет
            // + проверить не висит ли на нём какого-то вопроса.
            // + проверить не висит ли на нём какой то модерации.
            try {
                nowLastMessageDate();
                //проверить есть ли команда
                //// TODO: 31.10.2017
                message = processCommand(message);
                if(message.getAnswer() != null)
                    return message;

                //проверить не висит ли у него вопрос
                message = processPendingQuestion(message);
                if(message.getAnswer() != null)
                    return message;

                //проверить не висит ли у него модерации
                message = processPendingModeration(message);
                if(message.getAnswer() != null)
                    return message;

                //если нет, просто скипнуть сообщение
                return message;
            }
            catch (Exception e){
                e.printStackTrace();
                message.setAnswer(new Answer(e.getMessage()));
                return message;
            }

        }
        private Message processCommand(Message message) throws Exception{
            //сюда приходит сообщение уже без обращения и только от этого пользователя
            String text = message.getText().trim();
            String treatment = applicationManager.getBrain().getTreatment();
            String username = message.getAuthor().getName();

            // + обучение начать
            // + обучение стоп
            // + обучение чаще
            // + обучение реже
            // + обучение начинать в 10
            // + обучение заканчивать в 10
            // + обучение подписка
            // + обучение отписка
            // + обучение отмена
            // + обучение пропустить
            // + обучение (вопрос)*(ответ)
            // + обучение удалить (ответ)
            // + обучение статус        (кто я , моя статистика)
            // + обучение справка


            //<editor-fold desc="обучение начать">
            {
                if (text.toLowerCase().equals("обучение начать")) {
                    if (isLearningSessionOpened())
                        return message.withAnswer("Сессия обучения сейчас активна. Ответь на предыдущий вопрос.");
                    setLearningSessionOpened(true);
                    sendQuestionOrModeration();
                    return message.withAnswer("Сессия обучения начата.");
                }
            }
            //</editor-fold>

            //<editor-fold desc="обучение стоп">
            if(text.toLowerCase().equals("обучение стоп")){
                if(!isLearningSessionOpened())
                    return message.withAnswer("Сессия обучения сейчас не активна.");
                setLearningSessionOpened(false);
                revertQuestion();
                return message.withAnswer("Сессия обучения закрыта, вопросы помещены обратно в базу данных.");
            }
            //</editor-fold>

            //<editor-fold desc="обучение чаще">
            if(text.toLowerCase().equals("обучение чаще")){
                if(getQuestionFrequency() == 0)
                    return message.withAnswer("Подписка на вопросы не оформлена. " +
                            "Чтобы я начал тебе присылать вопросы, напиши мне \""+treatment+" обучение подписка\".");
                questionFrequency ++;
                if(getQuestionFrequency() > 10)
                    return message.withAnswer("Ок, я записал, что тебе надо присылать вопросы " + questionFrequency + " раз в день. \n" +
                            "Однако, я, скорее всего, столько не буду успевать тебе отправить, потому, будет меньше.");
                return message.withAnswer("Теперь я буду присылать тебе вопросы " + questionFrequency + " раз в день. \n" +
                        "Возможно, будет получаться меньше, если я не буду успевать.");
            }
            //</editor-fold>

            //<editor-fold desc="обучение реже">
            if(text.toLowerCase().equals("обучение реже")){
                if(getQuestionFrequency() == 0)
                    return message.withAnswer("Подписка на вопросы не оформлена. " +
                            "Чтобы я начал тебе присылать вопросы, напиши мне \""+treatment+" обучение подписка\".");
                questionFrequency --;
                if(getQuestionFrequency() < 1) {
                    questionFrequency = 1;
                    return message.withAnswer("Я не умею присылать вопросы реже одного раза в день.");
                }

                if(getQuestionFrequency() > 10)
                    return message.withAnswer("Ок, я записал, что тебе надо присылать вопросы " + questionFrequency + " раз в день. \n" +
                            "Однако, я, скорее всего, столько не буду успевать тебе отправить, потому, будет меньше.");

                return message.withAnswer("Теперь я буду присылать тебе вопросы " + questionFrequency + " раз в день. \n" +
                        "Возможно, будет получаться меньше, если я не буду успевать.");
            }
            //</editor-fold>

            //<editor-fold desc="обучение начинать в">
            if(text.toLowerCase().startsWith("обучение начинать в ")){
                if(getQuestionFrequency() == 0)
                    return message.withAnswer("Подписка на вопросы не оформлена. " +
                            "Чтобы я начал тебе присылать вопросы, напиши мне \""+treatment+" обучение подписка\".");
                String newTimeString = text.toLowerCase().replace("обучение начинать в ", "").replace(".", "");
                int newTime = 0;
                try {
                    newTime = Integer.parseInt(newTimeString);
                }
                catch (Exception e){
                    return message.withAnswer("После команды надо присылать число.");
                }
                if(newTime >= getDayFinishHour())
                    return message.withAnswer("Время начала дня не может быть таким же или больше времени конца дня. ("+newTime+"-"+getDayFinishHour()+")");
                setDayStartHour(newTime);

                return message.withAnswer("Теперь я буду присылать тебе вопросы в период с " + getDayStartHour() + " по "+getDayFinishHour()+" часов.\n");
            }
            //</editor-fold>

            //<editor-fold desc="обучение заканчивать в">
            if(text.toLowerCase().startsWith("обучение заканчивать в ")){
                if(getQuestionFrequency() == 0)
                    return message.withAnswer("Подписка на вопросы не оформлена. " +
                            "Чтобы я начал тебе присылать вопросы, напиши мне \""+treatment+" обучение подписка\".");
                String newTimeString = text.toLowerCase().replace("обучение заканчивать в ", "").replace(".", "");
                int newTime = 0;
                try {
                    newTime = Integer.parseInt(newTimeString);
                }
                catch (Exception e){
                    return message.withAnswer("После команды надо присылать число.");
                }
                if(getDayStartHour() >= newTime)
                    return message.withAnswer("Время начала дня не может быть таким же или больше времени конца дня. ("+getDayStartHour()+"-"+newTime+")");
                setDayFinishHour(newTime);

                return message.withAnswer("Теперь я буду присылать тебе вопросы в период с " + getDayStartHour() + " по "+getDayFinishHour()+" часов.\n");
            }
            //</editor-fold>

            //<editor-fold desc="обучение подписка">
            if(text.toLowerCase().trim().equals("обучение подписка")){
                if(getQuestionFrequency() != 0)
                    return message.withAnswer("Бот присылает тебе вопросы "+getQuestionFrequency()+" раз в день с "+getDayStartHour()+" до "+getDayFinishHour()+" часов.");

                setQuestionFrequency(2);

                /*
                Аркадий, поздравляю! Теперь я буду периодически присылать тебе новые вопросы, чтобы ты мог меня обучать.
                По умолчанию я буду присылать тебе вопросы 2 раза в день.
                .
                Напиши мне "Бот, обучение чаще" если хочешь получать от меня получать сообщения с вопросами чаще.
                Напиши мне "Бот, обучение реже" если хочешь получать от меня получать сообщения с вопросами реже.
                Напиши мне "Бот, обучение начинать в 10" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое начинать тебе присылать сообщения с вопросами.
                Напиши мне "Бот, обучение заканчивать в 22" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое заканчивать тебе присылать сообщения с вопросами.
                Напиши мне "Бот, обучение отписка" если ты не хочешь от меня получать сообщений с вопросами.
                * */


                String reply = username + ", поздравляю! Теперь я буду периодически присылать тебе новые вопросы, чтобы ты мог меня обучать.\n" +
                        "По умолчанию я буду присылать тебе вопросы "+getQuestionFrequency()+" раз в день в период с "+getDayStartHour()+" до "+getDayFinishHour()+" часов.\n" +
                        ".\n" +
                        "Напиши мне \""+treatment+" обучение чаще\" если хочешь получать от меня получать сообщения с вопросами чаще.\n" +
                        "Напиши мне \""+treatment+" обучение реже\" если хочешь получать от меня получать сообщения с вопросами реже.\n" +
                        "Напиши мне \""+treatment+" обучение начинать в 10\" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое начинать тебе присылать сообщения с вопросами.\n" +
                        "Напиши мне \""+treatment+" обучение заканчивать в 22\" если хочешь установить время (по моему часовому поясу. у меня сейчас 00:00), в которое заканчивать тебе присылать сообщения с вопросами.\n" +
                        "Напиши мне \""+treatment+" обучение отписка\" если ты не хочешь от меня получать сообщений с вопросами.";
                return message.withAnswer(reply);
            }
            //</editor-fold>

            //<editor-fold desc="обучение отписка">
            if(text.toLowerCase().trim().equals("обучение отписка")){
                if(getQuestionFrequency() == 0)
                    return message.withAnswer("Бот не присылает тебе вопросов, потому что ты не подписан на рассылку.\n" +
                            "Чтобы подписаться на рассылку, напиши мне \""+treatment+" обучение подписка\".");

                setQuestionFrequency(0);

                /*
                Аркадий, больше я не буду присылать тебе вопросов, но ты всегда можешь подписаться на рассылку снова,
                или поучить меня.
                .
                Напиши мне "Бот, обучение начать" чтобы начать сессию обучения и поучить меня.
                Напиши мне "Бот, обучение подписка" если хочешь периодически получать от меня вопросы.
                Напиши мне "Бот, обучение вопрос*ответ" чтобы добавить в базу ответ на вопрос.
                * */


                String reply = username + ", больше я не буду присылать тебе вопросов, но ты всегда можешь подписаться на рассылку снова," +
                        "или поучить меня.\n" +
                        ".\n" +
                        "Напиши мне \""+treatment+" обучение начать\" чтобы начать сессию обучения и поучить меня.\n" +
                        "Напиши мне \""+treatment+" обучение подписка\" если хочешь периодически получать от меня вопросы.\n" +
                        "Напиши мне \""+treatment+" обучение вопрос*ответ\" чтобы добавить в базу ответ на вопрос.\n";
                return message.withAnswer(reply);
            }
            //</editor-fold>

            //<editor-fold desc="обучение отмена">
            if(text.toLowerCase().trim().equals("обучение отмена")){
                if(pendingModeration == null && pendingQuestion == null)
                    return message.withAnswer("Нету вопроса чтобы его отменять.");

                revertQuestion();


                /*
                Аркадий, вопрос отменен.
                .
                Напиши мне "Бот, обучение начать" чтобы начать сессию обучения и поучить меня.
                Напиши мне "Бот, обучение вопрос*ответ" чтобы добавить в базу ответ на вопрос.
                * */


                String reply = username + ", вопрос отменен.\n" +
                        ".\n" +
                        "Напиши мне \""+treatment+" обучение начать\" чтобы начать сессию обучения и поучить меня.\n" +
                        "Напиши мне \""+treatment+" обучение вопрос*ответ\" чтобы добавить в базу ответ на вопрос.\n";
                return message.withAnswer(reply);
            }
            //</editor-fold>

            //<editor-fold desc="обучение пропустить">
            if(text.toLowerCase().trim().equals("обучение пропустить")){
                if(pendingModeration == null && pendingQuestion == null)
                    return message.withAnswer("Нету вопроса чтобы его пропустить.");

                UnknownMessage pendingQuestionOld = pendingQuestion;
                ArrayList<AnswerElement> pendingModerationOld = pendingModeration;
                sendQuestionOrModeration();
                revertQuestion(pendingQuestionOld, pendingModerationOld);

                return message.withAnswer("Вопрос пропущен.");
            }
            //</editor-fold>

            //<editor-fold desc="обучение статус">
            if(text.toLowerCase().trim().equals("обучение статус")){
                String result = "Статус учителя:\n";
                result += "Твой ID: " + (user.getId()) + "\n";
                result += "Счётчик общения с ботом: " + (getLsCounter()) + "\n";
                result += "ID бота для которого ты учитель: " + (getBotId()) + "\n";
                result += "Дата первого сообщения: " + (sdf.format(getFirstMessageDate())) + "\n";
                result += "Дата последнего вопроса: " + (sdf.format(getLastQuestionDate())) + "\n";
                result += "Дата последнего сообщения: " + (sdf.format(getLastMessageDate())) + "\n";
                result += "Доверенный учитель: " + (isAllowedTeacher()?"Да":"Нет") + "\n";
                result += "Плохой учитель: " + (isBadTeacher()?"Да":"Нет") + "\n";
                result += "Открыта сессия обучения: " + (isLearningSessionOpened()?"Да":"Нет") + "\n";
                result += "Было предложение подписаться: " + (isSubscriptionSuggested()?"Да":"Нет") + "\n";
                result += "Добавлено твоих ответов в базу данных: " + (getDatabaseAddCounter()) + "\n";
                result += "Добавлено твоих ответов в песочницу: " + (getSandboxAddCounter()) + "\n";
                result += "Отклонено твоих ответов на модерации: " + (getAnswersRejectedCounter()) + "\n";
                result += "Количество выполненных тобой модераций: " + (getReviewCounter()) + "\n";
                result += "Количество вопросов в день: " + (getQuestionFrequency()) + "\n";
                result += "Время начала дня: " + (getDayStartHour()) + "\n";
                result += "Время конца дня: " + (getDayFinishHour()) + "\n";
                result += "Количество вопросов на которые ты не ответил: " + (getSubscriptionFailsCounter()) + "\n";
                result += "Задано вопросов сегодня: " + (getTodayQuestionsCounter()) + "\n";
                result += "Есть ожидающий вопрос: " + (pendingQuestion != null?"Да":"Нет") + "\n";
                result += "Есть ожидающая модерация: " + (pendingModeration != null?"Да":"Нет") + "\n";
                result += "Комментарий: " + (getComment()) + "\n";
                return message.withAnswer(result);
            }
            //</editor-fold>

            //<editor-fold desc="обучение справка">
            if(text.toLowerCase().trim().equals("обучение справка")){
                String result = "Справка по обучению:\n";
                result += "Ты можешь обучать меня 3 способами:\n";
                result += ".\n";
                result += "1) Ты можешь в любой момент написать мне \""+treatment+" обучение Как дела?*Отлично!\". \n";
                result += "Так ты можешь добавлять мне в базу любой ответ на фразу, чтобы я мог его использовать.\n";
                result += ".\n";
                result += "2) Ты можешь написать мне \""+treatment+" обучение начать\" и поотвечать сразу на вопросы, которые " +
                        "мне задавали другие пользователи, на которые я не знаю ответа.\n";
                result += "Когда ты начал обучение и хочешь его остановить, напиши мне \""+treatment+" обучение стоп\".\n";
                result += ".\n";
                result += "3) Ты можешь написать мне \""+treatment+" обучение подписка\". Тогда я буду периодически " +
                        "присылать тебе новые вопросы, по несколько раз в день, а ты сможешь на них отвечать.\n";
                result += ".\n";
                result += "Ещё раз, вкратце о командах:\n";
                result += "\""+treatment+" обучение начать\" - Начинает сеанс обучения. Когда активен сеанс обучения, " +
                        "после того как ты ответишь на вопрос, буду тебе сразу присылать следующий.\n";
                result += "\""+treatment+" обучение стоп\" - Останавливает сеанс обучения, а висящий вопрос отправляет " +
                        "обратно в базу вопросов.\n";
                result += "\""+treatment+" обучение подписка\" - Подписывает тебя на вопросы. Если ты подписан, я буду тебе периодически " +
                        "(по умолчанию, 2 раза в день) присылать вопросы, на которые я не знаю ответа, в надежде на то, что ты на них ответишь.\n";
                result += "\""+treatment+" обучение отписка\" - Если ты подписан на вопросы, отписывает тебя от них.\n";
                result += "\""+treatment+" обучение чаще\" - Если ты подписан на вопросы, и хочешь, чтобы бот присылал их тебе чаще, эта команда " +
                        "увеличит количество вопросов в день на 1.\n";
                result += "\""+treatment+" обучение реже\" - Если ты подписан на вопросы, и хочешь, чтобы бот присылал их тебе реже, эта команда " +
                        "уменьшит количество вопросов в день на 1.\n";
                result += "\""+treatment+" обучение начинать в 10\" - Если ты подписан на вопросы, я буду тебе их присылать по умолчанию с 10 часов утра " +
                        "до 22 часов вечера. (По моему часовому поясу) Если ты обычно сидишь в ВК в другое время, ты можешь изменить " +
                        "этот промежуток времени. Эта команда задаёт время начала дня.\n";
                result += "\""+treatment+" обучение заканчивать в 22\" - Если ты подписан на вопросы, я буду тебе их присылать по умолчанию с 10 часов утра " +
                        "до 22 часов вечера. (По моему часовому поясу) Если ты обычно сидишь в ВК в другое время, ты можешь изменить " +
                        "этот промежуток времени. Эта команда задаёт время конца дня.\n";
                result += "\""+treatment+" обучение отмена\" - Если я тебе написал невовремя, ты можешь пропустить этот вопрос, и " +
                        "не отвечать на него этой командой.\n";
                result += "\""+treatment+" обучение пропустить\" - Если ты не знаешь ответа на вопрос, напиши мне эту команду, чтобы я прислал тебе " +
                        "новый вопрос.\n";
                result += "\""+treatment+" обучение (вопрос)*(ответ)\" - В любое время любой пользователь может добавить боту свой " +
                        "ответ этой командой.\n";
                result += "\""+treatment+" обучение удалить (ответ)\" - Если ты добавил в базу ответ с ошибкой, ты можешь его оттуда удалить." +
                        " Ты можешь удалять только свои ответы.\n";
                result += "\""+treatment+" обучение статус\" - Выводит ту информацию, которая доступна мне о тебе и об истории твоего обучения.\n";
                result += "\""+treatment+" обучение справка\" - Выводит эту справку.\n";
                return message.withAnswer(result);
            }
            //</editor-fold>

            //<editor-fold desc="обучение удалить ответ">
            if(text.startsWith("обучение удалить ")){
                String answerText = text.replace("обучение удалить ", "").trim().toLowerCase();
                if(!answerText.equals(""))
                    return message.withAnswer("Формат команды \"Обучение удалить (текст ответа)\".");

                if(isAllowedTeacher()){
                    try {
                        AnswerElement answerElement = applicationManager.getBrain().getAnswerDatabase().findByAnswerText(answerText);
                        if(answerElement == null)
                            return message.withAnswer("Ответа \""+answerText+"\" в базе не найдено.");
                        if(applicationManager.getBrain().getAnswerDatabase().removeAnswer(answerElement))
                            return message.withAnswer("Ответ \""+answerElement.toString()+"\" удален из базы.");
                        else
                            return message.withAnswer("! Ответ \""+answerElement.toString()+"\" НЕ удален из базы.");
                    }
                    catch (Exception e){
                        return message.withAnswer("При загрузке базы данных возникла ошибка: " + e.getMessage());
                    }
                }
                else {
                    AnswerElement toRemove = null;
                    for (AnswerElement answerElement:sandbox){
                        if(answerElement.getCreatedAuthor() == message.getAuthor() &&
                                answerElement.getAnswerText().equalsIgnoreCase(answerText)){
                            toRemove = answerElement;
                        }
                    }
                    if(toRemove == null)
                        return message.withAnswer("Среди твоих ответов нет ответа с текстом \""+answerText+"\".");
                    if(sandbox.remove(toRemove))
                        return message.withAnswer("Ответ \""+toRemove.toString()+"\" удален из базы.");
                    else
                        return message.withAnswer("! Ответ \""+toRemove.toString()+"\" НЕ удален из базы.");
                }
            }
            //</editor-fold>

            //<editor-fold desc="обучение вопрос*ответ">
            if(text.startsWith("обучение ")){
                String questionAndAnswer = text.replace("обучение ", "");
                if(!questionAndAnswer.contains("*"))
                    return message;
                String[] questionAnswer = questionAndAnswer.split("\\*");
                String question = questionAnswer[0].trim();
                String answer = questionAnswer[1].trim();
                if(question.length() == 0 || answer.length() == 0)
                    return message.withAnswer("Формат сообщения чтобы добавить в базу ответ: \"" + treatment + " обучение вопрос*ответ\".");
                AnswerElement answerElement = new AnswerElement(message.getAuthor(), question, answer);

                if(isAllowedTeacher()){
                    applicationManager.getBrain().getAnswerDatabase().addAnswer(answerElement);
                    databaseAddCounter ++;
                    return message.withAnswer("Ответ добавлен в базу: " + answerElement.toString());
                }
                else {
                    sandbox.add(answerElement);
                    sandboxAddCounter ++;
                    return message.withAnswer("Ответ добавлен в песочницу: " + answerElement.toString());
                }
            }
            //</editor-fold>
            return message;
        }
        private Message processPendingQuestion(Message message) throws Exception{
            //проверить не висит ли у него вопроса
            if(pendingQuestion != null){
                //ответ на вопрос - текст этого сообщения.
                // Сохранить ответ на вопрос PendingQuestion
                // Если это доверенный пользователь, внести в базу,
                // если недоверенный то в песочницу
                AnswerElement answerElement = new AnswerElement(user, pendingQuestion.getText(), message.getText());
                answerElement.setQuestionAuthor(pendingQuestion.getAuthor());
                answerElement.setAnswerAttachments(message.getAttachments());
                answerElement.setQuestionDate(pendingQuestion.getDate());
                answerElement.setQuestionAttachments(pendingQuestion.getAttachments());
                pendingQuestion = null;

                //сохранить ответ на вопрос. Если пользователь доверенный - сохранять в базу. если нет - в песочницу
                if(isAllowedTeacher()) {
                    databaseAddCounter ++;
                    applicationManager.getBrain().getAnswerDatabase().addAnswer(answerElement);
                }
                else {
                    addSandboxAddCounter();
                    sandbox.add(answerElement);
                }
                clearQuestion();
                //отправить пользлвателю ответ
                String userName = user.getName();
                String treatment = applicationManager.getBrain().getTreatment();
                String text = userName + ", спасибо! Я запомнил, что на " +
                        "\""+answerElement.getQuestionText()+"\" надо отвечать \""+answerElement.getAnswerText()+"\".\n" +
                        ".\n" +
                        "Ты добавил мне уже "+Math.max(databaseAddCounter, sandboxAddCounter)+" ответов, это мне очень помогает!\n" +
                        "Напиши мне \""+treatment+" обучение вопрос*ответ\" если ты хочешь добавить в базу ответ на какой-то вопрос.\n";
                if(isLearningSessionOpened())
                    text += "Открыта сессия обучения.\n" +
                            "Напиши мне \""+treatment+" обучение стоп\" чтобы остановить сессию обучения.\n" +
                            "Сейчас отправлю тебе ещё вопрос...";
                else
                    text += "Сессия обучения не открыта.\n" +
                            "Напиши мне \""+treatment+" обучение начать\" если ты хочешь начать сессию обучения и добавить ещё несколько ответов.\n";
                message.setAnswer(new Answer(text));
                if(isLearningSessionOpened()){
                    sendQuestionOrModeration();
                }
            }
            return message;
        }
        private Message processPendingModeration(Message message) throws Exception{
            //проверить не висит ли у него вопроса
            if(pendingModeration != null){
                //ответ на вопрос - номера, типа 1234567
                //1...8
                String text = message.getText().trim().replace(" ", "").replace(",", "").replace(".", "");
                String treatment = applicationManager.getBrain().getTreatment();
                String userName = user.getName();
                if(!F.isDigitsOnly(text)){
                    message.sendAnswer(new Answer(userName + ", формат ответа неправильный. " +
                            "Нужно в ответе написать номера хороших ответов. " +
                            "Можно через запятую, можно слитно, можно через пробелы. " +
                            "Но должны быть только цифры.\n" +
                            "Например:\n" +
                            treatment+" 1, 3, 4, 5, 7."));
                }
                //определить какие сообщения он одобрил
                ArrayList<AnswerElement> approvedAnswers = new ArrayList<>();
                ArrayList<AnswerElement> rejectedAnswers = new ArrayList<>();
                for (int i = 0; i < pendingModeration.size(); i++) {
                    if(text.contains(String.valueOf(i+1)))
                        approvedAnswers.add(pendingModeration.get(i));
                    else
                        rejectedAnswers.add(pendingModeration.get(i));
                }
                //указать редактора ответа, обновить счётчики, добавить в базу
                for (AnswerElement answerElement:approvedAnswers) {
                    answerElement.setEditedAuthor(message.getAuthor());
                    answerElement.setEditedDate(new Date());
                    UserData author = getByGlobalId(answerElement.getCreatedAuthor().getGlobalId());
                    if(author != null)
                        author.databaseAddCounter++;
                    databaseAddCounter++;
                    applicationManager.getBrain().getAnswerDatabase().addAnswer(answerElement);
                }
                //добавить счётчики юзерам, ответы которых были отклонены
                for(AnswerElement answerElement:rejectedAnswers){
                    UserData user = getByGlobalId(answerElement.getCreatedAuthor().getGlobalId());
                    if(user != null)
                        user.addAnswersRejectedCounter(1);
                }
                clearQuestion();

                //отправить пользлвателю ответ
                /*Аркадий, ответы были одобрены:
                * .
                * Кек -> Лол
                * .
                * Лол -> Kek
                * .
                * Ответы сохранены в базе ответов. Отклонено 4 ответов.
                * "Ты добавил мне уже "+Math.max(databaseAddCounter, sandboxAddCounter)+" ответов, это мне очень помогает!\n" +
                * Напиши мне ""+treatment+" обучение вопрос*ответ" если ты хочешь добавить в базу ответ на какой-то вопрос.
                * "Напиши мне \""+treatment+" обучение стоп\" чтобы остановить сессию обучения.\n"
                * "Напиши мне \""+treatment+" обучение стоп\" чтобы остановить сессию обучения.\n" +
                * "Сейчас отправлю тебе ещё вопрос...";
                * */
                String reply = userName + ", ответы были одобрены:\n" +
                        ".\n";
                for (AnswerElement answerElement:approvedAnswers)
                    reply += answerElement.getQuestionText() + " -> " + answerElement.getAnswerText() + "\n" +
                            ".\n";
                reply += "Ответы сохранены в базе ответов. Отклонено " + rejectedAnswers.size() + " ответов.\n";
                reply += "Ты добавил мне уже "+Math.max(databaseAddCounter, sandboxAddCounter)+" ответов, это мне очень помогает!\n";
                reply += "Напиши мне \""+treatment+" обучение вопрос*ответ\" если ты хочешь добавить в базу ответ на какой-то вопрос.";


                if(isLearningSessionOpened())
                    //сессия ОТКРЫТА
                    reply += "Открыта сессия обучения.\n" +
                            "Напиши мне \""+treatment+" обучение стоп\" чтобы остановить сессию обучения.\n" +
                            "Сейчас отправлю тебе ещё вопрос...";
                else
                    //сессия ЗАКРЫТА
                    reply += "Сессия обучения не открыта.\n" +
                            "Напиши мне \""+treatment+" обучение начать\" если ты хочешь начать сессию добавить ещё несколько ответов.\n";
                message.setAnswer(new Answer(reply));
                if(isLearningSessionOpened()){
                    sendQuestionOrModeration();
                }
            }
            return message;
        }

        private boolean isDay(){
            //функция проверяет день ли сейчас для пользователя и можно ли ему сейчас присылать вопросы
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(new Date());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minHour = Math.min(dayStartHour, dayFinishHour);
            int maxHour = Math.max(dayStartHour, dayFinishHour);
            return hour > minHour && hour < maxHour;
        }
        private int howMuchHoursSinceLastQuestion(){
            long last = lastQuestionDate.getTime();
            long now = new Date().getTime();
            long diffMS = now - last;
            long diffSEC = diffMS / 1000L;
            long diffMIN = diffSEC / 60L;
            long diffHR = diffMIN / 60L;
            return (int)diffHR;
        }
        private int howMuchHoursSinceLastMessage(){
            long last = lastMessageDate.getTime();
            long now = new Date().getTime();
            long diffMS = now - last;
            long diffSEC = diffMS / 1000L;
            long diffMIN = diffSEC / 60L;
            long diffHR = diffMIN / 60L;
            return (int)diffHR;
        }
        private int howMuchDaysSinceLastMessage(){
            long last = lastMessageDate.getTime();
            long now = new Date().getTime();
            long diffMS = now - last;
            long diffSEC = diffMS / 1000L;
            long diffMIN = diffSEC / 60L;
            long diffHR = diffMIN / 60L;
            long diffDAY = diffHR / 24L;
            return (int)diffDAY;
        }
        private int howMuchDaysSinceFirstMessage(){
            long last = firstMessageDate.getTime();
            long now = new Date().getTime();
            long diffMS = now - last;
            long diffSEC = diffMS / 1000L;
            long diffMIN = diffSEC / 60L;
            long diffHR = diffMIN / 60L;
            long diffDAY = diffHR / 24L;
            return (int)diffDAY;
        }
        private int howMuchHoursDelayBetweenMessages(){
            //Функция анализирует промежуток времени и частоту сообщений и описывает каким должен
            // быть промежуток между сообщениями, чтобы они были равномерно размазаны в течении дня
            //не меньше 1 часа
            //не больше 24 часов
            //Отправка начинается в момент когда юзер выставил начало дня
            //заканчивается когда количество сообщений или время исчерпано
            //10...22, 2:
            //10:00    16:00
            //delay = (22-10)/2 = 12/2 = 6;

            int max = Math.max(getDayStartHour(), getDayFinishHour());
            int min = Math.min(getDayStartHour(), getDayFinishHour());
            int difference = max - min;
            if(difference == 0)
                return 1;
            if(questionFrequency == 0)
                return 24;

            return difference / getQuestionFrequency();
        }
        private void sendMessageToThisUser(final Answer answer){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    VkAccount account = applicationManager.getCommunicator().getVkAccount(botId);
                    if(account == null)
                        applicationManager.getCommunicator().getActiveVkAccount();
                    if(account != null)
                        account.sendMessage(user.getId(), null, answer);
                    else {
                        revertQuestion();
                        log("! Ошибка отправки сообщения пользователю " + user.getName() + "\n" + answer.toString() + "\nОжидающие вопросы анулированы.");
                    }
                }
            }).start();
        }
        private boolean processQuestionsTimeout(){
            //если пользователю был задан вопрос и на него не был получен ответ, послать пользователя нахуй и добавить счётчик неудачных вопросов
            if(pendingModeration != null || pendingQuestion != null || howMuchHoursSinceLastQuestion() > 5){
                subscriptionFailsCounter ++;
                revertQuestion();
                setLearningSessionOpened(false);
                if(subscriptionFailsCounter > 5){
                    sendMessageToThisUser(new Answer("Я очень надеялся на твою помощь... " +
                            "Жаль, что ты мне не помог. " +
                            "Я вижу, что ты мне уже давно не отвечаешь, поэтому, я больше не буду тебе писать.\n" +
                            "Если ты хочешь, чтобы я снова тебе писал, напиши мне \""+applicationManager.getBrain().getTreatment()+" обучение подписка\"."));
                    setQuestionFrequency(0);
                }
                else {
                    sendMessageToThisUser(new Answer("Я очень надеялся на твою помощь... " +
                            "Жаль, что ты мне не помог. " +
                            "Я напишу тебе позже." +
                            "Если будет желание поучить меня, напиши мне \""+applicationManager.getBrain().getTreatment()+" обучение\" в любое удобное для тебя время."));
                }
                return true;
            }
            return false;
        }
        private void revertQuestion(){
            revertQuestion(pendingQuestion, pendingModeration);
            pendingQuestion = null;
            pendingModeration = null;
        }
        private void revertQuestion(UnknownMessage pendingQuestion, ArrayList<AnswerElement> pendingModeration){
            //вернуть заданный пользователю вопрос обратно в базу
            if(pendingQuestion != null){
                log(". Возврат вопроса для пользователя " + user.getName() + " обратно в базу неизвестных...");
                applicationManager.getBrain().getUnknownMessages().add(pendingQuestion);
            }
            if(pendingModeration != null){
                log(". Возврат модераций для пользователя " + user.getName() + " обратно в песочницу...");
                sandbox.addAll(pendingModeration);
            }
        }
        private void clearQuestion(){
            pendingQuestion = null;
            pendingModeration = null;
        }
        private void updateTodayQuestionCounter(){
            //когда наступил следующий день, счётчик сообщений надо сбросить в ноль
            //если прошлый вопрос был задан вчера и счётчик сообщений в день не толь - сбросить в ноль
            if(!F.isToday(lastQuestionDate) && todayQuestionsCounter != 0)
                todayQuestionsCounter = 0;
        }
        private void broadcastQuestions(){
            //проверить не висит ли на пользователе вопроса или модерации.
            // Если висят, то ничего ему не слать. Другие функции сами его накажут.
            if(pendingModeration != null || pendingQuestion != null)
                return;

            // проверить есть ли у пользователя подписка
            if(questionFrequency > 0){
                //у пользователя есть подписка, проверить надо ли ему ещё сегодня писать
                if(todayQuestionsCounter < questionFrequency){
                    //пользователю ещё надо сегодня отправить сообщения
                    if(isDay()){
                        //сейчас день, можно отправлять
                        if(howMuchHoursSinceLastQuestion() > howMuchHoursDelayBetweenMessages()){
                            //Мы отправляли сообщение достаточно давно, самое время отправлять

                            nowLastQuestionDate();
                            todayQuestionsCounter ++;
                            sendQuestionOrModeration();
                        }
                    }
                }
            }
            else {
                //если нету подписки - предложить. Но только один раз.
                //проверить:

                if(!subscriptionSuggested) {
                    //Мы ему подписку раньше не предлагали
                    if (howMuchDaysSinceFirstMessage() > 14) {
                        //Общаемся больше чем 2 недели
                        if (howMuchDaysSinceLastMessage() < 2) {
                            //последний раз общались недавно
                            if (getLsCounter() > 100) {
                                //Общаемся много, от него получено больше 100 сообщений
                                String treatment = getApplicationManager().getBrain().getTreatment();
                                String text = "Привет! Мы с тобой давно общаемся, поэтому я хотел бы предложить тебе принять участие в моём обучении!\n" +
                                        "Мне пишут много разных вопросов, на многие из которых я не знаю ответа. Я собираю такие вопросы для того, чтобы учителя помогли мне подобрать на них ответы.\n" +
                                        "Если ты хочешь добавлять в мою базу ответы:\n" +
                                        ".\n" +
                                        "Напиши мне \""+treatment+" обучение начать\" если ты хочешь ответить на несколько вопросов.\n" +
                                        "Напиши мне \""+treatment+" обучение вопрос*ответ\" если ты хочешь добавить в базу свой ответ на какой-то вопрос.\n" +
                                        "Напиши мне \""+treatment+" обучение подписка\" если ты хочешь чтобы я сам периодически присылал тебе новые вопросы.\n" +
                                        ".\n" +
                                        "Эти команды ты можешь писать мне в любое время.";
                                sendMessageToThisUser(new Answer(text));
                                subscriptionSuggested = true;
                            }
                        }
                    }
                }
            }
        }
        private void sendQuestion(){
            //когда принято решение, отправляет вопрос. Тут нет проверок, тупо отправляет вопрос
            //если не сталось вопросов, ничего не отправлять
            UnknownMessage unknownMessage = applicationManager.getBrain().getUnknownMessages().popTop();

            String userName = user.getName();
            String treatment = applicationManager.getBrain().getTreatment();
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            String text = userName + ", привет! Мне нужна твоя помощь. Я не знаю как ответить на эту фразу:\n" +
                ".\n" +
                unknownMessage.getText() + " " + unknownMessage.getAttachments() + "\n" +
                ".\n" +
                "Напиши мне, как правильно ответить на это сообщение?\n" +
                    "Твоё следующее сообщение будет сохранено как ответ на эту фразу.\n" +
                    (isLearningSessionOpened()?"Сессия обучения активна.\n":"") +
                "Напиши мне \""+treatment+" обучение отмена\" если ты не можешь сейчас отвечать на сообщения. " +
                    "Тогда я напишу тебе позже.\n" +
                "Напиши мне \""+treatment+" обучение пропустить\" если ты не хочешь отвечать на это сообщение. " +
                    "Тогда я напишу тебе другое.\n" +
                "Напиши мне \""+treatment+" обучение чаще\" если хочешь получать от меня получать сообщения с вопросами чаще.\n" +
                "Напиши мне \""+treatment+" обучение реже\" если хочешь получать от меня получать сообщения с вопросами реже.\n" +
                "Напиши мне \""+treatment+" обучение начинать в 10\" если хочешь установить время " +
                    "(по моему часовому поясу. У меня сейчас "+time+"), в которое начинать тебе присылать сообщения с вопросами.\n" +
                "Напиши мне \""+treatment+" обучение заканчивать в 22\" если хочешь установить время " +
                    "(по моему часовому поясу. у меня сейчас "+time+"), в которое заканчивать тебе присылать сообщения с вопросами.\n" +
                "Напиши мне \""+treatment+" обучение отписка\" если ты не хочешь от меня получать сообщений с вопросами.";
            setPendingQuestion(unknownMessage);
            sendMessageToThisUser(new Answer(text));

        }
        private void sendModeration(){
            //когда принято решение, отправляет модерацию. Тут нет проверок, тупо отправляет вопрос
            //если не сталось вопросов, ничего не отправлять
            ArrayList<AnswerElement> moderation = popModeration();

            String userName = user.getName();
            String treatment = applicationManager.getBrain().getTreatment();
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            ArrayList<Attachment> attachments = new ArrayList<>();

            String text = userName + ", привет! У меня накопилось немного ответов, качество которых я не могу оценить.\n" +
                    "Мне нужна твоя помощь.\n" +
                    "Вот ответы:\n" +
                    ".\n";
            for (int i = 0; i < moderation.size(); i++) {
                AnswerElement answerElement = moderation.get(i);
                String authorFullName = answerElement.getCreatedAuthor().getName();
                //1...8
                text += (i+1) + ") " + answerElement.getQuestionText() + " -> " + answerElement.getAnswerText() + "\n";
                if(answerElement.getAnswerAttachments().size() != 0){
                    text += "Ответ содержит вложения: ";
                    for (int j = 0; j < answerElement.getAnswerAttachments().size(); j++) {
                        Attachment attachment = answerElement.getAnswerAttachments().get(j);
                        if(attachments.size() < 10) {
                            attachments.add(attachment);
                            text += attachment.type + "[" + attachments.size() + "] ";
                        }
                        else
                            text += attachment.type + "[не поместилось] ";
                    }
                    text += "\n";
                }
                text += "Автор ответа: " + authorFullName + " (vk.com/id"+answerElement.getCreatedAuthor()+")\n";
                text += ".\n";
            }

            text +="Напиши мне в ответе номера хороших ответов.\n" +
                    "Например:  \""+treatment+" 3845.\n" +
                    "Твоё следующее сообщение будет воспринято мной как ответ на это сообщение.\n" +
                    (isLearningSessionOpened()?"Сессия обучения активна.\n":"") +
                    "Напиши мне \""+treatment+" обучение отмена\" если ты не можешь сейчас отвечать на сообщения. " +
                    "Тогда я напишу тебе позже.\n" +
                    "Напиши мне \""+treatment+" обучение чаще\" если хочешь получать от меня получать сообщения с вопросами чаще.\n" +
                    "Напиши мне \""+treatment+" обучение реже\" если хочешь получать от меня получать сообщения с вопросами реже.\n" +
                    "Напиши мне \""+treatment+" обучение начинать в 10\" если хочешь установить время " +
                    "(по моему часовому поясу. У меня сейчас "+time+"), в которое начинать тебе присылать сообщения с вопросами.\n" +
                    "Напиши мне \""+treatment+" обучение заканчивать в 22\" если хочешь установить время " +
                    "(по моему часовому поясу. у меня сейчас "+time+"), в которое заканчивать тебе присылать сообщения с вопросами.\n" +
                    "Напиши мне \""+treatment+" обучение отписка\" если ты не хочешь от меня получать сообщений с вопросами.";
            setPendingModeration(moderation);
            sendMessageToThisUser(new Answer(text, attachments));
        }
        private void sendQuestionOrModeration(){
            if(isAllowedTeacher() && hasModeration())
                sendModeration();
            else
                sendQuestion();
        }



        public User getUser() {
            return user;
        }
        public void setUser(User user) {
            this.user = user;
        }
        public long getLsCounter() {
            return lsCounter;
        }
        public void setLsCounter(long lsCounter) {
            this.lsCounter = lsCounter;
        }
        public long getBotId() {
            return botId;
        }
        public void setBotId(long botId) {
            this.botId = botId;
        }
        public Date getFirstMessageDate() {
            return firstMessageDate;
        }
        public Date getLastMessageDate() {
            return lastMessageDate;
        }
        public void setLastMessageDate(Date lastMessageDate) {
            this.lastMessageDate = lastMessageDate;
        }
        public void nowLastMessageDate() {
            setLastMessageDate(new Date());
        }
        public Date getLastQuestionDate() {
            return lastQuestionDate;
        }
        public void setLastQuestionDate(Date lastQuestionDate) {
            this.lastQuestionDate = lastQuestionDate;
        }
        public void nowLastQuestionDate() {
            this.lastQuestionDate = new Date();
        }
        public boolean isAllowedTeacher() {
            return allowedTeacher;
        }
        public void setAllowedTeacher(boolean allowedTeacher) {
            this.allowedTeacher = allowedTeacher;
        }
        public boolean isBadTeacher() {
            return badTeacher;
        }
        public void setBadTeacher(boolean badTeacher) {
            this.badTeacher = badTeacher;
        }
        public boolean isLearningSessionOpened() {
            return learningSessionOpened;
        }
        public void setLearningSessionOpened(boolean learningSessionOpened) {
            this.learningSessionOpened = learningSessionOpened;
        }
        public boolean isSubscriptionSuggested() {
            return subscriptionSuggested;
        }
        public void setSubscriptionSuggested(boolean subscriptionSuggested) {
            this.subscriptionSuggested = subscriptionSuggested;
        }
        public int getDatabaseAddCounter() {
            return databaseAddCounter;
        }
        public void setDatabaseAddCounter(int databaseAddCounter) {
            this.databaseAddCounter = databaseAddCounter;
        }
        public int getAnswersRejectedCounter() {
            return answersRejectedCounter;
        }
        public void setAnswersRejectedCounter(int answersRejectedCounter) {
            this.answersRejectedCounter = answersRejectedCounter;
        }
        public void addAnswersRejectedCounter(int cnt){
            setAnswersRejectedCounter(getAnswersRejectedCounter()+cnt);
        }
        public int getReviewCounter() {
            return reviewCounter;
        }
        public void setReviewCounter(int reviewCounter) {
            this.reviewCounter = reviewCounter;
        }
        public int getSandboxAddCounter() {
            return sandboxAddCounter;
        }
        public void setSandboxAddCounter(int sandboxAddCounter) {
            this.sandboxAddCounter = sandboxAddCounter;
        }
        public void addSandboxAddCounter() {
            setSandboxAddCounter(getSandboxAddCounter()+1);
        }
        public int getQuestionFrequency() {
            return questionFrequency;
        }
        public void setQuestionFrequency(int questionFrequency) {
            this.questionFrequency = questionFrequency;
        }
        public int getDayStartHour() {
            return dayStartHour;
        }
        public void setDayStartHour(int dayStartHour) {
            this.dayStartHour = dayStartHour;
        }
        public int getDayFinishHour() {
            return dayFinishHour;
        }
        public void setDayFinishHour(int dayFinishHour) {
            this.dayFinishHour = dayFinishHour;
        }
        public int getTodayQuestionsCounter() {
            return todayQuestionsCounter;
        }
        public void setTodayQuestionsCounter(int todayQuestionsCounter) {
            this.todayQuestionsCounter = todayQuestionsCounter;
        }
        public int getSubscriptionFailsCounter() {
            return subscriptionFailsCounter;
        }
        public void setSubscriptionFailsCounter(int subscriptionFailsCounter) {
            this.subscriptionFailsCounter = subscriptionFailsCounter;
        }
        public UnknownMessage getPendingQuestion() {
            return pendingQuestion;
        }
        public void setPendingQuestion(UnknownMessage pendingQuestion) {
            this.pendingQuestion = pendingQuestion;
        }
        public ArrayList<AnswerElement> getPendingModeration() {
            return pendingModeration;
        }
        public void setPendingModeration(ArrayList<AnswerElement> pendingModeration) {
            this.pendingModeration = pendingModeration;
        }
        public String getComment() {
            return comment;
        }
        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    //// TODO: 15.11.2017 команды!
}
