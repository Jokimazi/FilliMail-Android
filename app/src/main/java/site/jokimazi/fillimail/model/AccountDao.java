package site.jokimazi.fillimail.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface AccountDao {
    @Query("SELECT * FROM email_accounts")
    List<EmailAccount> getAllAccounts();

    @Query("SELECT * FROM email_accounts WHERE id = :id LIMIT 1")
    EmailAccount getAccountById(int id);

    @Insert
    void insert(EmailAccount account);

    @Update
    void update(EmailAccount account);

    @Delete
    void delete(EmailAccount account);
}