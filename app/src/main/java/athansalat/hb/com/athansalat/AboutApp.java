package athansalat.hb.com.athansalat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import athansalat.hb.com.athansalat.databinding.ActivityAboutBinding;

public class AboutApp extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Mettre à jour le texte directement ici au lieu de le hardcoder dans le layout
        binding.txt02.setText("هشام بن نادر - عين البيضاء - الجزائر");

        // Gérer le clic sur l'adresse email
        binding.txt031.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            // On utilise Uri.parse("mailto:") qui est la méthode correcte
            emailIntent.setData(Uri.parse("mailto:benaderh@gmail.com"));
            try {
                startActivity(emailIntent);
            } catch (ActivityNotFoundException e) {
                // Gérer le cas où aucune application d'email n'est installée
                Toast.makeText(this, "Aucune application d'email n'est installée.", Toast.LENGTH_SHORT).show();
            }
        });

        // Gérer le clic sur le lien Facebook
        binding.txt041.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/benaderh.hb.1"));
            try {
                startActivity(browserIntent);
            } catch (ActivityNotFoundException e) {
                // Gérer le cas où aucun navigateur n'est installé
                Toast.makeText(this, "Aucun navigateur web n'est installé.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}