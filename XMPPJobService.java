package com.sid.xmppconnect.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.util.Log;

import com.sid.xmppconnect.util.Application;

/**
 * Created by Jethva Sagar (Piml Sid) on 3/1/17
 * Email : jethvasagar2@gmail.com.
 */

public class XMPPJobService extends JobService{
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d("Sid","onStartJob()..." + jobParameters.getJobId());

       // String url = params.getExtras().getString(RssApplication.URL);
        //Intent i = new Intent(this, XMPPService.class); // starts the RssDownloadService service
       // i.putExtra(RssApplication.URL, url); // some extra data for the service
        //startService(i);
        Application.getXmppService(getApplicationContext());
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d("Sid","onStopJob()..." + jobParameters);
        return true;
    }
}
