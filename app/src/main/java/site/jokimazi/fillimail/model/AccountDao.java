package site.jokimazi.fillimail.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AccountDao {
    @Insert
    void insert(EmailAccount account);

    @Delete
        // Добавь эту аннотацию
    void delete(EmailAccount account);

    @Update
        // Добавь эту аннотацию
    void update(EmailAccount account);

    @Query("SELECT * FROM email_accounts")
    List<EmailAccount> getAllAccounts();

    @Query("SELECT * FROM email_accounts WHERE email = :email LIMIT 1")
    EmailAccount getAccountByEmail(String email);
}