/**
 * This is a custom implementation of an adapter for a ListView
 * 
 * It is used for the CityActivity to show a list of all the stations in that city
 * 
 */

package radio.app;

import java.util.List;

import android.content.Context;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class CustomArrayAdapter extends ArrayAdapter<String> {
	private final Context context;
	private final String[] values;
	
	// Database for storing favorites
	private FavoritesDataSource _dataSource;
	List<Favorite> favorites; // all the favorites
	
	// For Strings
	public CustomArrayAdapter(Context context, String[] values) {
		super(context, R.layout.rowlayout, values);
		this.context = context;
		this.values = values;
		
		/** Open the favorites database **/
		_dataSource = new FavoritesDataSource(context);
		_dataSource.open();
		favorites = this._dataSource.getAllFavorites();
	}
	
	static class ViewHolder {
		protected TextView text1, text2;
		protected CheckBox checkbox;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		ViewHolder viewHolder;
		
		// When convertView is not null, we can reuse it directly, there is
		// no need to reinflate it. We only inflate a new View when the convertView
		// supplied by ListView is null.
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.rowlayout, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.text1 = (TextView) convertView.findViewById(R.id._stationName1);
			viewHolder.text2 = (TextView) convertView.findViewById(R.id._stationName2);
			viewHolder.checkbox = (CheckBox) convertView.findViewById(R.id._favoriteStar);
			
			convertView.setTag(viewHolder);
		} 
		else {
			viewHolder = (ViewHolder)convertView.getTag();
		}
		
		// Break up the string into two
		String[] pieces = values[position].split(" - ");
		
		viewHolder.text1.setText(pieces[0]);
		if (pieces.length > 1)
			viewHolder.text2.setText(pieces[1]);

		// Star to show if station is favorited or not
		boolean isChecked = false;
		for (int i = 0; i < favorites.size(); i++) {
			if (favorites.get(i).getName().contains(pieces[0])) { 
				isChecked = true;
				break;
			}
		}
		
		if (isChecked)
			viewHolder.checkbox.setChecked(true);
		else 
			viewHolder.checkbox.setChecked(false);
		
		return convertView;
	}
}