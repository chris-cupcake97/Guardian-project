package com.androidapp.guardian.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.androidapp.guardian.utils.Alert;
import com.androidapp.guardian.utils.DAOAlert;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.androidapp.guardian.R;
import com.androidapp.guardian.activities.BlockedAppActivity;
import com.androidapp.guardian.activities.ChildSignedInActivity;
import com.androidapp.guardian.broadcasts.AppInstalledReceiver;
import com.androidapp.guardian.broadcasts.AppRemovedReceiver;
import com.androidapp.guardian.broadcasts.ScreenTimeReceiver;
import com.androidapp.guardian.models.App;
import com.androidapp.guardian.models.Child;
import com.androidapp.guardian.models.ScreenLock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.androidapp.guardian.NotificationChannelCreator.CHANNEL_ID;

import org.tensorflow.lite.examples.textclassification.client.Result;
import org.tensorflow.lite.examples.textclassification.client.TextClassificationClient;

public class MainForegroundService extends Service {
	public static final int NOTIFICATION_ID = 27;
	public static final String TAG = "MainServiceTAG";
	public static final String BLOCKED_APP_NAME_EXTRA = "com.androidapp.guardian.services.BLOCKED_APP_NAME_EXTRA";
	public static final int LOCATION_UPDATE_INTERVAL = 1;    //every 5 seconds
	public static final int LOCATION_UPDATE_DISPLACEMENT = 5;  //every 10 meters
	private ExecutorService executorService;
	private ArrayList<App> apps;
	/*private PhoneStateReceiver phoneStateReceiver;
	private SmsReceiver smsReceiver;*/
	private AppInstalledReceiver appInstalledReceiver;
	private AppRemovedReceiver appRemovedReceiver;
	private ScreenTimeReceiver screenTimeReceiver;
	private String uid;
	private String childEmail;
	private FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance("https://guardianapp-47975-default-rtdb.firebaseio.com/");
	private DatabaseReference databaseReference = firebaseDatabase.getReference("users");
	private TextClassificationClient client;
	private Handler handler;

	private NotificationReceiver nReceiver;
	DAOAlert daoAlert;
	
	@Override
	public void onCreate() {
		super.onCreate();
		executorService = Executors.newSingleThreadExecutor();
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		LockerThread thread = new LockerThread();
		executorService.submit(thread);
		new Thread(new Runnable() {
			@Override
			public void run() {
				getInstalledApplications();
			}
		}).start();
		Log.i(TAG, "onCreate: executed");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		FirebaseAuth auth = FirebaseAuth.getInstance();
		FirebaseUser user = auth.getCurrentUser();
		childEmail = user.getEmail();
		uid = user.getUid();
		
		Intent notificationIntent = new Intent(this, ChildSignedInActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_kidsafe).setContentIntent(pendingIntent).build();
		
		startForeground(NOTIFICATION_ID, notification);
		
		getUserLocation();
		
		Query appsQuery = databaseReference.child("childs").child(uid).child("apps");
		appsQuery.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if (dataSnapshot.exists()) {
					getApps();
				}
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
			
			}
		});
		
		Query locationQuery = databaseReference.child("childs").child(uid).child("location");
		locationQuery.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if (dataSnapshot.exists()) {
					setFence(dataSnapshot);
				}
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
			
			}
		});
		
		Query screenTimeQuery = databaseReference.child("childs").child(uid).child("screenLock");
		screenTimeQuery.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if (dataSnapshot.exists()) {
					ScreenLock screenLock = dataSnapshot.getValue(ScreenLock.class);
					Log.i(TAG, "onDataChangeX: hours: " + screenLock.getHours());
					Log.i(TAG, "onDataChangeX: minutes: " + screenLock.getMinutes());
					Log.i(TAG, "onDataChangeX: isLocked: " + screenLock.isLocked());
					
					if (screenLock.isLocked()) {
						screenTimeReceiver = new ScreenTimeReceiver(screenLock);
						IntentFilter screenTimeIntentFilter = new IntentFilter();
						screenTimeIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
						screenTimeIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
						registerReceiver(screenTimeReceiver, screenTimeIntentFilter);
					} else {
						if (screenTimeReceiver != null) {
							unregisterReceiver(screenTimeReceiver);
						}
					}
				}
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
			
			}
		});

		appInstalledReceiver = new AppInstalledReceiver(user);
		IntentFilter appInstalledIntentFilter = new IntentFilter();
		appInstalledIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
		//appInstalledIntentFilter.addAction(Intent.ACTION_PACKAGE_INSTALL);
		appInstalledIntentFilter.addDataScheme("package");
		registerReceiver(appInstalledReceiver, appInstalledIntentFilter);
		
		appRemovedReceiver = new AppRemovedReceiver(user);
		IntentFilter appRemovedIntentFilter = new IntentFilter();
		appRemovedIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		appRemovedIntentFilter.addDataScheme("package");
		registerReceiver(appRemovedReceiver, appRemovedIntentFilter);

		Log.i(TAG,"before");
		addMessageNootification();
		Log.i(TAG,"after");

		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (executorService != null) {
			executorService.shutdown();
		}

		if (appInstalledReceiver != null) {
			unregisterReceiver(appInstalledReceiver);
		}
		if (appRemovedReceiver != null) {
			unregisterReceiver(appRemovedReceiver);
		}
		if (screenTimeReceiver != null) {
			unregisterReceiver(screenTimeReceiver);
		}

		unregisterReceiver(nReceiver);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void getApps() {
		Query query = databaseReference.child("childs").orderByChild("email").equalTo(childEmail);
		query.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if (dataSnapshot.exists()) {
					//Log.i(TAG, "onDataChange: dataSnapshot value: "+dataSnapshot.getValue());
					//Log.i(TAG, "onDataChange: dataSnapshot as a string: "+dataSnapshot.toString());
					//Log.i(TAG, "onDataChange: dataSnapshot children: " + dataSnapshot.getChildren());
					//Log.i(TAG, "onDataChange: dataSnapshot key: " + dataSnapshot.getKey());
					
					DataSnapshot nodeShot = dataSnapshot.getChildren().iterator().next();
					Child child = nodeShot.getValue(Child.class);
					apps = child.getApps();
					
					Log.i(TAG, "onDataChange: child name: " + child.getName());
					//updateAppStats(apps);
					
				}
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
			
			}
		});
	}
	
	private void getUserLocation() {
		Log.i(TAG, "getUserLocation: executed");
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		LocationListener locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				if (location != null) {
					Log.i(TAG, "onLocationChanged: latitude: " + location.getLatitude());
					Log.i(TAG, "onLocationChanged: longitude: " + location.getLongitude());
					addUserLocationToDatabase(location, uid);
				} else {
					Log.i(TAG, "onLocationChanged: location is null");
				}
			}
			
			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
			
			}
			
			@Override
			public void onProviderEnabled(String provider) {
			
			}
			
			@Override
			public void onProviderDisabled(String provider) {
			
			}
		};
		
		//these two statements will be only executed when the permission is granted.
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISPLACEMENT, locationListener);
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISPLACEMENT, locationListener);
			return;
		}
		
	}


	private void addMessageNootification() {
		Log.i(TAG, "getUserLocation: executed");

		nReceiver = new NotificationReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("NOTIFICATION_LISTENER_EXAMPLE");
		registerReceiver(nReceiver,filter);
		daoAlert = new DAOAlert();

		client = new TextClassificationClient(getApplicationContext());

		LocationListener locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				if (location != null) {
					Log.i(TAG, "onLocationChanged: latitude: " + location.getLatitude());
					Log.i(TAG, "onLocationChanged: longitude: " + location.getLongitude());
					addUserLocationToDatabase(location, uid);
				} else {
					Log.i(TAG, "onLocationChanged: location is null");
				}
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {

			}

			@Override
			public void onProviderEnabled(String provider) {

			}

			@Override
			public void onProviderDisabled(String provider) {

			}
		};


	}
	
	private void addUserLocationToDatabase(Location location, String uid) {
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		HashMap<String, Object> update = new HashMap<>();
		update.put("latitude", latitude);
		update.put("longitude", longitude);
		databaseReference.child("childs").child(uid).child("location").updateChildren(update);
	}

	
	private void setFence(DataSnapshot dataSnapshot) {
		final com.androidapp.guardian.models.Location childLocation = dataSnapshot.getValue(com.androidapp.guardian.models.Location.class);
		Log.i(TAG, "setFence: getLatitude " + childLocation.getLatitude());
		Log.i(TAG, "setFence: getLongitude " + childLocation.getLongitude());
		Log.i(TAG, "setFence: isGeoFence " + childLocation.isGeoFence());
		Log.i(TAG, "setFence: isOutOfFence " + childLocation.isOutOfFence());
		Log.i(TAG, "setFence: getFenceCenterLatitude " + childLocation.getFenceCenterLatitude());
		Log.i(TAG, "setFence: getFenceCenterLongitude " + childLocation.getFenceCenterLongitude());
		Log.i(TAG, "setFence: getFenceDiameter " + childLocation.getFenceDiameter());
		
		if (childLocation.isGeoFence()) {
			Log.i(TAG, "setFence: true");
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			LocationListener locationListener = new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					Log.i(TAG, "setFence: changed");
					if (location != null) {
						float[] distanceInMeters = new float[1];
						Location.distanceBetween(childLocation.getFenceCenterLatitude(), childLocation.getFenceCenterLongitude(), location.getLatitude(), location.getLongitude(), distanceInMeters);
						
						boolean outOfFence = distanceInMeters[0] > childLocation.getFenceDiameter();
						if (outOfFence) {
							Log.i(TAG, "setFence: OUT OF FENCE");
							databaseReference.child("childs").child(uid).child("location").child("outOfFence").setValue(true);
						} else {
							databaseReference.child("childs").child(uid).child("location").child("outOfFence").setValue(false);
						}
					} else {
						Log.i(TAG, "setFence: location is null");
					}
				}
				
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
				
				}
				
				@Override
				public void onProviderEnabled(String provider) {
				
				}
				
				@Override
				public void onProviderDisabled(String provider) {
				
				}
			};
			
			//these two statements will be only executed when the permission is granted.
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISPLACEMENT, locationListener);
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_INTERVAL, LOCATION_UPDATE_DISPLACEMENT, locationListener);
				return;
			}
			
			
		}
		
	}

	
	private void getInstalledApplications(/*ArrayList<App> onlineAppsList*/) {
		PackageManager packageManager = getPackageManager();
		List<ApplicationInfo> applicationInfoList = packageManager.getInstalledApplications(0);
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
		prepareData(applicationInfoList, packageManager/*, onlineAppsList*/);
	}
	
	private void prepareData(List<ApplicationInfo> applicationInfoList, PackageManager packageManager/*, ArrayList<App> onlineAppsList*/) {
		ArrayList<App> appsList = new ArrayList<>();
		for (ApplicationInfo applicationInfo : applicationInfoList) {
			if (applicationInfo.packageName != null) {
				appsList.add(new App((String) applicationInfo.loadLabel(packageManager), applicationInfo.packageName, false));
			}
		}
		
		uploadApps(appsList);
		
	}
	
	private void uploadApps(ArrayList<App> appsList) {
		databaseReference.child("childs").child(uid).child("apps").setValue(appsList);
		Log.i(TAG, "uploadApps: done");
	}
	
	public String getTopAppPackageName() {
		String appPackageName = "";
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				appPackageName = getLollipopForegroundAppPackageName();
			} else {
				appPackageName = getKitkatForegroundAppPackageName();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return appPackageName;
	}
	
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private String getLollipopForegroundAppPackageName() {
		//Log.i(TAG, "getLollipopForegroundAppPackageName: executed");
		try {
			UsageStatsManager usageStatsManager = (UsageStatsManager) this.getSystemService(USAGE_STATS_SERVICE);
			long milliSecs = 60 * 1000;
			Date date = new Date();
			List<UsageStats> foregroundApps = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, date.getTime() - milliSecs, date.getTime());
			if (foregroundApps.size() == 0) {
				Log.i(TAG, "getLollipopForegroundAppPackageName: queryUsageSize: empty");
			}
			
			
			long recentTime = 0;
			String recentPkg = "";
			for (UsageStats stats : foregroundApps) {
                /*if (i == 0 && !"com.androidapp.guardian".equals(stats.getPackageName())) {
                    Log.i(TAG, "PackageName: " + stats.getPackageName() + " " + stats.getLastTimeStamp());
                }*/
				if (stats.getLastTimeStamp() > recentTime) {
					recentTime = stats.getLastTimeStamp();
					recentPkg = stats.getPackageName();
				}
				
			}
			
			//Log.i(TAG, "getLollipopForegroundAppPackageName: appPackageName: " + recentPkg);
			return recentPkg;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return "";
	}
	
	private String getKitkatForegroundAppPackageName() {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> tasks = activityManager.getRunningAppProcesses();
		return tasks.get(0).processName;
	}
	
	class LockerThread implements Runnable {
		
		private Intent intent = null;
		
		public LockerThread() {
			intent = new Intent(MainForegroundService.this, BlockedAppActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		
		@Override
		public void run() {
			while (true) {
				//Log.i(TAG, "run: thread running");
				
				if (apps != null) {
					
					String foregroundAppPackageName = getTopAppPackageName();
					Log.i(TAG, "run: foreground app: " + foregroundAppPackageName);
					
					//TODO:: need to handle com.google.android.gsf &  com.sec.android.provider.badge
					for (final App app : apps) {
						if (foregroundAppPackageName.equals(app.getPackageName()) && app.isBlocked()) {
							intent.putExtra(BLOCKED_APP_NAME_EXTRA, app.getAppName());
							startActivity(intent);
						}
						
					}
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

	class NotificationReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			client = new TextClassificationClient(context);
			client.load();
			//   handler = new Handler();
//            String temp = intent.getStringExtra("notification_event") + "\n" + txtView.getText();
			if (intent.getStringExtra("from")!=null){
				String from = intent.getStringExtra("from");
				String message = intent.getStringExtra("message");
//                txtView.setText(from+" "+message);
				classify(from,message);


//                Alert alert=new Alert(from,message,checkMessage(message));
//                daoAlert.add(alert).addOnSuccessListener(suc->{
//                    Toast.makeText(getApplicationContext(),"Record is inserted",Toast.LENGTH_SHORT).show();
//                }).addOnFailureListener(er->
//                {
//                    Toast.makeText(getApplicationContext(),""+er.getMessage(),Toast.LENGTH_SHORT).show();
//                });
			}


		}
	}

	private void classify(final String from,final String text) {
		handler.post(
				() -> {
					// Run text classification with TF Lite.

					List<Result> results = client.classify(text);

					// Show classification result on screen
					showResult(from, text, results);

				});
	}

	/** Show classification result on the screen. */
	private void showResult(final String from,final String inputText, final List<Result> results) {

		String textToShow = "Input: " + inputText + "\nOutput:\n";
		for (int i = 0; i < results.size(); i++) {
			Result result = results.get(i);
			if(result.getTitle().equals("Positive")){
				Alert alert;
				if(result.getConfidence()>0.67){
					textToShow += "Alert: Looks like predator!!!\n";
					alert=new Alert(from,inputText,"Looks like predator!!");
				}else{
					alert=new Alert(from,inputText,"looks okay.");
				}

				daoAlert.add(alert).addOnSuccessListener(suc->{
					Log.i(TAG, alert+" Added to database");
					Toast.makeText(getApplicationContext(),"Record is inserted",Toast.LENGTH_SHORT).show();
				}).addOnFailureListener(er->
				{
					Log.i(TAG, alert+" Failed to be Added to database");
					Toast.makeText(getApplicationContext(),""+er.getMessage(),Toast.LENGTH_SHORT).show();
				});
			}
			textToShow += String.format("    %s: %s\n", result.getTitle(), result.getConfidence());
		}
		textToShow += "---------\n";

	}
	
}