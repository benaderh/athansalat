package athansalat.hb.com.athansalat;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;

public class ModifV extends AppCompatActivity {

	private DbHelper mHelper;
	private SQLiteDatabase dataBase;

	private final ArrayList<String> user_ID = new ArrayList<>();
	private final ArrayList<String> user_TX = new ArrayList<>();
	private final ArrayList<String> user_FV = new ArrayList<>();
	private final ArrayList<String> user_AC = new ArrayList<>();

	private ListView userList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.modif_v);

		userList = findViewById(R.id.lvVille);

		try {
			mHelper = new DbHelper(this);
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, "Erreur BDD", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		userList.setOnItemClickListener((parent, view, position, id) -> {
			Intent i = new Intent(getApplicationContext(), ModifVUp.class);
			i.putExtra("IDv", user_ID.get(position));
			i.putExtra("TXv", user_TX.get(position));
			i.putExtra("update", true);
			startActivity(i);
		});

		userList.setOnItemLongClickListener((parent, view, position, id) -> {
			showDeleteConfirmationDialog(position);
			return true;
		});
	}

	private void showDeleteConfirmationDialog(final int position) {
		if (userList.getAdapter().getCount() <= 4) {
			Toast.makeText(this, "لا يمكن حذف المزيد من المدن. يجب أن تبقى 4 مدن على الأقل.", Toast.LENGTH_LONG).show();
			return;
		}

		new AlertDialog.Builder(this)
				.setTitle("حذف مدينة")
				.setMessage("هل أنت متأكد من رغبتك في حذف مدينة " + user_TX.get(position) + "؟\n سيتم حذف جميع مواقيتها بشكل نهائي.")
				.setPositiveButton("نعم", (dialog, which) -> deleteCity(position))
				.setNegativeButton("لا", null)
				.show();
	}

	private void deleteCity(int position) {
		String cityId = user_ID.get(position);
		String cityFv = user_FV.get(position);

		dataBase = mHelper.getWritableDatabase();
		dataBase.beginTransaction();
		try {
			dataBase.delete("tb_villes", "id_v = ?", new String[]{cityId});
			dataBase.delete("tb_horaires", "n_vi = ?", new String[]{cityId});
			// Réorganiser les favoris
			dataBase.execSQL("UPDATE tb_villes SET fv_v = fv_v - 1 WHERE fv_v > ?", new String[]{cityFv});
			dataBase.setTransactionSuccessful();
			Toast.makeText(this, "تم حذف المدينة بنجاح", Toast.LENGTH_SHORT).show();
		} finally {
			dataBase.endTransaction();
		}

		displayData(); // Rafraîchir la liste
	}

	@Override
	protected void onResume() {
		super.onResume();
		displayData();
	}

	private void displayData() {
		dataBase = mHelper.getReadableDatabase();
		try (Cursor mCursor = dataBase.rawQuery("SELECT * FROM tb_villes ORDER BY fv_v ASC", null)) {
			user_ID.clear();
			user_TX.clear();
			user_FV.clear();
			user_AC.clear();
			if (mCursor.moveToFirst()) {
				do {
					user_ID.add(mCursor.getString(mCursor.getColumnIndexOrThrow("id_v")));
					user_TX.add(mCursor.getString(mCursor.getColumnIndexOrThrow("tx_v")));
					user_FV.add(mCursor.getString(mCursor.getColumnIndexOrThrow("fv_v")));
					user_AC.add(mCursor.getString(mCursor.getColumnIndexOrThrow("ac_v")));
				} while (mCursor.moveToNext());
			}
		}
		ConnectorV adapter = new ConnectorV(this, user_ID, user_TX, user_FV, user_AC);
		userList.setAdapter(adapter);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}