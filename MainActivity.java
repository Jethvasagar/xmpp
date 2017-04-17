package com.sid.xmppconnect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;

import com.sid.xmppconnect.fragment.UserList;
import com.sid.xmppconnect.service.LocalBinder;
import com.sid.xmppconnect.service.XMPPService;
import com.sid.xmppconnect.util.Application;
import com.sid.xmppconnect.xmpp.MyXMPP;

import org.jivesoftware.smack.roster.Roster;

public class MainActivity extends AppCompatActivity {

    XMPPService mService;
    public static boolean mBounded;

    private final ServiceConnection mConnection = new ServiceConnection() {

        @SuppressWarnings("unchecked")
        @Override
        public void onServiceConnected(final ComponentName name,
                                       final IBinder service) {
            mService = ((LocalBinder<XMPPService>) service).getService();
            mBounded = true;

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container,new UserList(),"UserList")
                    .addToBackStack(null)
                    .commit();
            //mService.myXMPP.rosterList();
            Log.d("XMPP", "onServiceConnected");

        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mService = null;
           mBounded = false;
            Log.d("XMPP", "onServiceDisconnected");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        doBindService();

        if(mBounded){

        }

       // MainActivity activity = ((MainActivity) getActivity());
       // activity.getmService().xmpp.sendMessage(chatMessage);

    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() > 0){
            getSupportFragmentManager().popBackStackImmediate();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    void doBindService() {
        bindService(new Intent(this, XMPPService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }
    public XMPPService getmService() {
        return mService;
    }
}
