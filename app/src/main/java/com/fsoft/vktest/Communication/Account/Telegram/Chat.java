package com.fsoft.vktest.Communication.Account.Telegram;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

public class Chat {
    private long id = 0;
    private String type = "";
    private String title = "";
    private String username = "";
    private String first_name = "";
    private String last_name = "";
    private String description = "";
    private boolean all_members_are_administrators = false;

    public Chat() {
    }

    public Chat(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    @Override
    public String toString() {
        if(title == null || title.isEmpty())
            return first_name + " " + last_name;
        else
            return title;
    }
    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("is_bot", is_bot);
        if(first_name != null)
            jsonObject.put("first_name", first_name);
        if(last_name != null)
            jsonObject.put("last_name", last_name);
        if(username != null)
            jsonObject.put("username", username);
        if(language_code != null)
            jsonObject.put("language_code", language_code);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        id = jsonObject.getLong("id");
        username = jsonObject.getString("username");
        if(jsonObject.has("is_bot"))
            is_bot = jsonObject.getBoolean("is_bot");
        if(jsonObject.has("first_name"))
            first_name = jsonObject.getString("first_name");
        if(jsonObject.has("last_name"))
            last_name = jsonObject.getString("last_name");
        if(jsonObject.has("language_code"))
            language_code = jsonObject.getString("language_code");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isIs_bot() {
        return is_bot;
    }

    public void setIs_bot(boolean is_bot) {
        this.is_bot = is_bot;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLanguage_code() {
        return language_code;
    }

    public void setLanguage_code(String language_code) {
        this.language_code = language_code;
    }
}
