/** This is the starting activity for the app.
 *  This is the second version of the activity - after suggestions from other people.
 *  Replaces the original MainActivity
 *  
 *  If the default location is set, it will load the list of stations
 *  
 *  It also shows the favourite stations/songs of the user at the top
 *  
 *  TODO:
 *  
 	-Add autocomplete option (by cities)
 	-Custom background
 */

package radio.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity2 extends FragmentActivity {
	
	/******************
	 * PROPERTIES
	 ******************/
	public static final String PREFERENCE_FILENAME = "RadioAppPreferences"; // Shared preferences name
	private ListView _favoritesLV; 											// ListView for showing favorites
	private ListView _locationLV; 											// ListView for showing stations in the location
	private TextView _locationLabel;
	public  ProgressBar _progressBar;										// busy progress bar
	private FavoritesArrayAdapter adapter;
	public static FavoritesDataSource _dataSource; 							// Database for storing favorites
	// GPS
	private LocationManager locationManager;
	
	/****************************************************
	 * METHODS FOR CREATING, PAUSING, RESUMING, DESTROYING
	 ***************************************************/
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// No Title - Temporary TODO
		//requestWindowFeature(Window.FEATURE_NO_TITLE);

		// set the custom title
		// setCustomTitle();

		// Change to main2 layout
		setContentView(R.layout.main2);

		// If it's a search Intent
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) { // Check if it was a search Intent
		      /*city = getIntent().getStringExtra(SearchManager.QUERY).trim();
		      if (Character.isDigit(city.charAt(0))) {		// zipcode?
					Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
					List<Address> addresses;
					try {
						addresses = gcd.getFromLocationName(city, 1);
						if (addresses.size() > 0) {
							city = addresses.get(0).getLocality();// addresses.get(0).getLocality());
							//Log.i("Locality: ", locality);
							
						} else {
							showToast("Error obtaining location from zipcode.", true);
							return;
						}
					} catch (IOException e) {
						showToast("Error obtaining location from zipcode.", true);
						return;
					}
				}*/
		}
		
		// Action Bar! (From ActionBarSherlock)
		final ActionBar ab = getSupportActionBar();
		ab.setTitle("On The Radio");
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD); // standard navigation
		// background
		ab.setBackgroundDrawable(new ColorDrawable(android.R.color.primary_text_dark));//getResources().getDrawable(R.drawable.ad_action_bar_gradient_bak));
		
		
		// Retrieve widgets
		_favoritesLV = (ListView) findViewById(R.id.favoritesLV); 	// favorites
		_locationLV = (ListView) findViewById(R.id.locationLV); 	// location stations
		_locationLabel = (TextView) findViewById(R.id.locationLabel); // text
		_progressBar = (ProgressBar) findViewById(R.id.loadingBar); // busy progress bar
		
		// Location Manager for getting latitude/longitude
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// temporary - to delete a whole database
		//this.deleteDatabase("favorites.db");
		
		/** Open the favorites database **/
		_dataSource = new FavoritesDataSource(this);
		_dataSource.open();
		
		// Show the favorites
		updateFavorites();
		
		// Now look up the location and then automatically read the stations from that
		updateLocation();
		
		/**
		 * Check Shared preferences - if there is a default city already stored,
		 * just show the stations for that
		 **/
		/*SharedPreferences settings = getSharedPreferences(
				MainActivity2.PREFERENCE_FILENAME, MODE_PRIVATE);

		if (settings.contains("DEFAULT")) { // Default City Stored
			String defaultCity = settings.getString("DEFAULT", "");
			// start activity with that city
			search(defaultCity);

		}*/
	} // End of OnCreate

	/** onPause, onResume  and onDestroy methods **/
	@Override
	protected void onResume() {
		_dataSource.open();
		// update favorites
		updateFavorites();
		super.onResume();
	}

	@Override
	protected void onPause() {
		_dataSource.close();
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		_dataSource.close();
		super.onDestroy();
	}
	
	// TODO: Implement search - start looking at location
	@Override
	public boolean onSearchRequested() {
		_dataSource.close();
		return super.onSearchRequested();
	}
	
	/************************
	 * ACTION BAR METHODS
	 ***********************/
	
	/** Action Bar items (loaded as menu items) **/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.layout.main_header, menu);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	/** Selecting Action Bar items **/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ab_location:		// look up location
				updateLocation();
				return true;
			case R.id.ab_search:		// perform a new search
				this.onSearchRequested();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	/**********************************
	 * METHODS FOR HANDLING FAVORITE STATIONS
	 *********************************/
	
	/** Updates the favorites list **/
	public void updateFavorites() {
		// get all the favorites
		List<Favorite> values = _dataSource.getAllFavorites();

		// Populate the ListView
		adapter = new FavoritesArrayAdapter(
				MainActivity2.this,
				values);
		
		_favoritesLV.setAdapter(adapter);
		
		// now set the listview to listen for changes
		_favoritesLV.setOnItemClickListener(new MyOnItemClickListener());
	}
	
	/** removes from the favorites list **/
	public void removeFavorite(Favorite favorite) {
		this.updateFavorites();
		
		this.showToast("Unfavorited " + favorite.getName(), true);
	}
	
	/** Inner class used just for listening to the Favorites ListView **/
	public class MyOnItemClickListener implements OnItemClickListener {

		// When item selected start new station activity
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
			
			if (!MainActivity2.this.isNetworkAvailable()) {
				showToast(
						"No Internet connection! Please turn on wi-fi or your data connection.",
						false);
				return;
			}
			
			// Get the selected favorite
			Favorite fave = (Favorite)parent.getItemAtPosition(pos);
			
			// start the station activity
			Intent intent = new Intent(MainActivity2.this, StationActivity.class);//RadioApp.class);
			Bundle parameters = new Bundle();
			parameters.putString("STATION_NAME", fave.getName());
			intent.putExtras(parameters);

			MainActivity2.this.startActivity(intent); 
			
		}
	}
	
	/**********************************
	 * METHODS FOR HANDLING LOCATION
	 *********************************/
	
	/** Updates the location and then reads the stations for that location **/
	public void updateLocation() {
		
		new ReadLocationTask(this).execute();	
	}
	
	/***
	 * AsyncTask class for reading the location
	 */
	private class ReadLocationTask extends AsyncTask<String, Void, String> {
		private Context theContext;

		// constructor
		public ReadLocationTask(Activity activity) {
			theContext = activity;
		}

		@Override
		protected void onPreExecute() {
			// Things to be done before execution of long running operation. For

			// show the progress bar
			MainActivity2.this._progressBar.setVisibility(View.VISIBLE);
			MainActivity2.this._locationLabel.setText("Looking up current location...");
		}
		
		@Override
		protected String doInBackground(String... params) {
			// perform long running operation operation
			// read the location
			// get the latitude/longitude
			String locationProvider = LocationManager.NETWORK_PROVIDER;
			Location lastKnownLocation = locationManager
					.getLastKnownLocation(locationProvider);
			
			double _latitude = 0.0;
			double _longitude = 0.0;
			
			try {
				_latitude  = lastKnownLocation.getLatitude();
				_longitude = lastKnownLocation.getLongitude();
				Log.v("GPS", "Latitude: " + _latitude + ", Longitude: "
						+ _longitude);
			} catch (Exception e) {
				Log.e("GPS", "Could not get lat/lon of location");

			}

			Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
			List<Address> addresses;
			String locality = "";
			try {
				addresses = gcd.getFromLocation(_latitude, _longitude, 1);
				if (addresses.size() > 0) {
					locality = addresses.get(0).getLocality();// Log.e("GPS LOCALITY",
																// addresses.get(0).getLocality());
					return locality;
				} else {
					//showToast("Error obtaining location.", true);
					//MainActivity2.this._locationLabel.setText("Unable to lookup current location. Please try again later");
					return null;
				}

			} catch (IOException e) {
				//showToast("Error obtaining location.", true);
				//MainActivity2.this._locationLabel.setText("Unable to lookup current location. Please try again later");
				return null;
			}
		}

		@Override
		protected void onPostExecute(String result) {
			// execution of result of Long time consuming operation
			
			// check location result
			if (result == null) {
				MainActivity2.this._locationLabel.setText("Can't look up location. Please try again later.");
				
				// hide the progress bar
				MainActivity2.this._progressBar.setVisibility(View.INVISIBLE);
			}
			else { // start looking up stations 
				
				/** Setup the ListView by populating it with the list of radio stations **/
				new ReadStationsTask(result).execute(); // AsyncTask<String, Void, String[]> readStationsTask = 
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// Things to be done while execution of long running operation is in
			// progress. For example updating ProgessDialog
		}
	}
	
	/************************
	 * METHODS FOR SEARCHING
	 ***********************/
	/***
	 * AsyncTask class for reading all the stations
	 */
	private class ReadStationsTask extends AsyncTask<String, Void, String[]> {
		String _location;
		
		// constructor
		public ReadStationsTask(String _location) {
			this._location = _location;
		}

		@Override
		protected String[] doInBackground(String... params) {
			// perform long running operation operation
			// update the list of stations
			String channelJSON = readJSON(_location, false);
			Log.v(MainActivity2.class.getName(), "channelJSON: " + channelJSON);
			
			if (channelJSON == null) // no info - probably server is down
				return null;
			
			try {
				// create the json object from the string
				JSONObject jsonobj = new JSONObject(channelJSON);

				// get the array of stations
				JSONArray jsonArray = new JSONArray(
						jsonobj.getString("stations"));

				// Create the array of strings which will be used for populating
				// the spinner
				String[] stations = new String[jsonArray.length()];

				// If not stations found
				if (stations.length == 0) {
					showToast("No stations found!", false);
					finish();
				}

				// go through each station
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					
					// get the name and description
					String name = jsonObject.getString("name");
					String desc = properFormat(jsonObject.getString("desc"));

					stations[i] = name + " " + desc;
				}

				return stations;

			} catch (Exception e) {
				Log.e(MainActivity2.class.getName(),
						"JSON Exception: " + e.toString());
				
				return null;
			}
		}

		@Override
		protected void onPostExecute(String[] stations) {
			// execution of result of Long time consuming operation
			
			// hide the progress bar
			MainActivity2.this._progressBar.setVisibility(View.INVISIBLE);
			
			try {
				if (stations != null) {
					MainActivity2.this._locationLabel.setText("Stations in " + _location + " :");
					
					// Populate the ListView with all the stations
					ListView lv = (ListView) findViewById(R.id.locationLV);
					CustomArrayAdapter cadapter = new CustomArrayAdapter( 
							MainActivity2.this,
							stations);
		
					lv.setAdapter(cadapter);
					
					// now set the listview to listen for changes
					lv.setOnItemClickListener(new MyOnItemClickListenerLocation());
				}
				else 
					throw new Exception("");
			} catch (Exception e) {
				Log.e("Reading Stations", "Cannot execute AsyncTask: " + e.toString());
				
				/*Context context = getApplicationContext();
				int duration = Toast.LENGTH_LONG; // long
				Toast toast = Toast.makeText(context, "Website seems to be down - please try again later.", duration);
				toast.show();*/
				
				MainActivity2.this._locationLabel.setText("Could not look up stations. Please try again later");
				
			}
		}

		@Override
		protected void onPreExecute() {
			MainActivity2.this._locationLabel.setText("Looking up stations in " + _location);
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// Things to be done while execution of long running operation is in
			// progress. For example updating ProgessDialog
		}
	}
	
	/** Inner class used just for listening to the Location ListView **/
	public class MyOnItemClickListenerLocation implements OnItemClickListener {

		// When item selected start new station activity
		public void onItemClick(AdapterView<?> parent, View view, int pos,
				long id) {
				
			String _selectedChannelName = parent.getItemAtPosition(pos).toString();
			Log.v("Clicked List:", _selectedChannelName);
			
			Intent intent = new Intent(MainActivity2.this, StationActivity.class);

			Bundle parameters = new Bundle();
			parameters.putString("STATION_NAME", _selectedChannelName);
			intent.putExtras(parameters);

			MainActivity2.this.startActivity(intent); 
		}
	}
	
	/** Reads the JSON about the channel **/
	public String readJSON(String info, boolean station) {
		// Format the info properly (for city)
		info = info.trim();
		// replace spaces with proper html?
		info = info.replaceAll(" ", "&nbsp;");
	
		StringBuilder builder = new StringBuilder();
	
		// Create a HTTPClient
		HttpClient client = new DefaultHttpClient();
	
		// Open a GET connection to the Yes.com api
		HttpGet httpGet;
		if (station) {
			httpGet = new HttpGet("http://api.yes.com/1/station?name=" + info);
		} else { // get list of stations instead for the provided city/zipcode
			httpGet = new HttpGet("http://api.yes.com/1/stations?loc=" + info);
		}
	
		try {
			// Read all the lines one-by-one
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
	
				Log.e(MainActivity2.class.toString(),
						"RESPONSE BUILT: " + builder.toString());
	
			} else {
				Log.e(MainActivity2.class.toString(), "Failed to download file");
				//showToast("Website seems to be down - please try again later.", false);
				//finish();
				return null;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
	
	/*************************************
	 * HELPER METHODS
	 ************************************/

	/** For a custom title at the top **/
	private void setCustomTitle() {
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		// custom title
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.window_title);

		// set the custom texview and icon
		TextView title = (TextView) findViewById(R.id.title);
		ImageView icon = (ImageView) findViewById(R.id.windowSearchButton);
		title.setText("Search");
		icon.setImageResource(R.drawable.icon);
	}
	
	/** Checks if internet connection is available **/
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	/** Helper method for showing a Toast notification **/
	private void showToast(CharSequence text, boolean timeShort) {
		Context context = getApplicationContext();
		int duration = timeShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG; // long
																			// or
																			// short?

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	/** Formats the HTML Strings properly
	// TODO: (&lt; &gt;) **/
	private String properFormat(String str) {
		String proper = str.replaceAll("&amp;", "&");
		proper = proper.replaceAll("&quot;", "\"");
		proper = proper.replaceAll("&apos;", "\'");
	
		return proper;
	}
	
}