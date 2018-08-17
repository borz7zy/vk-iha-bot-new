package com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase;

import com.fsoft.vktest.AnswerInfrastructure.MessageComparison.MessagePreparer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Этот класс представляет НЕ полноценный обьект в базе используемый для подбора ответа
 * Created by Dr. Failov on 21.04.2017.
 */
public class AnswerMicroElement {
    public static final char GENDER_MALE = 'М';
    public static final char GENDER_FEMALE = 'Ж';
    public static final char GENDER_NEUTRAL = 'Н';
    public static final char GENDER_UNDEFINED = '-';

    /** Какая инфа про ответ должна хранится для подбора ответа:
     * + ID ответа (id, long. В случае коллизий генерировать новые)
     * + текст вопроса (подготовленный) (question_prepared_text, текст экранирован по правилам JSON)
     * + длина вопроса (в символах) (question_length)
     * + текст ответа (answer_text, текст экранирован по правилам JSON)
     * + список вложений в ответе (answer_attachments, массив JsonObject's)
     * + количество вложений фото в вопросе
     * + количество вложений видео в вопросе
     * + количество вложений документов в вопросе
     * + количество вложений музыки в вопросе
     * + пол бота (bot_gender, М\Ж\-)
     * + пол собеседника (user_gender, М\Ж\-)
     * */
    private long id = 0;
    private String questionTextPrepared = null;
    private int questionLength = 0;
    private int questionPhotos = 0;
    private int questionVideos = 0;
    private int questionMusic = 0;
    private int questionDocuments = 0;
    private int questionRecords = 0;
    private int questionStickers = 0;
    private String answerText = null;
    private ArrayList<Attachment> answerAttachments = new ArrayList<>();
    private char botGender = '-'; //М\Ж\Н\-    Мужской\Женский\Нейтральный\Не указан
    private char userGender = '-'; //М\Ж\Н\-
    private boolean validated = true;

    public AnswerMicroElement(JSONObject jsonObject, MessagePreparer preparer) throws JSONException, ParseException{
        if(jsonObject.has("id"))
            id = jsonObject.getLong("id");
        if(jsonObject.has("questionText")) {
            String questionText = jsonObject.getString("questionText");
            questionTextPrepared = preparer.prepare(questionText);
            questionLength = questionText.length();
        }
        if(jsonObject.has("answerText"))
            answerText = jsonObject.getString("answerText");
        if(jsonObject.has("questionAttachments")) {
            String questionAttachments = jsonObject.getString("questionAttachments");
            questionPhotos = questionAttachments.length() - questionAttachments.replace("P", "").length();
            questionVideos = questionAttachments.length() - questionAttachments.replace("V", "").length();
            questionMusic = questionAttachments.length() - questionAttachments.replace("M", "").length();
            questionDocuments = questionAttachments.length() - questionAttachments.replace("D", "").length();
            questionRecords = questionAttachments.length() - questionAttachments.replace("R", "").length();
            questionStickers = questionAttachments.length() - questionAttachments.replace("S", "").length();
        }
        if(jsonObject.has("botGender"))
            botGender = (char)jsonObject.getInt("botGender");
        if(jsonObject.has("userGender"))
            userGender = (char)jsonObject.getInt("userGender");
        if(jsonObject.has("validated"))
            validated = jsonObject.getBoolean("validated");
        if(jsonObject.has("answerAttachments")) {
            JSONArray jsonArray = jsonObject.getJSONArray("answerAttachments");
            for (int i = 0; i < jsonArray.length(); i++) {
                answerAttachments.add(new Attachment(jsonArray.getJSONObject(i)));
            }
            //answerAttachments.addAll(Attachment.parseAttachments(jsonArray, 0, 0, null));
        }
    }

    @Override
    public String toString() {
        // 228) привид как дила (1 фотографий) -> Привет, отлично! (+photo, photo)
        String result = id + ") " + questionTextPrepared;
        if(questionPhotos + questionVideos + questionMusic + questionDocuments + questionRecords + questionStickers != 0)
            result += " (";
        boolean needComma = false;
        if(questionPhotos != 0) {
            result += questionPhotos + " фотографий";
            needComma = true;
        }
        if(questionVideos != 0) {
            if(needComma)
                result += ", ";
            result += questionVideos + " видео";
            needComma = true;
        }
        if(questionDocuments != 0) {
            if(needComma)
                result += ", ";
            result += questionDocuments + " документов";
            needComma = true;
        }
        if(questionMusic != 0) {
            if(needComma)
                result += ", ";
            result += questionMusic + " аудио";
            needComma = true;
        }
        if(questionStickers != 0) {
            if(needComma)
                result += ", ";
            result += questionStickers + " стикеров";
            needComma = true;
        }
        if(questionRecords != 0) {
            if(needComma)
                result += ", ";
            result += questionRecords + " записей";
        }
        if(questionPhotos + questionVideos + questionMusic + questionDocuments + questionRecords + questionStickers != 0)
            result += ")";

        result += " -> " + answerText;

        if(answerAttachments.size() > 0)
            result += " (+";
        for (int i = 0; i < answerAttachments.size(); i++) {
            result += answerAttachments.get(i).getType();
            if(i < answerAttachments.size() - 1)
                result += ", ";
        }
        if(answerAttachments.size() > 0)
            result += ")";
        return result;
    }

    public boolean isSame(AnswerMicroElement answerMicroElement){
        //сравнение текста вопроса и ответов
        if(!answerMicroElement.getAnswerText().equals(getAnswerText()))
            return false;
        if(!answerMicroElement.getQuestionTextPrepared().equals(getQuestionTextPrepared()))
            return false;

        //сравнение содержимого вложений в ответах
        for(Attachment attachment:getAnswerAttachments())
            if(!answerMicroElement.getAnswerAttachments().contains(attachment))
                return false;
        for(Attachment attachment:answerMicroElement.getAnswerAttachments())
            if(!getAnswerAttachments().contains(attachment))
                return false;

        //сравниение содержимого вложений в вопросах
        if(answerMicroElement.getQuestionDocuments() !=(getQuestionDocuments()))
            return false;
        if(answerMicroElement.getQuestionMusic() !=(getQuestionMusic()))
            return false;
        if(answerMicroElement.getQuestionPhotos() !=(getQuestionPhotos()))
            return false;
        if(answerMicroElement.getQuestionStickers() !=(getQuestionStickers()))
            return false;
        if(answerMicroElement.getQuestionVideos() !=(getQuestionVideos()))
            return false;
        if(answerMicroElement.getQuestionRecords() !=(getQuestionRecords()))
            return false;
        return true;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getQuestionTextPrepared() {
        return questionTextPrepared;
    }
    public void setQuestionTextPrepared(String questionTextPrepared) {
        this.questionTextPrepared = questionTextPrepared;
    }
    public int getQuestionLength() {
        return questionLength;
    }
    public int getQuestionPhotos() {
        return questionPhotos;
    }
    public int getQuestionVideos() {
        return questionVideos;
    }
    public int getQuestionMusic() {
        return questionMusic;
    }
    public int getQuestionDocuments() {
        return questionDocuments;
    }
    public int getQuestionRecords() {
        return questionRecords;
    }
    public int getQuestionStickers() {
        return questionStickers;
    }
    public String getAnswerText() {
        return answerText;
    }
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
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
    public boolean isValidated() {
        return validated;
    }
}
