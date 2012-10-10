package com.greenteadigital.adbtcp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class AdbTCPservice extends Service {
	public static Process shell;
	public static String portStr;
	public static int genPortNumCalls = 0;
	public static OutputStream stdin;
	public static InputStream stdout;
	public static byte[] command_buffer;
	public static int response_length;
	public static byte[] response_buffer;
	public static String response_string;
	public static Object[] arrayListConv1;
	public static boolean available;
	public static String done;
	public static boolean shownToast = false;
	public static PowerManager.WakeLock wl;
	public static String ipAddress;
	public boolean foundIP;

	public static void getRootShell() {
		try {
			shell = Runtime.getRuntime().exec("su");
		} catch (IOException e) {
			e.printStackTrace();
		}
		stdin = shell.getOutputStream();
		stdout = shell.getInputStream();
		response_buffer = new byte[100 * 1000]; // 100kB buffer to hold shell
		// stdout
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

	public static void getPortsInUse() {
		SuExec("netstat");
		try {
			response_length = stdout.read(response_buffer); // num bytes
			// returned from
			// stdout
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		response_string = new String(response_buffer).substring(0,
				response_length);
		String[] responseLines = response_string.split(" ");
		int j;
		ArrayList<String> arrayList = new ArrayList<String>();
		for (j = 0; j < responseLines.length; j++) {
			if ((responseLines[j].indexOf(":") > -1) && (responseLines[j].indexOf("*") == -1)) {
				arrayList.add(responseLines[j].toString());
			}
		}
		arrayListConv1 = arrayList.toArray(); // returns Object[]
		done = Arrays.toString(arrayListConv1);
		// Log.i("AdbTcpNetstatString",done);
	}

	public static void genPortNum() {
		Random random = new Random();
		int portNum = (random.nextInt(64511) + 1024); 
		portStr = Integer.toString(portNum);
		genPortNumCalls++;
	}

	public void compare() {
		Context context = getApplicationContext();
		if (done.indexOf(":" + portStr) > -1) {
			available = false;
		} else {
			available = true;
		}
		if ((available == false) && (shownToast == false)) {
			StringBuilder fail = new StringBuilder("TCP Port: "
					+ portStr
					+ " already in use. Retrying...");
			Toast toast = Toast.makeText(context, fail.toString(),
					Toast.LENGTH_SHORT);
			toast.show();
			shownToast = true;
		}
	}

	public String getLocalIpAddress() {
		String ipAddress = "";
		SuExec("netcfg");
		try {
			response_length = stdout.read(response_buffer);
		} catch (IOException e1) {
			e1.printStackTrace();
			//Log.i("netcfg.IOexception",e1.toString());
		}
		response_string = new String(response_buffer).substring(0,response_length);
		String[] responseLines = response_string.split("\n");
		ArrayList<String> arrayList = new ArrayList<String>();
		for (int n = 0; n < responseLines.length; n++) {
			arrayList.add(responseLines[n].toString());
			//Log.i("benhall-responseLine.#"+Integer.toString(n), arrayList.get(n));
		}
		foundIP = false;
		for (int n = 0; n < arrayList.size(); n++) {
			if ((arrayList.get(n).contains("UP") ) &&
				(!arrayList.get(n).contains("0.0.0.0")) &&
				(!arrayList.get(n).contains("127.0.0.1"))) {
				Log.i("benhall-ipaddress "+arrayList.get(n), "length="+arrayList.get(n).length());
				ipAddress = arrayList.get(n).substring(40,55).replace(" ", "");
				foundIP = true;
			}
			if (!foundIP) {
				ipAddress = "*.*.*.*";
			}
		}
		//Log.i("benhall-ipaddress", ipAddress);
		return ipAddress;
	}

	/** Called when the service is first created. */
	@Override
	public void onCreate() {
		super.onCreate();
		getRootShell();
		genPortNum();
		getPortsInUse();
		compare();
		ipAddress = getLocalIpAddress();
		while (available == false) {
			genPortNum();
			compare();
		}
		if (available == true) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AdbTcpWakeLock");
			wl.acquire();
			Notification warn = new Notification(R.drawable.exclam,
					"ADBD listening at " + ipAddress + ":" + portStr,
					System.currentTimeMillis());
			warn.flags |= Notification.FLAG_NO_CLEAR; // makes notification
			// persistent,
			// bitwise-or'd in, via |=
			NotificationManager notifier = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			Context context = getApplicationContext();
			Intent i = new Intent();
			i.setClass(context, AdbUSBservice.class);
			PendingIntent pend = PendingIntent.getService(context, 0, i, 0);
			String subnote;
			if (!foundIP){
				subnote = "   Network interfaces down. Tap to exit.";
			} else {
				subnote = "   Tap to restart in USB mode";
			}
			warn.setLatestEventInfo(context, "ADB  " + ipAddress + ":" + portStr, subnote, pend);
			notifier.notify(1, warn);
			SuExec(String.format("setprop service.adb.tcp.port %s\n",
					portStr));
			SuExec("stop adbd");
			SuExec("start adbd");
			try {
				stdin.close();
				shell.destroy();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		shownToast = false;
		stopSelf(); // terminate this service, we're done
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}