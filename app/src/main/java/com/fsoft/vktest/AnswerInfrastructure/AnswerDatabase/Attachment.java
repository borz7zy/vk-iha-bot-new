package com.fsoft.vktest.AnswerInfrastructure.AnswerDatabase;

import com.fsoft.vktest.ApplicationManager;
import com.fsoft.vktest.BotService;
import com.fsoft.vktest.Communication.Account.VK.VkAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;

public class Attachment {
    private static ApplicationManager applicationManager = null;
    private static File attachments_folder = null;
    private static File downloads_folder = null;
    public static String TYPE_PHOTO = "photo";
    public static String TYPE_AUDIO = "audio";
    public static String TYPE_DOC = "doc";
    public static String TYPE_VIDEO = "video";
    public static String NETWORK_TG = "TG";
    public static String NETWORK_VK = "VK";

    private String type = "";      //тип вложения из списка выще
    private String filename = "";  //Имя файла если это вложение есть локально в папке attachments (часть базы).
    private ArrayList<OnlineAttachment> online = new ArrayList<>();
    private File file = null;      //Ссылка на файл с вложением (для отправки)

    public Attachment (com.perm.kate.api.Attachment attachment){
        OnlineAttachment onlineAttachment = new OnlineAttachment();
        onlineAttachment.setNetwork(NETWORK_VK);
        type = attachment.type;
        if(type == TYPE_DOC)
            onlineAttachment.setId(attachment.document.owner_id + "_" + attachment.document.id + "_" + attachment.document.access_key);
        if(type == TYPE_AUDIO)
            onlineAttachment.setId(attachment.audio.owner_id + "_" + attachment.audio.aid);
        if(type == TYPE_PHOTO)
            onlineAttachment.setId(attachment.photo.owner_id + "_" + attachment.photo.pid + "_" + attachment.photo.access_key);
        if(type == TYPE_VIDEO)
            onlineAttachment.setId(attachment.video.owner_id + "_" + attachment.video.vid + "_" + attachment.video.access_key);
        online.add(onlineAttachment);
    }
    public Attachment(String type, String filename) {
        this.type = type;
        this.filename = filename;
    }
    public Attachment(String type, File file) {
        this.type = type;
        this.filename = file.getName();
        this.file = file;
    }
    public Attachment(JSONObject jsonObject)throws JSONException, ParseException {
        fromJson(jsonObject);
    }
    public Attachment() {
    }


    public File getFile() throws Exception {
        if(file == null) {
            if(applicationManager == null)
                applicationManager = ApplicationManager.getInstance();
            //обновить адрес папки с вложениями если нужно
            if (attachments_folder == null)
                attachments_folder = new File(ApplicationManager.getHomeFolder(), "attachments");
            //обновить адрес папки с загрузками если нужно
            if (downloads_folder == null)
                downloads_folder = new File(ApplicationManager.getDownloadsFolder());

            if (attachments_folder != null && !filename.isEmpty())
                file = new File(attachments_folder, filename);

            //вот тут прикол. Надо скачать файл с соцсети.
            if(!online.isEmpty()){
                if(online.get(0).network.equals(NETWORK_VK)){
                    file = applicationManager.getCommunicator().downloadVkAttachment(this);
                    return file;
                }
                if(online.get(0).network.equals(NETWORK_TG)){
                    file = applicationManager.getCommunicator().downloadTgAttachment(this);
                    return file;
                }
            }

        }
        return file;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if(type != null)
            jsonObject.put("type", type);
        if(filename != null)
            jsonObject.put("file", filename);
        return jsonObject;
    }
    private void fromJson(JSONObject jsonObject)throws JSONException, ParseException {
        if(jsonObject.has("type"))
            type = jsonObject.getString("type");
        if(jsonObject.has("file"))
            filename = jsonObject.getString("file");
    }
    public boolean fileExists(){
        if(file == null)
            return false;
        return file.exists();
    }

    public boolean isDoc(){
        return getType().equals(TYPE_DOC);
    }
    public boolean isPhoto(){
        return getType().equals(TYPE_PHOTO);
    }
    public boolean isVideo(){
        return getType().equals(TYPE_VIDEO);
    }
    public boolean isAudio(){
        return getType().equals(TYPE_AUDIO);
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public ArrayList<OnlineAttachment> getOnline() {
        return online;
    }
    public boolean hasVkCache(){
        for(OnlineAttachment onlineAttachment:online)
            if(onlineAttachment.isVk())
                return true;
        return false;
    }
    public boolean hasTgCache(){
        for(OnlineAttachment onlineAttachment:online)
            if(onlineAttachment.isTg())
                return true;
        return false;
    }
    public OnlineAttachment getVkCache(){
        for(OnlineAttachment onlineAttachment:online)
            if(onlineAttachment.isVk())
                return onlineAttachment;
        return null;
    }
    public OnlineAttachment getTgCache(){
        for(OnlineAttachment onlineAttachment:online)
            if(onlineAttachment.isTg())
                return onlineAttachment;
        return null;
    }

    public class OnlineAttachment{
        private String network = "";   //(для входящих вложений) соцсеть откуда мы получили вложение
        private String id = "";        //(для входящих вложений) идентификатор вложения внутри соцсети

        public OnlineAttachment() {
        }

        public OnlineAttachment(String network, String id) {
            this.network = network;
            this.id = id;
        }

        public String getNetwork() {
            return network;
        }

        public void setNetwork(String network) {
            this.network = network;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isVk(){
            return network.equals(NETWORK_VK);
        }
        public boolean isTg(){
            return network.equals(NETWORK_TG);
        }
    }
    public static ArrayList<Attachment> convertAttachments(ArrayList<com.perm.kate.api.Attachment> attachments){
        ArrayList<Attachment> result = new ArrayList<>();
        for(com.perm.kate.api.Attachment attachment:attachments)
            result.add(new Attachment(attachment));
        return result;
    }
}
