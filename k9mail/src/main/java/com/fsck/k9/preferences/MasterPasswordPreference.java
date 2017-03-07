package com.fsck.k9.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.fsck.k9.R;

/**
 * Created by da-mkay on 27.02.17.
 */

public class MasterPasswordPreference extends DialogPreference {

    private String mCurPasswordHash;
    private EditText mPwdEditOld;
    private EditText mPwdEditNew;
    private EditText mPwdEditNewRpt;

    public MasterPasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.master_password_preference_dialog);
        setNegativeButtonText(android.R.string.cancel);
        setPositiveButtonText(android.R.string.ok);
    }

    public void setMasterPassword(String password) {
        mCurPasswordHash = password;
    }

    public String getMasterPassword() {
        return mCurPasswordHash;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mPwdEditOld = (EditText) view.findViewById(R.id.master_password_preference_old);
        mPwdEditNew = (EditText) view.findViewById(R.id.master_password_preference_new);
        mPwdEditNewRpt = (EditText) view.findViewById(R.id.master_password_preference_repeat);
        if (mCurPasswordHash == null) {
            mPwdEditOld.setVisibility(View.GONE);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Override the click listener for the OK-button such that it does NOT
        // close the dialog if the old password is wrong or the new passwords
        // do not match.

        final AlertDialog d = (AlertDialog) getDialog();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurPasswordHash != null && !PasswordHash.validatePassword(mCurPasswordHash, mPwdEditOld.getText().toString())) {
                    mPwdEditOld.setError(getContext().getText(R.string.master_lock_password_error_incorrect));
                    return;
                }
                if (mPwdEditNew.getText().toString().equals("")) {
                    mPwdEditNew.setError(getContext().getText(R.string.master_lock_password_error_empty));
                    return;
                }
                if (!mPwdEditNew.getText().toString().equals(mPwdEditNewRpt.getText().toString())) {
                    mPwdEditNewRpt.setError(getContext().getText(R.string.master_lock_password_error_mismatch));
                    return;
                }

                // Override mCurPasswordHash such that getMasterPassword returns the new password.
                mCurPasswordHash = PasswordHash.generateHashString(mPwdEditNew.getText().toString());
                d.dismiss();
            }
        });
    }
}
