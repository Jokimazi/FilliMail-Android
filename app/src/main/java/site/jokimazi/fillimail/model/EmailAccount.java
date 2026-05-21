package site.jokimazi.fillimail.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "email_accounts")
public class EmailAccount implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id; // Нужен первичный ключ для БД

    private final String email;
    private final String password;
    private final String imapHost;
    private final int imapPort;
    private final String smtpHost;
    private final int smtpPort;
    private final boolean useSSL;

    public EmailAccount(String email, String password, String imapHost, int imapPort,
                        String smtpHost, int smtpPort, boolean useSSL) {
        this.email = email;
        this.password = password;
        this.imapHost = imapHost;
        this.imapPort = imapPort;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.useSSL = useSSL;
    }

    // Геттеры и сеттеры (сеттер для ID нужен Room'у)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getImapHost() { return imapHost; }
    public int getImapPort() { return imapPort; }
    public String getSmtpHost() { return smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public boolean isUseSSL() { return useSSL; }
}