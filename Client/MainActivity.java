package com.gling.android.gcm;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.gling.android.gcm.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends Activity {

	private static final String HTTP_SERVER_URL = "http://intense-citadel-2226.herokuapp.com/";
	private Button btnRegister, btnSendRegidToServer;
	private GoogleCloudMessaging gcm;
	private String regid;
	private Context context;

	private static final String SENDER_ID = "867343655868";
	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	public final static String TAG = "GCM_TEST";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		context = getApplicationContext();
		btnRegister = (Button) findViewById(R.id.btnRegister);
		btnSendRegidToServer = (Button) findViewById(R.id.btnSendRegidToServer);

		btnSendRegidToServer.setEnabled(false);

		btnRegister.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// Check device for Play Services APK.
				// If this check succeeds, proceed with normal processing.
				// Otherwise, prompt user to get valid Play Services APK.
				if (checkPlayServices()) {
					regid = getRegistrationId();
					if (regid.isEmpty()) {
						Log.i(TAG,
								"Registration not found. Registering in background...");
						registerInBackground();
					} else {
						Log.i(TAG,
								"Already registered. regid is taken from sharedPreferences");
						Toast.makeText(getApplicationContext(),
								"Already registered!", Toast.LENGTH_LONG)
								.show();
						btnRegister.setEnabled(false);
						btnSendRegidToServer.setEnabled(true);
					}
				} else {
					Log.i(TAG, "No valid Google Play Services APK found.");
				}
			}
		});

		btnSendRegidToServer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendRegistrationIdToBackend();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkPlayServices();
	}

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i("GCM", "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	private String getRegistrationId() {
		final SharedPreferences prefs = getGCMPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			return "";
		}

		/*
		 * Check if app was updated; if so, it must clear the registration ID
		 * since the existing regID is not guaranteed to work with the new app
		 * version.
		 */
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i("GCM", "App version changed.");
			return "";
		}
		return registrationId;
	}

	private int getAppVersion(Context context2) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	private void registerInBackground() {
		new AsyncTask<Void, Void, String>() {
			private boolean error = false;

			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				// In case of a failure you should retry using exponential backoff
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging
								.getInstance(getApplicationContext());
					}
					regid = gcm.register(SENDER_ID);
					storeRegistrationId(regid);
					// Initiated by user: sendRegistrationIdToBackend();
					msg = "Device registered, registration ID = " + regid;
				} catch (IOException ex) {
					error = true;
					msg = "Error: " + ex.getMessage();
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT)
						.show();
				if (!error) {
					btnRegister.setEnabled(false);
					btnSendRegidToServer.setEnabled(true);
				}
			}
		}.execute(null, null, null);
	}

	private SharedPreferences getGCMPreferences(Context context) {
		// This sample app persists the registration ID in shared preferences,
		// but how you store the regID in your app is up to you.
		return getSharedPreferences(MainActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
	}

	private void storeRegistrationId(String regid) {
		final SharedPreferences prefs = getGCMPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i("GCM", "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regid);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	/**
	 * Sends the registration ID to your server over HTTP, so it can use
	 * GCM/HTTP or CCS to send messages to your app.
	 * 
	 * @throws IOException
	 */
	private void sendRegistrationIdToBackend() {
		Toast.makeText(getApplicationContext(), "Sending regid to backend",
				Toast.LENGTH_SHORT).show();

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				try {
					String url = HTTP_SERVER_URL;
					URL obj;
					obj = new URL(url);
					HttpURLConnection con = (HttpURLConnection) obj
							.openConnection();

					con.setRequestMethod("POST");

					String urlParameters = "regid=" + regid + "&name="
							+ getAccountName();

					Log.i(TAG, "Sending 'POST' request to URL : " + url);
					Log.i(TAG, "Post parameters : " + urlParameters);

					// Send post request to http server
					con.setDoOutput(true);
					DataOutputStream wr = new DataOutputStream(
							con.getOutputStream());
					wr.writeBytes(urlParameters);
					wr.flush();
					wr.close();

					// Wait for response from http server
					int responseCode = con.getResponseCode();
					Log.i(TAG, "Response Code : " + responseCode);
					BufferedReader in = new BufferedReader(
							new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();

					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();

					return "Response: " + response.toString();
				} catch (MalformedURLException e) {
					return e.getMessage();
				} catch (IOException e) {
					return e.getMessage();
				}
			}

			@Override
			protected void onPostExecute(String msg) {
				Log.i(TAG, "Response from server: " + msg);
			}
		}.execute(null, null, null);
	}

	private String getAccountName() {
		AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
		Account[] list = manager.getAccounts();

		for (Account account : list) {
			if (account.type.equalsIgnoreCase("com.google")) {
				return account.name;
			}
		}

		return "";
	}
}