package athansalat.hb.com.athansalat;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import athansalat.hb.com.athansalat.databinding.ModifVUpAdBinding;

public class ModifVUp extends AppCompatActivity {

	private ModifVUpAdBinding binding;
	private DbHelper mHelper;
	private String cityId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ModifVUpAdBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		try {
			mHelper = new DbHelper(this);
		} catch (IOException e) {
			e.printStackTrace();
			finish();
		}

		// Récupérer les données passées par l'activité précédente
		if (getIntent().getExtras() != null) {
			cityId = getIntent().getExtras().getString("IDv");
			String cityName = getIntent().getExtras().getString("TXv");
			binding.evTxtV.setText(cityName);
		}

		binding.saveBtn.setOnClickListener(v -> saveCityName());
	}

	private void saveCityName() {
		String newCityName = binding.evTxtV.getText().toString().trim();

		if (newCityName.isEmpty()) {
			Toast.makeText(this, "الرجاء إدخال اسم المدينة", Toast.LENGTH_SHORT).show();
			return;
		}

		SQLiteDatabase dataBase = mHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(DbHelper.KEY_TX, newCityName);

		// Mettre à jour la base de données
		int rowsAffected = dataBase.update("tb_villes", values, "id_v = ?", new String[]{cityId});
		dataBase.close();

		if (rowsAffected > 0) {
			Toast.makeText(this, "تم حفظ الاسم الجديد", Toast.LENGTH_SHORT).show();
			finish(); // Fermer l'activité et retourner à la liste
		} else {
			Toast.makeText(this, "حدث خطأ أثناء الحفظ", Toast.LENGTH_SHORT).show();
		}
	}
}