package bander.muclient;

import java.util.HashMap;

import bander.muclient.net.TextConnection;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;

public class Main extends Application {	
	private WifiLock mWifiLock = null;
	private HashMap<Uri, TextConnection> mConnections = new HashMap<Uri, TextConnection>();

	/** Gets connection for a Host URI.
	 * @param uri Host URI to get the connection for.
	 * @return The connection for the specified URI.
	 */
	public TextConnection getConnection(Uri uri) {
		return mConnections.get(uri);
	}
	/** Sets connection for a Host URI.
	 * @param uri Host URI to set the connection for.
	 * @param connection The connection to set.
	 */
	public void setConnection(Uri uri, TextConnection connection) {
		mConnections.put(uri, connection);
	}
	
	public WifiLock getWifiLock() {
		if (mWifiLock == null) {
			WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			mWifiLock = wifiManager.createWifiLock("MuClient");
		}
		return mWifiLock;
	}	
}
