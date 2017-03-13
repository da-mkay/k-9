package com.fsck.k9.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;

import com.fsck.k9.activity.K9ActivityCommon.K9ActivityMagic;
import com.fsck.k9.activity.masterlock.LockedActivity;
import com.fsck.k9.activity.misc.SwipeGestureDetector.OnSwipeGestureListener;


public class K9Activity extends LockedActivity implements K9ActivityMagic {

    private K9ActivityCommon mBase;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        mBase = K9ActivityCommon.newInstance(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateUnlocked(Bundle savedInstanceState) {
    }

    @Override
    public void onStartUnlocked() {
    }

    @Override
    public void onResumeUnlocked() {
    }

    @Override
    public void onPauseUnlocked() {
    }

    @Override
    public void onStopUnlocked() {
    }

    @Override
    public void onDestroyUnlocked() {
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mBase.preDispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void setupGestureDetector(OnSwipeGestureListener listener) {
        mBase.setupGestureDetector(listener);
    }
}
