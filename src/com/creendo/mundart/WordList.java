package com.creendo.mundart;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.creendo.mundart.data.MundartDb;

public class WordList extends AbstractWordList {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initCommons(); // view elements, db etc...
		mWordList.setFastScrollEnabled(true);

		// in the word list, we use the search bar
		mSearchBar.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				// Auto-generated method stub

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// Auto-generated method stub
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				currentFilterText = s.toString();
				new FetchWordListTask().execute(currentFilterText);
			}

		});

		new FetchWordListTask().execute(null);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		// save current scroll position
		savedScrollPos = mWordList.getFirstVisiblePosition();

		mWordsCursor.moveToPosition(position);
		Intent i = new Intent(this, WordTranslations.class);

		// we pass the id of the word or 0 in case of dialect word...
		i.putExtra(MundartDb.KEY, id);
		// and the type of the word (german word vs. dialect word)
		int typeColumn = mWordsCursor.getColumnIndex(MundartDb.KEY_TYPE);
		i.putExtra(MundartDb.KEY_TYPE, mWordsCursor.getInt(typeColumn));

		// finally, we can start the activity
		startActivityForResult(i, ACTIVITY_VIEW);
	}

	

	@Override
	Cursor mDoInBackground(String filter) {
		return (filter == null) ? mDb.fetchWords(null) : mDb.fetchWords(filter);
	}
}