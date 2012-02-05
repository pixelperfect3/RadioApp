package radio.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class CustomArrayAdapter extends ArrayAdapter<String> {
	private final Context context;
	private final String[] values;

	public CustomArrayAdapter(Context context, String[] values) {
		super(context, R.layout.rowlayout, values);
		this.context = context;
		this.values = values;
	}

	static class ViewHolder {
		protected TextView text1, text2;
		protected CheckBox checkbox;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
		final ViewHolder viewHolder = new ViewHolder();
		viewHolder.text1 = (TextView) rowView.findViewById(R.id._stationName1);
		viewHolder.text2 = (TextView) rowView.findViewById(R.id._stationName2);
		viewHolder.checkbox = (CheckBox) rowView.findViewById(R.id._favoriteCheckbox);
		/*TextView textView = (TextView) rowView.findViewById(R.id.label);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		textView.setText(values[position]);
		// Change the icon for Windows and iPhone
		String s = values[position];
		if (s.startsWith("Windows7") || s.startsWith("iPhone")
				|| s.startsWith("Solaris")) {
			imageView.setImageResource(R.drawable.no);
		} else {
			imageView.setImageResource(R.drawable.ok);
		}*/
		
		// Break up the string into two
		String[] pieces = values[position].split(" - ");
		
		rowView.setTag(viewHolder);
		viewHolder.text1.setText(pieces[0]);
		if (pieces.length > 1)
			viewHolder.text2.setText(pieces[1]);

		return rowView;
	}
}