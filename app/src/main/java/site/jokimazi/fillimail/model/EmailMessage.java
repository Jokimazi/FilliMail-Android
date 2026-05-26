package site.jokimazi.fillimail.model;

import java.io.Serializable;
import java.util.Date;

public class EmailMessage implements Serializable {
    public long uid;
    public int accountId;
    public String receiver; // ДОБАВЛЕНО: почта, на которую пришло письмо
    public String sender;
    public String subject;
    public String body;
    public Date date;

    public EmailMessage(long uid, int accountId, String receiver, String sender, String subject, String body, Date date) {
        this.uid = uid;
        this.accountId = accountId;
        this.receiver = receiver;
        this.sender = sender;
        this.subject = subject;
        this.body = body;
        this.date = date;
    }
}