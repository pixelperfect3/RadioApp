/** The "interface" for connecting to the database.
 * 
 *  Taken from:
 *  http://www.vogella.de/articles/AndroidSQLite/article.html
 *  
 *  
 */

package radio.app;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class FavoritesDataSource {

	// Database fields
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID, 
									MySQLiteHelper.COLUMN_TYPE,
									MySQLiteHelper.COLUMN_NAME };

	public FavoritesDataSource(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		
		database = dbHelper.getWritableDatabase();
		//Log.v("CREATING DATABASE", "3");
	}

	public void close() {
		dbHelper.close();
	}

	/** Deletes the table **/
	public void delete() {
		dbHelper.deleteTable();
	}
	
	/** Checks if a Favorite already exists or not **/
	public boolean hasFavorite(String name) {
		String sql = "SELECT * FROM " + MySQLiteHelper.TABLE_FAVORITES + " WHERE name = '" + name + "'";
		Cursor cursor = database.rawQuery(sql, null);
		
		if (!cursor.moveToFirst()) {
			cursor.close();
			return false;
		}
		
		cursor.close();
		return true;
	}
	
	/** Creates a Favorite object according to type and name **/
	public void createFavorite(Favorite.Type type, String name) {
		// Check if it already exists
		String sql = "SELECT * FROM " + MySQLiteHelper.TABLE_FAVORITES + " WHERE name = '" + name + "'";
		Cursor cursor = database.rawQuery(sql, null);
		
		if (!hasFavorite(name)) {//!cursor.moveToFirst()) {
		    // record does not exist, add it
			ContentValues values = new ContentValues();
			values.put(MySQLiteHelper.COLUMN_TYPE, type.name());
			values.put(MySQLiteHelper.COLUMN_NAME, name);
			long insertId = database.insert(MySQLiteHelper.TABLE_FAVORITES, null,
					values);
			Log.v("ADDING FAVORITE", "Favorite added with name: " + name + " id: " + insertId);
		} else {
		    // record not found
		}
		cursor.close();
	}

	/** Deletes a specific Favorite **/
	public void deleteFavorite(Favorite favorite) {
		String name = favorite.getName();
		Log.v("DELETING NAME!!", name);
		String sql = "SELECT * FROM " + MySQLiteHelper.TABLE_FAVORITES + " WHERE name = '" + name + "'";
		Cursor cursor = database.rawQuery(sql, null);
		if (cursor.moveToFirst()) {
			
		    // record exists
			database.delete(MySQLiteHelper.TABLE_FAVORITES, MySQLiteHelper.COLUMN_NAME
					+ " = '" + name + "'", null);
			Log.v("DELETING FAVORITE", "Favorite deleted with name: " + name);
		} else {
		    // record not found
		}
		cursor.close();
	}

	/** Gets all the favorites **/
	public List<Favorite> getAllFavorites() {
		List<Favorite> favorites = new ArrayList<Favorite>();
		Cursor cursor = database.query(MySQLiteHelper.TABLE_FAVORITES,
				allColumns, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Favorite favorite = cursorToFavorite(cursor);
			favorites.add(favorite);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return favorites;
	}

	/** Converts a cursor into a Favorite object **/
	private Favorite cursorToFavorite(Cursor cursor) {
		Favorite favorite = new Favorite();
		favorite.setId(cursor.getInt(0));
		favorite.setType(cursor.getInt(1) == 0 ? Favorite.Type.STATION : Favorite.Type.SONG);
		favorite.setName(cursor.getString(2));
		return favorite;
	}
}