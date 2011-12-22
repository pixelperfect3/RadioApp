package radio.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


/** TODO:
    -Custom background
    -Have main app name with search box (should lead to different activity)
    -Show favourite stations
    -Allow location by GPS **/


public class MainActivity extends Activity {
	/** PROPERTIES **/
	EditText searchText;
	
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
			showDialog("Nothing entered!");
			return;
		}
		
		// check internet connection
		if (!this.isNetworkAvailable()) {
			showDialog("Not Internet connection. Please turn on wi-fi or your data connection.");
			return;
		}
		
		// else, do a search
		
		// Temporary:
		// Start the other activity
		Intent intent = new Intent(this, RadioApp.class);
		/** Add Bundle here:
		
		Bundle parameters = new Bundle();
		parameters.putString("PARAM_IDENT", "parameter_value");
		intent.putExtras(parameters); */
		
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
	
	// Checks if internet connection is available
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null;
	}

	// Creates an alert dialog
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
	
}