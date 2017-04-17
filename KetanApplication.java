package com.ketan.openfire;

import android.app.Application;

/**
 * Created by Ketan Ramani on 15/2/17.
 * ketanramani36@gmail.com
 */
public class KetanApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // trust all SSL -> HTTPS connection
        SSLCertificateHandler.nuke();
    }
}
