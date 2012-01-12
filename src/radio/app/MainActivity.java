package radio.app;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/** TODO:
    -Custom background
    -Have main app name with search box (should lead to different activity)
    -Show favourite stations
    -Allow location by GPS **/


public class MainActivity extends Activity {
	/** PROPERTIES **/
	EditText searchText;
	
	// GPS
	private LocationManager myLocationManager;
	private LocationListener myLocationListener;
	private double latitude, longitude;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// No Title - Temporary TODO
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// set the custom title
		//setCustomTitle();
		
		
		// Change to main layout
        setContentView(R.layout.main);
        
        // Retrieve search textbox
        searchText = (EditText) findViewById(R.id.searchBox);
        
        // GPS Managers
        /*myLocationManager = (LocationManager)getSystemService(
        		  Context.LOCATION_SERVICE);

        myLocationListener = new MyLocationListener();
   		
        myLocationManager.requestLocationUpdates(
        		        LocationManager.GPS_PROVIDER,
        		        0,
        		        0,
        		        myLocationListener);*/
        
        // Get last known location
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        // Or use LocationManager.GPS_PROVIDER

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        latitude = lastKnownLocation.getLatitude();
        longitude = lastKnownLocation.getLongitude();
        Log.v("GPS", "Latitude: " + latitude + ", Longitude: " + longitude);
        
        // Attach listener to button
        //Button searchButton = (Button) findViewById(R.id.searchButton);
        //searchButton.
	}
	
	/** Method called when search button is pressed **/
	public void startSearch(View v) {
		// read text to search
		String toSearch = searchText.getText().toString();
		
		// if empty, return nothing - alert for now TODO:
		if (toSearch.equals("")) {
			showToast("Nothing entered!");
			return;
		}
		
		// else, do a search
		this.search(toSearch);
	}
	
	/** Method called when gps search button is pressed **/
	private void searchByGPS() {
		/*****
		 * TEMPORARY: Convert Latitude/Longitude to city
		 */
		Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
		List<Address> addresses;
		String locality = "";
		try {
			addresses = gcd.getFromLocation(this.latitude, this.longitude, 1);
			if (addresses.size() > 0) { 
			    locality = addresses.get(0).getLocality();//Log.e("GPS LOCALITY", addresses.get(0).getLocality());
			    this.search(locality);
			}
			else {
				showToast("Error obtaining location.");
				return;
			}
			
		} catch (IOException e) {
			showToast("Error obtaining location.");
			return;
		}
	}
	
	/** Method for sending search to the second activity **/
	private void search(String location) {
		// check internet connection
		if (!this.isNetworkAvailable()) {
			//showDialog("No Internet connection! Please turn on wi-fi or your data connection.");
			showToast("No Internet connection! Please turn on wi-fi or your data connection.");
			return;
		}
		
		// Send info to other city
		Intent intent = new Intent(this, RadioApp.class);
		/** Add Bundle here (City, location, etc.): **/
		
		Bundle parameters = new Bundle();
		parameters.putString("CITY_NAME", location);
		intent.putExtras(parameters);
		
		this.startActivity(intent);
	}
	
	// For a custom title at the top
	private void setCustomTitle() {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		// custom title
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
		
        // set the custom texview and icon
        TextView title = (TextView) findViewById(R.id.title);
        ImageView icon  = (ImageView) findViewById(R.id.viewIcon);
        title.setText("Search");
        icon.setImageResource(R.drawable.icon);
	}
	
	/*************************************
	 * HELPER METHODS
	 ************************************/
	
	/** Checks if internet connection is available **/
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	/** Creates an alert dialog with the passed in text **/
	private void showDialog(CharSequence text) {
		// build a dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setMessage(text)
		.setCancelable(false)
		/*.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                MyActivity.this.finish();
		           }
		       })*/
		// set only the negative button
		.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	/** Helper method for showing a Toast notification **/
	private void showToast(CharSequence text) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;//  Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
	
	/** Private class for handling GPS Updates **/
	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location argLocation) {
			// TODO Auto-generated method stub
			/*myLatitude.setText(String.valueOf(
			  argLocation.getLatitude()));
			myLongitude.setText(String.valueOf(
			  argLocation.getLongitude()));*/
		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
		}
	}
	
}