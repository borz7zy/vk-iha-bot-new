package com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase;

import com.fsoft.vktest.Utils.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * один элемент неизвестного ответа
 * Created by Dr. Failov on 28.04.2017.
 */
public class UnknownMessage {
    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.US);
    private String text = "";      //Текст вопроса
    private String preparedText = "";//Текст, который используется для быстрого сравнения слов и фраз без предварительной их подготовки
    private String attachments = "";//вложения в вопросе. Пример: PPPM - 3 фото и один трек (PMVDRS = PhotoDocumentVideoMusicRecordSticker)
    private Date date = new Date();//Когда это прислали
    private User author = null;       //Кто отправил
    private float familiarity = 0; //насколько это "знакомый" вопрос. Чем выше число, тем более точный ответ на это сообдение уже имеется
    private int frequency = 1;     //Количество раз сколько этот вопрос задавали

    public UnknownMessage() {
    }
    public UnknownMessage(String text, String attachments, User author, float familiarity) {
        this.text = text;
        this.attachments = attachments;
        this.author = author;
        this.familiarity = familiarity;
    }
    public UnknownMessage(String text, String attachments, Date date, User author, float familiarity) {
        this.text = text;
        this.attachments = attachments;
        this.date = date;
        this.author = author;
        this.familiarity = familiarity;
    }
    public UnknownMessage(String text, ArrayList<Attachment> attachments, Date date, User author, float familiarity) {
        this.text = text;
        this.attachments = "";
        if(attachments != null) {
            for (Attachment attachment : attachments) {
                switch (attachment.getType()) {
                    case "photo":
                        this.attachments += "P";
                        break;
                    case "video":
                        this.attachments += "V";
                        break;
                    case "audio":
                        this.attachments += "M";
                        break;
                    case "doc":
                        this.attachments += "D";
                        break;
                    case "record":
                        this.attachments += "R";
                        break;
                    case "sticker":
                        this.attachments += "S";
                        break;
                }
            }
        }
        this.date = date;
        this.author = author;
        this.familiarity = familiarity;
    }
    public UnknownMessage(String text, User author) {
        this.text = text;
        this.author = author;
    }
    public UnknownMessage(JSONObject jsonObject) throws Exception{
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("text", text);
        jsonObject.put("attachments", attachments);
        jsonObject.put("date", sdf.format(date));
        jsonObject.put("author", author.toJson());
        jsonObject.put("familiarity", familiarity);
        jsonObject.put("frequency", frequency);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject) throws ParseException, JSONException{
        text = jsonObject.optString("text", text);
        attachments = jsonObject.optString("attachments", attachments);
        date = sdf.parse(jsonObject.optString("date"));
        author = new User(jsonObject.optJSONObject("author"));
        familiarity = (float)jsonObject.optDouble("familiarity", familiarity);
        frequency = jsonObject.optInt("frequency", frequency);
    }

    @Override
    public String toString() {
        return text + " " + attachments +
                " (спросил https:\\vk.com\\id"+author+", " +
                "ответ известен на " + familiarity*100 + "%, " +
                "спрашивали "+frequency+"раз)";
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public User getAuthor() {
        return author;
    }
    public void setAuthor(User author) {
        this.author = author;
    }
    public float getFamiliarity() {
        return familiarity;
    }
    public void setFamiliarity(float familiarity) {
        this.familiarity = familiarity;
    }
    public int getFrequency() {
        return frequency;
    }
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
    public String getPreparedText() {
        return preparedText;
    }
    public void setPreparedText(String preparedText) {
        this.preparedText = preparedText;
    }
    public String getAttachments() {
        return attachments;
    }
    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }
}
