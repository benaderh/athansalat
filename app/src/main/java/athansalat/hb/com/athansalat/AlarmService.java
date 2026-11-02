package athansalat.hb.com.athansalat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.AssetFileDescriptor;
import java.io.IOException;

public class AlarmService extends Service {

    private static final String TAG = "AlarmService";
    private static final String CHANNEL_ID = "ALARM_SERVICE_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest; // Pour Android 8+

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Récupérer les données depuis l'intent (envoyées par AlarmReceiver)
        String prayerName = intent != null ? intent.getStringExtra("PRAYER_NAME") : "وقت الصلاة";
        String toneName   = intent != null ? intent.getStringExtra("TONE_NAME")   : "normal";
        int    volume     = intent != null ? intent.getIntExtra("VOLUME", 7)       : 7;

        if (prayerName == null) prayerName = "وقت الصلاة";
        if (toneName   == null) toneName   = "normal";

        // ---- Intent vers l'activité principale (tap sur la notification) ----
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                this, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ---- Full-screen intent → Lance l'écran d'alarme même quand l'appareil est verrouillé ----
        Intent alarmScreenIntent = new Intent(this, Alarm.class);
        alarmScreenIntent.putExtra("PRAYER_NAME", prayerName);
        alarmScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, 1, alarmScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ---- Notification en avant-plan (obligatoire pour ForegroundService) ----
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("تنبيه الصلاة")
                .setContentText(prayerName)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(mainPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(false)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        // ---- WakeLock : maintenir le CPU actif pendant la sonnerie (max 10 min) ----
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AthanSalat::AlarmWakeLock");
            wakeLock.acquire(10 * 60 * 1000L);
        }

        // ---- Jouer le son depuis assets/ ----
        playAlarmSound(toneName, volume);

        return START_NOT_STICKY;
    }

    /**
     * Joue un fichier audio depuis le dossier assets/AlarmAthan/.
     * Utilise AudioAttributes (API moderne) à la place de setAudioStreamType (déprécié).
     */
    private void playAlarmSound(String toneName, int volume) {
        // Demander le focus audio sur le flux ALARM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            //noinspection deprecation
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }

        mediaPlayer = new MediaPlayer();

        // Configuration AudioAttributes (remplace setAudioStreamType déprécié)
        AudioAttributes audioAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer.setAudioAttributes(audioAttrs);

        try {
            // Le son est dans assets/AlarmAthan/<toneName>.mp3
            // On tente d'abord le nom exact, puis on cherche un fichier correspondant
            String assetPath = "AlarmAthan/" + toneName + ".mp3";
            AssetFileDescriptor afd = openAssetFileDescriptor(assetPath);

            if (afd == null) {
                // Fallback sur "normal.mp3"
                Log.w(TAG, "Son introuvable: " + assetPath + " → utilisation de normal.mp3");
                afd = openAssetFileDescriptor("AlarmAthan/normal.mp3");
            }

            if (afd == null) {
                Log.e(TAG, "Aucun fichier son trouvable dans assets/AlarmAthan/");
                stopSelf();
                return;
            }

            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            // Volume : la DB stocke 0-10, MediaPlayer attend 0.0-1.0
            float vol = Math.max(0f, Math.min(volume / 10.0f, 1.0f));
            mediaPlayer.setVolume(vol, vol);

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> mp.start());
            mediaPlayer.setOnCompletionListener(mp -> stopSelf());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer erreur: what=" + what + " extra=" + extra);
                stopSelf();
                return true;
            });

        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la lecture du son", e);
            stopSelf();
        }
    }

    /**
     * Ouvre un AssetFileDescriptor depuis les assets, retourne null si introuvable.
     */
    private AssetFileDescriptor openAssetFileDescriptor(String path) {
        try {
            return getAssets().openFd(path);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (IllegalStateException ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Libérer le focus audio
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                //noinspection deprecation
                audioManager.abandonAudioFocus(null);
            }
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Crée le canal de notification avec IMPORTANCE_HIGH (son + vibration).
     * Le canal doit être de haute importance pour que l'alarme soit perçue.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "تنبيهات الصلاة",
                    NotificationManager.IMPORTANCE_HIGH  // HIGH = son + vibration
            );
            channel.setDescription("قناة إشعارات مواقيت الصلاة");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}