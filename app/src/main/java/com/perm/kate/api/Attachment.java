package com.perm.kate.api;

import java.io.Serializable;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



/**
 *
 *
 *
 {
     "type": "photo",
     "photo": {
     "id": 456242721,
     "album_id": -3,
     "owner_id": 358039995,
     "photo_75": "https://pp.userap...086/yWDdq_0a7PA.jpg",
     "photo_130": "https://pp.userap...087/MS3ZKdjoK8E.jpg",
     "photo_604": "https://pp.userap...088/QXCFMOwpDBQ.jpg",
     "photo_807": "https://pp.userap...089/DrWPPwLN1oc.jpg",
     "photo_1280": "https://pp.userap...08a/9P_GwOZmihM.jpg",
     "photo_2560": "https://pp.userap...08b/m0c7X_jpYVw.jpg",
     "width": 2560,
     "height": 1920,
     "text": "",
     "date": 1492744699,
     "access_key": "8f6927f7a74340b4ce"
 }

 *
 */
public class Attachment implements Serializable {
    private static final long serialVersionUID = 1L;
    public long id;//used only for wall post attached to message
    public String type; //photo,posted_photo,video,audio,link,note,app,poll,doc,geo,message,page,album
    public Photo photo; 
    //public Photo posted_photo; 
    public Video video; 
    public Audio audio; 
    public Link link; 
    public Note note; 
    public Graffiti graffiti;
    public VkApp app; 
    public VkPoll poll;
    public Geo geo;
    public Document document;
    public Message message;
    public WallMessage wallMessage;
    public Page page;
    public Gift gift;
    public Album album;

    public Attachment() {
    }
    public Attachment(Photo photo) {
        this.photo = photo;
        this.type = "photo";
    }
    public Attachment(Video video) {
        this.video = video;
        this.type = "video";
    }

    public Attachment(Document document) {
        this.document = document;
        this.type = "doc";
    }

    public Attachment(Audio audio) {
        this.audio = audio;
        this.type = "audio";
    }

    public static ArrayList<Attachment> parseAttachments(JSONArray attachments, long from_id, long copy_owner_id, JSONObject geo_json) throws JSONException {
        ArrayList<Attachment> attachments_arr=new ArrayList<Attachment>();
        if(attachments!=null){
            int size=attachments.length();
            for(int j=0;j<size;++j){
                Object att=attachments.get(j);
                if(att instanceof JSONObject==false)
                    continue;
                JSONObject json_attachment=(JSONObject)att;
                Attachment attachment=new Attachment();
                attachment.type=json_attachment.getString("type");
                if(attachment.type.equals("photo") || attachment.type.equals("posted_photo")){
                    JSONObject x=json_attachment.optJSONObject("photo");
                    if(x!=null)
                        attachment.photo=Photo.parse(x);
                }
                else if(attachment.type.equals("graffiti"))
                    attachment.graffiti=Graffiti.parse(json_attachment.getJSONObject("graffiti"));
                else if(attachment.type.equals("link"))
                    attachment.link=Link.parse(json_attachment.getJSONObject("link"));
                else if(attachment.type.equals("audio"))
                    attachment.audio=Audio.parse(json_attachment.getJSONObject("audio"));
                else if(attachment.type.equals("note"))
                    attachment.note=Note.parse(json_attachment.getJSONObject("note"));
                else if(attachment.type.equals("video"))
                    attachment.video=Video.parseForAttachments(json_attachment.getJSONObject("video"));
                else if(attachment.type.equals("poll")){
                    attachment.poll=VkPoll.parse(json_attachment.getJSONObject("poll"));
                    if(attachment.poll.owner_id==0){
                        //это устарело потому что поля copy_owner_id больше нет при парсинге 
                        //if(copy_owner_id!=0)
                        //    attachment.poll.owner_id=copy_owner_id;
                        //else
                        attachment.poll.owner_id=from_id;
                    }
                }
                else if(attachment.type.equals("doc"))
                    attachment.document=Document.parse(json_attachment.getJSONObject("doc"));
                else if(attachment.type.equals("wall"))
                    attachment.wallMessage=WallMessage.parse(json_attachment.getJSONObject("wall"));
                else if(attachment.type.equals("page"))
                    attachment.page=Page.parseFromAttachment(json_attachment.getJSONObject("page"));
                else if(attachment.type.equals("gift"))
                    attachment.gift=Gift.parse(json_attachment.getJSONObject("gift"));
                else if(attachment.type.equals("album"))
                    attachment.album=Album.parseFromAttachment(json_attachment.getJSONObject("album"));
                attachments_arr.add(attachment);
            }
        }
        
        //Geo тоже добавляем в attacmnets если он есть
        if(geo_json!=null){
            Attachment a=new Attachment();
            a.type="geo";
            a.geo=Geo.parse(geo_json);
            attachments_arr.add(a);
        }
        return attachments_arr;
    }

    public JSONObject toJson() throws JSONException{
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", type);
        if(type.equals("photo") && photo != null)
            jsonObject.put("photo", photo.toJson());
        if(type.equals("video") && video != null)
            jsonObject.put("video", video.toJson());
        if(type.equals("audio") && audio != null)
            jsonObject.put("audio", audio.toJson());
        if(type.equals("doc") && document != null)
            jsonObject.put("doc", document.toJson());
        return jsonObject;
    }

    @Override
    public String toString() {
        if(type.equals("photo")) {
            if(photo.access_key.equals(""))
                return type + photo.owner_id + "_" + photo.pid;
            else
                return type + photo.owner_id + "_" + photo.pid + "_" + photo.access_key;
        }
        if(type.equals("video")) {
            if(video.access_key.equals(""))
                return type + video.owner_id + "_" + video.vid;
            else
                return type + video.owner_id + "_" + video.vid + "_" + video.access_key;
        }
        if(type.equals("audio")){
            return type + audio.owner_id + "_" + audio.aid;
        }
        if(type.equals("doc")){
            if(document.access_key.equals(""))
                return type + document.owner_id + "_" + document.id;
            else
                return type + document.owner_id + "_" + document.id + document.access_key;
        }
        if(type.equals("wall")){
            return type + wallMessage.to_id + "_" + wallMessage.id;
        }
        return super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attachment that = (Attachment) o;

        if(!that.type.equals(type))
            return false;
        if(type.equals("photo") && !photo.equals(that.photo))
            return false;
        if(type.equals("video") && !video.equals(that.video))
            return false;
        if(type.equals("doc") && !document.equals(that.document))
            return false;
        if(type.equals("audio") && !audio.equals(that.audio))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return type != null ? type.hashCode() : 0;
    }
}
