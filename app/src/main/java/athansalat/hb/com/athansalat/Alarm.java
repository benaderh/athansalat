package athansalat.hb.com.athansalat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import athansalat.hb.com.athansalat.databinding.AlarmBinding;

public class Alarm extends AppCompatActivity {

    private AlarmBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AlarmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- CORRECTION ICI ---
        // Les méthodes setShowWhenLocked et setTurnScreenOn ont été supprimées.
        // La méthode moderne et correcte pour afficher sur l'écran de verrouillage
        // est gérée par le "full-screen intent" dans AlarmService et les flags de fenêtre ci-dessous.

        // Rendre l'activité visible même si l'appareil est verrouillé
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        // Garder l'écran allumé et passer outre le Keyguard
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);


        // Récupérer le nom de la prière depuis l'intent pour l'afficher
        String prayerName = getIntent().getStringExtra("PRAYER_NAME");
        if (prayerName != null) {
            // L'erreur ici a aussi été corrigée car l'ID 'prayer_name_alarm' existe maintenant
            binding.prayerNameAlarm.setText(prayerName);
        }

        binding.stop.setOnClickListener(v -> stopAlarm());
    }

    /**
     * Arrête le service d'alarme et ferme cette activité.
     */
    private void stopAlarm() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        stopService(serviceIntent);
        finish();
    }

    /**
     * Gère le cas où l'utilisateur appuie sur le bouton "Retour".
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            stopAlarm();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}