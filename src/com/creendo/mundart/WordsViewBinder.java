package com.creendo.mundart;

import android.database.Cursor;
import android.view.View;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.creendo.mundart.data.MundartDb;

public class WordsViewBinder implements SimpleCursorAdapter.ViewBinder {
	
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		// the textview
		int viewId = view.getId();
		switch (viewId) {
			case R.id.text:
				TextView noteName = (TextView) view;
				noteName.setText(cursor.getString(columnIndex));
				break;
			
			case R.id.tick:
				ImageView tickIcon = (ImageView) view;
				int user_defined_status = cursor.getInt(columnIndex);
				if (user_defined_status == MundartDb.UD_STATUS_SENT){
					tickIcon.setImageResource(R.drawable.tick);
					tickIcon.setVisibility(View.VISIBLE);
				} else {
					tickIcon.setVisibility(View.GONE);
				}
				break;
	
			case R.id.icon:
				ImageView cantonIcon = (ImageView) view;
				int dialectId = cursor.getInt(columnIndex);
				switch (dialectId){
					case 0:
						cantonIcon.setImageResource(R.drawable.all_list);
						break;
					case 1:
						cantonIcon.setImageResource(R.drawable.ag);
						break;
					case 2:
						cantonIcon.setImageResource(R.drawable.ar);
						break;
					case 3:
						cantonIcon.setImageResource(R.drawable.ai);
						break;
					case 4:
						cantonIcon.setImageResource(R.drawable.be);
						break;
					case 5:
						cantonIcon.setImageResource(R.drawable.bl);
						break;
					case 6:
						cantonIcon.setImageResource(R.drawable.bs);
						break;
					case 7:
						cantonIcon.setImageResource(R.drawable.gl);
						break;
					case 8:
						cantonIcon.setImageResource(R.drawable.gr);
						break;
					case 9:
						cantonIcon.setImageResource(R.drawable.lu);
						break;
					case 10:
						cantonIcon.setImageResource(R.drawable.nw);
						break;
					case 11:
						cantonIcon.setImageResource(R.drawable.ow);
						break;
					case 12:
						cantonIcon.setImageResource(R.drawable.sh);
						break;
					case 13:
						cantonIcon.setImageResource(R.drawable.so);
						break;
					case 14:
						cantonIcon.setImageResource(R.drawable.tg);
						break;
					case 15:
						cantonIcon.setImageResource(R.drawable.vs);
						break;
					case 16:
						cantonIcon.setImageResource(R.drawable.zg);
						break;
					case 17:
						cantonIcon.setImageResource(R.drawable.zh);
						break;
					case MundartDb.ICON_GERMAN_WORD:
						cantonIcon.setImageResource(R.drawable.all_list_de);
						break;
					default: 
						cantonIcon.setImageResource(R.drawable.all_list);
						break;
				}
		}
		return true;
	}
}
