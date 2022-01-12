package com.whatshide.android.models;

import java.io.Serializable;
import java.util.Date;

public class Chat implements Serializable {
    String id, sender,receiver,message,time,image_url,status;
    public Date dateObj;
    public String chatId, conversationId,conversationImageUrl,conversationName;
    public String senderName,receiverName,senderProfile,receiverProfile;
    public boolean selected = false;
    public Chat(String sender, String receiver, String message, String time, String image_url) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.time = time;
        this.image_url = image_url;
    }

    public Chat() {
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }
}
