package models;

public class PrivateMessage extends Message {
    private String senderId;
    private String receiverId;
    private int messageLength;
    private String message;
    private long sendTime;

    public PrivateMessage(String senderId, String receiverId, int messageLength, String message, long sendTime) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageLength = messageLength;
        this.message = message;
        this.sendTime = sendTime;
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

    public int getMessageLength() {
        return messageLength;
    }

    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
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
        return getSenderId();
    }

    @Override
    public void setMessageSourceId(String messageSourceId) {
        setSenderId(messageSourceId);
    }
}
