package com.tapnic.biketrackerle;

import android.app.Activity;

import de.greenrobot.event.EventBus;

public class BaseActivity  extends Activity{
    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }
}
