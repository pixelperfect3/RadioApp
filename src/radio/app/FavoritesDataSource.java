/** The "interface" for connecting to the database.
 * 
 *  Taken from:
 *  http://www.vogella.de/articles/AndroidSQLite/article.html
 *  
 *  TODO:
 *  -Add method to check if station/song already exists
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
	
	/** Creates a Favorite object according to type and name **/
	public void createFavorite(Favorite.Type type, String name) {
		// Check if it already exists
		/*Cursor cursor = database.query(MySQLiteHelper.TABLE_FAVORITES,
				allColumns, MySQLiteHelper.COLUMN_NAME + " = " + name, null,
				null, null, null);*/
		String sql = "SELECT * FROM " + MySQLiteHelper.TABLE_FAVORITES + " WHERE name = '" + name + "'";
		Cursor cursor = database.rawQuery(sql, null);
		
		if (!cursor.moveToFirst()) {
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
	}

	/** Deletes a specific Favorite **/
	// TODO: Delete by ID + TYPE + NAME?
	public void deleteFavorite(Favorite favorite) {
		String name = favorite.getName();
		Log.v("DELETING NAME!!", name);
		//Cursor cursor = database.rawQuery("SELECT * FROM " + MySQLiteHelper.TABLE_FAVORITES + " WHERE " + MySQLiteHelper.COLUMN_NAME + "= ?", new String[] { name });
		// Temporary TODO: Currently getting all favorites, but should only get one
		String sql = "SELECT * FROM " + MySQLiteHelper.TABLE_FAVORITES + " WHERE name = '" + name + "'";
		Cursor cursor = database.rawQuery(sql, null);
		if (cursor.moveToFirst()) {
			// TEMPORARY TODO: Get all favorites and list them out
			List<Favorite> faves = getAllFavorites();
			for (int i = 0; i < faves.size(); i++) {
				Log.v("FAVORITE!", faves.get(i).getId() + "," + faves.get(i).getName() + "," + faves.get(i).getType());
			}
			
			
		    // record exists
			database.delete(MySQLiteHelper.TABLE_FAVORITES, MySQLiteHelper.COLUMN_NAME
					+ " = '" + name + "'", null);
			Log.v("DELETING FAVORITE", "Favorite deleted with name: " + name);
		} else {
		    // record not found
		}
		/*Cursor cursor = database.query(MySQLiteHelper.TABLE_FAVORITES,
				allColumns, null, null, null, null, null);
		while (!cursor.isAfterLast()) {
			Favorite f = cursorToFavorite(cursor);
			if (f.getName().equals(name)) {
				database.delete(MySQLiteHelper.TABLE_FAVORITES, MySQLiteHelper.COLUMN_NAME
						+ " = " + name, null);
				Log.v("DELETING FAVORITE", "Favorite deleted with name: " + name);
			}
			cursor.moveToNext();
		}*/
		
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