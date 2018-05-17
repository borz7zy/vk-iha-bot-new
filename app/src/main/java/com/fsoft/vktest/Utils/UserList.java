package com.fsoft.vktest.Utils;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Универсальное решение для хранения разных всяких там список пользователей
 * Created by Dr. Failov on 01.01.2015.
 *
 * Полностью нахуй это переписываем
 * Edited by Dr. Failov on 27.09.2017.
 */

// TODO: 01.12.2017 добавление пользователя на некоторый срок, дата добавления пользователя

public class UserList extends CommandModule {
    private ArrayList<UserListElement> list = new ArrayList<>();
    private ArrayList<User> hardcodeDefined = new ArrayList<>();
    private String name; //allow, ignor ...
    private String shortDescription; //Список игнорируемых пользователей
    private String description; //Список пользователей, которым бот не будет отвечать. Кроме того, ....
    private FileStorage file;

    public UserList(String name, String shortDescription, String description, ApplicationManager applicationManager) {
        super(applicationManager);
        this.name = name;
        this.shortDescription = shortDescription;
        this.description = description;
        file = new FileStorage(name+"_list", applicationManager);
        load();
        childCommands.add(new Add(applicationManager));
        childCommands.add(new Has(applicationManager));
        childCommands.add(new Clr(applicationManager));
        childCommands.add(new Rem(applicationManager));
        childCommands.add(new Get(applicationManager));
    }


    public boolean has(User user){
        if(hardcodeDefined.contains(user))
            return true;
        return getIfExists(userId) != null;
    }
    public boolean has(String userId){
        try{
            Long longId = applicationManager.getCommunicator().getActiveAccount().resolveScreenName(userId);
            return has(longId);
        }
        catch (Exception e){
            return false;
        }
    }
    public void add(User user, String comment) throws Exception{
        log(". ("+name+") Внесение в список страницы " + userId + " ...");
        if (user == null)
            throw new Exception("Ошибка добавления страницы " + user + " в список " + name + ". Возможно, вы ввели неправильный ID страницы.");
        if (getIfExists(user) != null)
            throw new Exception("Ошибка добавления страницы " + userId + " в список " + name + ". Страница уже находится в этом списке.");
        if(!list.add(new UserListElement(userId, comment)))
            throw new Exception("Страница " + userId + " почему-то не добавлена в список " + name + ". Сейчас в этом списке " + list.size() + " страниц.");
        save();
    }
    public void rem(long userId) throws Exception{
        UserListElement existing = getIfExists(userId);
        if (userId == -1L)
            throw new Exception("Ошибка удаления страницы " + userId + " из списка " + name + ". Возможно, вы ввели неправильный ID страницы.");
        if (existing == null)
            throw new Exception("Ошибка удаления страницы " + userId + " из списка " + name + ". Страница не находится в этом списке.");
        if(!list.remove(existing))
            throw new Exception("Страница " + userId + " почему-то не удалена из списка " + name + ". Сейчас в этом списке " + list.size() + " страниц.");
        save();
    }
    public ArrayList<UserListElement> getList() {
        return list;
    }
    public void addHardcodeDefined(long id){
        hardcodeDefined.add(id);
    }





    private void load(){
        log(". Загрузка списка " + name + "...");
        String[] array = file.getStringArray("users", new String[]{});
        list.clear();
        for(String s:array){
            try {
                JSONObject jsonObject = new JSONObject(s);
                long id = jsonObject.getLong("id");
                String comment = jsonObject.getString("comment");
                UserListElement entry = new UserListElement(id, comment);
                list.add(entry);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        log(". Список " + name + " загружен.");
    }
    private void save(){
        log(". Сохранение списка " + name + "...");
        ArrayList<String> toWrite = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            try {
                JSONObject jsonObject = list.get(i).toJson();
                toWrite.add(jsonObject.toString());
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        String[] array = F.arrayListToArray(toWrite);
        file.put("users", array);
        file.commit();
        log(". Список " + name + " сохранен.");
    }
    private String getName() {
        return name;
    }
    private String getShortDescription() {
        return shortDescription;
    }
    private String getDescription() {
        return description;
    }

    private UserListElement getIfExists(User user){
        for (int i = 0; i < list.size(); i++) {
            if(list.get(i).equals(user))
                return list.get(i);
        }
        return null;
    }

    private class Get extends CommandModule{
        public Get(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals(name.toLowerCase())) {
                if(commandParser.getWord().toLowerCase().equals("get")) {
                    String result = getShortDescription() + "  (всего " + list.size() + " пользователей): \n";
                    for (UserListElement userListElement:list) {
                        result += userListElement.toString() + "\n";
                    }
                    return result;
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Просмотреть " + getShortDescription(),
                    getDescription(),
                    "botcmd " + name + " get"));
            return result;
        }
    }
    private class Add extends CommandModule{
        public Add(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals(name.toLowerCase())) {
                if(commandParser.getWord().toLowerCase().equals("add")) {
                    String newUser = commandParser.getWord();
                    String comment = commandParser.getText();

                    if(newUser.equals(""))
                        return "Введи ID или screen name пользователя, которого надо добавить в " + getShortDescription();
                    try {
                        add(newUser, comment);
                        return "Пользователь " + newUser + " добавлен в " + getShortDescription() + ".\n" +
                                "Сейчас здесь " + getList().size() + " пользователей.";
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        return e.getMessage();
                    }
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Добавить пользователя в " + getShortDescription(),
                    getDescription(),
                    "botcmd " + getName() + " add <ID или screen name пользователя> <причина внесения в список>"));
            return result;
        }

        private void add(String userId, String comment) throws Exception{
            Long longId = applicationManager.getCommunicator().getActiveAccount().resolveScreenName(userId);
            UserList.this.add(longId, comment);
        }
    }
    private class Rem extends CommandModule{
        public Rem(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals(name.toLowerCase())) {
                if(commandParser.getWord().toLowerCase().equals("add")) {
                    String userToDelete = commandParser.getWord();

                    if(userToDelete.equals(""))
                        return "Введи ID или screen name пользователя, которого надо удалить из " + getShortDescription();
                    try {
                        rem(userToDelete);
                        return "Пользователь " + userToDelete + " удалён из " + getShortDescription() + ".\n" +
                                "Сейчас здесь " + getList().size() + " пользователей.";
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        return e.getMessage();
                    }
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить пользователя из " + getShortDescription(),
                    getDescription(),
                    "botcmd " + getName() + " rem <ID или screen name пользователя>"));
            return result;
        }

        private void rem(String userId) throws Exception{
            Long longId = applicationManager.getCommunicator().getActiveAccount().resolveScreenName(userId);
            UserList.this.rem(longId);
        }

    }
    private class Has extends CommandModule{
        public Has(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals(name.toLowerCase())) {
                if(commandParser.getWord().toLowerCase().equals("has")) {
                    String userToCheck = commandParser.getWord();

                    if(userToCheck.equals(""))
                        return "Введи ID или screen name пользователя, которого надо проверить в " + getShortDescription();
                    try {
                        UserListElement entry = getIfExists(userToCheck);
                        if(entry == null)
                            return "Пользователя " + userToCheck + " нет в списке " + getName() + ".";
                        else
                            return "Пользователь " + userToCheck + " имеется в списке " + getName() + ":\n" +
                                    entry.toString() + "\n" +
                                "Сейчас здесь " + getList().size() + " пользователей.";
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        return e.getMessage();
                    }
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Проверить наличие пользователя в " + getShortDescription(),
                    getDescription(),
                    "botcmd " + getName() + " has <ID или screen name пользователя>"));
            return result;
        }

        private UserListElement getIfExists(String userId) throws Exception{
            Long longId = applicationManager.getCommunicator().getActiveAccount().resolveScreenName(userId);
            if(longId == -1 || longId == 0)
                throw new Exception("Не удалось опознать ID пользователя.");
            return UserList.this.getIfExists(longId);
        }

    }
    private class Clr extends CommandModule{
        public Clr(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().toLowerCase().equals(name.toLowerCase())) {
                if(commandParser.getWord().toLowerCase().equals("clr")) {
                    try {
                        list.clear();
                        save();
                        return getShortDescription() + " очищен. Теперь здесь 0 пользователей";
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        return e.getMessage();
                    }
                }
            }
            return super.processCommand(message);
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Очистить " + getShortDescription(),
                    getDescription(),
                    "botcmd " + getName() + " clr"));
            return result;
        }

    }



    private class UserListElement{
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault());

        private User user;
        private String comment;
        private Date date;

        public UserListElement(User user, String comment) {
            this.user = user;
            this.comment = comment;
            this.date = new Date();
        }
        public UserListElement(User user, String comment, Date date) {
            this.user = user;
            this.comment = comment;
            this.date = date;
        }
        public UserListElement(JSONObject jsonObject) throws JSONException, ParseException {
            fromJson(jsonObject);
        }

        public JSONObject toJson() throws JSONException{
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("id", id);

            if(date != null)
                jsonObject.put("date", sdf.format(date));

            if(comment != null)
                jsonObject.put("comment", comment);
            return jsonObject;
        }
        private void fromJson(JSONObject jsonObject)throws JSONException, ParseException{
            if(jsonObject.has("id"))
                id = new User(jsonObject.getJSONObject("User"));

            if(jsonObject.has("comment"))
                comment = jsonObject.getString("comment");

            if(jsonObject.has("date") && !jsonObject.isNull("date") && !jsonObject.getString("date").equals(""))
                date = sdf.parse(jsonObject.getString("date"));

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserListElement that = (UserListElement) o;

            return getId() == that.getId();

        }
        @Override
        public int hashCode() {
            return (int) (getId() ^ (getId() >>> 32));
        }

        @Override
        public String toString() {
            if (id >= 0)
                return  "http://vk.com/id" + id + "  (" + applicationManager.getCommunicator().getActiveAccount().getUserName(id) + ", " + comment + ", добавлен "+sdf.format(date)+")";
            else
                return "http://vk.com/club" + Math.abs(id) + "  (" + applicationManager.getCommunicator().getActiveAccount().getUserName(id) + ", " + comment + ", добавлен "+sdf.format(date)+")";
        }

        public long getId() {
            return id;
        }
        public void setId(long id) {
            this.id = id;
        }
        public String getComment() {
            return comment;
        }
        public void setComment(String comment) {
            this.comment = comment;
        }
        public Date getDate() {
            return date;
        }
        public void setDate(Date date) {
            this.date = date;
        }
    }
}
