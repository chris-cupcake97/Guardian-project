package com.androidapp.guardian.interfaces;

public interface OnAppClickListener {
    void onItemClick(String packageName, String appName, boolean blocked);
}
