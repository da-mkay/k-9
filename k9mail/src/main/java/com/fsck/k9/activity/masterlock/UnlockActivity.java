package com.fsck.k9.activity.masterlock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.preferences.PasswordHash;
import com.fsck.k9.service.MasterLockService;

public class UnlockActivity extends K9Activity {
    public static final String EXTRA_ORIGINAL_INTENT = "com.fsck.k9.activity.masterlock.EXTRA_ORIGINAL_INTENT";
    public static final String EXTRA_ORIGINAL_CLASS = "com.fsck.k9.activity.masterlock.EXTRA_ORIGINAL_CLASS";

    private EditText mPasswordEdit;
    private Intent mOriginalIntent;
    private String mOriginalClass;
    private boolean mCalledForResult;

    public UnlockActivity() {
        super();
        // This activity is always unlocked!
        getLockedActivityCommon().setLockEnabled(false);
    }

    @Override
    public void onCreateUnlocked(Bundle savedInstanceState) {
        super.onCreateUnlocked(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.unlock);
        mPasswordEdit = (EditText) findViewById(R.id.unlock_edit);

        mCalledForResult = getCallingActivity() != null;
        mOriginalIntent = getIntent().getParcelableExtra(EXTRA_ORIGINAL_INTENT);
        mOriginalClass = getIntent().getStringExtra(EXTRA_ORIGINAL_CLASS);
        if (mOriginalIntent != null) {
            try {
                // make explicit
                mOriginalIntent.setClass(this, Class.forName(mOriginalClass));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStartUnlocked() {
        super.onStartUnlocked();
        if (!MasterLockService.isLocked()) {
            goToOriginal();
        }
    }

    public void onUnlockButtonClick(View view) {
        String enteredPassword = mPasswordEdit.getText().toString();
        if (enteredPassword.isEmpty()) {
            mPasswordEdit.setError(getText(R.string.master_lock_password_error_empty));
            return;
        } else if (!PasswordHash.validatePassword(K9.getMasterPassword(), enteredPassword)) {
            mPasswordEdit.setError(getText(R.string.master_lock_password_error_incorrect));
            return;
        }
        getLockedActivityCommon().getMasterLockService().unlock();
        goToOriginal();
    }

    private void goToOriginal() {
        if (mOriginalIntent != null) {
            if (mCalledForResult) {
                mOriginalIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                startActivity(mOriginalIntent);
                finish(); // must be after startingActivity to get FORWARD_RESULT working
            } else {
                finish(); // must be before startActivity. Otherwise activity not shown
                startActivity(mOriginalIntent);
            }
            return;
        }
        finish();
    }
}
