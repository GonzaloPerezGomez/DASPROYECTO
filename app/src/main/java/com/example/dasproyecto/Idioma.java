package com.example.dasproyecto;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.Locale;

public class Idioma extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(wrap(base));
    }

    /**
     * Envuelve un Context con el locale guardado en preferencias.
     * Se usa desde Application.attachBaseContext y BaseActivity.attachBaseContext.
     */
    public static Context wrap(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String idioma = prefs.getString("idioma", "es");
        Locale locale = new Locale(idioma);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return context.createConfigurationContext(config);
    }
}
