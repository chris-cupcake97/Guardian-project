package com.androidapp.guardian.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.androidapp.guardian.R;
import com.androidapp.guardian.activities.ParentSignedInActivity;
import com.androidapp.guardian.adapters.AppAdapter;
import com.androidapp.guardian.interfaces.OnAppClickListener;
import com.androidapp.guardian.models.App;

import java.util.ArrayList;
import java.util.HashMap;

import static com.androidapp.guardian.activities.ParentSignedInActivity.CHILD_EMAIL_EXTRA;

public class AppsFragment extends Fragment implements OnAppClickListener {
	public static final String TAG = "AppsFragmentTAG";
	private FirebaseDatabase firebaseDatabase;
	private DatabaseReference databaseReference;
	private ArrayList<App> apps;
	private AppAdapter appAdapter;
	private RecyclerView recyclerViewApps;
	private Context context;
	private String childEmail;
	private String appName;
	private String packageName;
	private Bundle bundle;
	
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_apps, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		context = getContext();
		firebaseDatabase = FirebaseDatabase.getInstance("https://guardianapp-47975-default-rtdb.firebaseio.com/");
		databaseReference = firebaseDatabase.getReference("users");
		
		
		recyclerViewApps = view.findViewById(R.id.recyclerViewApps);
		recyclerViewApps.setHasFixedSize(true);
		recyclerViewApps.setLayoutManager(new LinearLayoutManager(getContext()));
		
		getData();
		initializeAdapter(this);
		
	}
	
	public void getData() {
		bundle = getActivity().getIntent().getExtras();
		if (bundle != null) {
			apps = bundle.getParcelableArrayList(ParentSignedInActivity.APPS_EXTRA);
			childEmail = bundle.getString(CHILD_EMAIL_EXTRA);
		}
	}
	
	public void initializeAdapter(OnAppClickListener onAppClickListener) {
		appAdapter = new AppAdapter(context, apps);
		appAdapter.setOnAppClickListener(onAppClickListener);
		recyclerViewApps.setAdapter(appAdapter);
	}
	
	@Override
	public void onItemClick(final String packageName, String appName, boolean blocked) {
		if (blocked) {
			Toast.makeText(context, appName + " " + "blocked", Toast.LENGTH_SHORT).show();
			updateAppState(packageName, blocked);
			
		} else {
			Toast.makeText(context, appName + " enabled", Toast.LENGTH_SHORT).show();
			updateAppState(packageName, blocked);
			
		}

	}
	
	
	private void updateAppState(final String packageName, final boolean blocked) {
		Query query = databaseReference.child("childs").orderByChild("email").equalTo(childEmail);
		query.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				DataSnapshot nodeShot = dataSnapshot.getChildren().iterator().next();
				final String key = nodeShot.getKey();
				Log.i(TAG, "onDataChange: key: " + key);
				Query query = databaseReference.child("childs").child(key).child("apps").orderByChild("packageName").equalTo(packageName);  //changed from appName
				query.addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						if (dataSnapshot.exists()) {
							DataSnapshot snapshot = dataSnapshot.getChildren().iterator().next();
							HashMap<String, Object> update = new HashMap<>();
							update.put("blocked", blocked);
							databaseReference.child("childs").child(key).child("apps").child(snapshot.getKey()).updateChildren(update);
							
						}
					}
					
					@Override
					public void onCancelled(@NonNull DatabaseError databaseError) {
					
					}
				});
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
			
			}
		});
	}
	
}
