package com.fsoft.vktest.AnswerInfrastructure;

import com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase.Attachment;
import com.fsoft.vktest.Communication.Account.Account;
import com.fsoft.vktest.Communication.Account.AccountBase;
import com.fsoft.vktest.Utils.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

/**
 *  * - Текст сообщения
 * - Как отправить ответ на него? (делегат)
 * - Вложения в сообщении
 * - кто отправил сообщение
 * - кто из ботов получил сообщение
 *
 * - участники чата (если сообщение из чата)
 * - название чата (если чат)
 * - id чата (если чат)
 *
 * - данные о посте (если это комментарий под постом на стене)
 *      - текст поста
 *      - автор поста
 *
 * ? фото отправителя
 * ? фото чата
 *
 *
 * - Откуда это сообщение?
 *      - со стены (пост на стене)
 *      - со стены (комментарий под постом)
 *      - из лички
 *      - из чата
 *      - прямо из программы
 *
 *
 * Created by Dr. Failov on 06.03.2017.
 */
public class MessageBase {
    //все возможные типы того, откуда это сообщение может быть получено
    //В зависимости от источника, мы будет заполнять разные доп. поля
    static public final String SOURCE_DIALOG = "dialog";
    static public final String SOURCE_CHAT = "chat";
    static public final String SOURCE_WALL = "wall";
    static public final String SOURCE_COMMENT = "comment";
    static public final String SOURCE_PROGRAM = "program";
    static public final String SOURCE_HTTP = "http";


    //это базовые поля. Они всегда должны быть заполнены.
    private String source = SOURCE_PROGRAM;//откуда мы эту хуйню получили
    private long source_id = 0L;           //Если это чат, то ID чата, если это стена, то ID стены, и т.д.
    private long message_id = 0L;           //Если это сообщение, то ID сообщения, или же ID коммента на стене, или же...
    private String text = "";               //что в этой хуйне написано
    private User author = null;               //кто эту хуйню написал
    private Date date = null;               //когда мы эту хуйню получили
    protected ArrayList<Attachment> attachments = new ArrayList<>();//что он к этой хуйне приложил
    protected ArrayList<User> mentions = new ArrayList<>();//Кого он в этой хуйне упомянул
    protected AccountBase botAccount = null;     // кто из ботов эту хуйню обнаружил
    protected Answer answer = null;            // когда ответ подобран, ложим его сюда
    // позволит нам отправить пользователю ответ в то же место откуда он нам написал сообщение

    //если мы в чате то эти поля будут заполнены. Иначе нет, идите нахуй и не используйте их
    protected ArrayList<Long> chat_users = null;
    protected String chat_title = null;
    protected long chat_id = 0L;

    //если мы работаем с комментарием, то это поле надо заполнить
    //если комментарий под постом  то эти поля будут заполнены. Иначе нет, идите нахуй и не используйте их
    protected long comment_post_author = 0L;
    protected String comment_post_text = null;
    protected long comment_post_id = 0L;
    protected long comment_wall_id = 0L;
    protected long comment_reply_comment_id = 0L;
    protected long comment_reply_user_id = 0L;

    public MessageBase(String source, String text, User author, ArrayList<Attachment> attachments, AccountBase botAccount) {
        this.source = source;
        this.text = text;
        this.author = author;
        this.attachments = attachments;
        this.botAccount = botAccount;
        this.date = new Date();
    }

    public MessageBase(MessageBase toCopy) {
        this.source = toCopy.source;
        this.source_id = toCopy.source_id;
        this.message_id = toCopy.message_id;
        this.text = toCopy.text;
        this.author = toCopy.author;
        this.attachments = toCopy.attachments;
        this.botAccount = toCopy.botAccount;
        this.answer = toCopy.answer;
        this.chat_users = toCopy.chat_users;
        this.chat_title = toCopy.chat_title;
        this.chat_id = toCopy.chat_id;
        this.comment_post_author = toCopy.comment_post_author;
        this.comment_post_text = toCopy.comment_post_text;
        this.comment_post_id = toCopy.comment_post_id;
        this.comment_wall_id = toCopy.comment_wall_id;
        this.comment_reply_comment_id = toCopy.comment_reply_comment_id;
        this.comment_reply_user_id = toCopy.comment_reply_user_id;
        this.date = new Date();
    }

    @Override
    public String toString() {
        return "(" + source + ", " + text + ", "  + author + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageBase that = (MessageBase) o;
        return message_id == that.message_id &&
                Objects.equals(text, that.text) &&
                Objects.equals(author, that.author);
    }

    @Override
    public int hashCode() {

        return Objects.hash(message_id, text, author);
    }

    public MessageBase withSource(String source) {
        this.source = source;
        return this;
    }
    public MessageBase withText(String text) {
        this.text = text;
        return this;
    }
    public MessageBase withAuthor(User author) {
        this.author = author;
        return this;
    }
    public boolean hasAttachments(){
        if(attachments == null)
            return false;
        if(attachments.isEmpty())
            return false;
        return true;
    }
    public MessageBase withAttachments(ArrayList<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }
    public MessageBase withBotAccount(Account botAccount) {
        this.botAccount = botAccount;
        return this;
    }
    public MessageBase withChat_users(ArrayList<Long> chat_users) {
        this.chat_users = chat_users;
        return this;
    }
    public MessageBase withChat_title(String chat_title) {
        this.chat_title = chat_title;
        return this;
    }
    public MessageBase withChat_id(long chat_id) {
        this.chat_id = chat_id;
        return this;
    }
    public MessageBase withComment_post_author(long comment_post_author) {
        this.comment_post_author = comment_post_author;
        return this;
    }
    public MessageBase withComment_post_text(String comment_post_text) {
        this.comment_post_text = comment_post_text;
        return this;
    }
    public MessageBase withComment_reply_comment_id(long comment_reply_comment_id) {
        this.comment_reply_comment_id = comment_reply_comment_id;
        return this;
    }
    public MessageBase withComment_reply_user_id(long comment_reply_user_id) {
        this.comment_reply_user_id = comment_reply_user_id;
        return this;
    }

    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public User getAuthor() {
        return author;
    }
    public void setAuthor(User author) {
        this.author = author;
    }
    public ArrayList<Attachment> getAttachments() {
        return attachments;
    }
    public void setAttachments(ArrayList<Attachment> attachments) {
        this.attachments = attachments;
    }
    public AccountBase getBotAccount() {
        return botAccount;
    }
    public void setBotAccount(AccountBase botAccount) {
        this.botAccount = botAccount;
    }
    public ArrayList<Long> getChat_users() {
        return chat_users;
    }
    public void setChat_users(ArrayList<Long> chat_users) {
        this.chat_users = chat_users;
    }
    public String getChat_title() {
        return chat_title;
    }
    public void setChat_title(String chat_title) {
        this.chat_title = chat_title;
    }
    public long getChat_id() {
        return chat_id;
    }
    public void setChat_id(long chat_id) {
        this.chat_id = chat_id;
    }
    public long getComment_post_author() {
        return comment_post_author;
    }
    public void setComment_post_author(long comment_post_author) {
        this.comment_post_author = comment_post_author;
    }
    public String getComment_post_text() {
        return comment_post_text;
    }
    public void setComment_post_text(String comment_post_text) {
        this.comment_post_text = comment_post_text;
    }
    public Answer getAnswer() {
        return answer;
    }
    public boolean hasAnswer() {
        return answer != null;
    }
    public void setAnswer(Answer answer) {
        this.answer = answer;
    }
    public void setAnswer(String answer) {
        this.answer = new Answer(answer);
    }
    public MessageBase withAnswer(Answer answer) {
        this.answer = answer;
        return this;
    }
    public MessageBase withAnswer(String text) {
        return withAnswer(new Answer(text));
    }
    public long getSource_id() {
        return source_id;
    }
    public void setSource_id(long source_id) {
        this.source_id = source_id;
    }
    public long getMessage_id() {
        return message_id;
    }
    public void setMessage_id(long message_id) {
        this.message_id = message_id;
    }
    public long getComment_post_id() {
        return comment_post_id;
    }
    public void setComment_post_id(long comment_post_id) {
        this.comment_post_id = comment_post_id;
    }
    public long getComment_reply_user_id() {
        return comment_reply_user_id;
    }
    public void setComment_reply_user_id(long comment_reply_user_id) {
        this.comment_reply_user_id = comment_reply_user_id;
    }
    public long getComment_reply_comment_id() {
        return comment_reply_comment_id;
    }
    public void setComment_reply_comment_id(long comment_reply_comment_id) {
        this.comment_reply_comment_id = comment_reply_comment_id;
    }
    public long getComment_wall_id() {
        return comment_wall_id;
    }
    public void setComment_wall_id(long comment_wall_id) {
        this.comment_wall_id = comment_wall_id;
    }
    public ArrayList<User> getMentions() {
        return mentions;
    }
    public void setMentions(ArrayList<User> mentions) {
        this.mentions = mentions;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
}
