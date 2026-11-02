package athansalat.hb.com.athansalat;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

public class FavouritesV extends ListActivity {
    ListView listVilles;
    ConnectorF sqlConnect;
    SQLiteDatabase SQLdb;
    DbHelper db;
    Cursor curF;
    int i, j;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //listVilles = (ListView) findViewById(R.id.listView1);
        sqlConnect = new ConnectorF(this);



        ListView listVilles= getListView();
        //	listview.setChoiceMode(listview.CHOICE_MODE_NONE);
        //	listview.setChoiceMode(listview.CHOICE_MODE_SINGLE);
        listVilles.setChoiceMode(listVilles.CHOICE_MODE_MULTIPLE);

        //--	text filtering
        //listVilles.setTextFilterEnabled(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_checked, sqlConnect.getAllRecord());
        //checked.get(1);
        listVilles.setAdapter(adapter);
        //listVilles.setChoiceMode(listVilles.CHOICE_MODE_MULTIPLE);
        listVilles.setItemChecked(0, true);
        listVilles.setItemChecked(1, true);
        listVilles.setItemChecked(2, true);
        //SQLdb=openOrCreateDatabase("db_horaires.sqlite", Context.MODE_PRIVATE, null);
        //SQLdb.execSQL("UPDATE tb_villes SET ac_v = 0");
        //cur.close();


    }

    public void onListItemClick(ListView parent, View v, int position, long id){
        CheckedTextView item = (CheckedTextView) v;
        //Toast.makeText(this,position + " checked : " + 	item.isChecked(), Toast.LENGTH_SHORT).show();
        //Toast.makeText(this,position, Toast.LENGTH_SHORT).show();
        //Toast.makeText(this,position, Toast.LENGTH_SHORT).show();
        //int tp=getSelectedItemPosition(position);
        //int tp = position;
        //Toast.makeText(this,"pos: "+ position+"   tp: " + tp, Toast.LENGTH_SHORT).show();
        //int i= position;
        SQLdb=openOrCreateDatabase("db_horaires.sqlite", Context.MODE_PRIVATE, null);
        SQLdb.execSQL("UPDATE tb_villes SET ac_v = 0");
        //int  i;
        i=position+1;
        if (position==0) {
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v = fv_v - 1");
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v = 67 WHERE fv_v = 0");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =0 WHERE fv_v = 1");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =1 WHERE fv_v = 3");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =3 WHERE fv_v = 0");
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v =3 WHERE fv_v = 4");
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v =4 WHERE fv_v = 0");
        }
        else if (position==1) {
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v = 68 WHERE fv_v = 2");
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v = fv_v - 1 WHERE fv_v > 2");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =0 WHERE fv_v = 2");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =2 WHERE fv_v = 1");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =1 WHERE fv_v = 0");
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v =4 WHERE fv_v = 0");

        }
        else if (position==2) {
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v = 68 WHERE fv_v = 3");
            //SQLdb.execSQL("UPDATE tb_villes SET fv_v = fv_v - 1 WHERE fv_v > 3");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =0 WHERE fv_v = 3");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =3 WHERE fv_v = 2");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v =2 WHERE fv_v = 0");
        }
        else {
            SQLdb.execSQL("UPDATE tb_villes SET fv_v = 100 WHERE fv_v = '" + i  + "'");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v = fv_v + 1 WHERE fv_v < '" + i + "'");
            SQLdb.execSQL("UPDATE tb_villes SET fv_v = 1 WHERE fv_v = 100");
        }

        //database = sqlHelper.getReadableDatabase();
        curF = SQLdb.rawQuery("SELECT  * FROM fv_villes", null);
        //int j;
        curF.moveToFirst();
        j= curF.getInt(3);
        curF.moveToNext();
        j= j + curF.getInt(3);
        curF.moveToNext();
        j= j + curF.getInt(3);
        if (j == 0) {
            SQLdb.execSQL("UPDATE tb_villes SET ac_v = 0");
            SQLdb.execSQL("UPDATE tb_villes SET ac_v = 1 WHERE fv_v = 1");

        }

        sqlConnect = new ConnectorF(this);



        ListView listVilles= getListView();
        //	listview.setChoiceMode(listview.CHOICE_MODE_NONE);
        //	listview.setChoiceMode(listview.CHOICE_MODE_SINGLE);
        listVilles.setChoiceMode(listVilles.CHOICE_MODE_MULTIPLE);

        //--	text filtering
        //listVilles.setTextFilterEnabled(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_checked, sqlConnect.getAllRecord());
        //checked.get(1);
        listVilles.setAdapter(adapter);
        //listVilles.setChoiceMode(listVilles.CHOICE_MODE_MULTIPLE);
        listVilles.setItemChecked(0, true);
        listVilles.setItemChecked(1, true);
        listVilles.setItemChecked(2, true);



    }


    //public void refresh(View view){          //refresh is onClick name given to the button
    //    onRestart();
    //}

    //@Override
    //protected void onRestart() {

      //  // TODO Auto-generated method stub
      //  super.onRestart();
      //  Intent i = new Intent(MainActivity.this, MainActivity.class);  //your class
      //  startActivity(new Intent(this, MainActivity.class));
      //  startActivity(i);
      //  finish();
    //    finish();
    //    startActivity(new Intent(this, MainActivity.class));
        //startActivity(getIntent());

    //}
 //-   private boolean back_answer = false;

 //-   @Override
 //-   public boolean onKeyDown(int keyCode, KeyEvent event)
 //-   {
        ////if(debugState) { Toast.makeText(GoogleAccountSwitcher.this, "key press", Toast.LENGTH_SHORT).show(); }
  //-      if (keyCode == KeyEvent.KEYCODE_BACK) {
  //-          startActivity(new Intent(this, MainActivity.class));
        //    //if(debugState) { Toast.makeText(GoogleAccountSwitcher.this, "BACK key press", Toast.LENGTH_SHORT).show(); }
        //    AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //    builder.setMessage("Etes vous sûr de vouloir quitter ?")
        //            .setCancelable(false)
        //            .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
        //                public void onClick(DialogInterface dialog, int id) {
        //                    back_answer = true;
        //                }
        //            })
        //            .setNegativeButton("Non", new DialogInterface.OnClickListener() {
        //                public void onClick(DialogInterface dialog, int id) {
        //                    back_answer = false;
        //                }
        //            });
        //    AlertDialog alert = builder.create();
        //    alert.show();
   //-     }
   //-    finish();
   //-    return back_answer;

    //-}

    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_BACK )
        {
            this.finish();
            startActivity(new Intent(this, MainActivity.class));
            return true;
        }
        //this.finish();
        return super.onKeyUp( keyCode, event );
    }

}
