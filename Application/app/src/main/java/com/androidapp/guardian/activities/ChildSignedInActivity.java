package com.androidapp.guardian.activities;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.androidapp.guardian.utils.Alert;
import com.androidapp.guardian.utils.DAOAlert;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.androidapp.guardian.R;
import com.androidapp.guardian.dialogfragments.InformationDialogFragment;
import com.androidapp.guardian.dialogfragments.PasswordValidationDialogFragment;
import com.androidapp.guardian.dialogfragments.PermissionExplanationDialogFragment;
import com.androidapp.guardian.interfaces.OnPasswordValidationListener;
import com.androidapp.guardian.interfaces.OnPermissionExplanationListener;
import com.androidapp.guardian.services.MainForegroundService;
import com.androidapp.guardian.utils.Constant;
import com.androidapp.guardian.utils.SharedPrefsUtils;
import com.androidapp.guardian.utils.Validators;
import org.tensorflow.lite.examples.textclassification.client.Result;
import org.tensorflow.lite.examples.textclassification.client.TextClassificationClient;


import java.util.List;


public class ChildSignedInActivity extends AppCompatActivity implements OnPermissionExplanationListener, OnPasswordValidationListener {
	public static final int JOB_ID = 38;
	public static final String CHILD_EMAIL = "childEmail";
	private static final String TAG = "ChildSignedInTAG";
	private FirebaseAuth auth;
	private FirebaseUser user;
	private ImageButton btnBack;
	private ImageButton btnSettings;
	private TextView txtTitle;
	private FrameLayout toolbar;
	private static final String TAG2 = "TextClassificationDemo";

	private TextClassificationClient client;
	private Handler handler;

	private NotificationReceiver nReceiver;
	DAOAlert daoAlert;
	String[] predatorsWords = {"8", "99", "142", "182","1174","ASL","CD9",
			"FYEO","GNOC","GYPO","HAK","IWSN","KFY","KPC","MIRL","MOS",
			"NIFOC","NSFW","P911","PAW","PAL","PIR","POS","PRON",
			"RUMORF","SWAK","TDTM","WTTO"};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_child_signed_in);

		startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
		nReceiver = new NotificationReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("NOTIFICATION_LISTENER_EXAMPLE");
		registerReceiver(nReceiver,filter);
		daoAlert = new DAOAlert();

		client = new TextClassificationClient(getApplicationContext());
		handler = new Handler();

		boolean childFirstLaunch = SharedPrefsUtils.getBooleanPreference(this, Constant.CHILD_FIRST_LAUNCH, true);

		if (childFirstLaunch){
			startActivity(new Intent(this, PermissionsActivity.class));
			//startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
		}
		else {
			
			auth = FirebaseAuth.getInstance();
			user = auth.getCurrentUser();

			
			String email = user.getEmail();
            /*PersistableBundle bundle = new PersistableBundle();
            bundle.putString(CHILD_EMAIL, email);*/
			
			toolbar = findViewById(R.id.toolbar);
			btnBack = findViewById(R.id.btnBack);
			btnBack.setImageDrawable(getResources().getDrawable(R.drawable.ic_home_));
			//btnSettings = findViewById(R.id.btnSettings);
			/*btnSettings.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startPasswordValidationDialogFragment();
				}
			});*/
			txtTitle = findViewById(R.id.txtTitle);
			txtTitle.setText(getString(R.string.home));
			
			//schedualJob(bundle);
			startMainForegroundService(email);
			
			if (!Validators.isLocationOn(this)) {
				startPermissionExplanationDialogFragment();
			}
			
			if (!Validators.isInternetAvailable(this))
				startInformationDialogFragment(getResources().getString(R.string.you_re_offline_ncheck_your_connection_and_try_again));
			
		}


	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.v(TAG2, "onStart");
		handler.post(
				() -> {
					client.load();
				});
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.v(TAG2, "onStop");
		handler.post(
				() -> {
					client.unload();
				});
	}
	
	private void startMainForegroundService(String email) {
		Intent intent = new Intent(this, MainForegroundService.class);
		intent.putExtra(CHILD_EMAIL, email);
		ContextCompat.startForegroundService(this, intent);
		
	}
	
	private void startPermissionExplanationDialogFragment() {
		PermissionExplanationDialogFragment permissionExplanationDialogFragment = new PermissionExplanationDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putInt(Constant.PERMISSION_REQUEST_CODE, Constant.CHILD_LOCATION_PERMISSION_REQUEST_CODE);
		permissionExplanationDialogFragment.setArguments(bundle);
		permissionExplanationDialogFragment.setCancelable(false);
		permissionExplanationDialogFragment.show(getSupportFragmentManager(), Constant.PERMISSION_EXPLANATION_FRAGMENT_TAG);
	}
	
	private void startInformationDialogFragment(String message) {
		InformationDialogFragment informationDialogFragment = new InformationDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(Constant.INFORMATION_MESSAGE, message);
		informationDialogFragment.setArguments(bundle);
		informationDialogFragment.setCancelable(false);
		informationDialogFragment.show(getSupportFragmentManager(), Constant.INFORMATION_DIALOG_FRAGMENT_TAG);
	}
	
	private void startPasswordValidationDialogFragment() {
		PasswordValidationDialogFragment passwordValidationDialogFragment = new PasswordValidationDialogFragment();
		passwordValidationDialogFragment.setCancelable(false);
		passwordValidationDialogFragment.show(getSupportFragmentManager(), Constant.PASSWORD_VALIDATION_DIALOG_FRAGMENT_TAG);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == Constant.DEVICE_ADMIN_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				Log.i(TAG, "onActivityResult: DONE");
			}
		}
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	@Override
	public void onOk(int requestCode) {
		startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
	}
	
	@Override
	public void onCancel(int switchId) {
		Toast.makeText(this, getString(R.string.canceled), Toast.LENGTH_SHORT).show();
		
	}
	
	@Override
	public void onValidationOk() {
		/*Intent intent = new Intent(ChildSignedInActivity.this, SettingsActivity.class);
		startActivity(intent);*/
	}

	/** Send input text to TextClassificationClient and get the classify messages. */
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
		// Run on UI thread as we'll updating our app UI
		runOnUiThread(
				() -> {
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


				});
	}




	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(nReceiver);
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
	
    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void schedualJob(PersistableBundle bundle) {
        ComponentName componentName = new ComponentName(this, UploadAppsService.class);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .setExtras(bundle)
                .build();
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);

        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            //Success
        } else {
            //Failure
        }
    }*/

    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void cancelJob() {
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
        //Job cancelled
    }*/
	
	
}
