package com.fsoft.vktest.Communication.Account.VK;

import com.fsoft.vktest.Communication.Account.Account;
import com.fsoft.vktest.AnswerInfrastructure.Answer;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.F;
import com.fsoft.vktest.Utils.Parameters;
import com.fsoft.vktest.Utils.TimeCounter;
import com.perm.kate.api.Api;
import com.perm.kate.api.Attachment;
import com.perm.kate.api.Audio;
import com.perm.kate.api.Comment;
import com.perm.kate.api.Document;
import com.perm.kate.api.Group;
import com.perm.kate.api.KException;
import com.perm.kate.api.Photo;
import com.perm.kate.api.User;
import com.perm.kate.api.Video;
import com.perm.kate.api.WallMessage;

import com.fsoft.vktest.Modules.Commands.CommandDesc;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Конкретно этот класс отвечает за зеркалирование функций, распределение нагрузки, обработку ошибок.
 *
 *
 * ВСЕ МЕТОДЫ ЭТОГО КЛАССА КРОМЕ МЕТОДОВ CommandModule АСИНХРОННЫ!!!
 * ВЫЗЫВАТЬ ТОЛЬКО ИЗ ВТОРОГО\ТРЕТЬЕГО ПОТОКА!
 * Этот класс должен отвечать только за редирект методов, за распределение нагрузки и
 * решение возникающих проблем. Также за процедуру логина.
 * Он же в идеале должен при вызове ЕГО методов извне обрабатывать ошибки, может, управлять очередью.
 * Если возникают эксепшоны, хэндлить их здесь.
 * Ну, выводить там окно повторного логина, например.
 *
 * Статус аккаунта описывать так:
 * + Аккаунт включен
 * + Токен OK
 * + Аккаунт запущен
 *
 * Общая схема такая:
 * - получаем пустой аккаунт
 * - выводим диалог логина
 * - работаем с ним пока не получим токен
 * - получаем токен
 * - выполняем тестовый запрос (любой)
 * - если тестовый запрос проходит, то token_ok = true
 * - если enabled = true то начинаем startAccount()
 * ...работаем
 * - если возникает ошибка то: token_ok=false; stopAccount();
 * - если ошибка не критическая, то: через 5 минут делаем startAccount() & token_ok=true
 *
 * Created by Dr. Failov on 11.11.20
 * 14.
 */
public class VkAccountCore extends Account {
    static public final String API_ID = "4485671";
    static public final int RETRIES = 3;//количество повторных попыток в случае ошибки

    //это имя пользователя которому принадлежит этот аккаунт. Оно хранится здесь временно.
    // Когда оно нам нужно, обращаемся к геттеру. если нужно получить имя аккаунта, обращаемся к toString()
    private String userName = null;
    private String screenName = null;
    //АПИ для доступа к серверам. Получать только через функцию, чтобы предотвратить перегрузку
    private Api api;
    private long apiCounter = 0; //счётчик доступа к АПИ
    private long errorCounter = 0; //счётчик ошибок при доступе к АПИ
    private OverloadSaver overloadSaver = new OverloadSaver();

    //эти переменные будут использоваться при обращении на сервер. Если пользователь ввёл капчу,
    // эти поля заполняются. Когда метод их использовал, они становятся снова null.
    private String captcha_key = null;
    private String captcha_sid = null;

    //Эти переменные будут использованы для валидации. А именно, когда она нужна - пользователь сможет пройти её
    private String validation_url = null;


    public VkAccountCore(ApplicationManager applicationManager, String fileName) {
        super(applicationManager, fileName);
    }
    @Override public void login() {
        super.login();
        new LoginWindow(this);
    }
    @Override protected void startAccount() {
        super.startAccount();
        if(isToken_ok())
            getUserName();
    }
    @Override public void stopAccount() {
        super.stopAccount();
    }
    @Override public String toString() {
        if(userName == null)
            return userName;
        return super.toString();
    }
    @Override public boolean isMine(String commandTreatment) {
        return super.isMine(commandTreatment) || commandTreatment.trim().equals(screenName.trim());
    }
    public boolean isGroupToken(){
        return getId()<0;
    }
    public boolean isReady(){
        return overloadSaver.isReady();
    }
    public boolean needValidation(){
        if(isRunning())
            return false;
        return validation_url != null;
    }
    public String getValidation_url() {
        return validation_url;
    }
    public void resetValidation_url() {
        this.validation_url = null;
    }

    //access to server API functions
        //info
    public User getUserAccount(long id){
        int retries = RETRIES;
        while(retries > 0) {
            retries--;
            try {
                ArrayList<Long> uid = new ArrayList<>();
                uid.add(id);
                ArrayList<User> users = api().getProfiles(
                        uid, null, "bdate,first_name,last_name," +
                                "about,interests,home_town,screen_name," +
                                "is_friend,books,photo_id", "Nom",
                        captcha_key, captcha_sid);
                captcha_key = null;
                captcha_sid = null;
                if (users.size() > 0) {
                    User me = users.get(0);
                    return me;
                }
                return null;
            } catch (Throwable e) {
                log("! Ошибка загрузки страницы пользователя: " + e.toString());
                if(!reportError(e))
                    return null;
            }
        }
        return null;
    }
    public String getUserName(long id){
        if(id < 0)
            return getGroupName(id);
        User user = getUserAccount(id);
        if(user != null){
            return user.first_name;
        }
        return String.valueOf(id);
    }
    public String getGroupName(long id){
        if(id >= 0)
            id = -id;
        int retries = RETRIES;
        while(retries > 0) {
            retries--;
            try {
                ArrayList<Long> gid = new ArrayList<>();
                gid.add(Math.abs(id));
                ArrayList<Group> users = api().getGroups(gid, null, null);
                if(users.size() > 0){
                    Group me = users.get(0);
                    String result = me.name;
                    log(". Имя сообщества загружено: " + result);
                    return( result);
                }
                return String.valueOf(id);
            } catch (Throwable e) {
                log("! Ошибка загрузки имени сообщества "+id+" аккаунтом "+this+": " + e.toString());
                if(!reportError(e))
                    return String.valueOf(id);
            }
        }
        return String.valueOf(id);
    }
    public String getUserFullName(long id){
        if(id < 0)
            return getGroupName(id);
        User user = getUserAccount(id);
        if(user != null){
            return user.first_name + " " + user.last_name;
        }
        return String.valueOf(id);
    }
    public long resolveScreenName(String screenName){
        if(screenName != null){
            screenName = screenName.replace("https://", "");
            screenName = screenName.replace("http://", "");
            screenName = screenName.replace("vk.com/", "");
            screenName = screenName.replace("m.vk.com/", "");
            screenName = screenName.replace("vk.me/", "");
            screenName = screenName.replace("m.vk.me/", "");
        }

        try{
            //[id00000|name] , [id179646426|Иван]
            String id = "";
            Pattern p = Pattern.compile("\\[id([0-9]+)\\|[^\\]]+\\]");
            // create matcher for pattern p and given string
            Matcher m = p.matcher(screenName.trim());

            // if an occurrence if a pattern was found in a given string...
            if (m.find()) {
                // ...then you can use group() methods.
                return Long.parseLong(m.group(1));
            }
        }
        catch (Exception e){}
        try{
            return Long.parseLong(screenName.trim());
        }
        catch (NumberFormatException e){}

        try{
            return Long.parseLong(screenName.replace("id", "").trim());
        }
        catch (NumberFormatException e){}

        try{
            //// 21.03.2017 https://vk.com/wall10299185?own=1
            return Long.parseLong(screenName.replace("wall", "").replace("?own=1", "").trim());
        }
        catch (NumberFormatException e){}

        try{
            return Long.parseLong(screenName.replace("club", "-").trim());
        }
        catch (NumberFormatException e){}

        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                return api().resolveScreenName(screenName);
            } catch (Throwable e) {
                log("! Ошибка resolveScreenName для "+screenName+".\n" + e.toString());
                if(!reportError(e))
                    return 0;
            }
        }
        return 0;
    }
    public Audio getAudio(long userID, long audioID){
        int retries = RETRIES;
        while(retries > 0) {
            retries--;
            try {
                ArrayList<Long> audioIDs = new ArrayList<>();
                audioIDs.add(audioID);
                ArrayList<Audio> audios = api().getAudio(userID, null, null, audioIDs, captcha_key, captcha_sid);
                captcha_key = null;
                captcha_sid = null;
                if (audios.size() > 0) {
                    Audio result = audios.get(0);
                    return result;
                }
                return null;
            } catch (Throwable e) {
                log("! Ошибка загрузки аудиозаписи: " + e.toString());
                if(!reportError(e))
                    return null;
            }
        }
        return null;
    }
    public boolean checkAudio(long userID, long audioID){
        return getAudio(userID, audioID) != null;
    }
    public boolean checkAudio(Audio audio){
        if(audio == null)
            return false;
        return getAudio(audio.owner_id, audio.aid) != null;
    }
    public Video getVideo(long userID, long videoID, String accessKey){
        int retries = RETRIES;
        while(retries > 0) {
            retries--;
            try {
                ArrayList<Video> items = api().getVideo(String.valueOf(videoID), userID, null, null, null, null, accessKey);
                if (items.size() > 0) {
                    Video result = items.get(0);
                    return result;
                }
                return null;
            } catch (Throwable e) {
                log("! Ошибка загрузки видеозаписи: " + e.toString());
                if(!reportError(e))
                    return null;
            }
        }
        return null;
    }
    public boolean checkVideo(long userID, long videoID, String accessKey){
        return getVideo(userID, videoID, accessKey) != null;
    }
    public boolean checkVideo(Video video){
        return checkVideo(video.owner_id, video.vid, video.access_key);
    }
    public Photo getPhoto(long userID, long videoID, String accessKey){
        int retries = RETRIES;
        while(retries > 0) {
            retries--;
            try {
                //uid_pid_accesskey
                //-23454_6788898_jhk2jh3k4
                String photos = userID + "_" + videoID + (accessKey == null?"":("_"+accessKey));
                ArrayList<Photo> items = api().getPhotosById(photos, null, null);
                if (items != null && items.size() > 0) {
                    return items.get(0);
                }
                return null;
            } catch (Throwable e) {
                log("! Ошибка загрузки фотографии: " + e.toString());
                if(!reportError(e))
                    return null;
            }
        }
        return null;
    }
    public boolean checkPhoto(long userID, long photoID, String accessKey){
        return getPhoto(userID, photoID, accessKey) != null;
    }
    public boolean checkPhoto(Photo photo){
        return checkPhoto(photo.owner_id, photo.pid, photo.access_key);
    }
    public Document getDocument(long userID, long docID, String accessKey){
        int retries = RETRIES;
        while(retries > 0) {
            retries--;
            try {
                //uid_pid_accesskey
                //-23454_6788898_jhk2jh3k4
                String documents = userID + "_" + docID + (accessKey == null?"":("_"+accessKey));
                ArrayList<Document> items = api().getDocsById(documents);
                if (items.size() > 0) {
                    return items.get(0);
                }
                return null;
            } catch (Throwable e) {
                log("! Ошибка загрузки документа: " + e.toString());
                if(!reportError(e))
                    return null;
            }
        }
        return null;
    }
    public boolean checkDocument(long userID, long docID, String accessKey){
        return getDocument(userID, docID, accessKey) != null;
    }
    public boolean checkDocument(Document document){
        return checkDocument(document.owner_id, document.id, document.access_key);
    }
    public boolean checkAttachment(Attachment attachment){
        if(attachment.type.equals("audio") && attachment.audio != null)
            return checkAudio(attachment.audio);
        if(attachment.type.equals("video") && attachment.video != null)
            return checkVideo(attachment.video);
        if(attachment.type.equals("doc") && attachment.document != null)
            return checkDocument(attachment.document);
        if(attachment.type.equals("photo") && attachment.photo != null)
            return checkPhoto(attachment.photo);
        return false;
    }
    public boolean checkAttachments(ArrayList<Attachment> attachments){
        for (Attachment attachment:attachments)
            if(!checkAttachment(attachment))
                return false;
        return true;
    }
        //page
    public void setOnline(){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                api().setOnline(captcha_key, captcha_sid);
                captcha_sid = null;
                captcha_key = null;
                return;
            } catch (Throwable e) {
                log("! Ошибка отправки статуса \"Онлайн\" для пользователя " + this + "\n" + e.toString());
                if(!reportError(e))
                    return;
            }
        }
    }
    public void setStatus(String newStatus){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                api().setStatus(newStatus);
                overloadSaver.markSend();
                return;
            } catch (Throwable e) {
                log("! Ошибка обновления статуса для пользователя " + this + "\n" + e.toString());
                if(!reportError(e))
                    return;
            }
        }
    }
    public String getStatus(long id){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                return api().getStatus(id).text;
            } catch (Throwable e) {
                log("! Ошибка получения статуса для пользователя " + this + "\n" + e.toString());
                if(!reportError(e))
                    return "";
            }
        }
        return "";
    }
    public boolean rePost(long wallId, long postId, String message){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            String obj = "wall"+wallId+"_"+postId;
            try {
                api().repostWallPost(obj, message, null, captcha_key, captcha_sid);
                captcha_sid = null;
                captcha_key = null;
                return true;
            } catch (Throwable e) {
                log("! Ошибка репоста записи "+obj+" на стену пользователя " + this + ".\n" + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }
        //groups
    public ArrayList<Long> getGroupAdmins(){
        //возвращает список админов ЭТОЙ группы
        if(getId() >= 0){
            log("! Получить список админов можно только для аккаунта сообщества.");
            ArrayList<Long> result = new ArrayList<>();
            result.add(getId());
            return result;
        }

        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                ArrayList<User> users = api().getGroupsManagers(Math.abs(getId()), 200, 0, null);
                ArrayList<Long> results = new ArrayList<>();
                for(User user:users)
                    if(user.about.equals("administrator") || user.about.equals("creator"))
                        results.add(user.uid);
                return results;
            } catch (Throwable e) {
                log("! Ошибка getGroupAdmins для "+this+".\n" + e.toString());
                if(!reportError(e))
                    return null;
            }
        }
        return null;
    }
    public boolean banGroupUserForMonth(long groupId, long userId){
        //https://vk.com/dev/groups.banUser

        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                long endDate = System.currentTimeMillis() + 1000L*60L*60L*24L*30L;//1 month
                String message = applicationManager.getParameters().get(
                        "banMessage",
                        "Блокировка ботом по команде администратора.",
                        "Сообщение блокировки",
                        "Сообщение, с которым пользователь получит блокировку по команде bcd ban");
                return api().addGroupBanUser(groupId, userId, endDate, 0, message, true);
            } catch (Throwable e) {
                log("! Ошибка banGroupUserForMonth для "+this+".\n" +
                        "Пользователь: "+userId+", группа: "+groupId+".\n" +
                        "Ошибка: " + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }
        //messages
    public ArrayList<com.perm.kate.api.Message> getMessages50(){
        return getMessages(50);
    }
    public ArrayList<com.perm.kate.api.Message> getMessages(int count){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                ArrayList<com.perm.kate.api.Message> messages = api().getMessages(0L, false, count);
                log(". Загружено " + messages.size() + " сообщений " + this + ".");
                return messages;
            } catch (Throwable e) {
                log("! Ошибка загрузки списка диалогов аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
    public ArrayList<com.perm.kate.api.Message> getDialogs100(){
        return getDialogs(0, 100);
    }
    public ArrayList<com.perm.kate.api.Message> getDialogs(long offset, int count){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                ArrayList<com.perm.kate.api.Message> dialogs = api().getMessagesDialogs(offset, count, captcha_key, captcha_sid);
                captcha_key = null;
                captcha_sid = null;
                log(". Загружено " + dialogs.size() + " диалогов " + this + ".");
                return dialogs;
            } catch (Throwable e) {
                log("! Ошибка загрузки списка диалогов аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
    public void markAsRead(long messageId){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                ArrayList<Long> mids = new ArrayList<>();
                mids.add(messageId);
                api().markAsNewOrAsRead(mids, true);
                return;
            } catch (Throwable e) {
                log("! Ошибка отметки сообщения "+messageId+" как прочитанное " + this + "\n" + e.toString());
                if(!reportError(e))
                    return;
            }
        }
    }
    public void markTyping(Long user_id, Long chat_id){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                api().setMessageActivity(chat_id==null?user_id:null, chat_id, true);
                return;
            } catch (Throwable e) {
                log("! Ошибка отправки уведомления \"" + this + " Набирает сообщение\"\n" + e.toString());
                if(!reportError(e))
                    return;
            }
        }
    }
    public boolean sendMessage(Long userId, Long chatId, Answer answer){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                if(userId != null && (userId == 100 || userId == 101 || userId == 333)) {
                    log("! Нельзя отправить сообщение администрации.");
                    return false;
                }
                String[] parts = F.splitText(answer.text, 4000);

                for (int i = 0; i < parts.length; i++) {
                    api().sendMessage(userId, chatId, (i!=0 ? "(часть " + i + ") " : "") + parts[i],
                            null, null, answer.getAttachmentStrings(), answer.forwarded, null, null, captcha_key, captcha_sid);
                    captcha_key = null;
                    captcha_sid = null;
                    overloadSaver.markSend();
                }
                return true;
            } catch (Throwable e) {
                if(e.toString().contains("Flood control: same message already sent")) {
                    answer.text = "(2) " + answer.text;
                    return sendMessage(userId, chatId, answer);
                }
                log("! Ошибка отправки сообщения на аккаунте " + this + "\n" + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }
    public void exitFromChat(long chat_id){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                api().removeUserFromChat(chat_id, getId());
                return;
            } catch (Throwable e) {
                log("! Ошибка выхода " + this + " из чата.\n" + e.toString());
                if(!reportError(e))
                    return;
            }
        }
    }
    public void exitFromChatAsync(final long chat_id){
        new Timer("Leave from chat tor " + this).schedule(new TimerTask() {
            @Override
            public void run() {
                exitFromChat(chat_id);
            }
        }, 5000);
    }
    public boolean addUserToChat(long chatId, long userID){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                Integer res = api().addUserToChat(chatId, userID);
                log(". Пришлашение в чат: " + res + "\n");
                return true;
            } catch (Throwable e) {
                log("! Ошибка addUserToChat для "+chatId+", "+userID+".\n" + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }
    public boolean backToChat(long chat_id){
        String text = applicationManager.getParameters().get(
                "back_to_chat_text",
                "Я вернулся!",
                "Текст возврата в чат",
                "Текст, с которым бот будет по команде backToChat возвращаться в чат.");
        return sendMessage(null, chat_id, new Answer(text));
    }
        //blacklist and friends
    public ArrayList<User> getBlacklist100(){
            int retries = RETRIES;
            while(retries > 0) {
                retries --;
                try {
                    ArrayList<User> mudaks = api().getBlackList(0L, 100L);
                    log(". Загружено " + mudaks.size() + " людей из ЧС " + this + ".");
                    return mudaks;
                } catch (Throwable e) {
                    log("! Ошибка загрузки списка ЧС аккаунта " + this + "\n" + e.toString());
                    if(!reportError(e))
                        return new ArrayList<User>();
                }
            }
            return new ArrayList<User>();
        }
    public boolean removeFromBlacklist(Long uid){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                boolean result = api().deleteFromBlackList(uid);
                overloadSaver.markSend();
                if(result)
                    log(". Пользователь " + uid + " успешно был удален из ЧС " + this + ".");
                return result;
            } catch (Throwable e) {
                log("! Ошибка удаления пользователя "+uid+" из ЧС аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }
    public boolean addToBlacklist(long id){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                api().addToBlackList(id);
                overloadSaver.markSend();
                return true;
            } catch (Throwable e) {
                log("! Ошибка добавления в ЧС пользователя "+id+" на аккаунте " + this + "\n" + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }
    public ArrayList<Long> getOutFriendRequests(){
        int retries = RETRIES;
        ArrayList<Long> users = new ArrayList<>();
        while(retries > 0) {
            retries --;
            try {
                ArrayList<Object[]> friends = api().getRequestsFriends(1);
                for (int i = 0; i < friends.size(); i++) {
                    users.add((Long)friends.get(i)[0]);
                }
                return users;
            } catch (Throwable e) {
                log("! Ошибка загрузки списка исходящих заявок в друзья для аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return users;
            }
        }
        return users;
    }
    public ArrayList<Long> getInFriendRequests(){
        int retries = RETRIES;
        ArrayList<Long> users = new ArrayList<>();
        while(retries > 0) {
            retries --;
            try {
                ArrayList<Object[]> friends = api().getRequestsFriends(0);
                for (int i = 0; i < friends.size(); i++) {
                    long id = (Long)friends.get(i)[0];
                    if(!users.contains(id))//2017-02 не знаю зачем этот костыль
                        users.add(id);
                }

                ArrayList<User> followers = api().getFollowers(getId(), 0, 50, null, null);
                for (int i = 0; i < followers.size(); i++) {
                    long id = followers.get(i).uid;
                    if(!users.contains(id))
                        users.add(id);
                }
                return users;
            } catch (Throwable e) {
                log("! Ошибка загрузки входящих заявок в друзья для аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return users;
            }
        }
        return users;
    }
    public long deleteFriend(long id){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                long result = api().deleteFriend(id);
                overloadSaver.markSend();
                return result;
            } catch (Throwable e) {
                log("! Ошибка удаления пользователя "+id+" из друзей аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return -1;
            }
        }
        return -1;
    }
    public int getFriendsCount(){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                ArrayList<Long> friends1 = api().getFriends(getId(), "", 0, captcha_key, captcha_sid);
                captcha_key = null;
                captcha_sid = null;
                int cnt = friends1.size();
                log(". ("+this+") Число друзей = " + cnt);
                return cnt;
            } catch (Throwable e) {
                log("! Ошибка подсчётка количества друзей аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return 0;
            }
        }
        return 0;
    }
    public long addFriend(long id){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                long result = api().addFriend(id, "", captcha_key, captcha_sid);
                captcha_key = null;
                captcha_sid = null;
                return result;
            } catch (Throwable e) {
                log("! Ошибка добавления "+id+" в друзья аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return 0;
            }
        }
        return 0;
    }
        //wall
    public ArrayList<WallMessage> getWallMessages20Unsafe(Long owner_id)throws Exception{
        return getWallMessagesUnsafe(owner_id, 20);
    }
    public ArrayList<WallMessage> getWallMessagesUnsafe(Long owner_id, int count)throws Exception{
        return getWallMessagesUnsafe(owner_id, count, 0);
    }
    public ArrayList<WallMessage> getWallMessagesUnsafe(Long owner_id, int count, int offset)throws Exception{
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                ArrayList<WallMessage> messages = api().getWallMessages(owner_id, count, offset, "");
                log(". Загружено " + messages.size() + " записей на стене " + owner_id+ ".");
                return messages;
            } catch (Throwable e) {
                log("! Ошибка загрузки списка записей со стены "+owner_id+" с аккаунта " + this + "\n" + e.toString());
                if(!reportError(e) || retries == 0)
                    throw e;
            }
        }
        return new ArrayList<>();
    }
    public ArrayList<WallMessage> getWallMessages20(Long owner_id){
        return getWallMessages(owner_id, 20);
    }
    public ArrayList<WallMessage> getWallMessages(Long owner_id, int count){
        return getWallMessages(owner_id, count, 0);
    }
    public ArrayList<WallMessage> getWallMessages(Long owner_id, int count, int offset){
        try{
            return getWallMessagesUnsafe(owner_id, count, offset);
        }catch (Exception e){
            return new ArrayList<>();
        }
    }
    public ArrayList<Comment> getWallComments20Unsafe(long owner_id, long post_id) throws Exception{
        return getWallCommentsUnsafe(owner_id, post_id, 20);
    }
    public ArrayList<Comment> getWallCommentsUnsafe(long owner_id, long post_id, int count)throws Exception{
        return getWallCommentsUnsafe(owner_id, post_id, count, 0);
    }
    public ArrayList<Comment> getWallCommentsUnsafe(long owner_id, long post_id, int count, int offset)throws Exception{
        //прихотят комментарии начиная с конца
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                ArrayList<Comment> comments = api().getWallComments(owner_id, post_id, offset, count, true).comments;;
                log(". Загружено " + comments.size() + " комментариев к записи " + owner_id+ "_"+post_id+".");
                return comments;
            } catch (Throwable e) {
                log("! Ошибка загрузки списка комментариев к записи "+owner_id+"_"+post_id+" c аккаунта " + this + "\n" + e.toString());
                if(!reportError(e) || retries == 0)
                    throw e;
            }
        }
        return new ArrayList<>();
    }
    public ArrayList<Comment> getWallComments20(long owner_id, long post_id){
        return getWallComments(owner_id, post_id, 20);
    }
    public ArrayList<Comment> getWallComments(long owner_id, long post_id, int count){
        return getWallComments(owner_id, post_id, count, 0);
    }
    public ArrayList<Comment> getWallComments(long owner_id, long post_id, int count, int offset){
        try {
            return getWallCommentsUnsafe(owner_id, post_id, count, offset);
        }
        catch (Exception e){
            return new ArrayList<>();
        }
    }
    public WallMessage getWallPost(long owner_id, long post_id){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                ArrayList<String> posts = new ArrayList<>();
                posts.add(owner_id + "_"+post_id);
                ArrayList<WallMessage> messages = api().getWallMessage(posts);
                if(messages.size() > 0)
                    return messages.get(0);
                return null;
            } catch (Throwable e) {
                log("! Ошибка загрузки поста "+owner_id + "_"+post_id+" с помощью аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return null;
            }
        }
        return null;
    }
    public boolean createWallMessage(long owner_id, Answer answer){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                api().createWallPost(owner_id, answer.text, answer.getAttachmentStrings(), null, false, false, false, null, null, null, 0L, captcha_key, captcha_sid);
                overloadSaver.markSend();
                captcha_key = null;
                captcha_sid = null;
                log(". Запись " + answer + " опубликована на стене " + owner_id+ " от имени "+this+".");
                return true;
            } catch (Throwable e) {
                log("! Ошибка отправки записи "+ answer + " на стену "+owner_id+" c аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }
    public boolean deletePost(long postId, long ownerId){
        try {
            api().removeWallPost(postId, ownerId);
            overloadSaver.markSend();
            log(". Запись " + postId + " удалена со стены " + ownerId+ " аккаунтом "+this+".");
            return true;
        } catch (Throwable e) {
            log("! Ошибка удаления записи "+ postId + " со стены "+ownerId+" c аккаунта " + this + ":\n" + e.toString());
            if(!reportError(e))
                return false;
        }
        return false;
    }
    public boolean createWallComment(long ownerID, long postID, Answer answer, Long replyTo){
        try {
            return createWallCommentUnsafe(ownerID, postID, answer, replyTo);
        }
        catch (Exception e){
            return false;
        }
    }
    public boolean createWallCommentUnsafe(long ownerID, long postID, Answer answer, Long replyTo) throws Exception{
        String[] parts = F.splitText(answer.text, 5000);

        for(int i=0; i<parts.length; i++) {
            String part = parts[i];
            ArrayList<String> attachments = new ArrayList<>();
            if(i == 0)
                attachments = answer.getAttachmentStrings();
            if(i > 0)
                part = "(часть "+i+") " + part;
            int retries = RETRIES;
            while (retries > 0) {
                retries--;
                try {
                    api().createWallComment(ownerID, postID, part, replyTo, attachments, false, captcha_key, captcha_sid);
                    overloadSaver.markSend();
                    captcha_key = null;
                    captcha_sid = null;
                    log(". Запись " + part + " опубликована на стене " + ownerID + " в комментариях к записи "+postID+" от имени " + this + ".");
                    break;
                } catch (Throwable e) {
                    log("! Ошибка отправки записи " + part + " на стену " + ownerID + " в комментариях к записи "+postID+ " c аккаунта " + this + ":\n" + e.toString());
                    if (!reportError(e) || retries == 0)
                        throw e;
                }
            }
        }
        return true;
    }
    public boolean deleteComment(long commentId, long ownerId){
        try {
            api().deleteWallComment(ownerId, commentId);
            overloadSaver.markSend();
            log(". Комментарий " + commentId + " удален со стены " + ownerId + "аккаунтом " + this + " успешно.");
            return true;
        } catch (Throwable e) {
            log("! Ошибка удаления комментария " + commentId + " со стены " + ownerId + " c аккаунта " + this + ":\n" + e.toString());
            reportError(e);
            return false;
        }
    }
    public boolean addLike(long owner_id, long item_id, String type){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                /* type:
                *   post — запись на стене пользователя или группы;
                    comment — комментарий к записи на стене;
                    photo — фотография;
                    audio — аудиозапись;
                    video — видеозапись;
                    note — заметка;
                    market — товар;
                    photo_comment — комментарий к фотографии;
                    video_comment — комментарий к видеозаписи;
                    topic_comment — комментарий в обсуждении;
                    market_comment — комментарий к товару;
                * */
                api().addLike(owner_id, item_id, type, null, captcha_key, captcha_sid);
                overloadSaver.markSend();
                captcha_key = null;
                captcha_sid = null;
                log(". Добавлен лайк з записи " + type + owner_id + "+" + item_id + " от имени "+this+".");
                return true;
            } catch (Throwable e) {
                log("! Ошибка добавления лайка к записи " + type + owner_id + "+" + item_id + " от имени "+this+":\n" + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }
        //attachments
    public File downloadFile(String link) throws Exception{
            //просто скачать файл по ссылке
            int retries = RETRIES;
            while(retries > 0) {
                retries--;
                try {
                    log(". DOWNLOAD: Connecting...");
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet httpGet = new HttpGet(link);
                    HttpResponse httpResponse = httpClient.execute(httpGet);

                    log(". DOWNLOAD: CreatingFile...");
                    File fileToSave = new File(applicationManager.getHomeFolder() + File.separator + "download_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
                    FileOutputStream fileOutputStream = new FileOutputStream(fileToSave);

                    log(". DOWNLOAD: Reading...");
                    InputStream inputStream = httpResponse.getEntity().getContent();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1)
                        fileOutputStream.write(buffer, 0, bytesRead);
                    fileOutputStream.close();
                    log(". DOWNLOAD: writed " + fileToSave.length() + " bytes");
                    log(". DOWNLOAD: saved to " + fileToSave.getPath() + ".");
                    return fileToSave;
                } catch (Throwable e) {
                    e.printStackTrace();
                    log("! DOWNLOAD: Ошибка загрузки документа " + link + ": " + e.toString());
                    if(!reportError(e) || retries == 0)
                        throw e;
                }
            }
            return null;
        }
    public Document uploadDocument(File file){
        if(file == null)
            return null;
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                log(". Uploading file: " + file.getPath());
                String fileName = file.getName();
                if(!fileName.contains("."))//это нужно потому что ВК не принимает файлы некоторых типов
                    fileName = fileName + ".bin";
                String[] deniedTypes = {".apk", ".mp3", ".exe", ".msi", ".zip", ".rar", ".m4a", ".htm", ".html"};
                for (String type : deniedTypes)
                    if(fileName.endsWith(type))
                        fileName = fileName.replace(type, type+"1");
                log(". File name to upload: " + fileName);

                long total = file.length();
                log(". File size to upload: " + total/1024L + " Kb.");
                byte[] fileArray = new byte[(int)total];
                RandomAccessFile f = new RandomAccessFile(file, "r");
                f.read(fileArray);

                log(". Getting server to upload...");
                String server = api().docsGetUploadServer();
                log(". Server to upload: " + server);

                log(". Sending...");
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost postRequest = new HttpPost(server);
                ByteArrayBody bab = new ByteArrayBody(fileArray, fileName);
                MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                reqEntity.addPart("file", bab);
                postRequest.setEntity(reqEntity);
                httpClient.getParams().setParameter("http.protocol.content-charset", "UTF-8");
                HttpResponse httpResponse = httpClient.execute(postRequest);

                log(". Reading...");
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent(), "UTF-8"));
                String sResponse;
                StringBuilder s = new StringBuilder();
                while ((sResponse = reader.readLine()) != null)
                    s = s.append(sResponse);
                String response = s.toString();
                log(". Received " + total + " bytes.");

                log(". Parsing...");
                JSONObject jsonObject = new JSONObject(response);
                if(!jsonObject.has("file"))
                    throw new Exception(response);
                String fileField = jsonObject.getString("file");
                log(". file =" + fileField);

                log("Saving...");
                return api().saveDoc(fileField);
            } catch (Throwable e) {
                log("! Ошибка отправки документа "+file.getName()+" c аккаунта " + this + "\n" + e.toString());
                if(!reportError(e))
                    return null;
            }
        }
        return null;
    }
    public File downloadDocument(Document document) throws Exception{
        // https://vk.com/doc10299185_438023291?api=1
        // https://vk.com/doc2314852_165123053?hash=d56be79a78fe0672bb&api=1
        // https://vk.com/doc10299185_443343501?hash=af6247a33eee2192f1&dl=1489793368539cc73abe3f116a2e&api=1

        log(". DOC: Загрузка файла с ВК...");
        String link = document.url;
        log(". DOC: Ссылка на загрузку: " + link);
        if(link == null || link.equals("")){
            throw new Exception(log("! DOC: Ссылка на загрузку пустая."));
        }
        File downloadedFile = downloadFile(link);
        if(downloadedFile == null){
            throw new Exception(log("! DOC: Ошибка. Документ не загружен."));
        }
        log(". DOC: Документ загружен: " + downloadedFile.getPath());
        return downloadedFile;
    }
    public boolean poll(long poll_id, long answer_id, long owner_id){
        int retries = RETRIES;
        while(retries > 0) {
            retries --;
            try {
                api().addPollVote(poll_id, answer_id, owner_id, 0L, captcha_key, captcha_sid);
                overloadSaver.markSend();
                captcha_key = null;
                captcha_sid = null;
                log(". Добавлен голос в опросе " + poll_id + " за " + answer_id + " от имени "+this+".");
                return true;
            } catch (Throwable e) {
                log("! Ошибка добавления голоса в опросе " + poll_id + " за " + answer_id + " от имени "+this+":\n" + e.toString());
                if(!reportError(e))
                    return false;
            }
        }
        return false;
    }


    private Api api(){
        if(api == null || !api.getAccess_token().equals(getToken()))
            api = new Api(getToken(), API_ID);
        if (!applicationManager.isRunning())
            return null;
        overloadSaver.waitUntilActive();
        apiCounter ++;
        overloadSaver.markRead();
        return api;
    }
    private boolean reportError(Throwable e){
        //return is do we need to do one more try
        e.printStackTrace();
        errorCounter ++;
        if(e instanceof NullPointerException)
            return false;
        if(e instanceof OutOfMemoryError){
            applicationManager.messageBox("У нас большие проблемы! Нам не хватает оперативной памяти!\n" +
                    "Это может означать, что база ответов слишком большая для твоего телефона, либо твой " +
                    "телефон слишком слабый для бота.\n" +
                    "Для начала - попробуй уменьшить размер базы ответов.");
            return false;
        }
        if(e.toString().contains("FileNotFound")){
            if(errorCounter*3>apiCounter && apiCounter > 100)
                deactivateFor5Minutes("Слишком много ошибок. Из " + apiCounter + " запросов " + errorCounter + " завершились ошибкой. Последняя ошибка: " + e.toString());
            return true;
        }
        if(e.toString().contains("Unable to resolve host")){
            if(errorCounter*3>apiCounter && apiCounter > 100)
                deactivateFor5Minutes("Слишком много ошибок. Из " + apiCounter + " запросов " + errorCounter + " завершились ошибкой. Последняя ошибка: " + e.toString());
            return true;
        }
        if(e.toString().contains("NetworkOnMainThreadException")){
            return false;
        }
        if(e instanceof KException) {
            KException ke = (KException)e;
            //help:     https://vk.com/dev/errors

            if(ke.getMessage().toLowerCase().contains("invalid access_token") || ke.getMessage().contains("invalid session")){
                applicationManager.messageBox("У нас проблема: аккаунт "+this+" сообщает о некорректном токене.\n" +
                        "Это означает, что в аккаунт надо войти заново.\n" +
                        "Такая ошибка возникает если, например, сменить пароль к аккаунту, или " +
                        "принудительно завершить все сессии в настройках на сайте ВК.");
                stopAccount();
                setToken_ok(false);
                setState("Остановлен по причине " + ke.getMessage());
                return false;
            }
            else if(ke.getMessage().toLowerCase().contains("can't set typing activity for this peer")){
                //не ебу что это за ошибка, но не думаю что это проблема
                return false;
            }
            else if(ke.getMessage().toLowerCase().contains("access to wall's post denied")){
                //недостаточно прав
                return false;
            }
            else if(ke.getMessage().toLowerCase().contains("group messages are disabled")){
                //на этом аккаунте отключили работу сообщений в сообщесте. Он больше нахуй не нужен.
                setState("Сообщения сообщества были отключены.");
                stopAccount();
                return false;
            }
            else if(ke.error_code == 6) { //Слишком много запросов в секунду.
                applicationManager.messageBox("У нас проблема: аккаунт "+this+" перегружен. " +
                        "ВК ограничивает количество запросов от приложения, " +
                        "при этом бот работает на максимально допустимой скорости." +
                        "Проверь, не запущен ли ещё где-то другой бот, который работает с этим же аккаунтом.\n" +
                        "Если запущен, выключи его, либо в параметрах обеих ботов увеличь значение " +
                        "request_gap в два раза, чтобы не было перегрузки аккаунта." +
                        "Несколько ботов не могут работать вместе на одном аккаунте со стандартными настройками.");
                overloadSaver.markSend();
                return true;
            }
            else if(ke.error_code == 9) { //Слишком много однотипных действий.
                applicationManager.messageBox("У нас проблема: аккаунт "+this+" перегружен. " +
                        "ВК ограничивает количество запросов от приложения, " +
                        "при этом бот работает на максимально допустимой скорости." +
                        "Проверь, не запущен ли ещё где-то другой бот, который работает с этим же аккаунтом.\n" +
                        "Если запущен, выключи его, либо в параметрах обеих ботов увеличь значение " +
                        "request_gap в два раза, чтобы не было перегрузки аккаунта." +
                        "Несколько ботов не могут работать вместе на одном аккаунте со стандартными настройками.");
                overloadSaver.markSend();
                return true;
            }
            else if(ke.error_code == 14) { //Требуется ввод кода с картинки (Captcha).
                deactivateFor5Minutes("Требуется ввод кода с картинки.");
                new CapthcaWindow().handleCaptcha(this, ke.captcha_sid, ke.captcha_img);
                return false;
            }
            else if(ke.error_code == 16) { //Требуется выполнение запросов по протоколу HTTPS, т.к.
                // пользователь включил настройку, требующую работу через безопасное соединение.
                applicationManager.messageBox("У нас проблема: аккаунт "+this+" требует работы по протоколу HTTPS. " +
                        "В настройках твоего аккаунта настроена принудительная работа по протоколу HTTPS. " +
                        "Бот с ним работать пока не умеет (да и это ему не нужно), поэтому не будет работать с этим аккаунтом " +
                        "пока установлена эта настройка.\n" +
                        "Чтобы отключить эту настройку, открой ВК с компьютера," +
                        "Нажми на аватарку сверху -> Настройки -> Безопасность -> Использовать защищенное соединение (HTTPS) -> Сними галочку.\n" +
                        "После этого можешь включать аккаунт снова, всё должно заработать.");
                stopAccount();
                setState("Остановлен по причине принудительного использования HTTPS протокола.");
                return false;
            }
            else if(ke.error_code == 17) { //Требуется валидация пользователя.
                //ke.redirect_uri
                //// TODO: 24.02.2017 implement it
                stopAccount();
                setToken_ok(false);
                setState("Остановлен по причине: Требуется валидация пользователя.");
                validation_url = ke.redirect_uri;
                return false;
            }
            else{ //левая ошибка какая-то
                // которую надо показать пользователю но мы с ней ничего сделать
                // не сможем
                if(errorCounter*3>apiCounter && apiCounter > 100)
                    deactivateFor5Minutes("Слишком много ошибок. Из " + apiCounter + " запросов " + errorCounter + " завершились ошибкой. Последняя ошибка: " + e.toString());
                return false;
            }
        }
        return false;
    }
    private void getUserName(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                User user = getUserAccount(getId());
                if(user != null){
                    userName = user.first_name + " " + user.last_name;
                    screenName = user.domain;
                }
                else{
                    userName = String.valueOf(getId());
                    screenName = String.valueOf(getId());
                }

            }
        }, "GetUserName thread").start();
    }

    private void deactivateFor5Minutes(final String reason){
        log("Деактивация аккаунта "+userName+" на 5 минут...");
        stopAccount();
        setToken_ok(false);
        setState("Аккаунт отключен на 5 минут по причине: " + reason);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Thread.currentThread().setName("Account activation after 5 minutes waiting");
                startAccount();
                setToken_ok(true);
                setState("Аккаунт снова запущен после ошибки: " + reason);
            }
        }, 300000);
    }
    private void deactivateFor10Minutes(final String reason){
        log("Деактивация аккаунта "+userName+" на 10 минут...");
        stopAccount();
        setToken_ok(false);
        setState("Аккаунт отключен на 10 минут по причине: " + reason);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Thread.currentThread().setName("Account activation after 10 minutes waiting");
                startAccount();
                setToken_ok(true);
                setState("Аккаунт снова запущен после ошибки: " + reason);
            }
        }, 600000);
    }

    //commands
    class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }
        public @Override String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            if(message.getText().toLowerCase().trim().equals("status") || message.getText().toLowerCase().trim().equals("acc status"))
                return  "Аккаунт " + VkAccountCore.this + " счетник обращений к API: "+apiCounter + "\n" +
                        "Аккаунт " + VkAccountCore.this + " счетник ошибок: "+errorCounter + "\n";
            return "";
        }
        public @Override ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    private class OverloadSaver{
        //этот класс будет занимать тем, что будет препятствовать слишком частому вызову опасных для частого выполнения функиций
        private long lastRequestTime = 0;
        private long requestGap = 0;
        private TimeCounter sendCounter = new TimeCounter(300000);//5 минут - время устаревания записей

        //подождать пока аккаунт освободится, но не более 10 секунд
        public void waitUntilActive(){
            int waiting = 0;
            while (!isReady() && waiting< 10000) {
                sleep(20);
                waiting += 20;
            }
        }
        public void markRead(){
            //по замыслу, данная функция предусматривает ручную установку типа выполняемой с аккаунтом операции
            // Операции чтения с сервера должны проводиться с интервалом, указанным ниже
            // операции записи имеют больший интервал.
            // Поэтому, тут две функции. Если их вызвать, ближайшее время этот аккаунт будет не готов для выполнения операций.
            lastRequestTime = System.currentTimeMillis();
            //до 10 000 установок -  3 запроса в секунду (gap 300)
            //10 000 ,,, установок = 5 запросов в секунду (gap 200)
            //100 000 ... установок - 8 запросов в секунду (gap 125)
            //
            requestGap = applicationManager.getParameters().get(
                    "request_gap",
                    125,
                    "Интервал запросов",
                    "Минимальное время в миллисекундах между запросами с одного аккаунта. " +
                            "Это требуется для того, чтобы не нарушать требования Вконтакте " +
                            "относительно частоты запросов на сервер.\n" +
                            "Слишком малое время может привести к ошибке на сервере" +
                            " TooManyRequestsPerSecond.");
        }
        public void markSend(){
            sendCounter.add(24853L);//write
            lastRequestTime = System.currentTimeMillis();
            int sends3minutes = sendCounter.countLastSec(24853L, 180);
            requestGap = (sends3minutes + 1) * 1000;
        }
        public boolean isReady(){
            if(!isEnabled() || !isRunning())
                return false;
            long now = System.currentTimeMillis();
            return (now - lastRequestTime) > requestGap;
        }

        private void sleep(int mili){
            try{
                Thread.sleep(mili);
            }
            catch (Throwable e){}
        }
    }
}
