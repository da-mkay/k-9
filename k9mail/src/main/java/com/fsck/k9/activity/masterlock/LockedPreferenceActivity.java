package com.fsck.k9.activity.masterlock;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * See {@link LockedActivityCommon}.
 * <p>
 * Created by da-mkay on 01.03.17.
 */

public abstract class LockedPreferenceActivity extends PreferenceActivity implements LockedActivityCommon.LockFiltered {
    private LockedActivityCommon mCommon;

    public LockedPreferenceActivity() {
        super();
        mCommon = new LockedActivityCommon(this);
    }

    public LockedActivityCommon getLockedActivityCommon() {
        return mCommon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCommon.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCommon.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCommon.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCommon.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCommon.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCommon.onDestroy();
    }

    protected void onMasterLockSettingsChanged() {
        getLockedActivityCommon().getMasterLockService().onSettingsChanged();
    }
}
