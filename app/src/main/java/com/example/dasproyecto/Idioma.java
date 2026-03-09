package com.example.dasproyecto;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.Locale;

/**
 * Clase Application de la app.
 * Se encarga de aplicar el idioma elegido por el usuario
 * desde el arranque de la aplicación.
 */
public class Idioma extends Application {

    /**
     * Se llama cuando arranca la app, antes de abrir cualquier pantalla.
     */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Se llama cuando cambia la configuración del dispositivo (rotación, idioma,
     * etc.).
     *
     * @param newConfig La nueva configuración del sistema.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Aplica el idioma elegido al contexto base de la app.
     *
     * @param base Contexto base del sistema.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(wrap(base));
    }

    /**
     * Envuelve un contexto con el idioma guardado en las preferencias.
     * Lo usan tanto Application como BaseActivity.
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
