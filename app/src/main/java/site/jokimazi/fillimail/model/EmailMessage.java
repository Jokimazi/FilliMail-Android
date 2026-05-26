package site.jokimazi.fillimail.model;

import java.io.Serializable;

public class EmailMessage implements Serializable {
    public long uid;
    public int accountId;
    public String sender;
    public String subject;
    public String body;

    public EmailMessage(long uid, int accountId, String sender, String subject, String body) {
        this.uid = uid;
        this.accountId = accountId;
        this.sender = sender;
        this.subject = subject;
        this.body = body;
    }
}