package models;

public abstract class Message {
    private long sendTime;
    private String messageSourceId;

    abstract public long getSendTime();

    abstract public void setSendTime(long sendTime);

    abstract public String getMessageSourceId();

    abstract public void setMessageSourceId(String messageSourceId);
}
