package com.fsck.k9.activity.masterlock;

import android.app.ListActivity;
import android.os.Bundle;

/**
 * See {@link LockedActivityCommon}.
 * <p>
 * Created by da-mkay on 01.03.17.
 */

public abstract class LockedListActivity extends ListActivity implements LockedActivityCommon.LockFiltered {
    private LockedActivityCommon mCommon;

    public LockedListActivity() {
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
}
