package com.androidapp.guardian.fragments;

import static android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.androidapp.guardian.R;
import com.androidapp.guardian.broadcasts.AdminR;
import com.androidapp.guardian.dialogfragments.InformationDialogFragment;
import com.androidapp.guardian.interfaces.OnFragmentChangeListener;
import com.androidapp.guardian.utils.Constant;

public class SettingsPermissionsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
	private Switch switchWriteSettingsPermission;
	private Switch switchOverlayPermission;
	private Switch switchPackageUsagePermission;
	private Switch switchDeviceAdminPermission;
	private Context context;
	private Activity activity;
	private View layout;
	private FragmentManager fragmentManager;
	private OnFragmentChangeListener onFragmentChangeListener;
	private Button btnPermissionsSettingsPrev;
	private Button btnPermissionsSettingsNext;
	private Switch switchClassificationPermission;
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_permissions_settings, container, false);
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		context = getContext();
		activity = getActivity();
		layout = view;
		fragmentManager = getFragmentManager();
		onFragmentChangeListener = (OnFragmentChangeListener) activity;
		
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			ImageView imgSecondDot = view.findViewById(R.id.imgSecondDot);
			imgSecondDot.setVisibility(View.GONE);
			ImageView imgThirdDot = view.findViewById(R.id.imgThirdDot);
			imgThirdDot.setVisibility(View.GONE);
			ImageView imgFourthDot = view.findViewById(R.id.imgFourthDot);
			imgFourthDot.setVisibility(View.GONE);
			FrameLayout layoutFirstPermission = view.findViewById(R.id.layoutFirstPermission);
			layoutFirstPermission.setVisibility(View.GONE);
			FrameLayout layoutSecondPermission = view.findViewById(R.id.layoutSecondPermission);
			layoutSecondPermission.setVisibility(View.GONE);
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
				FrameLayout layoutThirdPermission = view.findViewById(R.id.layoutThirdPermission);
				layoutThirdPermission.setVisibility(View.GONE);
			}
		}
		
		btnPermissionsSettingsNext = view.findViewById(R.id.btnPermissionsSettingsNext);
		btnPermissionsSettingsNext.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (checkAllPermissions()) {
					onFragmentChangeListener.onFragmentChange(Constant.PERMISSIONS_FRAGMENTS_FINISH);
				} else {
					startInformationDialogFragment(getString(R.string.please_allow_permissions));
				}
			}
		});
		
		btnPermissionsSettingsPrev = view.findViewById(R.id.btnPermissionsSettingsPrev);
		btnPermissionsSettingsPrev.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
					onFragmentChangeListener.onFragmentChange(Constant.PERMISSIONS_MAIN_FRAGMENT);
				else
					onFragmentChangeListener.onFragmentChange(Constant.PERMISSIONS_LOCATION_FRAGMENT);
			}
		});
		
		
		switchWriteSettingsPermission = view.findViewById(R.id.switchWriteSettingsPermission);
		switchWriteSettingsPermission.setChecked(isWriteSettingsPermissionGranted());
		switchWriteSettingsPermission.setOnCheckedChangeListener(this);
		switchOverlayPermission = view.findViewById(R.id.switchOverlayPermission);
		switchOverlayPermission.setChecked(isOverlayPermissionGranted());
		switchOverlayPermission.setOnCheckedChangeListener(this);
		switchPackageUsagePermission = view.findViewById(R.id.switchPackageUsagePermission);
		switchPackageUsagePermission.setChecked(isPackageUsagePermissionGranted());
		switchPackageUsagePermission.setOnCheckedChangeListener(this);
		switchDeviceAdminPermission = view.findViewById(R.id.switchDeviceAdminPermission);
		switchDeviceAdminPermission.setChecked(isDeviceAdmin());
		switchDeviceAdminPermission.setOnCheckedChangeListener(this);
		switchClassificationPermission = view.findViewById(R.id.switchClassificationPermission);
		switchClassificationPermission.setChecked(isReadingMessagePermissionGranted());
		switchClassificationPermission.setOnCheckedChangeListener(this);
		
	}
	
	private boolean isPackageUsagePermissionGranted() {
		return ContextCompat.checkSelfPermission(context, Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED;    //TODO:: check later
	}

	private boolean isReadingMessagePermissionGranted(){
		return NotificationManagerCompat.getEnabledListenerPackages(context) != null &&
				(NotificationManagerCompat.getEnabledListenerPackages(context)
						.contains(context.getPackageName()));
	}
	
	private boolean checkAllPermissions() {
		return isDeviceAdmin() && isWriteSettingsPermissionGranted() && isOverlayPermissionGranted();//TODO::PackageUsage
	}
	
	private boolean isWriteSettingsPermissionGranted() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.System.canWrite(context);
		} else {
			return true;//TODO::check below M
		}
	}
	
	private boolean isOverlayPermissionGranted() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return Settings.canDrawOverlays(context);
		} else {
			return true; //TODO:: check  below M
		}
	}
	
	private boolean isDeviceAdmin() {
		DevicePolicyManager devicePolicyManager = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
		ComponentName componentName = new ComponentName(context, AdminR.class);
		return devicePolicyManager.isAdminActive(componentName);
	}
	
	private void startInformationDialogFragment(String message) {
		InformationDialogFragment informationDialogFragment = new InformationDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(Constant.INFORMATION_MESSAGE, message);
		informationDialogFragment.setArguments(bundle);
		informationDialogFragment.setCancelable(false);
		informationDialogFragment.show(getChildFragmentManager(), Constant.INFORMATION_DIALOG_FRAGMENT_TAG);
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked) {
			switch (buttonView.getId()) {
				case R.id.switchWriteSettingsPermission:
					requestWriteSettingsPermission();
					break;
				
				case R.id.switchOverlayPermission:
					requestOverlayPermission();
					break;
				
				case R.id.switchPackageUsagePermission:
					requestPackageUsagePermission();
					break;
				case R.id.switchDeviceAdminPermission:
					requestDeviceAdminPermission();
					break;
				case R.id.switchClassificationPermission:
					allowForClassificationPermission();
					break;
			}
		}
		
	}
	
	private void requestWriteSettingsPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.System.canWrite(context)) {
				Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
				//startActivityForResult(intent, Constant.WRITE_SETTINGS_PERMISSION_REQUEST_CODE);
				startActivity(intent);
			} else {
				switchWriteSettingsPermission.setChecked(true);
				// switchWriteSettingsPermission.setEnabled(false);
			}
		}
	}


	private void allowForClassificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			String packageName = context.getPackageName();
			if (NotificationManagerCompat.getEnabledListenerPackages(context) != null &&
					!(NotificationManagerCompat.getEnabledListenerPackages(context)
			.contains(packageName))) {
				Intent intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
				startActivity(intent);
			} else {
				switchClassificationPermission.setChecked(true);
				// switchWriteSettingsPermission.setEnabled(false);
			}
		}
	}
	
	private void requestOverlayPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!Settings.canDrawOverlays(context)) {
				Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
				//startActivityForResult(intent, Constant.OVERLAY_PERMISSION_REQUEST_CODE);
				startActivity(intent);
			} else {
				switchOverlayPermission.setChecked(true);
				//switchOverlayPermission.setEnabled(false);
			}
		}
	}
	
	private void requestPackageUsagePermission() {
		if (ContextCompat.checkSelfPermission(context, Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED) {
			Intent intent = null;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
				startActivity(intent);
			}
			//startActivityForResult(intent, Constant.PACKAGE_USAGE_PERMISSION_REQUEST_CODE);
        /*if (ContextCompat.checkSelfPermission(context, Manifest.permission.PACKAGE_USAGE_STATS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.PACKAGE_USAGE_STATS)) {
                startPermissionExplanationFragment(Constant.PACKAGE_USAGE_PERMISSION_REQUEST_CODE, switchPackageUsagePermission.getId());

            } else {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
            }*/
		}
	}
	
	private void requestDeviceAdminPermission() {
		DevicePolicyManager devicePolicyManager = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
		ComponentName componentName = new ComponentName(context, AdminR.class);
		boolean adminActive = devicePolicyManager.isAdminActive(componentName);
		enableDeviceAdmin(componentName);
		
	}
	
	private void enableDeviceAdmin(ComponentName componentName) {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
		intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getResources().getString(R.string.device_admin_explanation));
		startActivityForResult(intent, Constant.DEVICE_ADMIN_REQUEST_CODE);
	}
	
	private void disableDeviceAdmin(DevicePolicyManager devicePolicyManager, ComponentName componentName) {
		devicePolicyManager.removeActiveAdmin(componentName);
	}
}
