package site.jokimazi.fillimail.model;

import java.io.Serializable;

public class EmailMessage implements Serializable {
    public String sender;
    public String subject;
    public String body;

    public EmailMessage(String sender, String subject, String body) {
        this.sender = sender;
        this.subject = subject;
        this.body = body;
    }
}