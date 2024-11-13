package com.androidapp.guardian.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.androidapp.guardian.models.App;
import com.androidapp.guardian.models.Child;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class UploadAppsService extends JobService {
    public static final String TAG = "UploadAppsService";
    private boolean jobCancelled;

    private ArrayList<App> apps;            //read from the database
    private List<ApplicationInfo> applicationInfoList;
    private PackageManager packageManager;


    private DatabaseReference databaseReference;
    private String childEmail;


    @Override
    public boolean onStartJob(JobParameters params) {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        childEmail = user.getEmail();

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://guardianapp-47975-default-rtdb.firebaseio.com/");
        databaseReference = firebaseDatabase.getReference("users");

        uploadApps(params);
        //to keep device awake
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    private void uploadApps(JobParameters params) {


        new Thread(new Runnable() {
            @Override
            public void run() {

                getRecentAppStats();
            }
        }).start();

        jobFinished(params, false);
    }

    private void prepareData(ArrayList<App> apps) {

        ArrayList<App> appsList = new ArrayList<>();
        getInstalledApplication();

        if (apps.isEmpty()) {
            Log.i(TAG, "prepareData: online appsList empty");
            for (ApplicationInfo applicationInfo : applicationInfoList) {
                if (applicationInfo.packageName != null) {
                    appsList.add(new App((String) applicationInfo.loadLabel(packageManager), (String) applicationInfo.packageName, false));
                }
            }

        } else {
            for (ApplicationInfo applicationInfo : applicationInfoList) {
                for (App app : apps) {
                    if (app.getPackageName().equals((String) applicationInfo.packageName)) {
                        appsList.add(new App((String) applicationInfo.loadLabel(packageManager), (String) applicationInfo.packageName, app.isBlocked()));
                        Log.i(TAG, "prepareData: if executed");
                    }
                }

            }

            //if the app is in the offline list but not in the online one, add it.
            for (ApplicationInfo applicationInfo : applicationInfoList) {
                if (!apps.contains(new App((String) applicationInfo.loadLabel(packageManager), applicationInfo.packageName, false))
                        && !apps.contains(new App((String) applicationInfo.loadLabel(packageManager), applicationInfo.packageName, true))) {
                    appsList.add(new App((String) applicationInfo.loadLabel(packageManager), (String) applicationInfo.packageName, false));
                }
            }

        }


        writeDataToDB(appsList);

    }

    private void getInstalledApplication() {
        packageManager = getPackageManager();
        applicationInfoList = packageManager.getInstalledApplications(0);
        Collections.sort(applicationInfoList, new ApplicationInfo.DisplayNameComparator(packageManager));
        Iterator<ApplicationInfo> iterator = applicationInfoList.iterator();
        while (iterator.hasNext()) {
            ApplicationInfo applicationInfo = iterator.next();
            if (applicationInfo.packageName.contains("com.google") || applicationInfo.packageName.matches("com.android.chrome"))
                continue;
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                iterator.remove();
            }
        }
    }

    private void getRecentAppStats() {
        Query query = databaseReference.child("childs").orderByChild("email").equalTo(childEmail);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    apps = new ArrayList<>();
                    DataSnapshot nodeShot = dataSnapshot.getChildren().iterator().next();
                    Child child = nodeShot.getValue(Child.class);
                    apps = child.getApps();

                    prepareData(apps);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void writeDataToDB(ArrayList<App> appsList) {
        final ArrayList<App> simpleAppInfo = new ArrayList<>();
        for (App app : appsList) {
            simpleAppInfo.add(new App(app.getAppName(), app.getPackageName(), app.isBlocked()));
        }

        Query query = databaseReference.child("childs").orderByChild("email").equalTo(childEmail);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.i(TAG, "onDataChange: executed");
                    //update the child's database and add the appsList
                    DataSnapshot nodeShot = dataSnapshot.getChildren().iterator().next();
                    String key = nodeShot.getKey();
                    //appList contains drawables, that's why it can't be added to the database.
                    //for now i will upload the names only
                    databaseReference.child("childs").child(key).child("apps").setValue(simpleAppInfo);
                    //databaseReference.child("childs").child(key).child("apps").removeValue();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

}
