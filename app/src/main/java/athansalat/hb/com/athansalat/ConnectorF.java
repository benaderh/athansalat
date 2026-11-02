package athansalat.hb.com.athansalat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.database.sqlite.SQLiteDatabase;

import android.content.Context;
import android.database.Cursor;

/**
 * 
 * @author manish.s
 *
 */

public class ConnectorF {
	
	private SQLiteDatabase database;
	DbHelper db;
	private Cursor cursor;
	
	//private static final String TABLE_Active = "fv_villes";

	
	public ConnectorF(Context context) {

		//db = new DbHelper(context);
		try {
			db = new DbHelper(context);
		} catch (IOException e2) {

			e2.printStackTrace();
		}


		try {
			db.createdatabase();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}
	
	// Getting All records
			public List<String> getAllRecord() {
				List<String> villesList = new ArrayList<String>();
				String selectQuery = "SELECT  * FROM fv_villes";

				database = db.getReadableDatabase();
				cursor = database.rawQuery(selectQuery, null);
				if (cursor.moveToFirst()) {
					do {
						villesList.add(cursor.getString(1));

					} while (cursor.moveToNext());
				}
				database.close();
				return villesList;
			}
	
}
