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

public class GSToSQLiteHoraires {
    private static final String TAG = "GSToSQLiteHoraires";
    private final Context context;
    private final DbHelper dbHelper;

    public GSToSQLiteHoraires(Context context) {
        this.context = context;
        try {
            this.dbHelper = new DbHelper(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateDatabaseFromSheet() {
        showToast("Téléchargement des horaires de prière...");
        String sheetUrl = "https://docs.google.com/spreadsheets/d/1HK63WD42PK1xQP1QJ0ZR43NxspeSFZvQ3bnQ6kfjmVs/export?format=csv&gid=0";
        fetchDataFromSheet(sheetUrl);
    }

    private void fetchDataFromSheet(String url) {
        RequestQueue queue = Volley.newRequestQueue(context);

        // CORRECTION : Simplification de la requête pour éviter la classe anonyme problématique.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // Succès
                    Map<String, String[]> sheetData = parseSheetData(response);
                    if (sheetData != null && !sheetData.isEmpty()) {
                        updateDatabaseInBackground(sheetData);
                    } else {
                        showToast("Aucune donnée d'horaire valide trouvée dans la feuille Google Sheet.");
                    }
                },
                error -> {
                    // Erreur
                    Log.e(TAG, "Erreur Volley: ", error);
                    showToast("Erreur de réseau. Vérifiez votre connexion internet.");
                });

        queue.add(stringRequest);
    }

    private Map<String, String[]> parseSheetData(String csvData) {
        Map<String, String[]> sheetData = new HashMap<>();
        String[] rows = csvData.split("\n");

        for (int i = 1; i < rows.length; i++) { // Ignorer l'en-tête
            String[] columns = rows[i].split(",");

            if (columns.length > 11) { // S'assurer qu'il y a assez de colonnes
                String n_vd = columns[0].trim();
                if (n_vd.isEmpty()) continue;

                String[] values = {
                        columns[4].trim(),  // E: t_fa
                        columns[5].trim(),  // F: t_do
                        columns[6].trim(),  // G: t_as
                        columns[7].trim(),  // H: t_ma
                        columns[8].trim(),  // I: t_is
                        columns[9].trim(),  // J: t_nf
                        columns[10].trim(), // K: t_do0
                        columns[11].trim()  // L: t_ch
                };
                sheetData.put(n_vd, values);
            }
        }
        return sheetData;
    }

    private void updateDatabaseInBackground(Map<String, String[]> sheetData) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // Tâche d'arrière-plan
            int updatedRows = updateDatabase(sheetData);
            final String resultMessage = (updatedRows >= 0)
                    ? "Mise à jour réussie pour " + updatedRows + " enregistrements."
                    : "Échec de la mise à jour des horaires.";

            // Revenir au thread principal
            handler.post(() -> {
                showToast(resultMessage);
                if (context instanceof MainActivity && updatedRows > 0) {
                    ((MainActivity) context).recreate(); // Rafraîchir l'écran principal
                }
            });
        });
    }

    private int updateDatabase(Map<String, String[]> sheetData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        int totalRowsUpdated = 0;
        try {
            for (Map.Entry<String, String[]> entry : sheetData.entrySet()) {
                String n_vd = entry.getKey();
                String[] values = entry.getValue();

                ContentValues contentValues = new ContentValues();
                contentValues.put("t_fa", values[0]);
                contentValues.put("t_do", values[1]);
                contentValues.put("t_as", values[2]);
                contentValues.put("t_ma", values[3]);
                contentValues.put("t_is", values[4]);
                contentValues.put("t_nf", values[5]);
                contentValues.put("t_do0", values[6]);
                contentValues.put("t_ch", values[7]);

                int rowsAffected = db.update("tb_horaires", contentValues, "n_vd = ?", new String[]{n_vd});
                totalRowsUpdated += rowsAffected;
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Erreur de mise à jour de la base de données", e);
            totalRowsUpdated = -1;
        } finally {
            db.endTransaction();
        }
        return totalRowsUpdated;
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}