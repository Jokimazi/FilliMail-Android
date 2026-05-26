package site.jokimazi.fillimail.model;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class EmailMessage implements Serializable {
    public long uid;
    public int accountId;
    public String folderName;
    public String receiver;
    public String senderName;
    public String senderEmail;
    public String subject;
    public String body;
    public Date date;

    public EmailMessage(long uid, int accountId, String folderName, String receiver, String senderName, String senderEmail, String subject, String body, Date date) {
        this.uid = uid;
        this.accountId = accountId;
        this.folderName = folderName;
        this.receiver = receiver;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.subject = subject;
        this.body = body;
        this.date = date;
    }

    public String getGravatarUrl() {
        if (senderEmail == null || senderEmail.isEmpty()) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(senderEmail.trim().toLowerCase().getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return "https://www.gravatar.com/avatar/" + sb.toString() + "?d=identicon";
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}