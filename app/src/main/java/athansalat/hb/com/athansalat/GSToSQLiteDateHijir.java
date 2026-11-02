package athansalat.hb.com.athansalat;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GSToSQLiteDateHijir {
    private static final String TAG = "GSToSQLiteDateHijir";
    private final Context context;
    private final DbHelper dbHelper;

    public GSToSQLiteDateHijir(Context context) {
        this.context = context;
        try {
            this.dbHelper = new DbHelper(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDatabaseFromSheet() {
        showToast("Téléchargement du calendrier Hijri...");
        String sheetUrl = "https://docs.google.com/spreadsheets/d/1HK63WD42PK1xQP1QJ0ZR43NxspeSFZvQ3bnQ6kfjmVs/export?format=tsv&gid=1009742703";
        fetchDataFromSheet(sheetUrl);
    }

    private void fetchDataFromSheet(String url) {
        RequestQueue queue = Volley.newRequestQueue(context);

        // CORRECTION : La requête est simplifiée, sans la surcharge problématique.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Succès : traiter la réponse
                    Map<String, String[]> sheetData = parseSheetData(response);
                    if (sheetData != null && !sheetData.isEmpty()) {
                        updateDatabaseInBackground(sheetData);
                    } else {
                        showToast("Aucune donnée valide trouvée dans la feuille Google Sheet.");
                    }
                },
                error -> {
                    // Erreur
                    Log.e(TAG, "Erreur Volley: ", error);
                    showToast("Erreur de réseau. Vérifiez votre connexion internet.");
                });

        queue.add(stringRequest);
    }

    private Map<String, String[]> parseSheetData(String tsvData) {
        Map<String, String[]> sheetData = new HashMap<>();
        String[] rows = tsvData.split("\n");

        // Commencer à i = 1 pour ignorer la ligne d'en-tête
        for (int i = 1; i < rows.length; i++) {
            String[] columns = rows[i].split("\t"); // Utiliser la tabulation comme séparateur pour TSV

            if (columns.length >= 3) {
                String d_gr1 = columns[0].trim();
                String d_hi1 = columns[1].trim();
                String n_jr = columns[2].trim();

                if (!d_gr1.isEmpty()) {
                    sheetData.put(d_gr1, new String[]{d_hi1, n_jr});
                }
            }
        }
        return sheetData;
    }

    private void updateDatabaseInBackground(Map<String, String[]> sheetData) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // Tâche d'arrière-plan
            boolean success = updateDatabase(sheetData);

            // Revenir au thread principal pour afficher le résultat
            handler.post(() -> {
                if (success) {
                    showToast("Calendrier Hijri mis à jour avec succès !");
                } else {
                    showToast("Échec de la mise à jour du calendrier Hijri.");
                }
            });
        });
    }

    private boolean updateDatabase(Map<String, String[]> sheetData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("tb_hijc", null, null);

            for (Map.Entry<String, String[]> entry : sheetData.entrySet()) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("d_gr1", entry.getKey());
                contentValues.put("d_hi1", entry.getValue()[0]);
                contentValues.put("n_jr", entry.getValue()[1]);
                db.insert("tb_hijc", null, contentValues);
            }
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la mise à jour de la base de données", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}