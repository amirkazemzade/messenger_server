package models;

public class Session {
    private String username;
    private String sid;

    public Session(String username, String sid) {
        this.username = username;
        this.sid = sid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getsid() {
        return sid;
    }

    public void setsid(String sid) {
        this.sid = sid;
    }
}
