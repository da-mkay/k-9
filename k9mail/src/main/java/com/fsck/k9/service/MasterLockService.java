package com.fsck.k9.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;

import static android.content.Intent.ACTION_MAIN;
import static android.content.Intent.CATEGORY_LAUNCHER;

/**
 * A central lock for all activities.
 * <p>
 * All activities should bind to this service in their onStart() method and unbind from this service
 * in their onStop() method to keep the current lock-state ([un]locked) while the app is in use, see
 * {@link com.fsck.k9.activity.masterlock.LockedActivityCommon}.
 * <p>
 * The current lock-state can be checked using the static method {@link #isLocked()}. For example
 * {@link com.fsck.k9.activity.masterlock.LockedActivityCommon} uses it to grant access to the
 * current activity or redirect the user to an {@link com.fsck.k9.activity.masterlock.UnlockActivity}
 * which forces the user to enter the master password.
 * <p>
 * The method {@link #unlock()} can then be used to unlock the app. It is mainly used by
 * {@link com.fsck.k9.activity.masterlock.UnlockActivity} to unlock the app after the correct master
 * password was entered.
 * <p>
 * When all activies unbind from the service (for example when app goes to background) it switches
 * automatically to the locked-state after some time.
 */
public class MasterLockService extends Service {
    /**
     * Broadcast intent sent when the app was locked, for example from the notification's lock button.
     */
    public static final String ACTION_LOCKED = "MasterLockService.locked";

    /**
     * XXX: Must not conflict with IDs in {@link com.fsck.k9.notification.NotificationIds}.
     */
    private static final int NOTIFICATION_ID_MASTER_LOCK = Integer.MAX_VALUE;

    private static final String ACTION_LOCK = "MasterLockService.lock";

    private static final String ACTION_AUTOLOCK = "MasterLockService.autolock";

    private static boolean mLocked = true;

    private final IBinder mBinder = new MasterLockBinder();
    private LocalBroadcastManager mLocalBroadcastManager;

    private AlarmManager mAlarmManager;
    private PendingIntent mLockAlarmIntent;

    private void stopLockService() {
        mLocked = true;
        stopForeground(true);
        stopSelf();
    }

    private void lock() {
        stopLockService();

        // Inform activity about lock state
        Intent intent = new Intent();
        intent.setAction(ACTION_LOCKED);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    public static boolean isLocked() {
        return mLocked;
    }

    public void unlock() {
        mLocked = false;
        startService(new Intent(this, MasterLockService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mAlarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        // Set up intent that is used in onUnbind() to auto-lock app after some time of inactivity
        Intent lockAlarmIntent = new Intent(this, AlarmReceiver.class);
        lockAlarmIntent.setAction(ACTION_AUTOLOCK);
        mLockAlarmIntent = PendingIntent.getBroadcast(this, 0, lockAlarmIntent, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlarmManager.cancel(mLockAlarmIntent);
        mLocked = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        // Remove a potentially pending auto-lock that was added in onUnbind()
        mAlarmManager.cancel(mLockAlarmIntent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All unbound --> lock in X seconds if unlocked
        long timeout = K9.getMasterLockTimeout();
        if (!mLocked && timeout >= 0) {
            long millis = SystemClock.elapsedRealtime() + timeout;
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, millis, mLockAlarmIntent);
        }
        return true; // We want onRebind to be called when a new activity binds to us
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_LOCK.equals(intent.getAction())) {
            // Locked from button in notification
            lock();
            return Service.START_NOT_STICKY;
        } else if (ACTION_AUTOLOCK.equals(intent.getAction())) {
            // Auto-locked after inactivity
            Toast.makeText(this, R.string.master_lock_toast_locked, Toast.LENGTH_SHORT).show();
            lock();
            return Service.START_NOT_STICKY;
        }

        // Clicking notification brings app to the front
        Intent notificationIntent = new Intent(this, Accounts.class);
        notificationIntent.setAction(ACTION_MAIN);
        notificationIntent.addCategory(CATEGORY_LAUNCHER);
        PendingIntent pNotificationIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        // Clicking lock-button locks the app
        Intent lockIntent = new Intent(this, MasterLockService.class);
        lockIntent.setAction(ACTION_LOCK);
        PendingIntent pLockIntent = PendingIntent.getService(this, 0, lockIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getText(R.string.master_lock_notification_title))
                .setContentText(getText(R.string.master_lock_notification_text))
                .setSmallIcon(R.drawable.icon) // TODO different icon?
                .setContentIntent(pNotificationIntent)
                .addAction(R.drawable.status_lock, getText(R.string.master_lock_button_lock), pLockIntent)
                .build();
        startForeground(NOTIFICATION_ID_MASTER_LOCK, notification);

        return Service.START_NOT_STICKY;
    }

    /**
     * Called when settings were saved.
     */
    public void onSettingsChanged() {
        if (!mLocked && !K9.useMasterLock()) {
            // App is unlocked, meaning that service is in foreground.
            // But lock is disabled now --> stop
            stopLockService();
        }
    }

    public class MasterLockBinder extends Binder {
        public MasterLockService getService() {
            // Return this instance of MasterLockService so clients can call public methods
            return MasterLockService.this;
        }
    }

    public static class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (ACTION_AUTOLOCK.equals(intent.getAction())) {
                Intent lockIntent = new Intent(context, MasterLockService.class);
                lockIntent.setAction(ACTION_AUTOLOCK);
                context.startService(lockIntent);
            }
        }
    }
}
