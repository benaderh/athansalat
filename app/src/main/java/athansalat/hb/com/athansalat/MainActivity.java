package athansalat.hb.com.athansalat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import athansalat.hb.com.athansalat.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;
    private DbHelper db;
    private SQLiteDatabase SQLdb;
    private Timer timer;
    private MyTimerTask myTimerTask;
    private Calendar currentDisplayCalendar;
    private int ifa, ido, ias, ima, iis, iNextF;
    private int h1224;
    private String HHhh;
    private final static int RQS_1 = 1;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String[]> openFileLauncherForHoraires;
    private ActivityResultLauncher<String[]> openFileLauncherForHijri;

    private ExcelToSQLiteHoraires excelHorairesImporter;
    private ExcelToSQLiteDateHijir excelDateHijirImporter;
    private GSToSQLiteHoraires gsHorairesImporter;
    private GSToSQLiteDateHijir gsDateHijirImporter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeImporters();
        initializeLaunchers();

        try {
            db = new DbHelper(this);
            db.createdatabase();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur critique: Impossible de charger la base de données.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentDisplayCalendar = Calendar.getInstance();
        setupClickListeners();
        loadPreferences();
        initializeUI();
        checkNotificationPermission();

        // Le timer est maintenant initialisé après que les temps de prière soient chargés
    }

    // ... (TOUT LE RESTE DU CODE EST IDENTIQUE JUSQU'À onDestroy) ...

    private void initializeImporters() {
        excelHorairesImporter = new ExcelToSQLiteHoraires(this);
        excelDateHijirImporter = new ExcelToSQLiteDateHijir(this);
        gsHorairesImporter = new GSToSQLiteHoraires(this);
        gsDateHijirImporter = new GSToSQLiteDateHijir(this);
    }

    private void initializeLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                checkExactAlarmPermissionAndSchedule();
            } else {
                Toast.makeText(this, "La permission pour les notifications est requise pour les alertes de prière.", Toast.LENGTH_LONG).show();
            }
        });
        openFileLauncherForHoraires = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                excelHorairesImporter.updateDatabaseFromExcelUri(uri);
            } else {
                Toast.makeText(this, "Aucun fichier sélectionné.", Toast.LENGTH_SHORT).show();
            }
        });
        openFileLauncherForHijri = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                excelDateHijirImporter.updateDatabaseFromExcelUri(uri);
            } else {
                Toast.makeText(this, "Aucun fichier sélectionné.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.next.setOnClickListener(this);
        binding.back.setOnClickListener(this);
        binding.retour.setOnClickListener(this);
        binding.v1.setOnClickListener(this);
        binding.v2.setOnClickListener(this);
        binding.v3.setOnClickListener(this);
    }

    private void loadPreferences() {
        try (Cursor curP = db.prrawQuery("SELECT * FROM tb_pref", null)) {
            if (curP != null && curP.moveToFirst()) {
                h1224 = curP.getInt(curP.getColumnIndexOrThrow("h2412"));
                int pro = curP.getInt(curP.getColumnIndexOrThrow("sizei"));
                applyTextSize(pro);
            }
        }
        HHhh = (h1224 == 0) ? "HH:mm:ss" : "hh:mm:ss";
    }

    private void applyTextSize(int pro) {
        float baseSize = Math.max(pro, 10) / 60.0f;
        binding.v1.setTextSize(baseSize * 15);
        binding.v2.setTextSize(baseSize * 15);
        binding.v3.setTextSize(baseSize * 15);
        binding.dGre.setTextSize(baseSize * 30);
        binding.dHij.setTextSize(baseSize * 30);
        binding.tDjc.setTextSize(baseSize * 60);
        binding.tFaj.setTextSize(baseSize * 50);
        binding.tDoh.setTextSize(baseSize * 50);
        binding.tAsr.setTextSize(baseSize * 50);
        binding.tMag.setTextSize(baseSize * 50);
        binding.tIsh.setTextSize(baseSize * 50);
        binding.bf.setTextSize(baseSize * 35);
        binding.bd.setTextSize(baseSize * 40);
        binding.ba.setTextSize(baseSize * 40);
        binding.bm.setTextSize(baseSize * 40);
        binding.bi.setTextSize(baseSize * 40);
        binding.af.setTextSize(baseSize * 40);
        binding.ad.setTextSize(baseSize * 40);
        binding.aa.setTextSize(baseSize * 40);
        binding.am.setTextSize(baseSize * 40);
        binding.ai.setTextSize(baseSize * 40);
    }

    private void initializeUI() {
        loadCityNames();
        updateUIForDay(currentDisplayCalendar);
    }

    private void updateUIForDay(Calendar calendar) {
        updateDateDisplay(calendar);
        updatePrayerTimesForDay(calendar);
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
        String cityName = ((TextView) cityView).getText().toString();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("مواقيت الصلاة / " + cityName);
        }
    }

    private void updatePrayerTimesForDay(Calendar calendar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("/MM/dd", Locale.US);
        String dateForQuery = dateFormat.format(calendar.getTime());
        try (Cursor prayerCursor = db.horawQuery("SELECT * FROM qr_horaires WHERE d_gr2 = '" + dateForQuery + "'", null)) {
            if (prayerCursor != null && prayerCursor.moveToFirst()) {
                ifa = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_fa"));
                ido = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_do"));
                ias = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_as"));
                ima = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_ma"));
                iis = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_is"));
                iNextF = prayerCursor.getInt(prayerCursor.getColumnIndexOrThrow("t_nf"));
                updatePrayerTimesUI();

                // Démarrer ou redémarrer le timer maintenant que nous avons les horaires
                startTimer();
            } else {
                Toast.makeText(this, "Aucune donnée de prière pour cette date.", Toast.LENGTH_SHORT).show();
                binding.tFaj.setText("--:--");
                binding.tDoh.setText("--:--");
                binding.tAsr.setText("--:--");
                binding.tMag.setText("--:--");
                binding.tIsh.setText("--:--");
            }
        }
    }

    private void updatePrayerTimesUI() {
        binding.tFaj.setText(formatTime(ifa));
        binding.tDoh.setText(formatTime(ido));
        binding.tAsr.setText(formatTime(ias));
        binding.tMag.setText(formatTime(ima));
        binding.tIsh.setText(formatTime(iis));
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
        } else if (id == R.id.v1 || id == R.id.v2 || id == R.id.v3) {
            handleCitySelection(v);
        }
    }

    private void handleCitySelection(View cityView) {
        String cityName = ((TextView) cityView).getText().toString();
        SQLdb = openOrCreateDatabase("db_horaires.sqlite", Context.MODE_PRIVATE, null);
        SQLdb.execSQL("UPDATE tb_villes SET ac_v = 0");
        SQLdb.execSQL("UPDATE tb_villes SET ac_v = 1 WHERE tx_v = ?", new String[]{cityName});
        setActiveCity(cityView);
        updatePrayerTimesForDay(currentDisplayCalendar);
        scheduleNextAlarm();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                checkExactAlarmPermissionAndSchedule();
            }
        } else {
            checkExactAlarmPermissionAndSchedule();
        }
    }

    private void checkExactAlarmPermissionAndSchedule() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission requise")
                        .setMessage("Pour garantir les alertes de prière, veuillez autoriser l'application à programmer des alarmes précises.")
                        .setPositiveButton("Paramètres", (dialog, which) -> startActivity(
                                new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:" + getPackageName()))))
                        .setNegativeButton("Annuler", null)
                        .show();
                return;
            }
        }
        // Vérifier l'optimisation batterie (Doze mode)
        checkBatteryOptimizationAndSchedule();
    }

    /**
     * Demande à l'utilisateur d'exempter l'app de l'optimisation batterie
     * pour que les alarmes fonctionnent quand l'écran est éteint (mode Doze).
     */
    private void checkBatteryOptimizationAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("تحسين البطارية")
                        .setMessage("لضمان التنبيهات عند إطفاء الشاشة، يُرجى إعفاء التطبيق من تحسين البطارية.")
                        .setPositiveButton("الإعدادات", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("لاحقاً", (dialog, which) -> scheduleNextAlarm())
                        .show();
                return;
            }
        }
        scheduleNextAlarm();
    }

    private void scheduleNextAlarm() {
        SQLdb = openOrCreateDatabase("db_horaires.sqlite", Context.MODE_PRIVATE, null);
        SQLdb.execSQL("UPDATE tb_alarms SET time0 =?, time = ? + diff where _id < 5", new Object[]{ifa, ifa});
        SQLdb.execSQL("UPDATE tb_alarms SET time0 =?, time = ? + diff where _id > 4 and _id < 9", new Object[]{ido, ido});
        SQLdb.execSQL("UPDATE tb_alarms SET time0 =?, time = ? + diff where _id > 8 and _id < 13", new Object[]{ias, ias});
        SQLdb.execSQL("UPDATE tb_alarms SET time0 =?, time = ? + diff where _id > 12 and _id < 17", new Object[]{ima, ima});
        SQLdb.execSQL("UPDATE tb_alarms SET time0 =?, time = ? + diff where _id > 16 and _id < 21", new Object[]{iis, iis});

        try (Cursor curA = db.qralrawQuery("SELECT * FROM qr_alarms", null)) {
            if (curA == null || !curA.moveToFirst()) {
                Log.e("MainActivity", "Impossible de lire les données d'alarme.");
                return;
            }
            Calendar now = Calendar.getInstance();
            int nowInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int nextAlarmTimeInMinutes = -1;
            String nextToneName  = "normal";
            int    nextVolume     = 7;
            String nextPrayerName = "وقت الصلاة";

            do {
                int alarmTime = curA.getInt(1); // colonne 'time'
                int alarmType = curA.getInt(curA.getColumnIndexOrThrow("type"));
                if (alarmType == 1 && alarmTime > nowInMinutes) {
                    nextAlarmTimeInMinutes = alarmTime;
                    try {
                        nextToneName  = curA.getString(curA.getColumnIndexOrThrow("tone_name"));
                        nextVolume    = curA.getInt(curA.getColumnIndexOrThrow("volume"));
                        nextPrayerName = getPrayerNameForTime(nextAlarmTimeInMinutes);
                    } catch (Exception ignored) {}
                    break;
                }
            } while (curA.moveToNext());

            // Si aucune alarme aujourd'hui → prendre la 1ère alarme active de demain
            if (nextAlarmTimeInMinutes == -1) {
                curA.moveToFirst();
                do {
                    int alarmType = curA.getInt(curA.getColumnIndexOrThrow("type"));
                    if (alarmType == 1) {
                        nextAlarmTimeInMinutes = curA.getInt(1);
                        try {
                            nextToneName  = curA.getString(curA.getColumnIndexOrThrow("tone_name"));
                            nextVolume    = curA.getInt(curA.getColumnIndexOrThrow("volume"));
                            nextPrayerName = getPrayerNameForTime(nextAlarmTimeInMinutes);
                        } catch (Exception ignored) {}
                        break;
                    }
                } while (curA.moveToNext());
            }

            if (nextAlarmTimeInMinutes == -1) {
                Log.w("MainActivity", "Aucune alarme active configurée.");
                return;
            }

            Calendar alarmCalendar = Calendar.getInstance();
            if (nextAlarmTimeInMinutes <= nowInMinutes) {
                alarmCalendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            alarmCalendar.set(Calendar.HOUR_OF_DAY, nextAlarmTimeInMinutes / 60);
            alarmCalendar.set(Calendar.MINUTE, nextAlarmTimeInMinutes % 60);
            alarmCalendar.set(Calendar.SECOND, 0);
            alarmCalendar.set(Calendar.MILLISECOND, 0);

            // Inclure les extras dans l'intent pour que AlarmReceiver sache quoi jouer
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("PRAYER_NAME", nextPrayerName);
            intent.putExtra("TONE_NAME",   nextToneName);
            intent.putExtra("VOLUME",      nextVolume);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, RQS_1, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                AlarmManager.AlarmClockInfo alarmClockInfo =
                        new AlarmManager.AlarmClockInfo(alarmCalendar.getTimeInMillis(), pendingIntent);
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
                Toast.makeText(this, "Prochaine alerte: " + formatTime(nextAlarmTimeInMinutes), Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Alarme programmée pour : " + alarmCalendar.getTime());
            }
        }
    }

    /** Retourne le nom de la prière selon l'heure en minutes de la journée. */
    private String getPrayerNameForTime(int timeInMinutes) {
        if (timeInMinutes <= ifa || timeInMinutes < ifa + 60) return "الفجر";
        if (timeInMinutes <= ido + 60)  return "الظهر";
        if (timeInMinutes <= ias + 60)  return "العصر";
        if (timeInMinutes <= ima + 60)  return "المغرب";
        return "العشاء";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (db != null) {
            db.close();
        }
        // Pour éviter les fuites de mémoire, on met le binding à null
        binding = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_favourite) startActivity(new Intent(this, FavouritesV.class));
        else if (itemId == R.id.menu_autres) startActivity(new Intent(this, Autres.class));
        else if (itemId == R.id.menu_modif_d) startActivity(new Intent(this, ModifD.class));
        else if (itemId == R.id.menu_modif_h) startActivity(new Intent(this, ModifH.class));
        else if (itemId == R.id.menu_modif_v) startActivity(new Intent(this, ModifV.class));
        else if (itemId == R.id.menu_alarms) startActivity(new Intent(this, ModifA.class));
        else if (itemId == R.id.menu_excel1) openFileLauncherForHoraires.launch(new String[]{"application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
        else if (itemId == R.id.menu_excel2) openFileLauncherForHijri.launch(new String[]{"application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
        else if (itemId == R.id.menu_gs1) gsHorairesImporter.updateDatabaseFromSheet();
        else if (itemId == R.id.menu_gs2) gsDateHijirImporter.updateDatabaseFromSheet();
        else if (itemId == R.id.menu_about) startActivity(new Intent(this, AboutApp.class));
        else return super.onOptionsItemSelected(item);
        return true;
    }

    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        myTimerTask = new MyTimerTask(this);
        timer.scheduleAtFixedRate(myTimerTask, 0, 1000);
    }

    // --- CLASSE TIMER TASK CORRIGÉE ---

    private static class MyTimerTask extends TimerTask {
        // Utiliser une WeakReference pour éviter les fuites de mémoire
        private final WeakReference<MainActivity> activityReference;

        MyTimerTask(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            // Récupérer l'activité. Si elle est détruite, on ne fait rien.
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            Calendar calendar = Calendar.getInstance();
            final String currentTimeStr = new SimpleDateFormat(activity.HHhh, Locale.US).format(calendar.getTime());
            final String[] timers = activity.calculateTimers();

            activity.runOnUiThread(() -> {
                // Re-vérifier au cas où l'activité serait détruite pendant le post
                MainActivity currentActivity = activityReference.get();
                if (currentActivity != null && !currentActivity.isFinishing()) {
                    currentActivity.binding.tDjc.setText(currentTimeStr);
                    currentActivity.updateTimerViews(timers);
                }
            });
        }
    }

    private String[] calculateTimers() {
        Calendar now = Calendar.getInstance();
        int iCurrT = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        String[] timers = new String[10];
        if (iCurrT < ifa) {
            timers[0] = formatRemainingTime(ifa - iCurrT);
            timers[9] = formatElapsedTime(1440 - iis + iCurrT);
        } else if (iCurrT < ido) {
            timers[1] = formatElapsedTime(iCurrT - ifa);
            timers[2] = formatRemainingTime(ido - iCurrT);
        } else if (iCurrT < ias) {
            timers[3] = formatElapsedTime(iCurrT - ido);
            timers[4] = formatRemainingTime(ias - iCurrT);
        } else if (iCurrT < ima) {
            timers[5] = formatElapsedTime(iCurrT - ias);
            timers[6] = formatRemainingTime(ima - iCurrT);
        } else if (iCurrT < iis) {
            timers[7] = formatElapsedTime(iCurrT - ima);
            timers[8] = formatRemainingTime(iis - iCurrT);
        } else {
            timers[9] = formatElapsedTime(iCurrT - iis);
            timers[0] = formatRemainingTime(1440 - iCurrT + iNextF);
        }
        return timers;
    }

    private String formatRemainingTime(int minutes) {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    private String formatElapsedTime(int minutes) {
        return String.format(Locale.US, "-%02d:%02d", minutes / 60, minutes % 60);
    }

    private void updateTimerViews(String[] timers) {
        if (binding == null) return; // Sécurité supplémentaire
        binding.bf.setText(timers[0]); binding.bf.setVisibility(timers[0] != null ? View.VISIBLE : View.INVISIBLE);
        binding.af.setText(timers[1]); binding.af.setVisibility(timers[1] != null ? View.VISIBLE : View.INVISIBLE);
        binding.bd.setText(timers[2]); binding.bd.setVisibility(timers[2] != null ? View.VISIBLE : View.INVISIBLE);
        binding.ad.setText(timers[3]); binding.ad.setVisibility(timers[3] != null ? View.VISIBLE : View.INVISIBLE);
        binding.ba.setText(timers[4]); binding.ba.setVisibility(timers[4] != null ? View.VISIBLE : View.INVISIBLE);
        binding.aa.setText(timers[5]); binding.aa.setVisibility(timers[5] != null ? View.VISIBLE : View.INVISIBLE);
        binding.bm.setText(timers[6]); binding.bm.setVisibility(timers[6] != null ? View.VISIBLE : View.INVISIBLE);
        binding.am.setText(timers[7]); binding.am.setVisibility(timers[7] != null ? View.VISIBLE : View.INVISIBLE);
        binding.bi.setText(timers[8]); binding.bi.setVisibility(timers[8] != null ? View.VISIBLE : View.INVISIBLE);
        binding.ai.setText(timers[9]); binding.ai.setVisibility(timers[9] != null ? View.VISIBLE : View.INVISIBLE);

        int currentMinuteOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE);
        binding.tFaj.setTextColor(currentMinuteOfDay >= ifa && currentMinuteOfDay < ido ? Color.WHITE : Color.parseColor("#ffffff99"));
        binding.tDoh.setTextColor(currentMinuteOfDay >= ido && currentMinuteOfDay < ias ? Color.WHITE : Color.parseColor("#ffffff99"));
        binding.tAsr.setTextColor(currentMinuteOfDay >= ias && currentMinuteOfDay < ima ? Color.WHITE : Color.parseColor("#ffffff99"));
        binding.tMag.setTextColor(currentMinuteOfDay >= ima && currentMinuteOfDay < iis ? Color.WHITE : Color.parseColor("#ffffff99"));
        binding.tIsh.setTextColor(currentMinuteOfDay >= iis ? Color.WHITE : Color.parseColor("#ffffff99"));
        if (currentMinuteOfDay < ifa) {
            binding.tFaj.setTextColor(Color.WHITE);
        }
    }
}