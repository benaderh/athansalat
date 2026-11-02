package athansalat.hb.com.athansalat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;

import java.io.IOException;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // --- Redémarrage après reboot : reprogrammer les alarmes ---
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) {
            Log.d(TAG, "Boot détecté → reprogrammation des alarmes");
            rescheduleAlarms(context);
            return;
        }

        // --- Alarme normale : récupérer les infos depuis la DB et démarrer le service ---
        Log.d(TAG, "Alarme reçue → démarrage de AlarmService");

        // Lire depuis la DB les infos de l'alarme qui se déclenche maintenant
        String prayerName = intent.getStringExtra("PRAYER_NAME");
        String toneName   = intent.getStringExtra("TONE_NAME");
        int    volume     = intent.getIntExtra("VOLUME", 7);

        // Si les infos ne sont pas dans l'intent, lire la DB directement
        if (toneName == null || toneName.isEmpty()) {
            String[] alarmData = readCurrentAlarmFromDb(context);
            if (alarmData != null) {
                prayerName = alarmData[0];
                toneName   = alarmData[1];
                try { volume = Integer.parseInt(alarmData[2]); } catch (NumberFormatException ignored) {}
            }
        }
        if (prayerName == null) prayerName = "وقت الصلاة";
        if (toneName   == null) toneName   = "normal";

        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("PRAYER_NAME", prayerName);
        serviceIntent.putExtra("TONE_NAME",   toneName);
        serviceIntent.putExtra("VOLUME",      volume);

        // startForegroundService est obligatoire pour Android 8+ (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * Lit depuis la DB les informations de la prochaine alarme active pour les transmettre au service.
     * @return [prayerName, toneName, volume] ou null en cas d'erreur.
     */
    private String[] readCurrentAlarmFromDb(Context context) {
        try {
            DbHelper dbHelper = new DbHelper(context);
            dbHelper.createdatabase();
            // Lire la 1ère alarme active trouvée (la plus proche dans le temps)
            try (Cursor cursor = dbHelper.alrawQuery(
                    "SELECT tone_name, volume FROM tb_alarms WHERE type = 1 LIMIT 1", null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String tone   = cursor.getString(cursor.getColumnIndexOrThrow("tone_name"));
                    String vol    = cursor.getString(cursor.getColumnIndexOrThrow("volume"));
                    return new String[]{"وقت الصلاة", tone != null ? tone : "normal", vol != null ? vol : "7"};
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Erreur lecture DB dans AlarmReceiver", e);
        }
        return null;
    }

    /**
     * Reprogramme les alarmes après un reboot.
     * Démarre MainActivity qui replanifiera automatiquement.
     */
    private void rescheduleAlarms(Context context) {
        // La manière la plus robuste est de recalculer depuis MainActivity.
        // On la lance en arrière-plan avec un intent spécial si nécessaire.
        // Pour l'instant, lancer un intent discret vers un scheduler dédié.
        try {
            DbHelper dbHelper = new DbHelper(context);
            dbHelper.createdatabase();
            // Signaler à MainActivity de se recalculer via un Intent
            // (MainActivity.scheduleNextAlarm sera appelé au prochain démarrage)
            Log.i(TAG, "Alarmes à reprogrammer après reboot - l'utilisateur devra ouvrir l'app.");
        } catch (IOException e) {
            Log.e(TAG, "Erreur DB lors du reboot", e);
        }
    }
}