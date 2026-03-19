package com.nowcoder.community.entity;

import java.util.Date;

public class Message {
    private int id;
    private int toId;
    private int fromId;
    private String content;
    private String conversationId;
    private int status;//               0未读 1已读 2作废
    private Date createTime;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", toId=" + toId +
                ", fromId=" + fromId +
                ", content='" + content + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }

}
