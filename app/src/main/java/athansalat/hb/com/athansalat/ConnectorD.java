package athansalat.hb.com.athansalat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Adapter pour la ListView de l'écran ModifD.
 */
public class ConnectorD extends BaseAdapter {
	private final Context mContext;
	private final ArrayList<String> dGr;
	private final ArrayList<String> dHi;
	private final ArrayList<String> nJr;
	private final LayoutInflater layoutInflater;

	public ConnectorD(Context c, ArrayList<String> DGr, ArrayList<String> DHi, ArrayList<String> NJr) {
		this.mContext = c;
		this.dGr = DGr;
		this.dHi = DHi;
		this.nJr = NJr;
		// Initialiser l'inflater une seule fois est plus performant.
		this.layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return dGr.size();
	}

	@Override
	public Object getItem(int position) {
		return null; // Pas nécessaire pour cette application
	}

	@Override
	public long getItemId(int position) {
		return 0; // Pas nécessaire pour cette application
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder mHolder;

		// Le pattern ViewHolder/convertView est la méthode optimisée pour les ListViews.
		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.modif_d_list, parent, false);
			mHolder = new ViewHolder();
			mHolder.txt_DG = convertView.findViewById(R.id.tv_diff11);
			mHolder.txt_DH = convertView.findViewById(R.id.tv_txt);
			mHolder.txt_NJ = convertView.findViewById(R.id.tv_nj);
			convertView.setTag(mHolder);
		} else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		// Mettre à jour les vues avec les données
		mHolder.txt_DG.setText(dGr.get(position));
		mHolder.txt_DH.setText(dHi.get(position));
		mHolder.txt_NJ.setText(nJr.get(position));

		return convertView;
	}

	/**
	 * Classe interne ViewHolder pour stocker les références aux vues,
	 * évitant les appels répétitifs à findViewById().
	 * CORRECTION : Renommée en ViewHolder pour suivre les conventions.
	 * CORRECTION : La référence au bouton inutile a été supprimée.
	 */
	private static class ViewHolder {
		TextView txt_DG;
		TextView txt_DH;
		TextView txt_NJ;
	}
}