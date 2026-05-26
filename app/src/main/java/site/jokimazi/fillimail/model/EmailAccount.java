package site.jokimazi.fillimail.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "email_accounts")
public class EmailAccount {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String email;
    public String password;
    public String imapHost;
    public int imapPort;
    public String smtpHost;
    public int smtpPort;
    public boolean ssl;

    public EmailAccount(String email, String password, String imapHost, int imapPort, String smtpHost, int smtpPort, boolean ssl) {
        this.email = email;
        this.password = password;
        this.imapHost = imapHost;
        this.imapPort = imapPort;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.ssl = ssl;
    }

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getImapHost() { return imapHost; }
    public int getImapPort() { return imapPort; }
    public String getSmtpHost() { return smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public boolean isSsl() { return ssl; }
}