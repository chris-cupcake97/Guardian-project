package com.androidapp.guardian.interfaces;

import android.view.View;

import com.androidapp.guardian.models.User;

public interface OnChildClickListener {
    void onItemClick(int position);

    void onWebFilterClick(boolean checked, User child);

    void onBtnLockClick(boolean checked, User child);

    void onLockPhoneSet(int hours, int minutes);

    void onLockCanceled();



}
