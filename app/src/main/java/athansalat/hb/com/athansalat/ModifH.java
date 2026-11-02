package athansalat.hb.com.athansalat;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import athansalat.hb.com.athansalat.databinding.ModifHBinding;
import athansalat.hb.com.athansalat.databinding.RowModifHBinding;

public class ModifH extends AppCompatActivity implements View.OnClickListener {

	private ModifHBinding binding;
	private DbHelper db;
	private SQLiteDatabase SQLdb;

	private Calendar currentDisplayCalendar;
	private int h1224;
	private int[] prayerTimes = new int[6]; // 0:fajr, 1:shorouq, 2:dhuhr, 3:asr, 4:maghrib, 5:isha

	// CORRECTION : Le tableau prayerTimeDbColumns a été supprimé.
	// Nous utiliserons directement les noms de colonnes.
	private final String[] prayerNames = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ModifHBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		try {
			db = new DbHelper(this);
			db.createdatabase();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, "Erreur BDD", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		currentDisplayCalendar = Calendar.getInstance();
		loadPreferences();
		setupClickListeners();
		initializeUI();

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(() -> {
					if (binding != null) {
						binding.tDjc.setText(new SimpleDateFormat(h1224 == 0 ? "HH:mm:ss" : "hh:mm:ss", Locale.US).format(new Date()));
					}
				});
			}
		}, 0, 1000);
	}

	// ... (Le reste du fichier reste identique à la version précédente)
	// J'inclus la méthode modifyPrayerTime pour montrer qu'elle n'utilise plus le tableau

	private void modifyPrayerTime(int prayerIndex, int minuteChange) {
		prayerTimes[prayerIndex] = (prayerTimes[prayerIndex] + minuteChange + 1440) % 1440;
		updateAllPrayerTimeViews();

		String nvd = getCurrentNvd();
		if (nvd != null) {
			// CORRECTION : Nous construisons le nom de la colonne directement ici.
			String[] columnNames = {"t_fa", "t_ch", "t_do", "t_as", "t_ma", "t_is"};
			String columnToUpdate = columnNames[prayerIndex];

			SQLdb = openOrCreateDatabase(DbHelper.DB_NAME, Context.MODE_PRIVATE, null);
			SQLdb.execSQL("UPDATE tb_horaires SET " + columnToUpdate + " = ? WHERE n_vd = ?", new Object[]{prayerTimes[prayerIndex], nvd});

			if (prayerIndex == 0) { // Fajr
				String previousNvd = getPreviousNvd(nvd);
				if (previousNvd != null) {
					SQLdb.execSQL("UPDATE tb_horaires SET t_nf = ? WHERE n_vd = ?", new Object[]{prayerTimes[0], previousNvd});
				}
			}
			SQLdb.close();
		}
	}

	// ... (Le reste du fichier Java reste exactement le même que dans ma réponse précédente)

	// -- COPIEZ LE RESTE DES MÉTHODES DE MA RÉPONSE PRÉCÉDENTE ICI --
	// (loadPreferences, setupClickListeners, initializeUI, etc.)

	private void loadPreferences() {
		try (Cursor curP = db.prrawQuery("SELECT * FROM tb_pref", null)) {
			if (curP != null && curP.moveToFirst()) {
				h1224 = curP.getInt(curP.getColumnIndexOrThrow("h2412"));
			}
		}
	}

	private void setupClickListeners() {
		binding.next.setOnClickListener(this);
		binding.back.setOnClickListener(this);
		binding.retour.setOnClickListener(this);
		binding.v1.setOnClickListener(this);
		binding.v2.setOnClickListener(this);
		binding.v3.setOnClickListener(this);
	}

	private void initializeUI() {
		setupPrayerRow(binding.rowFajr, 0);
		setupPrayerRow(binding.rowShorouq, 1);
		setupPrayerRow(binding.rowDhuhr, 2);
		setupPrayerRow(binding.rowAsr, 3);
		setupPrayerRow(binding.rowMaghrib, 4);
		setupPrayerRow(binding.rowIsha, 5);
		loadCityNames();
		updateUIForDay(currentDisplayCalendar);
	}

	private void setupPrayerRow(RowModifHBinding rowBinding, final int prayerIndex) {
		rowBinding.prayerName.setText(prayerNames[prayerIndex]);
		rowBinding.btnPlus.setOnClickListener(v -> modifyPrayerTime(prayerIndex, 1));
		rowBinding.btnMinus.setOnClickListener(v -> modifyPrayerTime(prayerIndex, -1));
	}

	private void updateUIForDay(Calendar calendar) {
		updateDateDisplay(calendar);
		updatePrayerTimesForDay(calendar);
		updateDhuhrButtonsState();
	}

	private void updateDateDisplay(Calendar calendar) {
		Date date = calendar.getTime();
		long daysSince1970 = date.getTime() / 86400000;
		int excelDateNumber = (int) (daysSince1970 + 25569);
		try (Cursor curH = db.hirawQuery("SELECT * FROM tb_hijc WHERE d_gr1 <= " + excelDateNumber + " ORDER BY d_gr1 DESC LIMIT 1", null)) {
			if (curH != null && curH.moveToFirst()) {
				int dayInHijriMonth = excelDateNumber - curH.getInt(curH.getColumnIndexOrThrow("d_gr1")) + 1;
				String hijriMonthAndYear = curH.getString(curH.getColumnIndexOrThrow("d_hi1"));
				binding.dHij.setText(String.format(Locale.getDefault(), "%d %s", dayInHijriMonth, hijriMonthAndYear));
			}
		}
		String dayOfWeekName = getDayOfWeekName(calendar.get(Calendar.DAY_OF_WEEK));
		String gregorianDate = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date);
		binding.dGre.setText(String.format("%s %s", dayOfWeekName, gregorianDate));
	}

	private String getDayOfWeekName(int dayOfWeek) {
		switch (dayOfWeek) {
			case Calendar.SUNDAY: return "الأحد";
			case Calendar.MONDAY: return "الاثنين";
			case Calendar.TUESDAY: return "الثلاثاء";
			case Calendar.WEDNESDAY: return "الأربعاء";
			case Calendar.THURSDAY: return "الخميس";
			case Calendar.FRIDAY: return "الجمعة";
			case Calendar.SATURDAY: return "السبت";
			default: return "";
		}
	}

	private void loadCityNames() {
		try (Cursor curV = db.virawQuery("SELECT * FROM qr_villes ORDER BY fv_v", null)) {
			if (curV != null && curV.moveToFirst()) {
				binding.v1.setText(curV.getString(curV.getColumnIndexOrThrow("tx_v")));
				if (curV.getInt(curV.getColumnIndexOrThrow("ac_v")) == 1) setActiveCity(binding.v1);
				if (curV.moveToNext()) {
					binding.v2.setText(curV.getString(curV.getColumnIndexOrThrow("tx_v")));
					if (curV.getInt(curV.getColumnIndexOrThrow("ac_v")) == 1) setActiveCity(binding.v2);
				}
				if (curV.moveToNext()) {
					binding.v3.setText(curV.getString(curV.getColumnIndexOrThrow("tx_v")));
					if (curV.getInt(curV.getColumnIndexOrThrow("ac_v")) == 1) setActiveCity(binding.v3);
				}
			}
		}
	}

	private void setActiveCity(View cityView) {
		binding.v1.setBackgroundColor(Color.TRANSPARENT);
		binding.v2.setBackgroundColor(Color.TRANSPARENT);
		binding.v3.setBackgroundColor(Color.TRANSPARENT);
		cityView.setBackgroundColor(Color.parseColor("#808080"));
	}

	private void updatePrayerTimesForDay(Calendar calendar) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("/MM/dd", Locale.US);
		String dateForQuery = dateFormat.format(calendar.getTime());
		String activeCityId = getActiveCityId();
		if (activeCityId == null) return;
		String query = "SELECT * FROM tb_horaires WHERE d_gr2 = '" + dateForQuery + "' AND n_vi = " + activeCityId;
		try (Cursor prayerCursor = db.getReadableDatabase().rawQuery(query, null)) {
			if (prayerCursor != null && prayerCursor.moveToFirst()) {
				prayerTimes[0] = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_fa"));
				prayerTimes[1] = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_ch"));
				prayerTimes[2] = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_do"));
				prayerTimes[3] = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_as"));
				prayerTimes[4] = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_ma"));
				prayerTimes[5] = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_is"));
				updateAllPrayerTimeViews();
			}
		}
	}

	private void updateAllPrayerTimeViews() {
		binding.rowFajr.prayerTime.setText(formatTime(prayerTimes[0]));
		binding.rowShorouq.prayerTime.setText(formatTime(prayerTimes[1]));
		binding.rowDhuhr.prayerTime.setText(formatTime(prayerTimes[2]));
		binding.rowAsr.prayerTime.setText(formatTime(prayerTimes[3]));
		binding.rowMaghrib.prayerTime.setText(formatTime(prayerTimes[4]));
		binding.rowIsha.prayerTime.setText(formatTime(prayerTimes[5]));
	}

	private String formatTime(int timeInMinutes) {
		int hours = timeInMinutes / 60;
		int minutes = timeInMinutes % 60;
		if (h1224 == 12) {
			if (hours >= 12) {
				if (hours > 12) hours -= 12;
			} else if (hours == 0) {
				hours = 12;
			}
		}
		return String.format(Locale.US, "%02d:%02d", hours, minutes);
	}

	private String getActiveCityId() {
		try (Cursor cursor = db.virawQuery("SELECT id_v FROM qr_villes WHERE ac_v = 1", null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return cursor.getString(0);
			}
		}
		return null;
	}

	private String getCurrentNvd() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("/MM/dd", Locale.US);
		String dateForQuery = dateFormat.format(currentDisplayCalendar.getTime());
		String activeCityId = getActiveCityId();
		if (activeCityId == null) return null;
		String query = "SELECT n_vd FROM tb_horaires WHERE d_gr2 = '" + dateForQuery + "' AND n_vi = " + activeCityId;
		try (Cursor cursor = db.getReadableDatabase().rawQuery(query, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				return cursor.getString(0);
			}
		}
		return null;
	}

	private String getPreviousNvd(String currentNvd) {
		try {
			int nvdInt = Integer.parseInt(currentNvd);
			int cityBaseId = (nvdInt / 1000) * 1000;
			int dayOfYear = nvdInt % 1000;
			if (dayOfYear == 1) {
				return String.valueOf(cityBaseId + 366);
			} else {
				return String.valueOf(nvdInt - 1);
			}
		} catch (NumberFormatException e) {
			Log.e("ModifH", "Impossible de parser nvd: " + currentNvd, e);
			return null;
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.next) {
			currentDisplayCalendar.add(Calendar.DAY_OF_YEAR, 1);
			updateUIForDay(currentDisplayCalendar);
			binding.retour.setVisibility(View.VISIBLE);
		} else if (id == R.id.back) {
			currentDisplayCalendar.add(Calendar.DAY_OF_YEAR, -1);
			updateUIForDay(currentDisplayCalendar);
			binding.retour.setVisibility(View.VISIBLE);
		} else if (id == R.id.retour) {
			currentDisplayCalendar = Calendar.getInstance();
			updateUIForDay(currentDisplayCalendar);
			binding.retour.setVisibility(View.INVISIBLE);
		} else if (id == R.id.v_1 || id == R.id.v_2 || id == R.id.v_3) {
			handleCitySelection(v);
		}
	}

	private void handleCitySelection(View cityView) {
		String cityName = ((TextView) cityView).getText().toString();
		SQLdb = openOrCreateDatabase(DbHelper.DB_NAME, Context.MODE_PRIVATE, null);
		SQLdb.execSQL("UPDATE tb_villes SET ac_v = 0");
		SQLdb.execSQL("UPDATE tb_villes SET ac_v = 1 WHERE tx_v = ?", new String[]{cityName});
		SQLdb.close();
		setActiveCity(cityView);
		updatePrayerTimesForDay(currentDisplayCalendar);
	}

	private void updateDhuhrButtonsState() {
		boolean isFixed = false;
		try (Cursor curV = db.virawQuery("SELECT * FROM qr_villes WHERE ac_v = 1", null)) {
			if (curV != null && curV.moveToFirst()) {
				boolean isJumuaah = currentDisplayCalendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
				boolean isJumuaahFixed = curV.getInt(curV.getColumnIndexOrThrow("djf_ac")) == 1;
				boolean isDohrFixed = curV.getInt(curV.getColumnIndexOrThrow("dof_ac")) == 1;
				if (isJumuaah && isJumuaahFixed) {
					isFixed = true;
				} else if (!isJumuaah && isDohrFixed) {
					isFixed = true;
				}
			}
		}
		binding.rowDhuhr.btnPlus.setEnabled(!isFixed);
		binding.rowDhuhr.btnMinus.setEnabled(!isFixed);
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