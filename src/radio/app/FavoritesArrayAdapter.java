/**
 * This is a custom implementation of an adapter for a ListView
 * 
 * It is used for the MainActivity to show a list of all the favorites
 * 
 */

package radio.app;

import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class FavoritesArrayAdapter extends ArrayAdapter<Favorite> {
	private final MainActivity context;
	private List<Favorite> favorites;

	// For Favorites
	public FavoritesArrayAdapter(MainActivity context, List<Favorite> favorites) {
		super(context, R.layout.rowlayout, favorites);
		this.context = context;
		this.favorites = favorites;
	}
	
	static class ViewHolder {
		protected TextView text1, text2;
		protected CheckBox checkbox;
	}
	
	/** Sets the List item's contents **/
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		final ViewHolder viewHolder;
		
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.rowlayout, parent, false);
			viewHolder = new ViewHolder();
			// Get the widgets
			viewHolder.text1 = (TextView) convertView.findViewById(R.id._stationName1);
			viewHolder.text2 = (TextView) convertView.findViewById(R.id._stationName2);
			viewHolder.checkbox = (CheckBox) convertView.findViewById(R.id._favoriteStar);
			viewHolder.checkbox.setChecked(true); // has to be checked to be a favorite
			viewHolder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					// get the favorite
					Favorite favorite = new Favorite();
					//Log.v("ViewHolder:", viewHolder.text1.getText().toString());
					favorite.setName(viewHolder.text1.getText().toString());
					favorite.setType(Favorite.Type.STATION);
					// favorite = (Favorite) viewHolder. .checkbox.getTag();
					//element.setSelected(buttonView.isChecked());
					
					// delete it
					MainActivity._dataSource.deleteFavorite(favorite);
					
					// refresh the view
					context.removeFavorite(favorite);
				}
			});
			
			convertView.setTag(viewHolder);
		}
		else {
			viewHolder = (ViewHolder)convertView.getTag();
		}
		
		// Get the name
		String name = favorites.get(position).getName();
		viewHolder.text1.setText(name);
		/*if (pieces.length > 1)
			viewHolder.text2.setText(pieces[1]);*/

		return convertView;
	}
}