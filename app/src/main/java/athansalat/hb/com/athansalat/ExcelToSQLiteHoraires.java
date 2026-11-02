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

public class ExcelToSQLiteHoraires {
    private static final String TAG = "ExcelToSQLiteHoraires";
    private final Context context;
    private final DbHelper dbHelper;

    public ExcelToSQLiteHoraires(Context context) {
        this.context = context;
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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        showToast("Début de la mise à jour des horaires...");

        executor.execute(() -> {
            // --- TÂCHE D'ARRIÈRE-PLAN ---
            final Map<String, String[]> excelData = readExcelData(fileUri, "Feuil1");
            final String resultMessage;

            if (excelData != null && !excelData.isEmpty()) {
                int updatedRows = updateDatabase(excelData);
                if (updatedRows >= 0) {
                    resultMessage = "Mise à jour réussie pour " + updatedRows + " enregistrements.";
                } else {
                    resultMessage = "Erreur lors de la mise à jour de la base de données.";
                }
            } else {
                resultMessage = "Aucune donnée lue. Vérifiez le fichier Excel et la feuille 'Feuil1'.";
            }
            // --- FIN DE LA TÂCHE D'ARRIÈRE-PLAN ---

            handler.post(() -> {
                showToast(resultMessage);
                // Optionnel : rafraîchir l'UI de MainActivity si elle est visible
                if (context instanceof MainActivity) {
                    ((MainActivity) context).recreate(); // Une façon simple de rafraîchir
                }
            });
        });
    }

    /**
     * Lit les données d'un fichier Excel à partir d'une URI.
     */
    private Map<String, String[]> readExcelData(Uri fileUri, String sheetName) {
        Map<String, String[]> excelData = new HashMap<>();
        try (InputStream fis = context.getContentResolver().openInputStream(fileUri);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Log.e(TAG, "La feuille '" + sheetName + "' est introuvable.");
                return null;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    String n_vd = getCellValueAsString(row.getCell(0));
                    if (n_vd == null || n_vd.isEmpty()) continue;

                    String[] values = {
                            getCellValueAsString(row.getCell(4)),  // E: t_fa
                            getCellValueAsString(row.getCell(5)),  // F: t_do
                            getCellValueAsString(row.getCell(6)),  // G: t_as
                            getCellValueAsString(row.getCell(7)),  // H: t_ma
                            getCellValueAsString(row.getCell(8)),  // I: t_is
                            getCellValueAsString(row.getCell(9)),  // J: t_nf
                            getCellValueAsString(row.getCell(10)), // K: t_do0
                            getCellValueAsString(row.getCell(11))  // L: t_ch
                    };
                    excelData.put(n_vd, values);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur de lecture du fichier Excel", e);
            return null;
        }
        return excelData;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return Long.toString((long) numValue);
                    } else {
                        return Double.toString(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    // Tente de récupérer la valeur calculée de la formule comme un nombre
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                } catch (IllegalStateException e) {
                    // Si ce n'est pas un nombre, tente de récupérer comme du texte
                    return cell.getStringCellValue().trim();
                }
            case BLANK:
            default:
                return "";
        }
    }

    /**
     * Met à jour la table tb_horaires avec les nouvelles données.
     */
    private int updateDatabase(Map<String, String[]> excelData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        int totalRowsUpdated = 0;
        try {
            for (Map.Entry<String, String[]> entry : excelData.entrySet()) {
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
            totalRowsUpdated = -1; // Signale une erreur
        } finally {
            db.endTransaction();
        }
        return totalRowsUpdated;
    }

    /**
     * Affiche un message Toast sur le thread principal.
     */
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}