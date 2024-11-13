package com.androidapp.guardian.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.androidapp.guardian.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ActivityLogFragment extends Fragment {
    public static final String TAG = "ActivityLogTAG";
    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance("https://guardianapp-47975-default-rtdb.firebaseio.com/");
        databaseReference = firebaseDatabase.getReference("users");
        checkAbusiveMessages();

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    String[] words = {"23", "hate"};

    private void checkAbusiveMessages() {
        String parentEmail = user.getEmail();
        Query query = databaseReference.child("childs").orderByChild("parentEmail").equalTo(parentEmail);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
					String content="";
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        String name = postSnapshot.child("name").getValue().toString();
                        Log.e(">>>>>", "Id ::" + postSnapshot.getKey());
                        Log.e(">>>>>", "Name ::" + name);
                        ArrayList<HashMap<String, String>> messages = (ArrayList<HashMap<String, String>>) postSnapshot.child("messages").getValue();
                        if (messages != null) {
                            for (int j = 0; j < messages.size(); j++) {
                                String msg = messages.get(j).get("message");
                                String fromWhom=messages.get(j).get("from");
                                String type=messages.get(j).get("type");
                                Log.e(">>>>>", "Message ::" + msg);
                                Log.e(">>>>>","Type ::"+type);
                                    assert type != null;
                                    if (type.contains("LABEL_1")) {
										content=name+" has received abusive message from "+fromWhom;
                                        Toast.makeText(getActivity(), name + " has received abusive messages.", Toast.LENGTH_SHORT).show();
                                    }
                            }
                        }
                    }
					((TextView)getView().findViewById(R.id.tv_messages)).setText(content);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
