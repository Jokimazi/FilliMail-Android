package site.jokimazi.fillimail;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.room.Room;
import site.jokimazi.fillimail.model.AppDatabase;

public class App extends Application {
    private static App instance;
    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // fallbackToDestructiveMigration пересоздаст базу, если мы добавим новые поля (как с SMTP)
        database = Room.databaseBuilder(this, AppDatabase.class, "fillimail_db")
                .fallbackToDestructiveMigration()
                .build();

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int theme = prefs.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);
    }

    public static App getInstance() { return instance; }
    public AppDatabase getDatabase() { return database; }
}