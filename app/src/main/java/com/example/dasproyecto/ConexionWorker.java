package com.example.dasproyecto;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Worker para realizar conexiones en segundo plano con la base de datos remota
 * utilizando la API nativa HttpURLConnection, tal y como se especifica en la
 * guía del curso.
 */
public class ConexionWorker extends Worker {

    private static final String TAG = "ConexionWorker";
    private static final String SERVER_URL = "http://34.133.172.131:81/";

    public ConexionWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String accion = getInputData().getString("accion");
        if (accion == null)
            return Result.failure();

        HttpURLConnection urlConnection = null;
        try {
            // Montar URL. Ej: http://104.198.26.237:81/login.php
            URL destino = new URL(SERVER_URL + accion + ".php");
            urlConnection = (HttpURLConnection) destino.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            // Configurar método POST y cabeceras
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Construir los parámetros usando Uri.Builder (soportado por PHP $_POST nativo)
            Uri.Builder builder = new Uri.Builder();
            for (String key : getInputData().getKeyValueMap().keySet()) {
                if (!key.equals("accion")) { // No enviamos 'accion' por POST si ya va en la URL (.php)
                    Object value = getInputData().getKeyValueMap().get(key);
                    if (value != null) {
                        builder.appendQueryParameter(key, String.valueOf(value));
                    }
                }
            }

            // Si la acción es Tareas, hay un campo 'accion' interno en tareas.php
            // (getTareas, insertTarea, etc)
            if (accion.equals("tareas")) {
                builder.appendQueryParameter("accion", getInputData().getString("tarea_accion"));
            }

            String parametros = builder.build().getEncodedQuery();

            // Enviar los parámetros via POST
            if (parametros != null && !parametros.isEmpty()) {
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();
            }

            // Recibir respuesta
            int statusCode = urlConnection.getResponseCode();
            Log.d(TAG, "Status code: " + statusCode);

            if (statusCode == 200) {
                BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }
                inputStream.close();

                Log.d(TAG, "Respuesta del servidor: " + result.toString());

                // Devolver el resultado al Activity
                Data resultados = new Data.Builder()
                        .putString("datos", result.toString())
                        .build();

                return Result.success(resultados);
            } else {
                Log.e(TAG, "Error del servidor, status: " + statusCode);
                return Result.failure();
            }

        } catch (Exception e) {
            Log.e(TAG, "Excepción en conexión de red", e);
            Data errorData = new Data.Builder()
                    .putString("datos", "{\"exito\":false, \"mensaje\":\"Error de red local: " + e.getMessage() + "\"}")
                    .build();
            // Retornamos success con el json de error, para que el Observer lo lea y
            // muestre el toast.
            return Result.success(errorData);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
