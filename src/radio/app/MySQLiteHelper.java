/** Helper class for reading from a database 
 * 
 *  taken from:
 *  http://www.vogella.de/articles/AndroidSQLite/article.html
 */

package radio.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

	public static final String TABLE_FAVORITES = "favorites";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_NAME = "name";

	private static final String DATABASE_NAME = "favorites.db";
	private static final int DATABASE_VERSION = 1;

	// Database creation sql statement
	private static final String DATABASE_CREATE = "create table "
			+ TABLE_FAVORITES + "( " + COLUMN_ID + " integer primary key autoincrement, "
									 + COLUMN_TYPE + " integer not null, " 
									 + COLUMN_NAME + " text not null);";

	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		
		database.execSQL(DATABASE_CREATE);
		Log.v("CREATING DATABASE", "Created Database");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS" + TABLE_FAVORITES);
		onCreate(db);
	}

	// deletes the table
	//public void deleteTable() {
		//SQLiteDatabase database = getWritableDatabase();
		//database.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
	//}
	
}