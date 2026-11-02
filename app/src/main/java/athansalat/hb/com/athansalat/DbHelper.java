package athansalat.hb.com.athansalat;


/**
 * Created by B on 14/11/2016.
 */
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DbHelper extends SQLiteOpenHelper {
	private Context mycontext;

	// Chemin dynamique basé sur le contexte (compatible avec tous les appareils Android)
	public String DB_PATH;
	public static String DB_NAME = "db_horaires.sqlite";
	private static String ho_TABLE = "qr_horaires";
	private static String al_TABLE = "tb_alarms";
	private static String qral_TABLE = "qr_alarms";
	private static String hi_TABLE = "tb_hijc";
	private static String vi_TABLE = "qr_villes";
	private static String fv_TABLE = "tb_villes";
	private static String pr_TABLE = "tb_pref";

	public static final String KEY_DG = "d_gr1";
	public static final String KEY_DH = "d_hi1";
	public static final String KEY_NJ = "n_jr";

	public static final String KEY_ID = "id_v";
	public static final String KEY_TX = "tx_v";
	public static final String KEY_FV = "fv_v";
	public static final String KEY_AC = "ac_v";


	//public static final String EMPLOYEE_TABLE = "employee";

	public SQLiteDatabase myDataBase;

    /*private String DB_PATH = "/data/data/"
                                + mycontext.getApplicationContext().getPackageName()
                                + "/databases/";
*/


/*	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, 1);

	}*/

/*	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_TABLE="CREATE TABLE "+hi_TABLE+" ("+KEY_DG+" INTEGER PRIMARY KEY, "+KEY_DH+" TEXT, "+KEY_NJ+" INTEGR)";
		db.execSQL(CREATE_TABLE);

	}*/

/*	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS "+hi_TABLE);
		onCreate(db);

	}*/


	public DbHelper(Context context) throws IOException  {
		super(context, DB_NAME, null, 1);
		this.mycontext = context;
		// Utiliser le chemin dynamique fourni par Android (pas de chemin codé en dur)
		this.DB_PATH = context.getDatabasePath(DB_NAME).getParent() + "/";
	}

	public void createdatabase() throws IOException{
		boolean dbexist = checkdatabase();
		if(dbexist)
		{
			System.out.println(" Database exists.");
		}
		else{
			this.getReadableDatabase();
			try{
				copydatabase();
			}
			catch(IOException e){
				throw new Error("Error copying database");
			}
		}
	}
	private boolean checkdatabase() {
		// Vérification correcte : on teste juste si le fichier existe
		File dbFile = new File(DB_PATH + DB_NAME);
		return dbFile.exists();
	}
	private void copydatabase() throws IOException {

		//Open your local db as the input stream
		InputStream myinput = mycontext.getAssets().open(DB_NAME);

		// Path to the just created empty db
		String outfilename = DB_PATH + DB_NAME;

		//Open the empty db as the output stream
		OutputStream myoutput = new FileOutputStream(outfilename);

		// transfer byte to inputfile to outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myinput.read(buffer))>0)
		{
			myoutput.write(buffer,0,length);
		}

		//Close the streams
		myoutput.flush();
		myoutput.close();
		myinput.close();

	}

	public void opendatabase() throws SQLException
	{
		//Open the database
		String mypath = DB_PATH + DB_NAME;
		myDataBase = SQLiteDatabase.openDatabase(mypath, null, SQLiteDatabase.OPEN_READONLY);

	}

	public synchronized void close(){
		if(myDataBase != null){
			myDataBase.close();
		}
		super.close();
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}


	// CORRECTION : Les méthodes rawQuery exécutent maintenant la vraie requête SQL passée en paramètre
	public Cursor horawQuery(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.rawQuery(sql, selectionArgs);
	}

	public Cursor alrawQuery(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.rawQuery(sql, selectionArgs);
	}

	public Cursor qralrawQuery(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.rawQuery(sql, selectionArgs);
	}

	public Cursor hirawQuery(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.rawQuery(sql, selectionArgs);
	}

	public Cursor virawQuery(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.rawQuery(sql, selectionArgs);
	}

	public Cursor fvrawQuery(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.rawQuery(sql, selectionArgs);
	}

	public Cursor prrawQuery(String sql, String[] selectionArgs) {
		SQLiteDatabase db = this.getReadableDatabase();
		return db.rawQuery(sql, selectionArgs);
	}

	@Override
	public void onCreate(SQLiteDatabase arg0) {
		// TODO Auto-generated method stub

	}

}