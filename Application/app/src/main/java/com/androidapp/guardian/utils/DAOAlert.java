package com.androidapp.guardian.utils;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DAOAlert {
    private DatabaseReference databaseReference;

    public DAOAlert(){
        FirebaseDatabase db= FirebaseDatabase.getInstance("https://guardianapp-47975-default-rtdb.firebaseio.com");

        databaseReference = db.getReference(Alert.class.getSimpleName());
        String uid = FirebaseAuth.getInstance().getUid();
        Log.i("DAOAlerts", "success in DAO alert instantiation");
//        databaseReference = db.getReference("users/childs");

    }

    public Task<Void> add(Alert alert){
        return databaseReference.push().setValue(alert);
    }
}
