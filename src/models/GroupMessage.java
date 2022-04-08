package models;

public class GroupMessage extends Message {

    private String groupId;
    private String senderId;
    private long sendTime;
    private String receiverId;
    private String message;

    public GroupMessage(String groupId, String senderId, long sendTime, String message) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.sendTime = sendTime;
        receiverId = null;
        this.message = message;
    }

    public GroupMessage(String groupId, String senderId, long sendTime, String receiverId, String message) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.sendTime = sendTime;
        this.receiverId = receiverId;
        this.message = message;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public long getSendTime() {
        return sendTime;
    }

    @Override
    public void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    @Override
    public String getMessageSourceId() {
        return null;
    }

    @Override
    public void setMessageSourceId(String messageSourceId) {

    }
}
