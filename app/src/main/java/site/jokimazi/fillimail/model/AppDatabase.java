package site.jokimazi.fillimail.model;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {EmailAccount.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AccountDao accountDao();
}