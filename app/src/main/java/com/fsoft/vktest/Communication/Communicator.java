package com.fsoft.vktest.Communication;

import com.fsoft.vktest.AnswerInfrastructure.*;
import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccount;
import com.fsoft.vktest.Communication.Account.Telegram.TgAccountCore;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.*;
import com.fsoft.vktest.Utils.CommandParser;
import com.perm.kate.api.Attachment;
import com.perm.kate.api.VkPoll;
import com.perm.kate.api.VkPollAnswer;
import com.perm.kate.api.WallMessage;

import com.fsoft.vktest.Modules.Commands.CommandDesc;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * class for communication with VK
 * Created by Dr. Failov on 05.08.2014.
 */
public class Communicator extends CommandModule {
    private ArrayList<VkAccount> vkAccounts = new ArrayList<>();
    private ArrayList<TgAccount> tgAccounts = new ArrayList<>();
    private WallManager wallManager = null;
    private FileStorage file = null;
    private boolean running = false;

    public Communicator(ApplicationManager applicationManager) {
        super(applicationManager);
        file = new FileStorage("communicator", applicationManager);
        wallManager = new WallManager(this);

        childCommands.add(new Status(applicationManager));
        childCommands.add(new Save(applicationManager));
        childCommands.add(new SendPost(applicationManager));
        childCommands.add(new SendComment(applicationManager));
        childCommands.add(new AddLikes(applicationManager));
        childCommands.add(new DeletePost(applicationManager));
        childCommands.add(new DeleteComment(applicationManager));
        childCommands.add(new AddAccount(applicationManager));
        childCommands.add(new RemAccount(applicationManager));
        childCommands.add(new GetAccountList(applicationManager));
        childCommands.add(new Ban(applicationManager));
        childCommands.add(new Repost(applicationManager));
        childCommands.add(new Poll(applicationManager));
        childCommands.add(new HttpGet(applicationManager));

        String accountList[] = file.getStringArray("VKaccounts", new String[0]);
        for (String acc:accountList)
            vkAccounts.add(new VkAccount(applicationManager, acc));

        accountList = file.getStringArray("TGaccounts", new String[0]);
        for (String acc:accountList)
            tgAccounts.add(new TgAccount(applicationManager, acc));
    }
    public FileStorage getFile() {
        return file;
    }
    public void startModule(){
        running = true;
        wallManager.startModule();
        for(VkAccount vkAccount:vkAccounts)
            vkAccount.login();
    }
    public void stopModule(){
        running = false;
        wallManager.stopModule();
        for(VkAccount vkAccount:vkAccounts)
            vkAccount.stopAccount();
    }
    public boolean containsVkAccount(long id){
        return getVkAccount(id) != null;
    }
    public boolean containsTgAccount(long id){
        return getTgAccount(id) != null;
    }
    public VkAccount getVkAccount(long id){
        for (VkAccount account : vkAccounts)
            if (account.getId() == id)
                return account;
        return null;
    }
    public TgAccount getTgAccount(long id){
        for (TgAccount account : tgAccounts)
            if (account.getId() == id)
                return account;
        return null;
    }
    public ArrayList<VkAccount> getVkAccounts() {
        return vkAccounts;
    }
    public ArrayList<TgAccount> getTgAccounts() {
        return tgAccounts;
    }
    public VkAccount getActiveVkAccount(){
        int cycles = 0;
        while(cycles ++ < 1000) {
            for (VkAccount account : vkAccounts)
                if (account.isReady())
                    return account;
            F.sleep(100);
        }
        return null;
    }
    public String addAccount(VkAccount vkAccount){
        if(vkAccount == null)
            return "Аккаунт не получен";
        if(containsVkAccount(vkAccount.getId()))
            return "Такой аккаунт уже есть";
        vkAccounts.add(vkAccount);

        String[] accountList = new String[vkAccounts.size()];
        for (int i = 0; i < vkAccounts.size(); i++)
            accountList[i] = vkAccounts.get(i).getFileName();
        file.put("VKaccounts", accountList).commit();
        return "Аккаунт " + vkAccount.getId() + " добавлен. Список аккаунтов сохранён.";
    }
    public String addAccount(TgAccount tgAccount){
        if(tgAccount == null)
            return "Аккаунт не получен";
        if(containsTgAccount(tgAccount.getId()))
            return "Такой аккаунт уже есть";
        tgAccounts.add(tgAccount);

        String[] accountList = new String[tgAccounts.size()];
        for (int i = 0; i < tgAccounts.size(); i++)
            accountList[i] = tgAccounts.get(i).getFileName();
        file.put("TGaccounts", accountList).commit();
        return "Аккаунт " + tgAccount.getId() + " добавлен. Список аккаунтов сохранён.";
    }
    public String remVkAccount(long id){
        VkAccount accountToRemove = getVkAccount(id);
        if(accountToRemove == null)
            return "Аккаунта с ID="+id+" нет.";
        if(running)
            accountToRemove.stopAccount();
        vkAccounts.remove(accountToRemove);

        String[] accountList = new String[vkAccounts.size()];
        for (int i = 0; i < vkAccounts.size(); i++)
            accountList[i] = vkAccounts.get(i).getFileName();
        file.put("VKaccounts", accountList).commit();

        if(accountToRemove.remove())
            return "Аккаунт VK " + id + " успешно удалён.";
        else
            return "Аккаунт VK " + id + " удалён из списка, но его файл удалить не получается.";
    }
    public String remTgAccount(TgAccountCore accountToRemove){
        if(accountToRemove == null)
            return log("Функция удаления TG аккаунта вызвана с аргументом null");
        if(running)
            accountToRemove.stopAccount();
        tgAccounts.remove(accountToRemove);

        String[] accountList = new String[tgAccounts.size()];
        for (int i = 0; i < tgAccounts.size(); i++)
            accountList[i] = tgAccounts.get(i).getFileName();
        file.put("TGaccounts", accountList).commit();

        if(accountToRemove.remove())
            return "Аккаунт TG " + accountToRemove + " успешно удалён.";
        else
            return "Аккаунт TG " + accountToRemove + " удалён из списка, но его файл удалить не получается.";
    }

    //todo кажется логичным засунуть сюда функции которые нужны для разных соцсетей, чтобы он сам решал где достать аккаунт



    private class Status extends CommandModule{
        public Status(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(com.fsoft.vktest.AnswerInfrastructure.Message message) {
            if(message.getText().toLowerCase().equals("status") || message.getText().toLowerCase().equals("communicator status"))
                return "Количество аккаунтов в программе: " + vkAccounts.size() + "\n";
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    private class Save extends CommandModule{
        public Save(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            if(message.getText().toLowerCase().equals("save"))
                return "Начиная с версии 5.0 команда сохранения больше не требуется. " +
                        "Все изменения в настройках программы сохраняются автоматически в момент изменения.";
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            return new ArrayList<>();
        }
    }
    private class SendPost extends CommandModule{
        public SendPost(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals("sendpost")) {
                try {
                    String ownerIDstring = commandParser.getWord();
                    if(ownerIDstring.equals(""))
                        return log("! Я не могу отправить пост, потому что ты не написал адрес стены получателя. \n" +
                                "Формат команды: botcmd SendPost <ссылка на стену> <текст сообщения>");
                    String text = commandParser.getText();
                    if(text.equals(""))
                        return log("! Я не могу отправить пост, потому что ты не написал текст сообщения. \n" +
                                "Формат команды: botcmd SendPost <ссылка на стену> <текст сообщения>");
                    if(applicationManager.getBrain().getFilter().containsForbidden(text))
                        return log("! Я не могу отправить твоё сообщение, потому что оно содержит текст, который запрещено отправлять.");
                    else
                        text = applicationManager.getBrain().getFilter().filterText(text);
                    long ownerID = getActiveVkAccount().resolveScreenName(ownerIDstring);
                    if(ownerID == 0 || ownerID == -1)
                        return log("! Я не могу отправить пост, потому что ты написал неправильный адрес стены, " +
                                "которую я не смог распознать: " + ownerIDstring);
                    String link = "http://vk.com/id"+ownerID;
                    if(ownerID < 0)
                        link = "http://vk.com/club" + (-ownerID);
                    if(getActiveVkAccount().createWallMessage(ownerID, new Answer(text, message.getAttachments()))) {
                        return log(". На стену " + link + " было отправлено " +
                                (message.getAttachments().isEmpty() ? "" : message.getAttachments().size() + " вложений и ") +
                                "сообщение: " + text);
                    }
                    else {
                        return log("! Ошибка отправки сообщения на стену " + link + " с " +
                                (message.getAttachments().isEmpty() ? "" : message.getAttachments().size() + " вложениями и ") +
                                "текстом: " + text);
                    }
                }
                catch (Throwable e){
                    e.printStackTrace();
                    return (log(". Ошибка отправки сообщения: " + e.toString() + "\n"));
                }
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Опубликовать сообщение на стене",
                    "Ты можешь от имени бота опубликовать сообщение на стене.\n" +
                            "Ссылка может быть либо на стену, либо на профиль, либо на группу, либо упоминание, либо просто ID.\n" +
                            "Вложения также опубликуются.\n" +
                            "Используя эту команду, ты можешь вести блог от " +
                            "имени бота без надобности входить в аккаунт под его именем.",
                    "botcmd SendPost <ссылка на стену> <текст сообщения>"));
            return result;
        }
    }
    private class SendComment extends CommandModule{
        public SendComment(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("sendcomment")) {
                try {
                    //https://vk.com/drfailov?w=wall10299185_13439
                    //https://vk.com/wall10299185_13439
                    //https://vk.com/im?peers=c158_358039995&sel=42366576&w=wall-79633946_1520%2Fd3df00fad96df358fb
                    //wall10299185_13439
                    //10299185_13439
                    String linkToPost = commandParser.getWord();
                    String text = commandParser.getText();
                    if(linkToPost.equals(""))
                        return log("! Я не могу отправить комментарий, потому что ты не написал адрес поста, под которым надо написать комментарий. \n" +
                                "Формат команды: botcmd SendComment <ссылка на пост> <текст сообщения>");
                    if(text.equals(""))
                        return log("! Я не могу отправить пост, потому что ты не написал текст сообщения. \n" +
                                "Формат команды: botcmd SendComment <ссылка на пост> <текст сообщения>");
                    if(applicationManager.getBrain().getFilter().containsForbidden(text))
                        return log("! Я не могу отправить твоё сообщение, потому что оно содержит текст, который запрещено отправлять.");
                    else
                        text = applicationManager.getBrain().getFilter().filterText(text);

                    String regex = "wall(\\-?[0-9]+)_([0-9]+)";
                    long wallId = 0;
                    long postId = 0;
                    try {
                        Pattern p = Pattern.compile(regex);
                        Matcher m = p.matcher(linkToPost);
                        if (m.find()) {
                            String wallIdString = m.group(1);
                            String postIdString = m.group(2);
                            wallId = Long.parseLong(wallIdString);
                            postId = Long.parseLong(postIdString);
                        }
                    }
                    catch (Exception e){ }
                    if(wallId == 0 || postId == 0){
                        try{
                            String[] parts = linkToPost.split("_");
                            String wallIdString = parts[0];
                            String postIdString = parts[1];
                            wallId = Long.parseLong(wallIdString);
                            postId = Long.parseLong(postIdString);
                        }
                        catch (Exception e){ }
                    }
                    if(wallId == 0 || postId == 0)
                        return log("! Я не могу отправить комментарий, потому что ты написал неправильный адрес поста, " +
                                "который я не смог распознать: " + linkToPost);

                    String link = "https://vk.com/wall"+wallId+"_" + postId;
                    if(getActiveVkAccount().createWallComment(wallId, postId, new Answer(text, message.getAttachments()), null)) {
                        return log(". Под постом " + link + " был оставлен комментарий с " +
                                (message.getAttachments().isEmpty() ? "" : message.getAttachments().size() + " вложениями и ") +
                                "сообщением: " + text);
                    }
                    else {
                        return log("! Ошибка отправки комментария под постом " + link + " с " +
                                (message.getAttachments().isEmpty() ? "" : message.getAttachments().size() + " вложениями и ") +
                                "текстом: " + text);
                    }
                }
                catch (Throwable e){
                    e.printStackTrace();
                    return (log(". Ошибка отправки комментария: " + e.toString() + "\n"));
                }
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Опубликовать комментарий под записью на стене",
                    "Я могу от имени бота опубликовать комментарий под постом на стене.\n" +
                            "Ссылка может быть либо на стену, либо на профиль, либо на группу, либо упоминание." +
                            "Вложения также опубликуются.\n",
                    "botcmd SendComment <ссылка на пост> <текст сообщения>"));
            return result;
        }
    }
    private class AddLikes extends CommandModule{
        public AddLikes(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("like")) {
                String link = commandParser.getWord();
                if(link.equals(""))
                    return log("! Я не могу ничего лайкнуть, потому что ты не написал ссылку на фотку, пост или комментарий который надо лайкнуть. \n" +
                            "Формат команды: botcmd like <ссылка>");

                if(link.contains("?reply="))
                    return likeComment(link);
                if(link.contains("photo"))
                    return likePhoto(link);
                if(link.contains("wall"))
                    return likePost(link);
            }
            return "";
        }
        private String likePhoto(String link){
            //https://vk.com/im?sel=c158&w=historyc158_photo&z=photo10299185_456243073%2Fmail2323530
            //https://vk.com/albums10299185?z=photo10299185_456243079%2Fphotos10299185
            //https://vk.com/drfailov?z=photo10299185_456243073%2Fphotos10299185
            //https://vk.com/photo10299185_456243041
            //https://vk.com/photo140830142_456242863?all=1
            //https://m.vk.com/photo10299185_456243041
            //https://vk .com/im?sel=c57&w=wall-132807795_8867&z=photo-132807795_456242214%2Falbum-132807795_00%2Frev
            //         photo-132807795_456242214
            String regex = "photo(\\-?[0-9]+)_([0-9]+)";
            long ownerId = 0;
            long photoId = 0;
            try {
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(link);
                if (m.find()) {
                    ownerId = Long.parseLong(m.group(1));
                    photoId = Long.parseLong(m.group(2));
                }
            }
            catch (Exception e){
                return log("! Не могу лайкнуть фотографию, потому что не могу распознать ссылку:\n" + link + "\n" + e.toString());
            }
            if(ownerId == 0 || photoId == 0)
                return log("! Не могу лайкнуть фотографию, потому что не могу распознать ссылку:\n" + link);
            int successCounter = 0;
            for (VkAccount vkAccount:vkAccounts) {
                if (vkAccount.addLike(ownerId, photoId, "photo"))
                    successCounter++;
                F.sleep(1000);
            }
            if(successCounter == 0)
                return log("! У меня не получилось лайкнуть фотографию ни одним из "+vkAccounts.size()+" аккаунтов, sorry.");
            return log(". К фотографии " + ownerId + "_" + photoId + " добавлено " + successCounter + " лайков." + " Всего аккаунтов: " + vkAccounts.size() + ".");
        }
        private String likePost(String link){
            //https://vk.com/drfailov?w=wall10299185_13439
            //https://vk.com/wall10299185_13439
            //https://vk.com/im?peers=c158_358039995&sel=42366576&w=wall-79633946_1520%2Fd3df00fad96df358fb
            String regex = "wall(\\-?[0-9]+)_([0-9]+)";
            long ownerId = 0;
            long postId = 0;
            try {
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(link);
                if (m.find()) {
                    ownerId = Long.parseLong(m.group(1));
                    postId = Long.parseLong(m.group(2));
                }
            }
            catch (Exception e){
                return log("! Не могу лайкнуть запись, потому что не могу распознать ссылку:\n" + link + "\n" + e.toString());
            }
            if(ownerId == 0 || postId == 0)
                return log("! Не могу лайкнуть запись, потому что не могу распознать ссылку:\n" + link);
            int successCounter = 0;
            for (VkAccount vkAccount:vkAccounts) {
                if (vkAccount.addLike(ownerId, postId, "post"))
                    successCounter++;
                F.sleep(1000);
            }
            if(successCounter == 0)
                return log("! У меня не получилось лайкнуть запись ни одним из "+vkAccounts.size()+" аккаунтов, sorry.");
            return log(". К записи " + ownerId + "_" + postId + " добавлено " + successCounter + " лайков." + " Всего аккаунтов: " + vkAccounts.size() + ".");
        }
        private String likeComment(String link){
            //https://vk.com/wall10299185_13439?reply=19248
            String regex = "wall(\\-?[0-9]+)_([0-9]+)\\?reply=([0-9]+)";
            long ownerId = 0;
            long postId = 0;
            long commentId = 0;
            try {
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(link);
                if (m.find()) {
                    ownerId = Long.parseLong(m.group(1));
                    postId = Long.parseLong(m.group(2));
                    commentId = Long.parseLong(m.group(3));
                }
            }
            catch (Exception e){
                return log("! Не могу лайкнуть комментарий, потому что не могу распознать ссылку:\n" + link + "\n" + e.toString());
            }
            if(ownerId == 0 || commentId == 0)
                return log("! Не могу лайкнуть комментарий, потому что не могу распознать ссылку:\n" + link);
            int successCounter = 0;
            for (VkAccount vkAccount:vkAccounts) {
                if (vkAccount.addLike(ownerId, commentId, "comment"))
                    successCounter++;
                F.sleep(1000);
            }
            if(successCounter == 0)
                return log("! У меня не получилось лайкнуть комментарий ни одним из "+vkAccounts.size()+" аккаунтов, sorry.");
            return log(". К комментарию " + ownerId + "_" + commentId + " добавлено " + successCounter + " лайков." + " Всего аккаунтов: " + vkAccounts.size() + ".");
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Лайкнуть фото, пост или комментарий всеми аккаунтами",
                    "Используя все доступные мне аккаунты я попытаюсь добавить лайк на запись, или фото, или комментарий.\n" +
                            "Тип объекта по ссылке я определю самостоятельно.\n" +
                            "Внимание! Подобная деятельность нарушает правила сайта \"ВКонтакте\"! " +
                            "Ты используешь данную возможность на свой страх и риск!",
                    "botcmd like <ссылка>"));
            return result;
        }
    }
    private class DeletePost extends CommandModule{
        public DeletePost(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("deletepost")) {
                //https://vk.com/wall10299185_13439
                String link = commandParser.getWord();
                String regex = "wall(\\-?[0-9]+)_([0-9]+)";
                long ownerId = 0;
                long postId = 0;
                try {
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(link);
                    if (m.find()) {
                        ownerId = Long.parseLong(m.group(1));
                        postId = Long.parseLong(m.group(2));
                    }
                }
                catch (Exception e){
                    return log("! Не могу удалить запись, потому что не могу распознать ссылку:\n" + link + "\n" + e.toString());
                }
                if(ownerId == 0 || postId == 0)
                    return log("! Не могу удалить запись, потому что не могу распознать ссылку:\n" + link);
                for (VkAccount vkAccount:vkAccounts)
                    if (vkAccount.deletePost(postId, ownerId))
                        return log(". Запись успешно удалена аккаунтом " + vkAccount);
                return log("! Не могу удалить запись " + link + " ни одним из моих " + vkAccounts.size() + " аккаунтов.");
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить запись со стены",
                    "Можно дистанционно удалить запись со стены, если хотя бы у одного аккаунта в боте достаточно прав это сделать.\n" +
                            "Обрати внимание: чтобы эта функция работала, в боте должен быть хотя бы один " +
                            "аккаунт с достаточными правами доступа к стене!",
                    "botcmd DeletePost <ссылка на пост>"));
            return result;
        }
    }
    private class DeleteComment extends CommandModule{
        public DeleteComment(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("deletecomment")) {
                //https://vk.com/wall10299185_13439?reply=48320
                String link = commandParser.getWord();
                String regex = "wall(\\-?[0-9]+)_([0-9]+)\\?reply=([0-9]+)";
                long ownerId = 0;
                long postId = 0;
                long commentId = 0;
                try {
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(link);
                    if (m.find()) {
                        ownerId = Long.parseLong(m.group(1));
                        postId = Long.parseLong(m.group(2));
                        commentId = Long.parseLong(m.group(3));
                    }
                }
                catch (Exception e){
                    return log("! Не могу удалить комментарий, потому что не могу распознать ссылку:\n" + link + "\n" + e.toString());
                }
                if(ownerId == 0 || postId == 0 || commentId == 0)
                    return log("! Не могу удалить комментарий, потому что не могу распознать ссылку:\n" + link);
                for (VkAccount vkAccount:vkAccounts)
                    if (vkAccount.deleteComment(commentId, ownerId))
                        return log(". Комментарий успешно удален аккаунтом " + vkAccount);
                return log("! Не могу удалить комментарий " + link + " ни одним из моих " + vkAccounts.size() + " аккаунтов.");
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить комментарий со стены",
                    "Можно дистанционно удалить комментарий со стены, если хотя бы у одного аккаунта в боте достаточно прав это сделать.\n" +
                            "Обрати внимание: чтобы эта функция работала, в боте должен быть хотя бы один " +
                            "аккаунт с достаточными правами доступа к стене!\n" +
                            "Ссылка на комментарий должна быть вот такого формата: https://vk.com/wall000_000?reply=000",
                    "botcmd DeleteComment <ссылка на комментарий>"));
            return result;
        }
    }
    private class AddAccount extends CommandModule{
        public AddAccount(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("acc")){
                if(commandParser.getWord().toLowerCase().trim().equals("add")){
                    long id = commandParser.getLong();
                    String token = commandParser.getWord();

                    if(id == 0)
                        return "Я не получил от тебя ID добавляемого аккаунта. \n" +
                                "Правильный формат команды: botcmd acc add <ID аккаунта> <Токен аккаунта>";
                    if(token.trim().equals(""))
                        return "Я не получил от тебя токен добавляемого аккаунта. \n" +
                                "Правильный формат команды: botcmd acc add <ID аккаунта> <Токен аккаунта>";
                    if(containsVkAccount(id))
                        return "Аккаунт, который ты пытаешься добавить, уже добавлен. \n" +
                                "Если добавить его дважды, будут возникать ошибки в работе бота.";

                    VkAccount vkAccount = new VkAccount(applicationManager, "account"+id);
                    vkAccount.setToken(token);
                    vkAccount.setId(id);
                    addAccount(vkAccount);
                    if(running) {
                        vkAccount.login();
                        return "Аккаунт " + id + " добавлен в список аккаунтов и уже запущен.";
                    }
                    return "Аккаунт " + id + " добавлен и ожидает запуска коммуникатора.";
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Добавить аккаунт",
                    "Используя ID и токен, можно добавить аккаунт удалённо.\n" +
                            "Если ты не знаешь, что такое токен, советую почитать эту статью: vk.com/dev/access_token\n" +
                            "Если ты хочешь войти по логину и паролю, сделай это из окна программы.\n" +
                            "Обрати внимание: ТОКЕН - это НЕ ПАРОЛЬ. Войти по паролю можно только вручную из программы.\n" +
                            "Обрати внимание: токен, полученный в другой стране, городе или даже в другом районе, может не работать.\n" +
                            "Обрати внимание: токен - это временный ключ, и нет гарантии, что он будет работать всегда. Иногда он может " +
                            "самопроизвольно отключиться.",
                    "botcmd acc add <ID аккаунта> <Токен аккаунта>"));
            return result;
        }
    }
    private class RemAccount extends CommandModule{
        public RemAccount(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("acc")){
                if(commandParser.getWord().toLowerCase().trim().equals("rem")){
                    long id = commandParser.getLong();
                    if(!containsVkAccount(id))
                        return "Ты пытаешься удалить аккаунт " + id + ", которого нет.\n" +
                                "Правильный формат команды: botcmd acc rem <ID аккаунта>";
                    return remVkAccount(id);
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить аккаунт",
                    "Если удалить аккаунт из бота, бот перестанет на нём отвечать.\n" +
                            "При удалении аккаунта из бота также удаляется файл, в котором хранятся настройки аккаунта.",
                    "botcmd acc rem <ID аккаунта>"));
            return result;
        }
    }
    private class GetAccountList extends CommandModule{
        public GetAccountList(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("accs")){
                String result = "Список аккаунтов в программе: ";
                for(VkAccount vkAccount:vkAccounts) {
                    String status = "работает";
                    String link = "https://vk.com/id" + vkAccount.getId();
                    if(vkAccount.getId() < 0)
                        link = "https://vk.com/club" + (-vkAccount.getId());
                    if(vkAccount.isEnabled() || vkAccount.isRunning())
                        status = "работает";
                    if(!vkAccount.isEnabled() || vkAccount.isRunning())
                        status = "не включён, но запущен";
                    if(vkAccount.isEnabled() || !vkAccount.isRunning())
                        status = "включён, но не запущен";
                    if(!vkAccount.isEnabled())
                        status = "выключен";
                    result += vkAccount + " (" + status + ", "+link+" )\n";
                }
                result += "\nВсего аккаунтов: " + vkAccounts.size() + "\n";
                return result;
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Получить список всех аккаунтов",
                    "Покажет список и состояние аккаунтов ВК в программе",
                    "botcmd accs"));
            return result;
        }
    }
    private class Ban extends CommandModule{
        public Ban(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("ban")){
                if(!message.getSource().equals(Message.SOURCE_COMMENT))
                    return "Эта команда умеет работать только в комментариях.\n" +
                            "Чтобы забанить пользователя, напиши эту команду под его постом.";
                long wallId = message.getSource_id();
                if(wallId == 0)
                    return "Не могу получить ID стены, на которой ты это написал.";
                VkAccount wallAccount = getVkAccount(wallId);
                if(wallAccount == null)
                    return "Для того, чтобы заблокировать пользователя, необходимо, чтобы этот " +
                            "аккаунт ("+ getActiveVkAccount().getUserFullName(wallId)+") был добавлен в бота.";
                long idToBan = message.getComment_reply_user_id();
                if(idToBan == 0)
                    idToBan = message.getComment_post_author();
                if(idToBan == 0)
                    return "Не могу получить ID пользователя, которого нужно забанить.";
                if(wallId > 0){
                    if(wallAccount.addToBlacklist(idToBan))
                        return "Пользователь " + getActiveVkAccount().getUserFullName(idToBan) +
                                " успешно заблокирован на странице " + wallAccount + ".";
                    else
                        return "Не могу заблокировать пользователя " + getActiveVkAccount().getUserFullName(idToBan) +
                                " на странице " + wallAccount + "!";
                }
                if(wallId < 0){
                    ArrayList<Long> admins = wallAccount.getGroupAdmins();
                    for (Long adminId:admins){
                        VkAccount adminAccount = getVkAccount(adminId);
                        if(adminAccount != null) {
                            if(adminAccount.banGroupUserForMonth(Math.abs(wallId), idToBan))
                                return "Пользователь " + getActiveVkAccount().getUserFullName(idToBan) +
                                        " был успешно заблокирован в группе " + wallAccount + ". " +
                                        "Для блокировки использовался аккаунт " + adminAccount + ".";
                            else
                                return "Несмотря на то, что " + adminAccount + " является администратором сообщества, " +
                                        "у меня не получилось заблокировать пользователя " + getActiveVkAccount().getUserFullName(idToBan) +
                                        " в группе " + wallAccount + " из-за непредвиденной ошибки в момент блокировки.";
                        }
                    }
                    return "Я не могу заблокировать пользователя " + getActiveVkAccount().getUserFullName(idToBan)
                            + " в группе " + wallAccount + " потому что у меня нет аккаунта ни одного из администраторов группы.";
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Заблокировать пользователя",
                    "Напиши эту команду в комментариях под постом, автора которого надо забанить, " +
                            "либо в ответ на комментарий, автора которого надо забанить.\n" +
                            "Бот заблокирует пользователя на том аккаунте, на стене которого будет команда.\n" +
                            "Бот сможет заблокировать пользователя, только если аккаунт, на стене которого написана команда, есть в боте.\n" +
                            "Например: Петя написал гадость на стене iHA. Ты пишешь ему в комментарий bcd ban, и " +
                            "если в боте есть аккаунт iHA и достаточно полномочий, бот заблокирует Петю в iHA.",
                    "botcmd ban"));
            return result;
        }
    }
    private class Repost extends CommandModule{
        public Repost(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("repost")) {
                String link = commandParser.getWord();
                if(link.equals(""))
                    return log("! Я не могу ничего репостнуть, потому что ты не написал ссылку на пост, который нужно репостнуть. \n" +
                            "Формат команды: botcmd repost <ссылка на пост> <сообщение (не обязательно)>");

                String comment = commandParser.getText();

                //https://vk.com/drfailov?w=wall10299185_13439
                //https://vk.com/wall10299185_13439
                //https://vk.com/im?peers=c158_358039995&sel=42366576&w=wall-79633946_1520%2Fd3df00fad96df358fb
                String regex = "wall(\\-?[0-9]+)_([0-9]+)";
                long ownerId = 0;
                long postId = 0;
                try {
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(link);
                    if (m.find()) {
                        ownerId = Long.parseLong(m.group(1));
                        postId = Long.parseLong(m.group(2));
                    }
                }
                catch (Exception e){
                    return log("! Не могу репостнуть запись, потому что не могу распознать ссылку:\n" + link + "\n" + e.toString());
                }
                if(ownerId == 0 || postId == 0)
                    return log("! Не могу репостнуть запись, потому что не могу распознать ссылку:\n" + link);
                int successCounter = 0;
                message.sendAnswer(new Answer("Начинаю репостить..."));
                for (VkAccount vkAccount:vkAccounts) {
                    if (vkAccount.rePost(ownerId, postId, comment))
                        successCounter++;
                    F.sleep(5500);
                }
                if(successCounter == 0)
                    return log("! У меня не получилось репостнуть запись ни одним из "+vkAccounts.size()+" аккаунтов, sorry.");
                return log(". Запись wall" + ownerId + "_" + postId + " репостнута на " + successCounter + " аккаунтов." + " Всего аккаунтов: " + vkAccounts.size() + ".");
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Репостнуть запись на стены всех аккаунтов",
                    "Ты можешь репостнуть запись сразу на все стены аккаунтов, чтобы донести её до " +
                            "друзей твоего бота.\n" +
                            "Обрати внимание, я буду репостить записи не сразу, а с " +
                            "промежутками в несколько секунд, чтобы не вызывать подозрения.\n" +
                            "Внимание! Подобная деятельность нарушает правила сайта \"ВКонтакте\"! " +
                            "Ты используешь данную возможность на свой страх и риск!",
                    "botcmd repost <ссылка на пост> <сообщение (не обязательно)>"));
            return result;
        }
    }
    private class Poll extends CommandModule{
        public Poll(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("repost")) {
                String link = commandParser.getWord();
                if(link.equals(""))
                    return log("! Я не могу проголосовать, потому что ты не написал ссылку на пост, который содержит опрос. \n" +
                            "Формат команды: botcmd poll <ссылка на пост с опросом> <номер ответа (нумерация с 1)>");
                int variant = commandParser.getInt();
                if(variant == 0)
                    return log("! Я не могу проголосовать, потому что ты не написал номер варианта за который надо проголосовать.\n" +
                            "Формат команды: botcmd poll <ссылка на пост с опросом> <номер ответа (нумерация с 1)>");

                //https://vk.com/drfailov?w=wall10299185_13439
                //https://vk.com/wall10299185_13439
                //https://vk.com/im?peers=c158_358039995&sel=42366576&w=wall-79633946_1520%2Fd3df00fad96df358fb
                String regex = "wall(\\-?[0-9]+)_([0-9]+)";
                long ownerId = 0;
                long postId = 0;
                try {
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(link);
                    if (m.find()) {
                        ownerId = Long.parseLong(m.group(1));
                        postId = Long.parseLong(m.group(2));
                    }
                }
                catch (Exception e){
                    return log("! Не могу проголосовать, потому что не могу распознать ссылку:\n" + link + "\n" + e.toString());
                }
                if(ownerId == 0 || postId == 0)
                    return log("! Не могу проголосовать, потому что не могу распознать ссылку:\n" + link);

                WallMessage wallMessage = getActiveVkAccount().getWallPost(ownerId, postId);
                if(wallMessage == null)
                    return log("! Я не могу проголосовать, потому что не могу загрузить сожержимое поста:" + link +
                    "\nЕсли это закрытая стена, я не смогу проголосовать.");
                VkPoll poll = null;
                if(wallMessage.attachments == null || wallMessage.attachments.size() == 0)
                    return log("! Я не могу проголосовать, потому что пост не содержит никаких вложений.");
                for (Attachment attachment:wallMessage.attachments)
                    if(attachment.type.toLowerCase().trim().equals("poll"))
                        poll = attachment.poll;
                if(poll == null)
                    return log("! Я не могу проголосовать, потому что среди вложений поста нету опросов.");
                ArrayList<VkPollAnswer> answers = VkPoll.getPollAnswers(poll.answers_json);
                if(answers == null || answers.isEmpty())
                    return log("! Я не могу проголосовать, так как, по неизвестной никому причине," +
                            " не могу получить список вариантов ответа опроса \""+poll.question+"\".");
                if(answers.size() > variant)
                    return log("! Я не могу проголосовать, потому что в опросе \""+poll.question+"\" нет "+variant+
                            " вариантов ответа. Их там всего "+answers.size()+".\n" +
                            "Выбери какой нибудь вариант, который есть в списке.");
                VkPollAnswer answer = answers.get(variant-1);
                if(answer == null)
                    return log("! Я не могу проголосовать в опросе \""+poll.question+"\" потому что " +
                            "не могу загрузить "+variant+ " вариант ответа.");

                long pollAnswerId = answer.id;
                long pollOwnerId = poll.owner_id;
                long pollId = poll.id;
                int successCounter = 0;
                message.sendAnswer(new Answer("Начинаю голосовать..."));
                for (VkAccount vkAccount:vkAccounts) {
                    if (vkAccount.poll(pollId, pollAnswerId, pollOwnerId))
                        successCounter++;
                    F.sleep(5500);
                }
                if(successCounter == 0)
                    return log("! У меня не получилось проголосовать ни одним из "+vkAccounts.size()+" аккаунтов, sorry.");
                return log(". В опросе poll" + ownerId + "_" + pollId + " проголосовали " + successCounter + " аккаунтов." + " Всего аккаунтов: " + vkAccounts.size() + ".");
            }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Проголосовать в опросе",
                    "Ты можешь проголосовать в опросе всеми аккаунтами, которые есть в боте.\n" +
                            "Обрати внимание, я буду голосовать не сразу, а с " +
                            "промежутками в несколько секунд, чтобы не вызывать подозрения.\n" +
                            "Внимание! Подобная деятельность нарушает правила сайта \"ВКонтакте\"! " +
                            "Ты используешь данную возможность на свой страх и риск!",
                    "botcmd poll <ссылка на пост с опросом> <номер ответа (нумерация с 1)>"));
            return result;
        }
    }
    private class HttpGet extends CommandModule{
        public HttpGet(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().trim().equals("httpget")){
                String link = commandParser.getText();
                if(link.equals(""))
                    return "Я не могу выполить запрос потому, что ты не указал ссылку." +
                            "Формат команды: botcmd httpGet <ссылка>";
                try {
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response = httpclient.execute(new org.apache.http.client.methods.HttpGet(link));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        String responseString = out.toString();
                        out.close();
                        return "Запрос выполнен.\n" +
                                "Статус: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase() + "\n" +
                                "Текст ответа:\n" +
                                responseString + "\n";
                    } else {
                        response.getEntity().getContent().close();
                        return "Запрос выполнен, но получена ошибка.\n" +
                                "Статус: " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase() + "\n";
                    }
                }
                catch (Exception e){
                    return "Запрос не выполнен из-за ошибки.\n" +
                            "Ошибка: " + e.toString() + "\n";
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Отправить GET запрос на адрес",
                    "Команда отправит GET запрос по указанной ссылке и отобразит ответ.",
                    "botcmd httpGet <ссылка>"));
            return result;
        }
    }
}
