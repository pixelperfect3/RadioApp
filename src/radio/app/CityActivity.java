/** 
 *   
 *  This class is the second activity of the app and does the heavy lifting.
 *
 *  It loads all the stations for a city and allows the user to see what they are currently playing
 *  
 *  
 *  
 *  @author shayan javed (shayanj@gmail.com)
 *  TODO:
    -Change this class so that it only shows a ListView of stations (can favorite/unfavorite from here).
      then once user selects a station open up a different activity for the station. (look at google reader for example)
    -Add search functionality (android search button and window button)
    -Customize the Spinner
    -Prettier backgrounds
    -Just generally less ugly
    -Use star icons for favouriting							[Functionality DONE]
    	http://stackoverflow.com/questions/3443588/how-to-get-favorites-star
    	* Add Toast notification once favorited
    -Add arrows for moving forward/backward along radio stations
    -Maybe arrows for showing previously played songs? 
    
    -For songs, show options to explore in Amazon/google Music, etc. To open a link, here's an example: [DONE for the most part]
    	Intent i = new Intent(Intent.ACTION_VIEW, 
       		Uri.parse("http://www.amazon.com/s/url=search-alias=digital-music&field-keywords=lady+gaga+born+this+way"));
		startActivity(i);
    
      OR:
      
      	Intent i = new Intent();
	  	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.putExtra(SearchManager.QUERY, mSong.getArtits() + " " + mSong.getName());
		i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, "artist");
		i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, "album");
		i.putExtra(MediaStore.EXTRA_MEDIA_TITLE, mSong.getName());
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
		startActivity(Intent.createChooser(i, "Search for " + mSong.getName()));
    
    
    	URL for android market: 
    	https://market.android.com/search?q=lady+gaga+born+this+way&c=music
 */

package radio.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class CityActivity extends FragmentActivity {
	/** PROPERTIES **/
	
	// The context
	Context context;

	// Strings for location, name, artist, song
	private String _location = "";
	private String _selectedChannelName = "";
	private String _currentArtist = "";
	private String _currentSong = "";

	// The TextViews
	TextView _locationTV, _stationInfoTV, _currentArtistTV, _currentSongTV;

	// Checkbox for default city and favorite station
	CheckBox _defaultCityCheckbox, _favoriteCheckbox;

	// Time
	Time now;

	// Shared Preferences (for default city)
	public final String PREFERENCE_FILENAME = "CityActivityPreferences";
	private SharedPreferences _settings;

	// Database for storing favorites
	private FavoritesDataSource _dataSource;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set layout 
		setContentView(R.layout.city);

		// get the city from the Bundle passed in
		String city = getIntent().getExtras().getString("CITY_NAME");
		if (city != null) {
			_location = city.toUpperCase();
			this._locationTV = (TextView) findViewById(R.id._location);
			this._locationTV.setText(_location);
		}

		// Action Bar! (From ActionBarSherlock)
		final ActionBar ab = getSupportActionBar();
		ab.setTitle(city);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD); // standard navigation
		// background
		//ab.setBackgroundDrawable(android.R.color.primary_text_dark);
		//getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ad_action_bar_gradient_bak));
		
		// Check if internet connection is available
		if (!isNetworkAvailable()) {
			showToast("No internet connection!", true);

			// Create an alert dialog
			// showDialog("No Internet Connection! Enable WiFi or 3G");
			return;
		}

		/** Open the favorites database **/
		_dataSource = new FavoritesDataSource(this);
		_dataSource.open();
		/*_dataSource.delete(); // TODO: Temporary!
		_dataSource = new FavoritesDataSource(this);
		_dataSource.open();
		_dataSource.delete();*/
		
		/** Setup the ListView by populating it with the list of radio stations **/
		// read the list of stations in an asynctask
		AsyncTask<String, Void, String[]> readStationsTask = new ReadStationsTask(
				this).execute();

		Log.v("CHANNELS LOADED:", this._selectedChannelName);
		// the Time object
		now = new Time();

		// Show if it's the default city or not
		_settings = getSharedPreferences(MainActivity.PREFERENCE_FILENAME,
				MODE_PRIVATE);
		_defaultCityCheckbox = (CheckBox) findViewById(R.id._defaultCheckbox);
		
		if (_settings.contains("DEFAULT")) { // Default City Stored
			String defaultCity = _settings.getString("DEFAULT", "");
			// if city is the same as the default city, set it to checked
			if (city.equalsIgnoreCase(defaultCity))
				_defaultCityCheckbox.setChecked(true);
		}

	} // End of OnCreate

	/** onPause and onResume methods **/
	@Override
	protected void onResume() {
		_dataSource.open();
		super.onResume();
	}

	@Override
	protected void onPause() {
		_dataSource.close();
		super.onPause();
	}
	
	/** Action Bar items (loaded as menu items) **/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.layout.main_menu, menu);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	// selecting menu items
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.ab_refresh:		// refresh the song
				this.refresh(null);
				return true;
			case R.id.ab_search:		// perform a new search
				// TODO:
				// Look at performing new search
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	
	/** Updates the current song via button click **/
	public void refresh(View view) {
		// progress bar
		CityActivity.this.setProgressBarIndeterminate(true);
		CityActivity.this.setProgressBarIndeterminateVisibility(true);

		if (!isNetworkAvailable()) {
			// Alert!
			showDialog("No Internet Connection! Enable WiFi or 3G");
			return;
		}

		// Try to read the JSON information and then update the TextView
		AsyncTask readTask = new ReadSongTask(this)
				.execute(_selectedChannelName);// updateCurrentSong(_selectedChannelName);
	}

	/** Updates the default city via the check box **/
	public void setDefaultCity(View view) {
		SharedPreferences.Editor prefEditor = _settings.edit();

		if (this._defaultCityCheckbox.isChecked()) { // set the default city to
														// the current one
			prefEditor.putString("DEFAULT", this._location.toUpperCase());
			prefEditor.commit();
		} else { // remove it as default
			prefEditor.remove("DEFAULT");
			prefEditor.commit();
		}
	}

	/** Updates the current song on the selected channel **/
	private boolean updateCurrentSong(String channelName) {
		// Read all the JSON information
		// Need to add the "[" and "]" so that it can be properly parsed
		String handle = new Scanner(channelName).next();
		String readJSONFeed = "[" + readJSON(handle, true) + "]";

		try {
			// convert it to a json array
			JSONArray jsonArray = new JSONArray(readJSONFeed);
			if (jsonArray.length() < 1) // nothing read
				return false;
			
			// get the first object which has all the info
			JSONObject firstJson = jsonArray.getJSONObject(0);

			// get the name of the artist and the song
			JSONObject currentSong = firstJson.getJSONObject("now");
			String artist = properFormat(currentSong.getString("artist"));
			String song = properFormat(currentSong.getString("song"));

			// don't bother updating if it's still the same song
			if (song.equals(_currentSong)) {
				return true;
			}

			_currentArtist = artist;
			_currentSong = song;

			// Get the current time
			// now = new Time();
			now.setToNow();

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(CityActivity.class.getName(), "ERROR!!!: " + e.toString());
			// t.setText("Exception " + e.toString());
			//showToast("Error reading song info", false);
		}

		return false;
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

				Log.e(CityActivity.class.toString(),
						"RESPONSE BUILT: " + builder.toString());

			} else {
				Log.e(CityActivity.class.toString(), "Failed to download file");
				finish();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			this._stationInfoTV.setText("Exception " + e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			this._stationInfoTV.setText("Exception " + e.toString());
		}
		return builder.toString();
	}

	/** Allows the user to set the current station as a favorite **/
	public void setFavoriteStation(View view) {
		// TODO:
		if (this._favoriteCheckbox.isChecked()) { // set as favorite
			this._dataSource.createFavorite(Favorite.Type.STATION, this._selectedChannelName);
		} else { // remove as favorite
			Favorite f = new Favorite();
			f.setName(this._selectedChannelName);
			f.setType(Favorite.Type.STATION);
			this._dataSource.deleteFavorite(f);
		}
	}
	
	/** Searches for the song through the Android Market **/
	public void searchAndroidMarket(View view) {
		// parse song first
		StringBuilder s = new StringBuilder(
				"https://market.android.com/search?q=");// lady+gaga+born+this+way&c=music");
		s = convertSongInfo(s);
		s.append("&c=music");

		// make sure it's valid
		String url = s.toString();
		if (url.length() == 0) {
			showToast("No song currently selected", false);
			return;
		}

		// now open it
		this.openURL(url.toString());
	}

	/** Searches for the song through Amazon **/
	public void searchAmazon(View view) {
		// parse song first
		StringBuilder s = new StringBuilder(
				"http://www.amazon.com/s/url=search-alias=digital-music&field-keywords=");
		s = convertSongInfo(s);

		// make sure it's valid
		String url = s.toString();
		if (url.length() == 0) {
			showToast("No song currently selected", false);
			return;
		}

		// now open it
		this.openURL(url.toString());
	}

	/** Searches for the song through Amazon **/
	public void searchGrooveshark(View view) {
		// parse song first
		StringBuilder s = new StringBuilder(
				"http://html5.grooveshark.com/#/search/");
		s = convertSongInfo(s);

		// make sure it's valid
		String url = s.toString();
		if (url.length() == 0) {
			showToast("No song currently selected", false);
			return;
		}

		// now open it
		this.openURL(url.toString());
	}

	/** Searches for the song on Youtube **/
	public void searchYoutube(View view) {
		// parse song first
		StringBuilder s = new StringBuilder(
				"http://m.youtube.com/results?search_query=");
		s = convertSongInfo(s);

		// make sure it's valid
		String url = s.toString();
		if (url.length() == 0) {
			showToast("No song currently selected", false);
			return;
		}

		// now open it
		this.openURL(url.toString());
	}

	/** Converts the song info into a useful URL format **/
	// TODO: Do it once and store or do it all the time?
	private StringBuilder convertSongInfo(StringBuilder s) {
		String[] artist = this._currentArtist.split(" ");
		String[] song = this._currentSong.split(" ");

		for (int i = 0; i < artist.length; i++)
			if (i != artist.length - 1)
				s.append(artist[i] + "+");
			else
				s.append(artist[i]);

		for (int i = 0; i < song.length; i++)
			s.append("+" + song[i]);

		return s;
	}

	/** Opens a URL Intent **/
	private void openURL(String url) {
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(i);
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

	/** Creates an alert dialog **/
	private void showDialog(CharSequence text) {
		// build a dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// set the message
		builder.setMessage(text).setCancelable(false)
		/*
		 * .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		 * public void onClick(DialogInterface dialog, int id) {
		 * MyActivity.this.finish(); } })
		 */
		// set only the negative button
				.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/** Checks if internet connection is available **/
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	/** Inner class used just for listening to the Spinner **/
	public class MyOnItemSelectedListener implements OnItemSelectedListener {

		// When item selected
		// Should update the name if it's different from the current one and
		// then refresh the song name
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			// Get the selected station
			//String station = parent.getItemAtPosition(pos).toString();

			//Log.v("SELECTED STATION!!:", station);
			// Extract the 4-letter name
			//Scanner scan = new Scanner(station);
			//String name = station;//scan.next();

			// Try to update the song if possible
			/*if (name.equals(_selectedChannelName)) {
				// same channel, so no need to update
				return;
			} else {*/
				// Try to read the JSON information and then update the TextView
			_selectedChannelName = parent.getItemAtPosition(pos).toString();

			@SuppressWarnings("unused")
			AsyncTask<String, Void, Boolean> readTask = new ReadSongTask(
					CityActivity.this).execute(_selectedChannelName);// updateCurrentSong(_selectedChannelName);
			
		}

		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	}

	// Formats the HTML Strings properly
	// TODO: (&lt; &gt;)?
	private String properFormat(String str) {
		String proper = str.replaceAll("&amp;", "&");
		proper = proper.replaceAll("&quot;", "\"");
		proper = proper.replaceAll("&apos;", "\'");

		return proper;
	}

	/***
	 * AsyncTask class for reading all the stations
	 */
	private class ReadStationsTask extends AsyncTask<String, Void, String[]> {
		// The dialog bar to show indeterminate progress
		private ProgressDialog dialog;
		private Context theContext;

		// constructor
		public ReadStationsTask(Activity activity) {
			theContext = activity;
			CityActivity.this.setProgressBarIndeterminate(true);
			CityActivity.this.setProgressBarIndeterminateVisibility(true);
			// start the progress dialog
			dialog = new ProgressDialog(theContext);
		}

		@Override
		protected String[] doInBackground(String... params) {
			// perform long running operation operation
			// update the list of stations

			String channelJSON = readJSON(_location, false);
			Log.v(CityActivity.class.getName(), "channelJSON: " + channelJSON);

			try {
				// create the json object from the string
				JSONObject jsonobj = new JSONObject(channelJSON);

				// get the array of stations
				JSONArray jsonArray = new JSONArray(
						jsonobj.getString("stations"));

				/** TODO: Perhaps part below should be in postExecute()? **/

				// Create the array of strings which will be used for populating
				// the spinner
				String[] stations = new String[jsonArray.length()];

				// If not stations found
				if (stations.length == 0) {
					showToast("No stations found!", false);
					finish(); // TODO: Properly show dialog
				}

				// go through each station
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					// get the station name
					String name = jsonObject.getString("name");
					// get the description
					String desc = properFormat(jsonObject.getString("desc"));

					// add to the string array
					stations[i] = name + " " + desc;
				}

				return stations;

			} catch (Exception e) {
				Log.e(CityActivity.class.getName(),
						"JSON Exception: " + e.toString());
				return null;
			}
		}

		@Override
		protected void onPostExecute(String[] result) {
			// execution of result of Long time consuming operation
			// dismiss the dialog
			dialog.dismiss();
			//CityActivity.this.setProgressBarIndeterminate(false);
			//CityActivity.this.setProgressBarIndeterminateVisibility(false);
			/*
			 * String time = now.format("%H:%M");
			 * _stationInfoTV.setText("Name: " + _selectedChannelName +
			 * "\nArtist: " + _currentArtist + "\nSong: " + _currentSong +
			 * "\nTime: " + time);
			 * CityActivity.this.setProgressBarIndeterminate(false);
			 * CityActivity.this.setProgressBarIndeterminateVisibility(false);
			 */

			try {
				// get list of all the stations from the task
				String[] stations = new String[1];

				// get() causes a block on the UI Thread!
				stations = this.get();

				if (stations != null) {
					// set selected channel to the first one
					CityActivity.this._selectedChannelName = stations[0];
					Log.v("SELECTED CHANNEL:", CityActivity.this._selectedChannelName);
					
					// Populate the ListView
					ListView lv = (ListView) findViewById(R.id._stationsList);
					CustomArrayAdapter cadapter = new CustomArrayAdapter( // TODO: Not working for some reason
							CityActivity.this,
							stations);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							CityActivity.this, android.R.layout.simple_list_item_1,
							stations);
					//adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					lv.setAdapter(cadapter);
					// now set the listview to listen for changes
					lv.setOnItemSelectedListener(new MyOnItemSelectedListener());
				}
			} catch (Exception e) {
				Log.e("Reading Stations", "Cannot execute AsyncTask");
			}
		}

		@Override
		protected void onPreExecute() {
			// Things to be done before execution of long running operation. For
			// example showing ProgessDialog

			// set the properties of the progressdialog
			// make sure it's indeterminate
			this.dialog.setMessage("Loading list of stations");
			this.dialog.setIndeterminate(true);
			this.dialog.show();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// Things to be done while execution of long running operation is in
			// progress. For example updating ProgessDialog
		}
	}

	/** End of AsyncTask class **/

	/***
	 * AsyncTask class for reading info about a station
	 */
	private class ReadSongTask extends AsyncTask<String, Void, Boolean> {
		// The dialog bar to show indeterminate progress
		private ProgressDialog dialog;
		private Context theContext;

		// constructor
		public ReadSongTask(Activity activity) {
			theContext = activity;
			CityActivity.this.setProgressBarIndeterminate(true);
			CityActivity.this.setProgressBarIndeterminateVisibility(true);
			// start the progress dialog
			dialog = new ProgressDialog(theContext);
		}

		@Override
		protected Boolean doInBackground(String... params) {
			// perform long running operation operation
			// update the song
			boolean update = updateCurrentSong(params[0]);
			return update;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// execution of result of Long time consuming operation
			// dismiss the dialog
			dialog.dismiss();
			String time = now.format("%H:%M");
			_stationInfoTV.setText("Station: " + _selectedChannelName
					+ "\n\tTime: " + time);
			_currentArtistTV.setText("Artist:\n\t" + _currentArtist);
			_currentSongTV.setText("Song:\n\t" + _currentSong);
			
			// Check if channel is already a favorite and update the star checkbox
			// TODO: Make it more efficient so you don't have to check everytime
			// TODO Temporary: We are currently getting all comments, but only get selected ones
			List<Favorite> favorites = CityActivity.this._dataSource.getAllFavorites();
			boolean isChecked = false;
			for (int i = 0; i < favorites.size(); i++) {
				if (favorites.get(i).getName().equals(_selectedChannelName)) { 
					isChecked = true;
					break;
				}
			}
			
			if (isChecked)
				CityActivity.this._favoriteCheckbox.setChecked(true);
			else
				CityActivity.this._favoriteCheckbox.setChecked(false);
			
			CityActivity.this.setProgressBarIndeterminate(false);
			CityActivity.this.setProgressBarIndeterminateVisibility(false);
		}

		@Override
		protected void onPreExecute() {
			// Things to be done before execution of long running operation. For
			// example showing ProgessDialog

			// set the properties of the progressdialog
			// make sure it's indeterminate
			this.dialog.setMessage("Loading station info");
			this.dialog.setIndeterminate(true);
			this.dialog.show();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// Things to be done while execution of long running operation is in
			// progress. For example updating ProgessDialog
		}
	}
	/** End of AsyncTask class **/

}