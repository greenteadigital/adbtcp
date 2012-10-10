package com.greenteadigital.adbtcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
public class AdbUSBservice extends Service {
	public static Process shell;
	public static OutputStream stdin;
	public static InputStream stdout;
	public static byte[] command_buffer;
	public static void getRootShell() {
		try {
			shell = Runtime.getRuntime().exec("su");
		} catch (IOException e) {
			e.printStackTrace();
		}
		stdin = shell.getOutputStream();
		stdout = shell.getInputStream();
	}
	public static void SuExec(String command) {
		command = command + "\n";
		command_buffer = command.getBytes();	
		try {
			stdin.write(command_buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			stdin.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/** Called when the service is first created. */
	@Override
	public void onCreate() {
		super.onCreate();
		AdbTCPservice.wl.release();
		NotificationManager notifier = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Context context = getApplicationContext();
		CharSequence text = "ADB daemon restarted in USB mode.";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);		
		getRootShell();
		SuExec("setprop service.adb.tcp.port -1");
		SuExec("stop adbd");
		SuExec("start adbd");
		try {
			stdin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		notifier.cancel(1);
		toast.show();
		stopSelf(); // service and program are done
	}
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}