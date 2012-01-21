package radio.app;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/** TODO:
 	-Look up default city - if not there, then show this screen, otherwise move to the second screen. (PARTIALLY DONE)
    -Custom background
    -Have main app name with search box (should lead to different activity)
    -Show favourite stations (cities?)
    -Allow location by GPS (kinda working - get's location)
    -Add autocomplete option (by cities)
    -Add ImageButtons for search (the magnifying glass and the location icon from Google Maps)
 	 	-Add onfocus, onclick images
 **/


public class MainActivity extends Activity {
	/** PROPERTIES **/
	
	// Search text box
	EditText _searchText;
	
	// Shared preferences name
	public final String PREFERENCE_FILENAME = "RadioAppPreferences";
	private String _defaultCity = "";
	
	// GPS
	private LocationManager locationManager;
	//private LocationListener myLocationListener;
	private double _latitude, _longitude;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// No Title - Temporary TODO
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// set the custom title
		//setCustomTitle();
		
		// Change to main layout
        setContentView(R.layout.main);
        
        // Retrieve search textbox
        _searchText = (EditText) findViewById(R.id.searchBox);
        
        // Set the listener for the Search Textbox - on pressing enter should start a search
        TextView.OnEditorActionListener exampleListener = new TextView.OnEditorActionListener() {
        	public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
        		   //if(actionId == EditorInfo.IME_NULL){
        			   startSearch(_searchText);//match this behavior to your 'Send' (or Confirm) button
        		  // }
        		   return true;
        		}
        };
        _searchText.setOnEditorActionListener(exampleListener);
        
        // Get last known location
       
        // Or use LocationManager.GPS_PROVIDER

        // Store latitude and longitude
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
       
        
        /** Check Shared preferences - if there is a default city already stored, just show the stations for that **/
        SharedPreferences settings = getSharedPreferences(this.PREFERENCE_FILENAME, MODE_PRIVATE);
       
        if (settings.contains("DEFAULT")) { 	// Default City Stored
        	String defaultCity = settings.getString("DEFAULT", "");
        	// start activity with that city
        	search(defaultCity);
        	
        	// TEMPORARY TODO: Remove it
        	SharedPreferences.Editor prefEditor = settings.edit();
        	prefEditor.remove("DEFAULT");
        }
        else { // TEMPORARY TODO: Store Gainesville as default city
        	// WORKS! Commented out for now
        	SharedPreferences.Editor prefEditor = settings.edit();
        	prefEditor.remove("DEFAULT");
        	/*prefEditor.putString("DEFAULT", "Gainesville");
        	prefEditor.commit();*/
        }
        
        // End of OnCreate
	}
	
	/** Method called when search button is pressed **/
	public void startSearch(View v) {
		// read text to search
		String toSearch = _searchText.getText().toString();
		
		// if empty, return nothing - alert for now TODO:
		if (toSearch.equals("")) {
			showToast("Nothing entered!", true);
			return;
		}
		
		// else, do a search
		this.search(toSearch);
	}
	
	/** Method called when gps search button is pressed **/
	public void searchByGPS(View v) {
		/*****
		 * TEMPORARY: Convert Latitude/Longitude to city
		 */
		// get the latitude/longitude
		String locationProvider = LocationManager.NETWORK_PROVIDER;
		Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
	    try {
	       	_latitude = lastKnownLocation.getLatitude();
	       	_longitude = lastKnownLocation.getLongitude();
	       	Log.v("GPS", "Latitude: " + _latitude + ", Longitude: " + _longitude);
	    } catch (Exception e) {
	    	Log.e("GPS", "Could not get lat/lon of location");
	        	
	    }
		
		Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
		List<Address> addresses;
		String locality = "";
		try {
			addresses = gcd.getFromLocation(this._latitude, this._longitude, 1);
			if (addresses.size() > 0) { 
			    locality = addresses.get(0).getLocality();//Log.e("GPS LOCALITY", addresses.get(0).getLocality());
			    this.search(locality);
			}
			else {
				showToast("Error obtaining location.", true);
				return;
			}
			
		} catch (IOException e) {
			showToast("Error obtaining location.", true);
			return;
		}
	}
	
	/** Method for sending search to the second activity **/
	private void search(String location) {
		// check internet connection
		if (!this.isNetworkAvailable()) {
			//showDialog("No Internet connection! Please turn on wi-fi or your data connection.");
			showToast("No Internet connection! Please turn on wi-fi or your data connection.", false);
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
        ImageView icon  = (ImageView) findViewById(R.id.windowSearchButton);
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
	private void showToast(CharSequence text, boolean timeShort) {
		Context context = getApplicationContext();
		int duration = timeShort? Toast.LENGTH_SHORT : Toast.LENGTH_LONG; // long or short?

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