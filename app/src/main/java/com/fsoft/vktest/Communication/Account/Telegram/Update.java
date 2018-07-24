package com.fsoft.vktest.Communication.Account.Telegram;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;

/*
*
* https://core.telegram.org/bots/api#update
*
* Created by Dr. Failov 2018-07-24
* */
public class Update {
    private long update_id = 0;
    private Message message = null;

    public Update(JSONObject jsonObject) throws JSONException, ParseException {
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("update_id", update_id);
        if(message != null)
            jsonObject.put("message", message.toJson());
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        update_id = jsonObject.getLong("update_id");
        if(jsonObject.has("message"))
            message = new Message(jsonObject.getJSONObject("message"));
    }

}
