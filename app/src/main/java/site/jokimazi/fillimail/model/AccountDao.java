package site.jokimazi.fillimail.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AccountDao {
    @Insert
    void insert(EmailAccount account);

    @Query("SELECT * FROM email_accounts")
    List<EmailAccount> getAllAccounts();
}