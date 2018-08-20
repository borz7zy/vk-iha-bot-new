package com.fsoft.vktest.Utils;

/*
* 2018-05-14
* Этот класс представляет пользователя
* в тех местах, где надо понять кто сделал что либо
* например
*
* кто создал этот ответ?
* кто написал это сообщение?
* кто редактировал ответ?
*
*
* Тут должно быть:
* string имя соцсети(vk, telegram)
* string однозначное имя пользователя без всяких там собачек, решеток
* string Удобочитаемое имя
*
*
* {"network":"vk","id":"drfailov","name":"Роман Папуша"}
* */

import org.json.JSONException;
import org.json.JSONObject;

import java.security.cert.Extension;
import java.text.ParseException;
import java.util.Objects;


public class User {
    public static final String NETWORK_TELEGRAM = "tg";
    public static final String NETWORK_VK = "vk";
    public static boolean isValidNetwork(String network){
        return network.equals(NETWORK_TELEGRAM)
                || network.equals(NETWORK_VK);
    }


    private String network = ""; //имя соцсети
    private long id = 0L;        //однозначное имя пользователя без всяких там собачек, решеток
    private String username = "";//текстовое обращение. Может быть не всегда.
    private String name = "";    //Удобочитаемое имя

    public User() {
    }

    public User(String network, long id, String name) {
        this.network = network;
        this.id = id;
        this.name = name;
    }
    public User(String networkAndId)throws Exception{
        parseGlobalId(networkAndId);
    }
    public User(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    @Override
    public String toString() {
        String result = network+":";
        if(username.isEmpty())
            result += id;
        else
            result += username;
        if(!name.isEmpty())
            result += " (" + name+")";
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (o.getClass() == String.class){
            o.equals(network+":"+id);
        }
        User user = (User) o;
        return Objects.equals(network, user.network) &&
                ((Objects.equals(id, user.id) && id != 0L && user.id != 0L) ||
                        (username != null && user.username != null
                                && Objects.equals(username.toLowerCase(), user.username.toLowerCase())
                                && !username.isEmpty() && !user.username.isEmpty()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(network, id, name);
    }

    public User tg(long id){
        network = NETWORK_TELEGRAM;
        this.id = id;
        return this;
    }
    public User vk(long id){
        network = NETWORK_VK;
        this.id = id;
        return this;
    }
    public User tg(long id, String name){
        network = NETWORK_TELEGRAM;
        this.id = id;
        this.name = name;
        return this;
    }
    public User vk(long id, String name){
        network = NETWORK_VK;
        this.id = id;
        this.name = name;
        return this;
    }

    public JSONObject toJson() throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("network", network);
        jsonObject.put("id", id);
        jsonObject.put("username", username);
        jsonObject.put("name", name);
        return jsonObject;
    }
    public void fromJson(JSONObject jsonObject) throws JSONException, ParseException {
        network = jsonObject.optString("network", network);
        id = jsonObject.optLong("id", id);
        name = jsonObject.optString("name", name);
        username = jsonObject.optString("username", username);
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public long getId() {
        return id;
    }
    public User parseGlobalId(String globalId) throws Exception {
        String[] parts = globalId.split(":");
        if(parts.length < 2)
            throw new Exception("Текст не соответствует формату. Формат ID аккаунта должен быть \"network:id\" или \"network:username\".");
        if(!isValidNetwork(parts[0]))
            throw new Exception("Такой network мне неизвестен. Формат ID аккаунта должен быть \"network:id\" или \"network:username\".");
        network = parts[0];
        try{
            id = Long.parseLong(parts[1]);
            if(id == 0)
                throw new Exception("ID=0 невалидный. Формат ID аккаунта должен быть \"network:id\" или \"network:username\".");
        }
        catch (NumberFormatException e){
            username = parts[1].replace("@", "");
        }
        return this;
    }
    public String getGlobalId() {
        if(id == 0)
            return network+":"+username;
        else
            return network+":"+id;
    }

    public void setId(long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
}
