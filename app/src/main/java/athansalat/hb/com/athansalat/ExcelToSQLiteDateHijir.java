package athansalat.hb.com.athansalat;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExcelToSQLiteDateHijir {
    private static final String TAG = "ExcelToSQLiteDateHijir";
    private final Context context;
    private final DbHelper dbHelper;

    public ExcelToSQLiteDateHijir(Context context) {
        this.context = context;
        // On ne crée qu'une seule instance de DbHelper pour éviter les problèmes de base de données
        try {
            this.dbHelper = new DbHelper(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Point d'entrée public. Lance le processus d'importation sur un thread d'arrière-plan.
     * @param fileUri L'URI du fichier Excel sélectionné par l'utilisateur.
     */
    public void updateDatabaseFromExcelUri(Uri fileUri) {
        // Créer un Executor pour exécuter la tâche en arrière-plan
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Créer un Handler pour communiquer avec le thread principal (pour afficher les Toasts)
        Handler handler = new Handler(Looper.getMainLooper());

        showToast("Début de l'importation du calendrier Hijri...");

        executor.execute(() -> {
            // --- TÂCHE D'ARRIÈRE-PLAN ---
            final Map<String, String[]> excelData = readExcelData(fileUri, "Feuil2");
            final boolean success;

            if (excelData != null && !excelData.isEmpty()) {
                success = updateDatabase(excelData);
            } else {
                success = false;
            }
            // --- FIN DE LA TÂCHE D'ARRIÈRE-PLAN ---

            // Revenir sur le thread principal pour afficher le résultat
            handler.post(() -> {
                if (success) {
                    showToast("Calendrier Hijri mis à jour avec succès !");
                } else {
                    showToast("Échec de la mise à jour du calendrier Hijri. Vérifiez le fichier ou les logs.");
                }
            });
        });
    }

    /**
     * Lit les données d'un fichier Excel à partir d'une URI.
     * @param fileUri L'URI du fichier.
     * @param sheetName Le nom de la feuille à lire.
     * @return Une Map contenant les données, ou null en cas d'erreur.
     */
    private Map<String, String[]> readExcelData(Uri fileUri, String sheetName) {
        Map<String, String[]> excelData = new HashMap<>();
        try (InputStream fis = context.getContentResolver().openInputStream(fileUri);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Log.e(TAG, "La feuille '" + sheetName + "' n'a pas été trouvée dans le fichier Excel.");
                return null;
            }

            // Commencer à la deuxième ligne (index 1) pour ignorer l'en-tête
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String d_gr1 = getCellValueAsString(row.getCell(0));
                    String d_hi1 = getCellValueAsString(row.getCell(1));
                    String n_jr = getCellValueAsString(row.getCell(2));

                    // Ajouter les données uniquement si la clé (date grégorienne) n'est pas vide
                    if (d_gr1 != null && !d_gr1.isEmpty()) {
                        excelData.put(d_gr1, new String[]{d_hi1, n_jr});
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la lecture du fichier Excel", e);
            return null;
        }
        return excelData;
    }

    /**
     * Méthode modernisée pour extraire la valeur d'une cellule en tant que String.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        // Utilise getCellType() qui remplace getCellTypeEnum()
        CellType cellType = cell.getCellType();
        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Si c'est une date, vous voudrez peut-être la formater différemment
                    return cell.getDateCellValue().toString();
                } else {
                    // Convertit le nombre en chaîne sans le ".0" pour les entiers
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Tenter d'évaluer la formule
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
            default:
                return "";
        }
    }

    /**
     * Met à jour la base de données SQLite avec les données lues depuis Excel.
     * @param excelData Les données à insérer.
     * @return true si la mise à jour a réussi, false sinon.
     */
    private boolean updateDatabase(Map<String, String[]> excelData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // Vider la table avant d'insérer les nouvelles données
            db.delete("tb_hijc", null, null);

            // Insérer les nouvelles données
            for (Map.Entry<String, String[]> entry : excelData.entrySet()) {
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
            // La base de données n'est pas fermée ici car dbHelper gère le cycle de vie.
        }
    }

    /**
     * Affiche un message Toast sur le thread principal.
     */
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}