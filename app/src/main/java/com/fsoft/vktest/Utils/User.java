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
import java.text.ParseException;
import java.util.Objects;


public class User {
    public static final String NETWORK_TELEGRAM = "telegram";
    public static final String NETWORK_VK = "vk";


    private String network = ""; //имя соцсети
    private long id = 0L;        //однозначное имя пользователя без всяких там собачек, решеток
    private String name = "";    //Удобочитаемое имя

    public User() {
    }

    public User(String network, long id, String name) {
        this.network = network;
        this.id = id;
        this.name = name;
    }
    public User(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    @Override
    public String toString() {
        return getGlobalId();
        //return name + "("+network+", "+id+")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(network, user.network) &&
                Objects.equals(id, user.id) &&
                Objects.equals(name, user.name);
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
        jsonObject.put("name", name);
        return jsonObject;
    }
    public void fromJson(JSONObject jsonObject) throws JSONException, ParseException {
        network = jsonObject.optString("network", network);
        id = jsonObject.optLong("id", id);
        name = jsonObject.optString("name", name);
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
    public String getGlobalId() {
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
}
