package com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase;

import android.app.Activity;

import com.fsoft.vktest.AnswerInfrastructure.Message;
import com.perm.kate.api.Attachment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Этот класс представляет полноценный обьект в базе используемый для поиска и редактирования
 * Для подбора ответа этот обьект не используется
 * Created by Dr. Failov on 21.04.2017.
 */
public class AnswerElement{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm", Locale.getDefault());
    public static final char GENDER_MALE = 'М';
    public static final char GENDER_FEMALE = 'Ж';
    public static final char GENDER_NEUTRAL = 'Н';
    public static final char GENDER_UNDEFINED = '-';

    /** Какая инфа про ответ должна хранится:
     * + ID ответа (id, long. В случае коллизий генерировать новые)
     * + дата когда кто-то впервые задал этот вопрос
     * + дата внесения ответа в базу (created_date, yyyy-MM-dd_HH:mm)
     * + дата редактирования (edited_date, yyyy-MM-dd_HH:mm)
     * + автор вопроса (кто спросил об этом бота первым, что оно попало в неизвестные)
     * + автор внесения ответа (created_author, vk ID)
     * + автор редактирования ответа (edited_author, vk ID)
     * + текст вопроса (question_text, текст экранирован по правилам JSON)
     * + текст ответа (answer_text, текст экранирован по правилам JSON)
     * + список вложений в ответе (answer_attachments, массив JsonObject's)
     * + набор вложений в вопросе (question_attachments, 1P0V0D0M)
     * + пол бота (bot_gender, М\Ж\-)
     * + пол собеседника (user_gender, М\Ж\-)
     * */
    private long id = 0;
    private Date questionDate = null;
    private Date createdDate = null;
    private Date editedDate = null;
    private long questionAuthor = 0;
    private long createdAuthor = 0;
    private long editedAuthor = 0;
    private String questionText = null;
    private String answerText = null;
    private String questionAttachments = "";//ex. PPPM - 3 фото и один трек (PMVDRS = PhotoDocumentVideoMusicRecordSticker)
    private ArrayList<Attachment> answerAttachments = new ArrayList<>();
    private char botGender = '-'; //М\Ж\Н\-    Мужской\Женский\Нейтральный\Не указан
    private char userGender = '-'; //М\Ж\Н\-
    private int timesUsed = 0; //Количество раз, сколько раз бот использовал этот ответ

    public AnswerElement(long id, Date questionDate, Date createdDate, Date editedDate, long questionAuthor, long createdAuthor, long editedAuthor, String questionText, String answerText, String questionAttachments, ArrayList<Attachment> answerAttachments, char botGender, char userGender, int timesUsed) {
        this.id = id;
        this.questionDate = questionDate;
        this.createdDate = createdDate;
        this.editedDate = editedDate;
        this.questionAuthor = questionAuthor;
        this.createdAuthor = createdAuthor;
        this.editedAuthor = editedAuthor;
        this.questionText = questionText;
        this.answerText = answerText;
        this.questionAttachments = questionAttachments;
        this.answerAttachments = answerAttachments;
        this.botGender = botGender;
        this.userGender = userGender;
        this.timesUsed = timesUsed;
    }

    public AnswerElement(long createdAuthor, String questionText, String answerText) {
        this.createdAuthor = createdAuthor;
        this.questionText = questionText;
        this.answerText = answerText;
        createdDate = new Date();
    }
    public AnswerElement(UnknownMessage unknownMessage, Message answer){
        questionDate = unknownMessage.getDate();
        createdDate = new Date();
        editedDate = new Date();
        questionAuthor = unknownMessage.getAuthor();
        createdAuthor = answer.getAuthor();
        editedAuthor = answer.getAuthor();
        questionText = unknownMessage.getText();
        answerText = answer.getText();
        questionAttachments = unknownMessage.getAttachments();
        answerAttachments = answer.getAttachments();
        botGender = '-';
        userGender = '-';
        timesUsed = 0;
    }
    public AnswerElement(JSONObject jsonObject) throws JSONException, ParseException{
        fromJson(jsonObject);
    }

    public JSONObject toJson() throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        if(questionDate != null)
            jsonObject.put("questionDate", sdf.format(questionDate));
        if(createdDate != null)
            jsonObject.put("createdDate", sdf.format(createdDate));
        if(editedDate != null)
            jsonObject.put("editedDate", sdf.format(editedDate));
        jsonObject.put("questionAuthor", questionAuthor);
        jsonObject.put("createdAuthor", createdAuthor);
        jsonObject.put("editedAuthor", editedAuthor);
        if(questionText != null)
            jsonObject.put("questionText", questionText);
        if(answerText != null)
            jsonObject.put("answerText", answerText);
        if(questionAttachments != null)
            jsonObject.put("questionAttachments", questionAttachments);
        if(!answerAttachments.isEmpty()){
            JSONArray jsonArray = new JSONArray();
            for(Attachment attachment:answerAttachments)
                jsonArray.put(attachment.toJson());
            jsonObject.put("answerAttachments", jsonArray);
        }
        jsonObject.put("botGender", (int)botGender);
        jsonObject.put("userGender", (int)userGender);
        jsonObject.put("timesUsed", timesUsed);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException{
        if(jsonObject.has("id"))
            id = jsonObject.getLong("id");
        if(jsonObject.has("questionDate"))
            questionDate = sdf.parse(jsonObject.getString("questionDate"));
        if(jsonObject.has("createdDate"))
            createdDate = sdf.parse(jsonObject.getString("createdDate"));
        if(jsonObject.has("editedDate"))
            editedDate = sdf.parse(jsonObject.getString("editedDate"));
        if(jsonObject.has("questionAuthor"))
            questionAuthor = jsonObject.getLong("questionAuthor");
        if(jsonObject.has("createdAuthor"))
            createdAuthor = jsonObject.getLong("createdAuthor");
        if(jsonObject.has("editedAuthor"))
            editedAuthor = jsonObject.getLong("editedAuthor");
        if(jsonObject.has("questionText"))
            questionText = jsonObject.getString("questionText");
        if(jsonObject.has("answerText"))
            answerText = jsonObject.getString("answerText");
        if(jsonObject.has("questionAttachments"))
            questionAttachments = jsonObject.getString("questionAttachments");
        if(jsonObject.has("botGender")) //поскльку в json char не работает, добавляем int
            botGender = (char)jsonObject.getInt("botGender");
        if(jsonObject.has("userGender"))
            userGender = (char)jsonObject.getInt("userGender");
        if(jsonObject.has("timesUsed"))
            timesUsed = jsonObject.getInt("timesUsed");
        if(jsonObject.has("answerAttachments")) {
            JSONArray jsonArray = jsonObject.getJSONArray("answerAttachments");
            answerAttachments.addAll(Attachment.parseAttachments(jsonArray, 0, 0, null));
        }
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnswerElement that = (AnswerElement) o;

        if (getId() != that.getId()) return false;
        if (getCreatedAuthor() != that.getCreatedAuthor()) return false;
        if (getCreatedAuthor() != that.getCreatedAuthor()) return false;
        if (getEditedAuthor() != that.getEditedAuthor()) return false;
        if (getBotGender() != that.getBotGender()) return false;
        if (getUserGender() != that.getUserGender()) return false;
        if (getCreatedDate() != null ? !getCreatedDate().equals(that.getCreatedDate()) : that.getCreatedDate() != null)
            return false;
        if (getEditedDate() != null ? !getEditedDate().equals(that.getEditedDate()) : that.getEditedDate() != null)
            return false;
        if (getQuestionText() != null ? !getQuestionText().equals(that.getQuestionText()) : that.getQuestionText() != null)
            return false;
        if (getAnswerText() != null ? !getAnswerText().equals(that.getAnswerText()) : that.getAnswerText() != null)
            return false;
        if (getQuestionAttachments() != null ? !getQuestionAttachments().equals(that.getQuestionAttachments()) : that.getQuestionAttachments() != null)
            return false;
        return getAnswerAttachments() != null ? getAnswerAttachments().equals(that.getAnswerAttachments()) : that.getAnswerAttachments() == null;

    }
    @Override
    public int hashCode() {
        int result = (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + (getCreatedDate() != null ? getCreatedDate().hashCode() : 0);
        result = 31 * result + (getEditedDate() != null ? getEditedDate().hashCode() : 0);
        result = 31 * result + (int) (getCreatedAuthor() ^ (getCreatedAuthor() >>> 32));
        result = 31 * result + (int) (getEditedAuthor() ^ (getEditedAuthor() >>> 32));
        result = 31 * result + (getQuestionText() != null ? getQuestionText().hashCode() : 0);
        result = 31 * result + (getAnswerText() != null ? getAnswerText().hashCode() : 0);
        result = 31 * result + (getQuestionAttachments() != null ? getQuestionAttachments().hashCode() : 0);
        result = 31 * result + (getAnswerAttachments() != null ? getAnswerAttachments().hashCode() : 0);
        result = 31 * result + (int) getBotGender();
        result = 31 * result + (int) getUserGender();
        return result;
    }
    @Override
    public String toString() {
        return "AnswerElement{" +
                "\nID ответа: " + id +
                "\nДата задания вопроса: " + (questionDate == null?"не указана":sdf.format(questionDate)) +
                "\nДата создания: " + (createdDate == null?"не указана":sdf.format(createdDate)) +
                "\nДата редактирования: " + (editedDate == null?"не редактировалось":sdf.format(editedDate)) +
                "\nАвтор вопроса: " + (questionAuthor == 0?"не указан":("http://vk.com/id" + questionAuthor)) +
                "\nАвтор ответа: " + (createdAuthor == 0?"не указан":("http://vk.com/id" + createdAuthor)) +
                "\nАвтор редактирования: " + (editedAuthor == 0?"не редактировалось":("http://vk.com/id" + editedAuthor)) +
                "\nТекст вопроса: " + questionText +
                "\nТекст ответа: " + answerText +
                "\nВложения в вопросе: " + questionAttachments +
                (questionAttachments.equals("")?"":"\n(P-Фото, M-Музыка, V-Видео, D-Документ, R-Запись, S-Стикер)") +
                "\nВложения в ответе: " + (answerAttachments.size() == 0?"нет вложений":getAnswerAttachmentsAsString()) +
                "\nПол бота: " + botGender +
                "\nПол пользователя: " + userGender +
                "\nСчётчик использования: " + timesUsed +
                '}';
    }
    public String toShortString() {
        //228) Как дела? (+P) -> Нормально)) (+doc, doc), 20 использований
        String result = id + ") " + questionText;
        if(questionAttachments != null && !questionAttachments.equals(""))
            result += " (+" + questionAttachments + ")";

        result += " -> " + answerText;

        result += getAnswerAttachmentsAsString();
        if(timesUsed != 0)
            result += ", " + timesUsed + " использований";
        return result;
    }
    public String getAnswerAttachmentsAsString(){
        String result = "";

        if(answerAttachments.size() > 0)
            result += " (+";
        for (int i = 0; i < answerAttachments.size(); i++) {
            result += answerAttachments.get(i).type;
            if(i < answerAttachments.size() - 1)
                result += ", ";
        }
        if(answerAttachments.size() > 0)
            result += ")";

        return result;
    }

    /*
        * Этот метод позволядет сравнить именно суть ответа, не учитывая ID, автора, дату и т.д.*/
    public boolean equalAnswer(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnswerElement that = (AnswerElement) o;

        if (getBotGender() != that.getBotGender()) return false;
        if (getUserGender() != that.getUserGender()) return false;
        if (getQuestionText() != null ? !getQuestionText().equals(that.getQuestionText()) : that.getQuestionText() != null)
            return false;
        if (getAnswerText() != null ? !getAnswerText().equals(that.getAnswerText()) : that.getAnswerText() != null)
            return false;
        if (getQuestionAttachments() != null ? !getQuestionAttachments().equals(that.getQuestionAttachments()) : that.getQuestionAttachments() != null)
            return false;
        return getAnswerAttachments() != null ? getAnswerAttachments().equals(that.getAnswerAttachments()) : that.getAnswerAttachments() == null;

    }

    public boolean hasQuestionPhotos(){
        return questionAttachments.contains("P");
    }
    public boolean hasQuestionVideos(){
        return questionAttachments.contains("V");
    }
    public boolean hasQuestionMusic(){
        return questionAttachments.contains("M");
    }
    public boolean hasQuestionDocuments(){
        return questionAttachments.contains("D");
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public Date getCreatedDate() {
        return createdDate;
    }
    public Date getEditedDate() {
        return editedDate;
    }
    public long getCreatedAuthor() {
        return createdAuthor;
    }
    public void setCreatedAuthor(long createdAuthor) {
        this.createdAuthor = createdAuthor;
    }
    public long getEditedAuthor() {
        return editedAuthor;
    }
    public void setEditedAuthor(long editedAuthor) {
        this.editedAuthor = editedAuthor;
    }
    public String getQuestionText() {
        return questionText;
    }
    public void setQuestionText(String questionText, long author) {
        //author - имя автора кто вносит эти правки
        this.questionText = questionText;
        editedDate = new Date();
        editedAuthor = author;
    }
    public String getAnswerText() {
        return answerText;
    }
    public void setAnswerText(String answerText, long author) {
        //author - имя автора кто вносит эти правки
        this.answerText = answerText;
        editedDate = new Date();
        editedAuthor = author;
    }
    public String getQuestionAttachments() {
        return questionAttachments;
    }
    public void setQuestionAttachments(String questionAttachments) {
        this.questionAttachments = questionAttachments;
    }
    public ArrayList<Attachment> getAnswerAttachments() {
        return answerAttachments;
    }
    public void setAnswerAttachments(ArrayList<Attachment> answerAttachments) {
        this.answerAttachments = answerAttachments;
    }
    public char getBotGender() {
        return botGender;
    }
    public void setBotGender(char botGender) {
        this.botGender = botGender;
    }
    public char getUserGender() {
        return userGender;
    }
    public void setUserGender(char userGender) {
        this.userGender = userGender;
    }
    public int getTimesUsed() {
        return timesUsed;
    }
    public void setTimesUsed(int timesUsed) {
        this.timesUsed = timesUsed;
    }

    public Date getQuestionDate() {
        return questionDate;
    }
    public void setQuestionDate(Date questionDate) {
        this.questionDate = questionDate;
    }
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    public void setEditedDate(Date editedDate) {
        this.editedDate = editedDate;
    }
    public long getQuestionAuthor() {
        return questionAuthor;
    }
    public void setQuestionAuthor(long questionAuthor) {
        this.questionAuthor = questionAuthor;
    }
    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
}
