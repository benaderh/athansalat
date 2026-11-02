package athansalat.hb.com.athansalat;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ListView;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ModifD extends AppCompatActivity {

	private DbHelper mHelper;
	// --- CORRECTION : Une seule variable pour la base de données ---
	private SQLiteDatabase database;

	private final ArrayList<String> user_DG0 = new ArrayList<>();
	private final ArrayList<String> user_DG = new ArrayList<>();
	private final ArrayList<String> user_DH = new ArrayList<>();
	private final ArrayList<String> user_NJ = new ArrayList<>();

	private ListView userList;
	private int selectedItemPosition = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.modif_d);

		userList = findViewById(R.id.lvHijirD);

		try {
			mHelper = new DbHelper(this);
			// Obtenir une instance de la base de données dès le début
			database = mHelper.getWritableDatabase();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, "Erreur BDD", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		userList.setOnItemLongClickListener((parent, view, position, id) -> {
			showMonthModificationDialog(position);
			return true;
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		displayData();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Fermer la base de données lorsque l'activité est détruite
		if (database != null && database.isOpen()) {
			database.close();
		}
	}

	private void showMonthModificationDialog(final int position) {
		String currentDuration = user_NJ.get(position).substring(0, 2);
		String monthName = user_DH.get(position).substring(2);
		boolean isCurrently29 = currentDuration.equals("29");

		String title = "تغيير مدة الشهر";
		String message = "هل تريد تغيير مدة شهر " + monthName + "\n" +
				"من " + currentDuration + " يوم إلى " + (isCurrently29 ? "30" : "29") + " يوم؟" + "\n\n" +
				"ملاحظة: هذا سيؤدي إلى تعديل تاريخ بداية كل الشهور التالية.";

		new AlertDialog.Builder(this)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton("نعم", (dialog, which) -> {
					selectedItemPosition = position;
					modifyMonthDuration(position, isCurrently29 ? 1 : -1);
				})
				.setNegativeButton("لا", null)
				.create()
				.show();
	}

	private void modifyMonthDuration(int position, int dayChange) {
		String gregorianDateKey = user_DG0.get(position);

		database.beginTransaction();
		try {
			database.execSQL("UPDATE tb_hijc SET n_jr = n_jr + ? WHERE d_gr1 = ?", new Object[]{dayChange, gregorianDateKey});
			database.execSQL("UPDATE tb_hijc SET d_gr1 = d_gr1 + ? WHERE d_gr1 > ?", new Object[]{dayChange, gregorianDateKey});

			database.setTransactionSuccessful();
			Toast.makeText(this, "تم تحديث التقويم بنجاح", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(this, "حدث خطأ أثناء التحديث", Toast.LENGTH_SHORT).show();
			Log.e("ModifD", "Error modifying month duration", e);
		} finally {
			database.endTransaction();
		}

		displayData();
	}

	private void displayData() {
		// La base de données est déjà ouverte, pas besoin de la rouvrir
		try (Cursor mCursor = database.rawQuery("SELECT * FROM tb_hijc ORDER BY d_gr1 ASC", null)) {
			user_DG0.clear();
			user_DG.clear();
			user_DH.clear();
			user_NJ.clear();

			if (mCursor.moveToFirst()) {
				do {
					user_DG0.add(mCursor.getString(mCursor.getColumnIndexOrThrow(DbHelper.KEY_DG)));
					long excelDate = mCursor.getLong(mCursor.getColumnIndexOrThrow(DbHelper.KEY_DG));
					long millis = (excelDate - 25569) * 86400000L;
					Date date = new Date(millis);
					String dayOfWeek = new SimpleDateFormat("E", Locale.getDefault()).format(date);
					String formattedDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date);
					user_DG.add(getArabicDayName(dayOfWeek) + "  " + formattedDate);
					user_DH.add("1 " + mCursor.getString(mCursor.getColumnIndexOrThrow(DbHelper.KEY_DH)));
					user_NJ.add(mCursor.getString(mCursor.getColumnIndexOrThrow(DbHelper.KEY_NJ)) + " يوم");
				} while (mCursor.moveToNext());
			}

			ConnectorD disadpt = new ConnectorD(this, user_DG, user_DH, user_NJ);
			userList.setAdapter(disadpt);
			userList.setSelection(selectedItemPosition);
		}
	}

	private String getArabicDayName(String dayAbbreviation) {
		switch (dayAbbreviation.toLowerCase()) {
			case "sun": return "الأحد";
			case "mon": return "الاثنين";
			case "tue": return "الثلاثاء";
			case "wed": return "الأربعاء";
			case "thu": return "الخميس";
			case "fri": return "الجمعة";
			case "sat": return "السبت";
			default: return dayAbbreviation;
		}
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