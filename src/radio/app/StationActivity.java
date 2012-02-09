package radio.app;

/**
 * This Activity shows information about a selected station 
 * 
 * Should show the current song playing (with links to Amazon, Grooveshark, Android Market and Youtube)
 * 
 * Allow user to favorite the station (top-right) and see last 5 songs played
 * 
 * TODO:
 * -Show time
 * -Custom bg for header
 * -Show last 5 songs?
 * -
 */

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
import android.graphics.drawable.ColorDrawable;
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
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class StationActivity extends FragmentActivity {

	/******************
	 * PROPERTIES
	 ******************/

	// The context
	Context context;

	// Strings for location, name, artist, song
	private String _stationName = "";
	private String _selectedChannelName = "";
	private String _currentArtist = "";
	private String _currentSong = "";

	// The TextViews
	TextView _stationInfoTV, _currentArtistTV, _currentSongTV;

	// Checkbox (star) for favoriting
	CheckBox _favoriteCheckbox;

	// Time
	Time now;

	// Database for storing favorites
	private FavoritesDataSource _dataSource;

	/****************************************************
	 * METHODS FOR CREATING, PAUSING, RESUMING, DESTROYING
	 ***************************************************/

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set layout to the "old city"
		setContentView(R.layout.station);

		// get the text views
		this._currentArtistTV = (TextView) findViewById(R.id._currentArtistTV);
		this._currentSongTV = (TextView) findViewById(R.id._currentSongTV);

		// favorite checkbox
		this._favoriteCheckbox = (CheckBox) findViewById(R.id._favoriteCheckbox);

		// get the station from the Bundle passed in
		_stationName = getIntent().getExtras().getString("STATION_NAME");

		// extract the first four letters for the channel name
		String[] split = this._stationName.split(" ");
		_selectedChannelName = split[0];
		_stationName = split[0] + " " + split[1];
		// Action Bar! (From ActionBarSherlock)
		final ActionBar ab = getSupportActionBar();
		ab.setTitle(_stationName);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD); // standard
																	// navigation
		// ab.setDisplayHomeAsUpEnabled(true);
		ab.setDisplayShowHomeEnabled(false);

		// background
		ab.setBackgroundDrawable(new ColorDrawable(
				android.R.color.primary_text_dark));// getResources().getDrawable(R.drawable.ad_action_bar_gradient_bak));
		// ab.setBackgroundDrawable(android.R.color.primary_text_dark);
		// getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ad_action_bar_gradient_bak));

		// Check if internet connection is available
		if (!isNetworkAvailable()) {
			showToast("No internet connection!", true);

			return;
		}

		/** Open the favorites database **/
		_dataSource = new FavoritesDataSource(this);
		_dataSource.open();

		// the Time object
		now = new Time();

		// Try to read the JSON information and read the current song

		Log.e("SELECTED CHANNEL2:", this._selectedChannelName);
		if (this._selectedChannelName != "") {
			AsyncTask<String, Void, Boolean> readTask = new ReadSongTask(this)
					.execute(_selectedChannelName);// updateCurrentSong(_selectedChannelName);
		}

		// StationActivity.this.setProgressBarIndeterminate(false);
		// StationActivity.this.setProgressBarIndeterminateVisibility(false);

	}

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

	@Override
	protected void onDestroy() {
		_dataSource.close();
		super.onDestroy();
	}

	/************************
	 * ACTION BAR METHODS
	 ***********************/

	/** Action Bar items (loaded as menu items) **/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.layout.station_header, menu);

		// Check if channel is already a favorite and update the star checkbox
		// TODO: Make it more efficient so you don't have to check everytime
		// TODO Temporary: We are currently getting all comments, but only get
		// selected ones
		List<Favorite> favorites = this._dataSource.getAllFavorites();
		boolean isChecked = false;
		for (int i = 0; i < favorites.size(); i++) {
			if (favorites.get(i).getName().equals(_stationName)) {
				isChecked = true;
				break;
			}
		}

		MenuItem item = menu.getItem(1);

		if (isChecked) {
			item.setChecked(true);
			item.setIcon(android.R.drawable.btn_star_big_on);
		} else {
			item.setChecked(false);
			item.setIcon(android.R.drawable.btn_star_big_off);
		}

		return super.onCreateOptionsMenu(menu);
	}

	/** selecting menu items **/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ab_refresh: // refresh the song
			this.refresh(null);
			return true;
		case R.id.ab_search: // perform a new search
			// TODO:
			// Look at performing new search
			return true;
		case R.id.ab_favorite:
			// TODO: Implement favoriting a station
			// change the star icon appropriately
			item.setChecked(!item.isChecked());
			if (item.isChecked()) {
				item.setIcon(android.R.drawable.btn_star_big_on);
				this.setFavoriteStation(true);
			} else {
				item.setIcon(android.R.drawable.btn_star_big_off);
				this.setFavoriteStation(false);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/************************
	 * STATION METHODS
	 ************************/
	
	/** Updates the current song via button click **/
	public void refresh(View view) {
		if (!isNetworkAvailable()) {
			// Alert!
			showDialog("No Internet Connection! Enable WiFi or 3G");
			return;
		}

		// Try to read the JSON information and then update the TextView
		AsyncTask readTask = new ReadSongTask(this)
				.execute(_selectedChannelName);// updateCurrentSong(_selectedChannelName);
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
			Log.e(StationActivity.class.getName(), "ERROR!!!: " + e.toString());
			// this.showToast("Nothing playing on this station right now",
			// false);
			// t.setText("Exception " + e.toString());
			// showToast("Error reading song info", false);
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

				Log.e(StationActivity.class.toString(), "RESPONSE BUILT: "
						+ builder.toString());

			} else {
				Log.e(StationActivity.class.toString(),
						"Failed to download file");
				finish();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			this._stationInfoTV.setText("Exception " + e.toString());
			this.showToast("Nothing playing on this station right now", false);
		} catch (IOException e) {
			e.printStackTrace();
			this._stationInfoTV.setText("Exception " + e.toString());
			this.showToast("Nothing playing on this station right now", false);
		}
		return builder.toString();
	}

	/** Allows the user to set the current station as a favorite **/
	public void setFavoriteStation(boolean setFavorite) {// View view) {
		// TODO:
		if (setFavorite) { // set as favorite
			this._dataSource.createFavorite(Favorite.Type.STATION,
					this._stationName);
			this.showToast(this._stationName + " added as a favorite", true);
		} else { // remove as favorite
			Favorite f = new Favorite();
			f.setName(this._stationName);
			f.setType(Favorite.Type.STATION);
			this._dataSource.deleteFavorite(f);
			this.showToast(this._stationName + " removed as a favorite", true);
		}
	}

	/** Inner class used just for listening to the Spinner **/
	public class MyOnItemSelectedListener implements OnItemSelectedListener {
	
		// When item selected
		// Should update the name if it's different from the current one and
		// then refresh the song name
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			// Get the selected station
	
			_selectedChannelName = parent.getItemAtPosition(pos).toString();
	
			@SuppressWarnings("unused")
			AsyncTask<String, Void, Boolean> readTask = new ReadSongTask(
					StationActivity.this).execute(_selectedChannelName);// updateCurrentSong(_selectedChannelName);
	
		}
	
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.
		}
	}
	
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
			StationActivity.this.setProgressBarIndeterminate(true);
			StationActivity.this.setProgressBarIndeterminateVisibility(true);
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
			/*
			 * _stationInfoTV.setText("Station: " + _selectedChannelName +
			 * "\n\tTime: " + time);
			 */
			if (result) {
				_currentArtistTV.setText("Artist:\n\t" + _currentArtist);
				_currentSongTV.setText("Song:\n\t" + _currentSong);
			} else
				// no response
				StationActivity.this.showToast(
						"Nothing playing on this station right now", true);
	
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

	/**********************
	 * SEARCH SONG METHODS
	 *********************/
	
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

	/******************
	 * HELPER METHODS
	 *****************/

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
	

	// Formats the HTML Strings properly
	// TODO: (&lt; &gt;)?
	private String properFormat(String str) {
		String proper = str.replaceAll("&amp;", "&");
		proper = proper.replaceAll("&quot;", "\"");
		proper = proper.replaceAll("&apos;", "\'");

		return proper;
	}

}
