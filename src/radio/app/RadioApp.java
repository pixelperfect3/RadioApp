package radio.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/** TODO:
    -Customize the Spinner
    -Customize the Button (so they don't look so ugly)
    -Prettier backgrounds
    -Just generally less ugly
    -Use star icons for favouriting
    -Add arrows for moving forward/backward along radio stations
    -Maybe arrows for showing previously played songs? 
    
    -For songs, show options to explore in Amazon/google Music, etc. To open a link, here's an example:
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
    
    **/


public class RadioApp extends Activity {
	// The context
	Context context;
	
	// Strings for location, name, artist, song
	private String _location = "gainesville";
	private String _selectedChannelName = ""; // 105.3 Gainesville at first - WYKS
	private String _currentArtist = ""; 
	private String _currentSong = "";

	// The TextView
	TextView _locationTV, _stationInfoTV;

	// The Button
	Button button;

	// Time
	Time now;
	
	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// set the custom title
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		// indeterminate amount of time for a task
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS); 
		
		// set layout to the "city"
        setContentView(R.layout.city);
 
        // custom title
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
		
        // set the custom texview and icon
        TextView title = (TextView) findViewById(R.id.title);
        ImageView icon  = (ImageView) findViewById(R.id.windowSearchButton);
        title.setText("Stations");
        //icon.setImageResource(R.drawable.icon);
        
        
		// To indicate it's busy
		//RadioApp.this.setProgressBarIndeterminate(true);
		//RadioApp.this.setProgressBarIndeterminateVisibility(true);
		
		//setProgressBarIndeterminateVisibility(true);
		
		// get the context
		//this.context = getApplicationContext();
		
		// get the text views
		this._stationInfoTV =(TextView)findViewById(R.id.text);
		this._locationTV = (TextView)findViewById(R.id.location);

		// get the city from the Bundle passed in
		String city = getIntent().getExtras().getString("CITY_NAME");
		if (city != null) {
			_location = city;
			this._locationTV.setText(_location);
		}
		
		// Check if internet connection is available
		if (!isNetworkAvailable()) {
			//showToast("No internet connection!");

			// Create an alert dialog
			showDialog("No Internet Connection! Enable WiFi or 3G");
			return;
		}
		
		/** Setup the Spinner by populating it with the list of radio stations **/
		// read the list of stations in an asynctask
		AsyncTask<String, Void, String[]> readStationsTask = new ReadStationsTask(this).execute();
		
		try {
			// get list of all the stations from the task
			String[] stations = new String[1];
			
			stations = readStationsTask.get();
			
			if (stations != null) {
				// set selected channel to the first one
				this._selectedChannelName = stations[0];
				
				
				// Populate the Spinner
				Spinner s = (Spinner) findViewById(R.id.spinner1);
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
						android.R.layout.simple_spinner_item, stations);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				s.setAdapter(adapter);
				// now set the Spinner to listen for changes
				s.setOnItemSelectedListener(new MyOnItemSelectedListener());
			}
		} catch (Exception e) {
			Log.e("Reading Stations", "Cannot execute AsyncTask");
		}

		// the Time object
		now = new Time();

		

		// Try to read the JSON information and then update the TextView
		//boolean update = updateCurrentSong(_selectedChannelName);
		if (this._selectedChannelName != "") {
			AsyncTask readTask = new ReadSongTask(this).execute(_selectedChannelName);//updateCurrentSong(_selectedChannelName);
	
			try {
				// if can't update, make toast saying there was an error with reading
				if ( (!(Boolean)readTask.get()) ) { //(!update) {
					CharSequence text = "Error reading information";
					showToast(text, true);
				}
			} catch (Exception e) {
				
			}
		}
		
		//RadioApp.this.setProgressBarIndeterminate(false);
		//RadioApp.this.setProgressBarIndeterminateVisibility(false);

	}

	/** Updates the current song via button click **/
	public void refresh(View view) {
		// progress bar
		RadioApp.this.setProgressBarIndeterminate(true);
		RadioApp.this.setProgressBarIndeterminateVisibility(true);
		
		if (!isNetworkAvailable()) {
			// Alert!
			showDialog("No Internet Connection! Enable WiFi or 3G");
			return;
		}

		// Try to read the JSON information and then update the TextView
		AsyncTask readTask = new ReadSongTask(this).execute(_selectedChannelName);//updateCurrentSong(_selectedChannelName);

		try {
			// if can't update, make toast saying there was an error with reading
			if ( (!(Boolean)readTask.get()) ) { //(!update) {
				CharSequence text = "Error reading information";
				showToast(text, true);
			}
		} catch (Exception e) {
			
		}
		// if can't update, make toast saying there was an error with reading
		/*if ( ((String)readTask.get()) == null ) { //(!update) {
			CharSequence text = "Error reading information";
			showToast(text);
		}*/
	}

	/** Updates the current song on the selected channel **/
	private boolean updateCurrentSong(String channelName) {
		// Read all the JSON information
		// Need to add the "[" and "]" so that it can be properly parsed 
		String readJSONFeed = "[" + readJSON(channelName, true) + "]";

		try {
			// convert it to a json array
			JSONArray jsonArray = new JSONArray(readJSONFeed);
			// get the first object which has all the info
			JSONObject firstJson = jsonArray.getJSONObject(0);

			// Channel name
			String name = firstJson.getString("name");

			// get the rest of the info
			// for ex., the current song
			JSONObject currentSong = firstJson.getJSONObject("now");

			// get the name of the artist and the song
			String artist = properFormat(currentSong.getString("artist"));
			String song = properFormat(currentSong.getString("song"));

			// don't bother updating if it's still the same song
			if (song.equals(_currentSong)) {
				return true;
			}

			// update the current song and artist
			_currentArtist = artist;
			_currentSong = song;

			//Log.i(RadioApp.class.getName(),
			//		"Number of entries " + jsonArray.length());

			// get all the json objects
			//String text = "";
			/*(for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				text += jsonObject.toString() + "Next:\n";
				//Log.i(RadioApp.class.getName(), jsonObject.getString("text"));
			}*/

			// Get the current time
			//now = new Time();
			now.setToNow();

			// Update all the info in the AsyncTask!
			
			//String time = now.format("%H:%M");
			// Alternative:
			/* String delegate = "hh:mm aaa";
	           String time = (String) DateFormat.format(delegate,Calendar.getInstance().getTime()); */

			// update the text
			//t.setText("Name: " + name + "\nArtist: " + _currentArtist + "\nSong: " + _currentSong + "\nTime: " + time);//readJSONFeed);
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			Log.e(RadioApp.class.getName(), "ERROR!!!: " + e.toString());
			//t.setText("Exception " + e.toString());
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
			httpGet = new HttpGet(
					"http://api.yes.com/1/station?name=" + info);
		} else { // get list of stations instead for the provided city/zipcode
			httpGet = new HttpGet(
					"http://api.yes.com/1/stations?loc=" + info);
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
				
				Log.e(RadioApp.class.toString(), "RESPONSE BUILT: " + builder.toString());
				
			} else {
				Log.e(RadioApp.class.toString(), "Failed to download file");
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


	/** Helper method for showing a Toast notification **/
	private void showToast(CharSequence text, boolean timeShort) {
		Context context = getApplicationContext();
		int duration = timeShort? Toast.LENGTH_SHORT : Toast.LENGTH_LONG; // long or short?

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}

	// Creates an alert dialog
	private void showDialog(CharSequence text) {
		// build a dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// set the message
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

	/** Checks if internet connection is available **/
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	/** Inner class used just for listening to the Spinner **/
	public class MyOnItemSelectedListener implements OnItemSelectedListener {

		// When item selected
		// Should update the name if it's different from the current one and 
		// then refresh the song name
		public void onItemSelected(AdapterView<?> parent,
				View view, int pos, long id) {
			// Get the selected station
			String station = parent.getItemAtPosition(pos).toString();

			// Extract the 4-letter name
			Scanner scan = new Scanner(station);
			String name = scan.next();

			// Try to update the song if possible
			if (name.equals(_selectedChannelName)) {
				// same channel, so no need to update
				return;
			} else {
				// update
				_selectedChannelName = name;
				// Try to read the JSON information and then update the TextView
				AsyncTask readTask = new ReadSongTask(RadioApp.this).execute(_selectedChannelName);//updateCurrentSong(_selectedChannelName);

				try {
					// if can't update, make toast saying there was an error with reading
					if ( (!(Boolean)readTask.get()) ) { //(!update) {
						CharSequence text = "Error reading information";
						showToast(text, true);
					}
				} catch (Exception e) {
					
				}
			}


			// Toast.makeText(parent.getContext(), "The planet is " +
			// parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
		}

		public void onNothingSelected(AdapterView parent) {
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
			RadioApp.this.setProgressBarIndeterminate(true);
			RadioApp.this.setProgressBarIndeterminateVisibility(true);
			// start the progress dialog
			dialog = new ProgressDialog(theContext);
		}
		
		@Override
		protected String[] doInBackground(String... params) {
			// perform long running operation operation
			// update the list of stations
			
			String channelJSON = readJSON(_location, false);
			Log.i(RadioApp.class.getName(),"channelJSON: " + channelJSON);

			try {
				// create the json object from the string
				JSONObject jsonobj = new JSONObject(channelJSON);

				// get the array of stations
				JSONArray jsonArray = new JSONArray(jsonobj.getString("stations"));

				/** TODO: Perhaps part below should be in postExecute()? **/
				
				// Create the array of strings which will be used for populating the spinner
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
				Log.e(RadioApp.class.getName(), "JSON Exception: " + e.toString());
				return null;
			}
		}

		@Override
		protected void onPostExecute(String[] result) {
			// execution of result of Long time consuming operation
			// dismiss the dialog
			dialog.dismiss();
			RadioApp.this.setProgressBarIndeterminate(false);
			RadioApp.this.setProgressBarIndeterminateVisibility(false);
			/*String time = now.format("%H:%M");
			_stationInfoTV.setText("Name: " + _selectedChannelName + "\nArtist: " + _currentArtist + "\nSong: " + _currentSong + "\nTime: " + time);
			RadioApp.this.setProgressBarIndeterminate(false);
			RadioApp.this.setProgressBarIndeterminateVisibility(false);*/
		}

		@Override
		protected void onPreExecute() {
			// Things to be done before execution of long running operation. For example showing ProgessDialog
			
			// set the properties of the progressdialog
			// make sure it's indeterminate
			this.dialog.setMessage("Loading info");
			this.dialog.setIndeterminate(true);
	        this.dialog.show();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// Things to be done while execution of long running operation is in progress. For example updating ProgessDialog
		}
	} /** End of AsyncTask class **/
	
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
			RadioApp.this.setProgressBarIndeterminate(true);
			RadioApp.this.setProgressBarIndeterminateVisibility(true);
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
			_stationInfoTV.setText("Name: " + _selectedChannelName + "\nArtist: " + _currentArtist + "\nSong: " + _currentSong + "\nTime: " + time);
			RadioApp.this.setProgressBarIndeterminate(false);
			RadioApp.this.setProgressBarIndeterminateVisibility(false);
		}

		@Override
		protected void onPreExecute() {
			// Things to be done before execution of long running operation. For example showing ProgessDialog
			
			// set the properties of the progressdialog
			// make sure it's indeterminate
			this.dialog.setMessage("Loading info");
			this.dialog.setIndeterminate(true);
	        this.dialog.show();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			// Things to be done while execution of long running operation is in progress. For example updating ProgessDialog
		}
	} /** End of AsyncTask class **/

}