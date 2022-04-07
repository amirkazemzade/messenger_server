package models;

public class GroupMessage extends Message {

    private String groupId;
    private long sendTime;

    public GroupMessage(String groupId, long sendTime) {
        this.groupId = groupId;
        this.sendTime = sendTime;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
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
