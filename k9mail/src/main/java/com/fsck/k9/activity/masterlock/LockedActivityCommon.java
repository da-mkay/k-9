package com.fsck.k9.activity.masterlock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.fsck.k9.K9;
import com.fsck.k9.service.MasterLockService;

import static com.fsck.k9.activity.masterlock.UnlockActivity.EXTRA_ORIGINAL_CLASS;
import static com.fsck.k9.activity.masterlock.UnlockActivity.EXTRA_ORIGINAL_INTENT;

/**
 * Common code of LockedActivity, LockedListActivity and LockedPreferenceActivity.
 * <p>
 * Those activities check in their onCreate- and onShow-method if the app is unlocked. If so,
 * nothing special happens. But if the app is locked, the user is redirected to UnlockActivity
 * forcing the user to unlock the app.
 * <p>
 * NOTE: Subclasses should override onXXXUnlocked() methods instead of their original onXXX()
 * methods to ensure proper handling when the user is redirected to UnlockActivity (example:
 * {@link LockedActivityCommon.LockFiltered#onCreateUnlocked(Bundle)}).
 * <p>
 * Created by da-mkay on 01.03.17.
 */

class LockedActivityCommon {

    private Activity mActivity;
    private LockFiltered mLockFiltered;

    private MasterLockService mMasterLockService;
    private boolean mLockEnabled = true;

    private LocalBroadcastManager mLocalBroadcastManager;
    private IntentFilter mIntentFilter;

    private boolean mUnlockActivityStarted = false;
    private boolean mPassedCreate = false;
    private boolean mPassedStart = false;
    private boolean mPassedResume = false;

    /*
     * Constructors
     */

    LockedActivityCommon(LockedActivity activity) {
        mActivity = activity;
        mLockFiltered = activity;
    }

    LockedActivityCommon(LockedListActivity activity) {
        mActivity = activity;
        mLockFiltered = activity;
    }

    LockedActivityCommon(LockedPreferenceActivity activity) {
        mActivity = activity;
        mLockFiltered = activity;
    }

    /*
     * Methods
     */

    public void setLockEnabled(boolean enabled) {
        mLockEnabled = enabled;
    }

    protected MasterLockService getMasterLockService() {
        return mMasterLockService;
    }

    /**
     * Check if app is unlocked.
     * <p>
     * If app is locked the user is redirected to UnlockActivity and the method returns false.
     * Otherwise the method returns true.
     * <p>
     * If onCreateUnlocked was not called before (the Activity was not fully initialized) then
     * the Activity is finished before redirecting. Otherwise it is kept on the back stack.
     * <p>
     * After unlocking, UnlockActivity will return to the Activity either by simply returning to
     * the Activity on the stack or re-sending the intent that was used to run the activity in the
     * first place.
     *
     * @return false, if redirected to UnlockActivity, true otherwise.
     */
    private boolean requireUnlocked() {
        if (K9.useMasterLock() && K9.getMasterPassword() != null && MasterLockService.isLocked()) {
            // Unlock required
            if (!mUnlockActivityStarted) {
                mUnlockActivityStarted = true;
                boolean calledForResult = mActivity.getCallingActivity() != null;

                Intent unlockIntent = new Intent(mActivity.getIntent()); // copy intent to ensure target has all permissions
                unlockIntent.setClass(mActivity, UnlockActivity.class); // make explicit
                unlockIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                if (calledForResult) {
                    unlockIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                }

                if (!mPassedCreate) {
                    // onCreateUnlocked was not called, meaning that the Activity was not
                    // initialized completely. So we tell UnlockActivity to resend the original
                    // intent.
                    Intent originalIntent = new Intent(mActivity.getIntent());
                    unlockIntent.putExtra(EXTRA_ORIGINAL_INTENT, originalIntent);
                    unlockIntent.putExtra(EXTRA_ORIGINAL_CLASS, mActivity.getClass().getName());
                    if (!calledForResult) {
                        mActivity.finish(); // finish before startActivity. Otherwise Activity may not show up ?!
                    }
                } // else: keep current activity on stack, UnlockActivity simply returns to it later

                mActivity.startActivity(unlockIntent);
                if (!mPassedCreate && calledForResult) {
                    mActivity.finish(); // must be after startActivity since we want a result back
                }
            }
            return false;
        }
        return true;
    }

    /*
     * Callbacks
     */

    protected void onCreate(Bundle savedInstanceState) {
        if (K9.useMasterLock() && K9.secureMasterLock()) {
            // Do not show in recents screen, disable screenshots etc.
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(mActivity);
        mIntentFilter = new IntentFilter(MasterLockService.ACTION_LOCKED);

        // Redirect?
        if (!mLockEnabled || requireUnlocked()) {
            mPassedCreate = true;
            mLockFiltered.onCreateUnlocked(savedInstanceState);
        }
        Log.d(LockedActivityCommon.class.getSimpleName(), "LOCK " + mActivity.getClass().getName());
    }

    protected void onStart() {
        // Bind to MasterLockService
        Intent intent = new Intent(mActivity, MasterLockService.class);
        mActivity.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // Redirect?
        if (!mLockEnabled || requireUnlocked()) {
            mPassedStart = true;
            mLockFiltered.onStartUnlocked();
        }
    }

    protected void onResume() {
        //mActivity.getWindow().getDecorView().getRootView().setVisibility(View.VISIBLE);
        mActivity.findViewById(android.R.id.content).setVisibility(View.VISIBLE);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    protected void onPause() {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        //mActivity.getWindow().getDecorView().getRootView().setVisibility(GONE);
        mActivity.findViewById(android.R.id.content).setVisibility(View.GONE);
    }

    protected void onStop() {
        // Unbind from MasterLockService
        if (mMasterLockService != null) {
            mActivity.unbindService(mServiceConnection);
            mMasterLockService = null;
        }
        // Call onStopUnlocked() if onStartUnlocked() was called before.
        if (mPassedStart) {
            mPassedStart = false;
            mLockFiltered.onStopUnlocked();
        }

        mUnlockActivityStarted = false;
    }

    protected void onDestroy() {
        // Call onDestroyUnlocked() if onCreateUnlocked() was called before.
        if (mPassedCreate) {
            mPassedCreate = false;
            mLockFiltered.onDestroyUnlocked();
        }
    }

    /**
     * Callbacks for service binding
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mMasterLockService = ((MasterLockService.MasterLockBinder) binder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Error
            mMasterLockService = null;
        }
    };

    /**
     * Receiver for broadcasts send by {@link MasterLockService}.
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            if (MasterLockService.ACTION_LOCKED.equals(action)) {
                requireUnlocked();
            }
        }
    };

    interface LockFiltered {
        void onCreateUnlocked(Bundle savedInstanceState);

        void onStartUnlocked();

        void onStopUnlocked();

        void onDestroyUnlocked();
    }
}
