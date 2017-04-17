package com.sid.xmppconnect.service;

import android.os.Binder;

import java.lang.ref.WeakReference;

/**
 * Created by Jethva Sagar (Piml Sid) on 3/1/17
 * Email : jethvasagar2@gmail.com.
 */

public class LocalBinder<S> extends Binder{

    private final WeakReference<S> mService;

    public LocalBinder(final S service){
        mService = new WeakReference<S>(service);
    }
    public S getService(){
        return mService.get();
    }
}
