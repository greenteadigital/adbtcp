package com.greenteadigital.adbtcp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


public class AdbTCP extends Activity {
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /* Activity will not be seen b/c manifest sets:
         android:theme="@android:style/Theme.Translucent.NoTitleBar" */
        
        Context context = getApplicationContext();
        Intent i = new Intent();
        i.setClass(context, AdbTCPservice.class);
        context.startService(i);
        finish();
    }
}