package com.sid.xmppconnect.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sid.xmppconnect.MainActivity;
import com.sid.xmppconnect.R;
import com.sid.xmppconnect.util.Config;
import com.sid.xmppconnect.xmpp.MyXMPP;

/**
 * Created by Jethva Sagar (Piml Sid) on 3/1/17
 * Email : jethvasagar2@gmail.com.
 */

public class XMPPService extends Service{

    public static boolean isStartService = false;
    public static ConnectivityManager cm;
    public static MyXMPP myXMPP;
    public static String LOG = "Sid";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG,"onBind()");
        isStartService = true;
        return new LocalBinder<XMPPService>(this);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG,"onCreate()");
        isStartService = true;
        cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        myXMPP = MyXMPP.getInstance(XMPPService.this, Config.DOMAIN_NAME,Config.USERNAME,Config.PASSWORD);
        myXMPP.connect();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG,"onStartCommnad()");
        isStartService = true;
        return Service.START_NOT_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG,"onUnBind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG,"onDestroy()");
        myXMPP.disconnect();
    }

    public static boolean isNetworkConnected(){
        return cm.getActiveNetworkInfo()!=null;
    }
}
