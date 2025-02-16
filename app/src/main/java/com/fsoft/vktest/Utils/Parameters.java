package com.fsoft.vktest.Utils;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.Modules.CommandModule;
import com.fsoft.vktest.Modules.Commands.CommandDesc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import android.content.Context;

/**
 * Класс, который занимается хранением различных параметров программы, которые может менять пользователь.
 * Список парамеров формируется автоматически основываясь на заросах значений параметров из программы
 * Таким образом, чтобы добавить новый параметр не нужно редактировать никаких файлов, достаточно
 * запросить этот параметр и программа сама его впишет в список
 *
 * Created by Dr. Failov on 06.08.2016.
 */
public class Parameters extends CommandModule{
    private FileStorage storage = null;
    private ArrayList<Parameter> parameters = null;
//    private Context context; // Add Context

    public Parameters(ApplicationManager applicationManager){
        super(applicationManager);
        if(parameters == null){
            try{
                storage = new FileStorage("Parameters", applicationManager);
                parameters = new ArrayList<>();
                String data = storage.getString("data", "[]");
                JSONArray jsonArray = new JSONArray(data);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    parameters.add(new Parameter(jsonObject));
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public int get(String name, int def, String shortDescription, String description){
        Parameter ethalon = new Parameter(name, shortDescription, description, def, def);
        if(parameters.contains(ethalon)){
            Parameter parameter = parameters.get(parameters.indexOf(ethalon));
            return parameter.getIntValue();
        }
        else {
            parameters.add(ethalon);
            save();
            return def;
        }
    }
    public double get(String name, double def, String shortDescription, String description){
        Parameter ethalon = new Parameter(name, shortDescription, description, def, def);
        if(parameters.contains(ethalon)){
            Parameter parameter = parameters.get(parameters.indexOf(ethalon));
            return parameter.getDoubleValue();
        }
        else {
            parameters.add(ethalon);
            save();
            return def;
        }
    }
    public String get(String name, String def, String shortDescription, String description){
        Parameter ethalon = new Parameter(name, shortDescription, description, def, def);
        if(parameters.contains(ethalon)){
            Parameter parameter = parameters.get(parameters.indexOf(ethalon));
            return parameter.getStringValue();
        }
        else {
            parameters.add(ethalon);
            save();
            return def;
        }
    }
    public boolean get(String name, boolean def, String shortDescription, String description){
        Parameter ethalon = new Parameter(name, shortDescription, description, def, def);
        if(parameters.contains(ethalon)){
            Parameter parameter = parameters.get(parameters.indexOf(ethalon));
            return parameter.isBoolValue();
        }
        else {
            parameters.add(ethalon);
            save();
            return def;
        }
    }
    public String[] get(String name, String[] def, String shortDescription, String description){
        Parameter ethalon = new Parameter(name, shortDescription, description, def, def);
        if(parameters.contains(ethalon)){
            Parameter parameter = parameters.get(parameters.indexOf(ethalon));
            return parameter.getStringArrayValue();
        }
        else {
            parameters.add(ethalon);
            save();
            return def;
        }
    }

    private void save(){
        if(parameters != null){
            try{
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < parameters.size(); i++)
                    jsonArray.put(parameters.get(i).toJSON());
                String data = jsonArray.toString();
                storage.put("data", data);
                storage.commit();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    private Parameter getByName(String name){
        //case insensitive
        for (Parameter parameter:parameters)
            if(parameter.getName().equalsIgnoreCase(name))
                return parameter;
        return null;
    }
    private ArrayList<Parameter> simpleSearch(String query){
        if(query.equals(""))
            return parameters;
        ArrayList<Parameter> result = new ArrayList<>();
        query = query.toLowerCase();
        for (Parameter parameter:parameters){
            if(parameter.getName().toLowerCase().contains(query)
                    || parameter.getShortDescription().toLowerCase().contains(query)
                    || parameter.getDescription().toLowerCase().contains(query))
                result.add(parameter);
        }
        return result;
    }
    private ArrayList<Parameter> intersect(ArrayList<Parameter> array1, ArrayList<Parameter> array2){
        ArrayList<Object> objArray1 = new ArrayList<>();
        ArrayList<Object> objArray2 = new ArrayList<>();
        objArray1.addAll(array1);
        objArray2.addAll(array2);

        ArrayList<Object> objResult = F.intersect(objArray1, objArray2);

        ArrayList<Parameter> result = new ArrayList<>();
        for (Object object:objResult)
            if(object instanceof Parameter)
                result.add((Parameter) object);
        return result;
    }
    private ArrayList<Parameter> complexSearch(String query){
        //сложная для понимания хуйня.
        //Эта функция находит параметры, в которых встречаются все слова из поискового запроса в любых комбинациях
        //Сначала запрос разбивается на слова, после чего выполняется простой поиск по параметрам для каждого из слов.
        //Резуотиаиты поиска по словам объединяются в общий массив при помощи функции intersect
        String[] words = query.split(" ");
        if(words.length == 0)
            return parameters;
        ArrayList<Parameter> result = simpleSearch(words[0]);
        for (int i = 1; i < words.length; i++)
            result = intersect(result, simpleSearch(words[i]));
        return result;
    }
    private ArrayList<Parameter> search(String query){
        //Выполняется для начала простой поиск. Если он ничего не находит - выполняется сложный поиск
        ArrayList<Parameter> result = simpleSearch(query);
        if(result.isEmpty())
            result = complexSearch(query);
        return result;
    }

    @Override
    public String processCommand(Message message) {
        CommandParser commandParser  = new CommandParser(message.getText());
        String word = commandParser.getWord();
        //-------------- PARAMETER LIST
        if(word.toLowerCase().equals("parameterlist")){
            String query = commandParser.getText();
            ArrayList<Parameter> results = parameters;
            if(!query.equals(""))
                results = search(query);
            String result = "Список всех параметров: \n\n";
            if(!query.equals(""))
                result = "Параметры по запросу \""+query+"\": \n\n";
            for (int i = 0; i < results.size(); i++)
                result += results.get(i).toString();
            result += "Всего параметров: " + results.size() + "\n";
            return result;
        }

        //-------------- PARAMETER RESET
        if(word.toLowerCase().equals("parameterreset")){
            String name = commandParser.getWord();
            Parameter parameter = getByName(name);
            if(parameter == null)
                return "Параметра с именем " + name + " нет.";
            parameter.reset();
            save();
            return "" + parameter.getShortDescription() + " сброшен к стандартному значению: " + parameter.getValueAsString();
        }

        //-------------- PARAMETER SET
        if(word.toLowerCase().equals("parameterset")){
            String name = commandParser.getWord();
            Parameter parameter = getByName(name);
            if(parameter == null)
                return "Параметра с именем " + name + " нет.\n" +
                        "Некоторые параметры становятся доступными для редактирования только после первого обращения программы к ним.\n" +
                        "Попробуй обратиться к функции, которую этот параметр обслуживает, чтобы он появился.";

            if(parameter.getType().equals(Parameter.TYPE_BOOL))
                parameter.setBoolValue(commandParser.getBoolean());
            else if(parameter.getType().equals(Parameter.TYPE_INT))
                parameter.setIntValue(commandParser.getInt());
            else if(parameter.getType().equals(Parameter.TYPE_DOUBLE))
                parameter.setDoubleValue(commandParser.getDouble());
            else if(parameter.getType().equals(Parameter.TYPE_STRING))
                parameter.setStringValue(commandParser.getText());
            else
                return "Эта команда не применяется для параметров типа " + parameter.getType() + ".";

            save();
            return parameter.getShortDescription() + " теперь " + parameter.getValueAsString() + ".";
        }

        //-------------- PARAMETER ... LIST || ADD || REM
        if(word.toLowerCase().equals("parameter")){
            String name = commandParser.getWord();
            Parameter parameter = getByName(name);
            if(parameter == null)
                return "Параметра с именем " + name + " нет.\n" +
                        "Некоторые параметры становятся доступными для редактирования только после первого обращения программы к ним.\n" +
                        "Попробуй обратиться к функции, которую этот параметр обслуживает, чтобы он появился.";

            if(!parameter.getType().equals(Parameter.TYPE_STRING_ARRAY))
                return "Эта команда не применяется для параметров типа " + parameter.getType() + ".";

            String cmd = commandParser.getWord().toLowerCase();
            if(cmd.equals(""))
                return "Введите команду для параметра " + parameter.getShortDescription() + ": ADD, REM или LIST.";

            if(cmd.equals("add")){
                String text = commandParser.getText();
                if(text.equals(""))
                    return "Введите текст для добавления в " + parameter.getShortDescription() + ".";
                parameter.addStringArrayValue(text);
                save();
                return "Новый элемент добавлен в " + parameter.getShortDescription() + ".\n" + parameter.getValueAsString();
            }


            else if(cmd.equals("rem")){
                String text = commandParser.getText();
                if(text.equals(""))
                    return "Введите текст для удаления из " + parameter.getShortDescription() + ".";
                if(!parameter.remStringArrayValue(text))
                    return "Элемента \"" + text + " нет в массиве " + parameter.getName() +
                            ".\n Вот что там есть:\n" + parameter.getValueAsString();
                save();
                return "Элемент удалён из " + parameter.getShortDescription() + ".\n" + parameter.getValueAsString();
            }


            else if(cmd.equals("list")){
                String result = "Ниже представлен " + parameter.getShortDescription() + ":\n";
                result += parameter.getValueAsString();

                result += "\n\nЗначение по умолчанию:\n";
                result += parameter.getDefaultValueAsString();

                result += "\n\nПолное описание параметра:\n";
                result += parameter.getDescription();

                result += "\n\nИмя параметра:\n";
                result += parameter.getName();

                result += "\n\nТип параметра:\n";
                result += parameter.getType();
                return result;
            }
            else {
                return "Такой команды нет. Для списков есть только ADD, REM, LIST.";
            }
        }

        return "";
    }
    public ArrayList<CommandDesc> getHelp() {
        ArrayList<CommandDesc> result = new ArrayList<>();
        //+
        result.add(new CommandDesc("Вывести список изменяемых параметров",
                "Многие настройки в программе сохранены в виде изменяемых параметров. " +
                        "Эта команда выведет полный список параметров с их описаниями.\n" +
                        "Также можно добавить текстовый запрос для поиска по параметрам (необязательно).\n" +
                        "Поиск работает даже если слова из запроса встречаются в другой последовательности в результатах поиска.",
                "botcmd ParameterList <запрос для поиска>"));

        for (Parameter parameter:parameters) {
            if(parameter.type.equals(Parameter.TYPE_STRING_ARRAY)){
                //+
                result.add(new CommandDesc("Показать " + parameter.getShortDescription(),
                        parameter.getDescription() + "\n",
                        "botcmd parameter " + parameter.getName() + " list"));

                //+
                result.add(new CommandDesc("Добавить элемент в " + parameter.getShortDescription(),
                        parameter.getDescription() + "\n" +
                                "Текущее значение: " + parameter.getValueAsString() + "\n" +
                                "Стандартное значение: " + parameter.getDefaultValueAsString() + "\n",
                        "botcmd parameter " + parameter.getName() + " add <новое значение типа STRING>"));

                //+
                result.add(new CommandDesc("Удалить элемент из " + parameter.getShortDescription(),
                        parameter.getDescription() + "\n" +
                                "Текущее значение: " + parameter.getValueAsString() + "\n" +
                                "Стандартное значение: " + parameter.getDefaultValueAsString() + "\n",
                        "botcmd parameter " + parameter.getName() + " rem <имеющееся в массиве значение типа STRING>"));
            }
            else {
                //+
                result.add(new CommandDesc("Задать " + parameter.getShortDescription(),
                        parameter.getDescription() + "\n" +
                                "Текущее значение: " + parameter.getValueAsString() + "\n" +
                                "Стандартное значение: " + parameter.getDefaultValueAsString() + "\n",
                        "botcmd ParameterSet " + parameter.getName() + " <новое значение типа " + parameter.getType() + ">"));
            }

            //+
            result.add(new CommandDesc("Сбросить " + parameter.getName(),
                    parameter.getDescription() + "\n" +
                            "Если функция работает неправильно после изменения параметра, можно восстановить его стандартное значение.\n" +
                            "Текущее значение: " + parameter.getValueAsString() + "\n" +
                            "Стандартное значение: " + parameter.getDefaultValueAsString() + "\n",
                    "botcmd ParameterReset " + parameter.getName()));
        }
        return result;
    }

    private class Parameter{
        static final String TYPE_INT = "INT";
        static final String TYPE_DOUBLE = "DOUBLE";
        static final String TYPE_BOOL = "BOOL";
        static final String TYPE_STRING = "STRING";
        static final String TYPE_STRING_ARRAY = "STRING_ARRAY";

        private String name = ""; //DEFAULT_USER_SURNAME
        private String shortDescription = ""; //Стандартная фамилия пользователя
        private String description = ""; //Определяет, с какой фамилией будут создаваться новые пользователи
        private String type = TYPE_STRING;

        private int intValue = 0;           //Текущее значение параметра.
        private double doubleValue = 0;     // В зависимости от типа заполняется одна из этих переменных
        private String stringValue = "";    //может быть изменено пользователем
        private boolean boolValue = false;
        private String[] stringArrayValue = new String[0];

        private int intDefaultValue = 0;   //Стандартное значение параметра.
        private double doubleDefaultValue = 0;  //В зависимости от типа заполняется одна из этих переменных
        private String stringDefaultValue = ""; //заполняется один раз - при создании параметра
        private boolean boolDefaultValue = false;
        private String[] stringArrayDefaultValue = new String[0];

        public Parameter(String name) {
            this.name = name;
        }
        public Parameter(String name, String shortDescription, String description, int intValue, int intDefaultValue) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.description = description;
            this.intValue = intValue;
            this.intDefaultValue = intDefaultValue;
            type = TYPE_INT;
        }
        public Parameter(String name, String shortDescription, String description, double doubleValue, double doubleDefaultValue) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.description = description;
            this.doubleValue = doubleValue;
            this.doubleDefaultValue = doubleDefaultValue;
            type = TYPE_DOUBLE;
        }
        public Parameter(String name, String shortDescription, String description, String stringValue, String stringDefaultValue) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.description = description;
            this.stringValue = stringValue;
            this.stringDefaultValue = stringDefaultValue;
            type = TYPE_STRING;
        }
        public Parameter(String name, String shortDescription, String description, boolean boolValue, boolean boolDefaultValue) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.description = description;
            this.boolValue = boolValue;
            this.boolDefaultValue = boolDefaultValue;
            type = TYPE_BOOL;
        }
        public Parameter(String name, String shortDescription, String description, String[] stringArrayValue, String[] stringArrayDefaultValue) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.description = description;
            this.stringArrayValue = stringArrayValue;
            this.stringArrayDefaultValue = stringArrayDefaultValue;
            type = TYPE_STRING_ARRAY;
        }

        public Parameter(JSONObject jsonObject) throws Exception{
            name = jsonObject.getString("name");
            description  = jsonObject.getString("description");
            shortDescription  = jsonObject.getString("shortDescription");
            type  = jsonObject.getString("type");
            if(type.equals(TYPE_STRING)){
                stringValue = jsonObject.getString("value");
                stringDefaultValue = jsonObject.getString("default_value");
            }
            if(type.equals(TYPE_INT)){
                intValue = jsonObject.getInt("value");
                intDefaultValue = jsonObject.getInt("default_value");
            }
            if(type.equals(TYPE_DOUBLE)){
                doubleValue = jsonObject.getDouble("value");
                doubleDefaultValue = jsonObject.getDouble("default_value");
            }
            if(type.equals(TYPE_BOOL)){
                boolValue = jsonObject.getBoolean("value");
                boolDefaultValue = jsonObject.getBoolean("default_value");
            }
            if(type.equals(TYPE_STRING_ARRAY)){
                JSONArray jsonArrayValues = jsonObject.getJSONArray("value");
                JSONArray jsonArrayDefaultValues = jsonObject.getJSONArray("default_value");

                stringArrayValue = new String[jsonArrayValues.length()];
                stringArrayDefaultValue = new String[jsonArrayDefaultValues.length()];

                for (int i = 0; i < jsonArrayValues.length(); i++)
                    stringArrayValue[i] = jsonArrayValues.getString(i);

                for (int i = 0; i < jsonArrayDefaultValues.length(); i++)
                    stringArrayDefaultValue[i] = jsonArrayDefaultValues.getString(i);
            }
        }
        public JSONObject toJSON() throws Exception{
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("name", name);
            jsonObject.put("description", description);
            jsonObject.put("shortDescription", shortDescription);
            jsonObject.put("type", type);

            if(type.equals(TYPE_STRING)){
                jsonObject.put("value", stringValue);
                jsonObject.put("default_value", stringDefaultValue);
            }
            if(type.equals(TYPE_INT)){
                jsonObject.put("value", intValue);
                jsonObject.put("default_value", intDefaultValue);
            }
            if(type.equals(TYPE_DOUBLE)){
                jsonObject.put("value", doubleValue);
                jsonObject.put("default_value", doubleDefaultValue);
            }
            if(type.equals(TYPE_BOOL)){
                jsonObject.put("value", boolValue);
                jsonObject.put("default_value", boolDefaultValue);
            }
            if(type.equals(TYPE_STRING_ARRAY)){
                JSONArray valueArray = new JSONArray();
                for(String value:stringArrayValue)
                    valueArray.put(value);

                JSONArray defaultValueArray = new JSONArray();
                for(String value:stringArrayDefaultValue)
                    defaultValueArray.put(value);

                jsonObject.put("value", valueArray);
                jsonObject.put("default_value", defaultValueArray);
            }
            return jsonObject;
        }



        public String getValueAsString(){
            if(type.equals(TYPE_BOOL))
                return String.valueOf(boolValue);
            if(type.equals(TYPE_DOUBLE))
                return String.valueOf(doubleValue);
            if(type.equals(TYPE_INT))
                return String.valueOf(intValue);
            if(type.equals(TYPE_STRING))
                return stringValue;
            if(type.equals(TYPE_STRING_ARRAY))
                return F.arrayToStringMultipleLines(getStringArrayValue());
            return "NO_TYPE";
        }
        public String getDefaultValueAsString(){
            if(type.equals(TYPE_BOOL))
                return String.valueOf(boolDefaultValue);
            if(type.equals(TYPE_DOUBLE))
                return String.valueOf(doubleDefaultValue);
            if(type.equals(TYPE_INT))
                return String.valueOf(intDefaultValue);
            if(type.equals(TYPE_STRING))
                return stringDefaultValue;
            if(type.equals(TYPE_STRING_ARRAY))
                return F.arrayToString(getStringArrayDefaultValue());
            return "NO_TYPE";
        }
        public String getName() {
            return name;
        }
        public String getDescription() {
            return description;
        }
        public String getType() {
            return type;
        }
        public int getIntValue() {
            return intValue;
        }
        public double getDoubleValue() {
            return doubleValue;
        }
        public String getStringValue() {
            return stringValue;
        }
        public boolean isBoolValue() {
            return boolValue;
        }
        public String getShortDescription() {
            return shortDescription;
        }
        public String[] getStringArrayValue() {
            return stringArrayValue;
        }
        public void addStringArrayValue(String value) {
            String[] newArray = new String[stringArrayValue.length + 1];
            for (int i = 0; i < stringArrayValue.length; i++)
                newArray[i] = stringArrayValue[i];
            newArray[stringArrayValue.length] = value;
            stringArrayValue = newArray;
        }
        public boolean remStringArrayValue(String value){
            if(!F.isArrayContains(stringArrayValue, value))
                return false;
            String[] newArray = new String[stringArrayValue.length - 1];
            int indexToWrite = 0;
            for (int i = 0; i < stringArrayValue.length; i++){
                if(!stringArrayValue[i].equals(value)){
                    newArray[indexToWrite] = stringArrayValue[i];
                    indexToWrite ++;
                    if(indexToWrite >= newArray.length)
                        break;
                }
            }
            newArray = F.trimArray(newArray);
            stringArrayValue = newArray;
            return true;
        }
        public String[] getStringArrayDefaultValue() {
            return stringArrayDefaultValue;
        }
        public int getIntDefaultValue() {
            return intDefaultValue;
        }
        public double getDoubleDefaultValue() {
            return doubleDefaultValue;
        }
        public String getStringDefaultValue() {
            return stringDefaultValue;
        }
        public boolean isBoolDefaultValue() {
            return boolDefaultValue;
        }


        @Override
        public boolean equals(Object o) {
            return o.getClass() == this.getClass() && name.equals(((Parameter)o).name);
        }
        public void reset(){
            stringArrayValue = stringArrayDefaultValue;
            stringValue = stringDefaultValue;
            intValue = intDefaultValue;
            boolValue = boolDefaultValue;
            doubleValue = doubleDefaultValue;
        }

        public void setName(String name) {
            this.name = name;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public void setType(String type) {
            this.type = type;
        }
        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }
        public void setDoubleValue(double doubleValue) {
            this.doubleValue = doubleValue;
        }
        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }
        public void setBoolValue(boolean boolValue) {
            this.boolValue = boolValue;
        }
        public void setIntDefaultValue(int intDefaultValue) {
            this.intDefaultValue = intDefaultValue;
        }
        public void setDoubleDefaultValue(double doubleDefaultValue) {
            this.doubleDefaultValue = doubleDefaultValue;
        }
        public void setStringDefaultValue(String stringDefaultValue) {
            this.stringDefaultValue = stringDefaultValue;
        }
        public void setBoolDefaultValue(boolean boolDefaultValue) {
            this.boolDefaultValue = boolDefaultValue;
        }
        public void setShortDescription(String shortDescription) {
            this.shortDescription = shortDescription;
        }
        public void setStringArrayValue(String[] stringArrayValue) {
            this.stringArrayValue = stringArrayValue;
        }
        public void setStringArrayDefaultValue(String[] stringArrayDefaultValue) {
            this.stringArrayDefaultValue = stringArrayDefaultValue;
        }

        @Override
        public String toString() {
            return "//" + description + "\n" + type + " " + name + " = " + getValueAsString() + "; //def = " + getDefaultValueAsString() + "\n\n";
        }
    }
}