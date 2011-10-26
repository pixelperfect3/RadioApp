package radio.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

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
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RadioApp extends Activity {
	// TODO:
	// Add a spinner to get a list of all the channels
	private String _selectedChannelName = "WYKS"; // 105.3 Gainesville
	
	// The TextView
	TextView t;
	
	// The Button
	Button button;
	
	// Time
	Time now;
	
/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// get the text view
		t=(TextView)findViewById(R.id.text); 
		
		// time object
		now = new Time();
		
		// Check if internet connection is available
		boolean net = this.isNetworkAvailable();
		if (!net) {
			//showToast("No internet connection!");
			
			// Create an alert dialog
			showDialog("No Internet Connection! Enable WiFi or 3G");
			return;
		}
		
		// Try to read the JSON information and then update the TextView
		boolean update = updateCurrentSong(_selectedChannelName);
		
		// if can't update, make toast saying there was an error with reading
		if (!update) {
			CharSequence text = "Error reading information";
			showToast(text);
		}
		
		// Add the listener to the button
		// The button will refresh the current song being played
		/*final Button button = (Button) findViewById(R.id.refreshButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	updateCurrentSong(_selectedChannelName);
            }
        });*/
		
	}

	// Updates the current song via button click
	public void refresh(View view) {
		// Check if internet connection is available
		boolean net = this.isNetworkAvailable();
		if (!net) {
			//showToast("No internet connection!");
			
			// Create an alert dialog
			showDialog("No Internet Connection! Enable WiFi or 3G");
			return;
		}
		
		// Try to read the JSON information and then update the TextView
		boolean update = updateCurrentSong(_selectedChannelName);
		
		// if can't update, make toast saying there was an error with reading
		if (!update) {
			CharSequence text = "Error reading information";
			showToast(text);
		}
	}
	
	// Updates the current song on the selected channel
	private boolean updateCurrentSong(String channelName) {
		// Read all the JSON information
		// Need to add the "[" and "]" so that it can be properly parsed 
		String readJSONFeed = "[" + readJSON(channelName) + "]";
		
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
			String artist = currentSong.getString("artist");
			String song = currentSong.getString("song");
			//Log.i(RadioApp.class.getName(),
			//		"Number of entries " + jsonArray.length());
			
			// get all the json objects
			String text = "";
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				text += jsonObject.toString() + "Next:\n";
				//Log.i(RadioApp.class.getName(), jsonObject.getString("text"));
			}
			
			// Get the current time
			//now = new Time();
			now.setToNow();
			String delegate = "hh:mm aaa";
			String time = now.format("%H:%M");
	        //String time = (String) DateFormat.format(delegate,Calendar.getInstance().getTime());
			
			// update the text
		    t.setText("Name: " + name + "\nArtist: " + artist + "\nSong: " + song + "\nTime: " + time);//readJSONFeed);
		    return true;
			
		} catch (Exception e) {
			e.printStackTrace();
			t.setText("Exception " + e.toString());
		}
		
		return false;
	}
	
	// Reads the JSON about the channel
	public String readJSON(String channelName) {
		StringBuilder builder = new StringBuilder();
		
		// Create a HTTPClient
		HttpClient client = new DefaultHttpClient();
		
		// Open a GET connection to the Yes.com api
		HttpGet httpGet = new HttpGet(
				//"http://twitter.com/statuses/user_timeline/vogella.json");
				"http://api.yes.com/1/station?name=" + channelName);
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
			} else {
				Log.e(RadioApp.class.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			t.setText("Exception " + e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			t.setText("Exception " + e.toString());
		}
		return builder.toString();
	}


	// Helper method for showing a Toast notification
	private void showToast(CharSequence text) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;//  Toast.LENGTH_SHORT;

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
	
	// Checks if internet connection is available
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}
}