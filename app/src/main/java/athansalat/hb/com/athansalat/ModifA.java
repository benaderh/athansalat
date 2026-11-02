package athansalat.hb.com.athansalat;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import athansalat.hb.com.athansalat.databinding.ModifABinding;
import athansalat.hb.com.athansalat.databinding.SectionAlarmBinding;

public class ModifA extends AppCompatActivity {

    private ModifABinding binding;
    private DbHelper db;

    private final String[] spinnerItems = {
            "adhan_abu_bakr", "adhan_fajr_afasy", "adhan_makkah", "iqama", "wakeup",
            "takbir", "dua_adhan", "dua_sabah", "dua_masaa", "bird", "alarm_clock"
    };

    private final String[] spinnerDisplayItems = {
            "أذان أبو بكر", "أذان فجر لعفاسي", "أذان مكة", "إقامة", "إيقاظ",
            "تكبير", "دعاء الأذان", "دعاء الصباح", "دعاء المساء", "عصفور", "منبه"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ModifABinding.inflate(getLayoutInflater());
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

        setupPrayerSection(binding.sectionFajr, 1);
        setupPrayerSection(binding.sectionDhuhr, 5);
        setupPrayerSection(binding.sectionAsr, 9);
        setupPrayerSection(binding.sectionMaghrib, 13);
        setupPrayerSection(binding.sectionIsha, 17);
    }

    private void setupPrayerSection(SectionAlarmBinding sectionBinding, int preAdhanAlarmId) {
        // Le nom de la prière est maintenant déterminé par son ID de base
        switch (preAdhanAlarmId) {
            case 1: sectionBinding.tvPrayerTitle.setText("الفجر"); break;
            case 5: sectionBinding.tvPrayerTitle.setText("الظهر"); break;
            case 9: sectionBinding.tvPrayerTitle.setText("العصر"); break;
            case 13: sectionBinding.tvPrayerTitle.setText("المغرب"); break;
            case 17: sectionBinding.tvPrayerTitle.setText("العشاء"); break;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerDisplayItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        sectionBinding.spinnerPreAdhan.setAdapter(adapter);
        sectionBinding.spinnerAdhan.setAdapter(adapter);

        loadAlarmData(sectionBinding, preAdhanAlarmId);
        setupSectionListeners(sectionBinding, preAdhanAlarmId);
    }

    private void loadAlarmData(SectionAlarmBinding sectionBinding, int preAdhanAlarmId) {
        try (Cursor cursor = db.alrawQuery("SELECT * FROM tb_alarms WHERE _id >= " + preAdhanAlarmId + " LIMIT 4", null)) {
            // Alarme Pre-Adhan
            if (cursor != null && cursor.moveToPosition(0)) {
                sectionBinding.cbPreAdhan.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow("type")) == 1);
                int preAdhanMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("diff"));
                sectionBinding.tvPreAdhanValue.setText(String.valueOf(Math.abs(preAdhanMinutes)));
                int preAdhanVolume = cursor.getInt(cursor.getColumnIndexOrThrow("volume"));
                sectionBinding.seekbarPreAdhanVolume.setProgress(preAdhanVolume);
                sectionBinding.tvPreAdhanVolume.setText(String.valueOf(preAdhanVolume));
                sectionBinding.spinnerPreAdhan.setSelection(cursor.getInt(cursor.getColumnIndexOrThrow("tone_int")));
            }

            // Alarme Adhan
            if (cursor != null && cursor.moveToPosition(1)) {
                sectionBinding.cbAdhan.setChecked(cursor.getInt(cursor.getColumnIndexOrThrow("type")) == 1);
                int adhanVolume = cursor.getInt(cursor.getColumnIndexOrThrow("volume"));
                sectionBinding.seekbarAdhanVolume.setProgress(adhanVolume);
                sectionBinding.tvAdhanVolume.setText(String.valueOf(adhanVolume));
                sectionBinding.spinnerAdhan.setSelection(cursor.getInt(cursor.getColumnIndexOrThrow("tone_int")));
            }
        } catch (Exception e) {
            Log.e("ModifA", "Erreur lors du chargement des données d'alarme", e);
        }
    }

    private void setupSectionListeners(SectionAlarmBinding sectionBinding, final int preAdhanAlarmId) {
        final int adhanAlarmId = preAdhanAlarmId + 1;

        // --- Alarme "avant Adhan" ---
        sectionBinding.btnPreAdhanPlus.setOnClickListener(v -> updateAlarmDiffValue(sectionBinding.tvPreAdhanValue, preAdhanAlarmId, 1));
        sectionBinding.btnPreAdhanMinus.setOnClickListener(v -> updateAlarmDiffValue(sectionBinding.tvPreAdhanValue, preAdhanAlarmId, -1));
        sectionBinding.cbPreAdhan.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmType(preAdhanAlarmId, isChecked));

        setupVolumeControls(sectionBinding.seekbarPreAdhanVolume, sectionBinding.tvPreAdhanVolume, preAdhanAlarmId);
        setupSpinnerControls(sectionBinding.spinnerPreAdhan, preAdhanAlarmId);

        // --- Alarme Adhan ---
        sectionBinding.cbAdhan.setOnCheckedChangeListener((buttonView, isChecked) -> updateAlarmType(adhanAlarmId, isChecked));
        setupVolumeControls(sectionBinding.seekbarAdhanVolume, sectionBinding.tvAdhanVolume, adhanAlarmId);
        setupSpinnerControls(sectionBinding.spinnerAdhan, adhanAlarmId);
    }

    private void setupVolumeControls(SeekBar seekBar, TextView textView, int alarmId) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    textView.setText(String.valueOf(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateAlarmVolume(alarmId, seekBar.getProgress());
            }
        });
    }

    private void setupSpinnerControls(android.widget.Spinner spinner, int alarmId) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAlarmTone(alarmId, position, spinnerItems[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateAlarmDiffValue(TextView textView, int alarmId, int direction) {
        try {
            int currentValue = Integer.parseInt(textView.getText().toString());
            currentValue += direction;
            if (currentValue < 0) currentValue = 0;

            textView.setText(String.valueOf(currentValue));
            int dbValue = -currentValue;

            SQLiteDatabase db = openOrCreateDatabase(DbHelper.DB_NAME, Context.MODE_PRIVATE, null);
            db.execSQL("UPDATE tb_alarms SET diff = ? WHERE _id = ?", new Object[]{dbValue, alarmId});
            db.close();
        } catch (NumberFormatException e) {
            Log.e("ModifA", "Erreur de format de nombre", e);
        }
    }

    private void updateAlarmType(int alarmId, boolean isChecked) {
        SQLiteDatabase db = openOrCreateDatabase(DbHelper.DB_NAME, Context.MODE_PRIVATE, null);
        db.execSQL("UPDATE tb_alarms SET type = ? WHERE _id = ?", new Object[]{isChecked ? 1 : 0, alarmId});
        db.close();
    }

    private void updateAlarmVolume(int alarmId, int volume) {
        SQLiteDatabase db = openOrCreateDatabase(DbHelper.DB_NAME, Context.MODE_PRIVATE, null);
        db.execSQL("UPDATE tb_alarms SET volume = ? WHERE _id = ?", new Object[]{volume, alarmId});
        db.close();
    }

    private void updateAlarmTone(int alarmId, int toneIndex, String toneName) {
        SQLiteDatabase db = openOrCreateDatabase(DbHelper.DB_NAME, Context.MODE_PRIVATE, null);
        db.execSQL("UPDATE tb_alarms SET tone_int = ?, tone_name = ? WHERE _id = ?", new Object[]{toneIndex, toneName, alarmId});
        db.close();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}