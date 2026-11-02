package athansalat.hb.com.athansalat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Adapter pour la ListView de l'écran ModifV.
 */
public class ConnectorV extends BaseAdapter {
	private final Context mContext;
	private final ArrayList<String> idV;
	private final ArrayList<String> txV;
	private final LayoutInflater layoutInflater;

	public ConnectorV(Context c, ArrayList<String> IDv, ArrayList<String> TXv, ArrayList<String> FVv, ArrayList<String> ACv) {
		this.mContext = c;
		this.idV = IDv;
		this.txV = TXv;
		// Les listes FVv et ACv ne sont pas utilisées dans le layout, donc on les ignore ici.
		this.layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return idV.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder mHolder;

		if (convertView == null) {
			convertView = layoutInflater.inflate(R.layout.modif_v_list, parent, false);
			mHolder = new ViewHolder();
			mHolder.txt_ID = convertView.findViewById(R.id.tv_id);
			mHolder.txt_TX = convertView.findViewById(R.id.tv_tx);
			convertView.setTag(mHolder);
		} else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		mHolder.txt_ID.setText(idV.get(position));
		mHolder.txt_TX.setText(txV.get(position));

		return convertView;
	}

	/**
	 * ViewHolder pour stocker les références aux vues.
	 * CORRECTION : Ne contient que les vues qui existent réellement dans le layout.
	 */
	private static class ViewHolder {
		TextView txt_ID;
		TextView txt_TX;
	}
}