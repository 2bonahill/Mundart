package com.creendo.mundart;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.creendo.mundart.data.MundartDb;

public class UserWordList extends AbstractWordList {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initCommons(); // view elements, db etc...

		// we don't want to see the search bar here
		mSearchBar.setVisibility(View.GONE);

		new FetchWordListTask().execute(null);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		// save current scroll position
		savedScrollPos = mWordList.getFirstVisiblePosition();
		mWordsCursor.moveToPosition(position);

		Intent i = new Intent(this, WordEdit.class);

		// we pass the id of the word...
		i.putExtra(MundartDb.KEY, id);
		// ...and the type of the word (german word vs. dialect word)
		int type = mWordsCursor.getInt(mWordsCursor
				.getColumnIndex(MundartDb.KEY_TYPE));
		i.putExtra(MundartDb.KEY_TYPE, type);

		/*
		 * tell WordEdit.class that this call is originated in the context of
		 * user defined words.
		 * 
		 * if the user clicked on a german word, the edit erman word mode will
		 * be enabled (no translations visible). if the user clicked on a
		 * dialect word, he will be able to edit his own translation
		 */
		if (type == mDb.TYPE_GERMAN_WORD) {
			i.putExtra(WordEdit.KEY_USAGE_MODE, WordEdit.MODE_EDIT_GERMAN_WORD);
		} else if (type == mDb.TYPE_DIALECT_WORD) {
			i.putExtra(WordEdit.KEY_USAGE_MODE, WordEdit.MODE_EDIT_TRANSLATION);
		}

		startActivityForResult(i, ACTIVITY_EDIT);
	}

	

	@Override
	Cursor mDoInBackground(String filter) {
		// TODO Auto-generated method stub
		return (filter == null) ? mDb.fetchUserWords(null) : mDb
				.fetchUserWords(filter);
	}

}