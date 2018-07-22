package com.fsoft.vktest.Communication;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Utils.CommandParser;
import com.fsoft.vktest.Utils.FileStorage;

import com.fsoft.vktest.Modules.Commands.CommandDesc;

import org.json.JSONArray;

import java.util.ArrayList;

/**
 * Этот класс управляет работой стен. Поскольку каждая стена должна быть автономна, этот класс
 * лишь следит за корректностью их работы и предоставляет возможности по по управлению стенами
 * Created by Dr. Failov on 11.03.2017.
 */
public class WallManager extends CommandModule {
    private Communicator communicator = null;
    private ArrayList<Wall> walls = new ArrayList<>();
    private FileStorage fileStorage = null;
    private boolean running = false;

    public WallManager(Communicator communicator) {
        super(communicator.getApplicationManager());
        this.communicator = communicator;
        this.fileStorage = communicator.getFile();
        childCommands.add(new AddWall(applicationManager));
        childCommands.add(new RemWall(applicationManager));
        childCommands.add(new GetWalls(applicationManager));
        childCommands.add(new ClrWalls(applicationManager));
        try {
            String wallsJsonString = fileStorage.getString("walls", "[]");
            JSONArray wallsJson = new JSONArray(wallsJsonString);
            for (int i=0; i<wallsJson.length(); i++)
                walls.add(new Wall(wallsJson.getLong(i), communicator));
        }
        catch (Exception e){
            e.printStackTrace();
            log("! Ошибка загрузки списка стен во время запуска WallManager: " + e.toString());
        }
    }
    @Override
    public String processCommand(Message message) {
        String result = super.processCommand(message);
        for (Wall wall:walls)
            result += wall.processCommand(message);
        return result;
    }
    @Override
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = super.getHelp();
        for (Wall wall: walls)
            result.addAll(wall.getHelp());
        return result;
    }
    @Override
    public void stop() {
        super.stop();
        stopModule();
    }

    public void startModule(){
        running = true;
        for (Wall wall : walls)
            wall.startWall();
    }
    public void stopModule(){
        running = false;
        for (Wall wall : walls)
            wall.stopWall();
    }
    public String addWall(long id){
        try {
            if(id == 0)
                return log("! Ошибка добавления стены " + id + " потому что с этим номером что-то не так. Проверь правильность написания ID стены.");
            if(hasWall(id))
                return log("! Ошибка добавления стены " + id + " в список стен, поскольку эта стена уже в этом списке есть.");
            Wall wall = new Wall(id, communicator);
            walls.add(wall);
            saveWalls();
            if(running) {
                wall.startWall();
                return log(". Стена " + id + " успешно добавлена в список обрабатываемых и уже запущена. Сейчас в обработке " + walls.size() + " стен.");
            }
            else
                return log(". Стена " + id + " успешно добавлена в список обрабатываемых и будет запущена, когда модуль стен будет запущен. Сейчас в обработке " + walls.size() + " стен.");
        }
        catch (Exception e){
            e.printStackTrace();
            return log("! Ошибка добавления стены "+id+" в список обрабатываемых: " + e.toString() +
                "\nПопробуй проверить сохранность файлов бота.");
        }
    }
    public String remWall(long id){
        try {
            if(id == 0)
                return log("! Ошибка удаления стены " + id + " потому что с этим номером что-то не так. Проверь правильность написания ID стены.");
            if(!hasWall(id))
                return log("! Ошибка удаления стены " + id + ", поскольку этой стены нету в списке обрабатываемых.");
            Wall wall = getWall(id);

            walls.remove(wall);
            if(running)
                wall.stopWall();
            wall.delete();
            saveWalls();
            return log(". Стена " + id + " успешно удалена из списка обрабатываемых, файл с её данными также удалён.");
        }
        catch (Exception e){
            e.printStackTrace();
            return log("! Ошибка удаленя стены "+id+" из списка обрабатываемых: " + e.toString() +
                "\nПопробуй проверить сохранность файлов бота.");
        }
    }
    public Wall getWall(long id){
        for (Wall wall : walls)
            if(wall.getId() == id)
                return wall;
        return null;
    }
    public boolean hasWall(long id){
        return getWall(id) != null;
    }
    public ArrayList<Wall> getWalls() {
        return walls;
    }
    public int getMessagesDetected() {
        int total = 0;
        for (Wall wall : walls)
            total += wall.getPostsDetected();
        return total;
    }
    public int getCommentsDetected() {
        int total = 0;
        for (Wall wall : walls)
            total += wall.getCommentsDetected();
        return total;
    }
    public int getMessagesReplied() {
        int total = 0;
        for (Wall wall : walls)
            total += wall.getMessagesReplied();
        return total;
    }
    private void saveWalls(){
        JSONArray jsonArray = new JSONArray();
        for (int i=0; i<walls.size(); i++)
            jsonArray.put(walls.get(i).getId());
        String wallsJsonString = jsonArray.toString();
        fileStorage.put("walls", wallsJsonString).commit();
    }

    private class AddWall extends CommandModule{
        public AddWall(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override
        public String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getWord().equals("add"))
                    return add(commandParser.getWord());

            return "";
        }
        public String add(long userId){
            log(". Внесение в список стен страницы " + userId + " ...");
            try {
                if (userId == 0L)
                    return "Не могу добавить стену " + userId + " в список стен, поскольку такой стены нет. " +
                            "Проверь правильность написания ID или адреса стены.\n" +
                            "Адрес или ID стены надо писать через пробел после слова add в команде.";
                if (hasWall(userId))
                    return "Стена " + userId + " уже обрабатывается.";
                return addWall(userId);
            }
            catch (Throwable e){
                e.printStackTrace();
                return log("! Не могу добавить стену " + userId + " из-за ошибки: " + e.toString());
            }
        }
        public String add(String userId){
            try{
                Long longId = communicator.getActiveVkAccount().resolveScreenName(userId);
                return add(longId);
            }
            catch (Throwable e){
                e.printStackTrace();
                return log("! Не могу разобрать " + userId + " как имя стены. Проверь правильно ли написан ID стены или адрес стены?\n" + e.toString());
            }
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Добавить стену в список обрабатываемых",
                    "Чтобы бот отвечал на чьей либо стене, либо в группе, её можно добавить в список обрабатываемых.\n" +
                            "Обрати внимание, бот будет отвечать только под несколькими последними записями, а не на всей стене.\n" +
                            "Не стоит добавлять бота в крупные паблики! В пабликах с большой активностью очень часто вылезает капча, " +
                            "которая будет мешать боту работать.\n" +
                            "Никогда не добавляй бота на стены не спросив разрешения у владельца стены или группы! " +
                            "Боты очень часто мешают на стенах, особенно, если бот без обращения!",
                    "botcmd wall add <ссылка или ID>"));
            return result;
        }
    }
    private class RemWall extends CommandModule{
        public RemWall(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override public
        String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getWord().equals("rem"))
                    return rem(commandParser.getWord());

            return "";
        }
        public String rem(String wallName){
            try{
                Long longId = communicator.getActiveVkAccount().resolveScreenName(wallName);
                return rem(longId);
            }
            catch (Throwable e){
                e.printStackTrace();
                return log("! Ошибка разбора текста \"" + wallName+ "\" как имя стены: " + e.toString());
            }
        }
        public String rem(long wallId){
            log(". Удаление из списка стен страницы " + wallId + " ...");
            try {
                if (wallId == 0L)
                    return log("! Имя стены для удаления указано неправильно. Проверь правильность имени стены.\n"+
                            "Адрес или ID стены надо писать через пробел после слова add в команде.");
                if (!hasWall(wallId))
                    return log("! Ты пытаешься удалить стену " + wallId + ", которая сейчас не обрабатывается ботом.");
                return remWall(wallId);
            }
            catch (Throwable e){
                e.printStackTrace();
                return "Ошибка удаления стены " + wallId + " из списка обрабатываемых:\n" + e.toString();
            }
        }

        @Override public
        ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Удалить стену из списка обрабатываемых",
                    "Эта команда позволит убрать бота с одной из стен, на которых он сейчас работает.\n" +
                            "Никогда не добавляй бота на стены не спросив разрешения у владельца стены или группы!",
                    "botcmd wall rem <ID или ссылка на стену>\n\n"));
            return result;
        }
    }
    private class GetWalls extends CommandModule{
        public GetWalls(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override public
        String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getWord().equals("get"))
                {
                    //[link|name] не использовать, потому что владельцу стены прилетает уведомление
                    if(walls.size() == 0)
                        return "В бота не добавлено ни одной стены. \n" +
                                "Чтобы бот работал на стене, используйте команду bcd wall add.";
                    String resut = "Бот сейчас работает на этих стенах: \n";
                    for (int i = 0; i < walls.size(); i++) {
                        Wall wall = walls.get(i);
                        long id = wall.getId();
                        resut += wall.getWallName();
                        if(id >= 0)
                            resut += ", http://vk.com/id" + id + "\n";
                        else
                            resut += ", http://vk.com/club" + Math.abs(id) + "\n";
                    }
                    return resut;
                }

            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Получить список обрабатываемых стен",
                    "Эта команда позволяет посмотреть, на каких стенах отвечает твой бот.\n" +
                            "Обрати внимание, бот отвечает только под несколькими последними записями, а не на всей стене.\n" +
                            "Никогда не добавляй бота на стены не спросив разрешения у владельца стены или группы! ",
                    "botcmd wall get"));
            return result;
        }
    }
    private class ClrWalls extends CommandModule{
        public ClrWalls(ApplicationManager applicationManager) {
            super(applicationManager);
        }

        @Override public
        String processCommand(Message message) {
            CommandParser commandParser = new CommandParser(message.getText());
            if(commandParser.getWord().equals("wall"))
                if(commandParser.getWord().equals("clr"))
                {
                    int old = walls.size();
                    if(old == 0)
                        return log(". Бот и так не работает ни на одной стене.");
                    for (Wall wall : walls)
                        wall.delete();
                    walls.clear();
                    saveWalls();
                    return log(". Бот удалён с " + old + " стен. Больше он не работает ни на одной стене.");
                }
            return "";
        }

        @Override
        public ArrayList<CommandDesc> getHelp() {
            ArrayList<CommandDesc> result = new ArrayList<>();
            result.add(new CommandDesc("Очистить список обрабатываемых стен",
                    "Этой командой можно удалить из бота сразу все стены.",
                    "botcmd wall clr"));
            return result;
        }
    }
}
