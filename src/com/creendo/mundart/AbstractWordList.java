/*
 * Copyright (C) 2011 Creendo
 */
package com.creendo.mundart;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.creendo.mundart.data.MundartDb;

public abstract class AbstractWordList extends ListActivity {

	protected static String TAG = "Mundart";

	protected static final int ACTIVITY_VIEW = 0;
	protected static final int ACTIVITY_EDIT = 1;

	protected MundartDb mDb;
	protected Cursor mWordsCursor;
	protected ListView mWordList;
	protected EditText mSearchBar;
	protected SimpleCursorAdapter words;
	protected String currentFilterText;

	protected static final int ADD_ID = Menu.FIRST;
	protected static final int ACTIVITY_ADD = 2;

	protected static final int SEND_ID = ADD_ID + 1;
	protected static final int ACTIVITY_SEND = 3;

	protected int savedScrollPos = 0;

	protected ProgressDialog mProgressBar;
	AlertDialog.Builder mThanksDialog;

	protected JSONArray mWords2Send;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// add the menu items. one for adding new words
		// and one for sending
		menu.add(0, ADD_ID, 0, R.string.menu_add);
		menu.add(0, SEND_ID, 0, R.string.menu_send);
		return true;
	}

	/**
	 * This method populates the ListView. It gets invoked by the AsyncTask
	 * FetchWordListTask which will retrieve the words from the database and
	 * store them in the mWordsCursor Cursor.
	 * 
	 * An extra layout file (word_list_entry.xml) and a custom ViewBinder to
	 * show the flag, the word/translation and the tick in case of a
	 * synchronized user word
	 */
	protected void populateWords() {
		// define the columns of the cursor in which we are interested
		// (this corresponds to the SELECT xxx of the query)
		String[] from = new String[] { MundartDb.KEY_DIALECTS,
				MundartDb.KEY_WORDS, "user_defined" };
		int[] to = new int[] { R.id.icon, R.id.text, R.id.tick };
		words = new SimpleCursorAdapter(this, R.layout.word_list_entry,
				mWordsCursor, from, to);
		words.setViewBinder(new WordsViewBinder());
		setListAdapter(words);

		// now scroll to the last scroll position
		mWordList.setSelection(savedScrollPos);
	}

	/**
	 * This method deals with the GUI stuff for the common UI elements of both:
	 * - WordList - UserWordList
	 */
	protected void initCommons() {
		// set the wordlist view. word_list is a linear layout, containing
		// 1) the search bar 2) the list of words 3) the "no results" text view
		setContentView(R.layout.word_list);

		mDb = new MundartDb(this);
		mDb.open();

		// the word list (it has the id="list").
		// it is where the words will be displayed
		mWordList = getListView();

		// The searchbar. Whenever the textfield changes, we filter the list
		mSearchBar = (EditText) findViewById(R.id.search);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		new FetchWordListTask().execute(currentFilterText);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.app.ActivityGroup#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mDb.close();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// store the current scroll position
		savedScrollPos = mWordList.getFirstVisiblePosition();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case ADD_ID:
			Intent i = new Intent(this, WordEdit.class);
			/*
			 * tell WordEdit.class that this call is originated from the
			 * translation list. this means that the word edit class will create
			 * a new translation
			 */
			i.putExtra(WordEdit.KEY_USAGE_MODE, WordEdit.MODE_ADD_GERMAN_WORD);
			startActivityForResult(i, ACTIVITY_ADD);
			break;
		case SEND_ID:
			try {
				sendUserTranslations();
			} catch (JSONException e) {
				mProgressBar.dismiss();
			}

			break;
		}
		return super.onMenuItemSelected(featureId, item);

	}

	/**
	 * This method sends all the translations with user_defined set to 1. If the
	 * sending has been successful, the tag will be set to 2.
	 * 
	 * fetchUserTranslations4Sync returns: german_word || translation || dialect
	 * code
	 */
	private void sendUserTranslations() throws JSONException {
		/*
		 * show a progress bar
		 */
		mProgressBar = ProgressDialog.show(AbstractWordList.this, "",
				"Wörter werden gesendet...", true);
		mProgressBar.show();
		/*
		 * prepare the tankyou dialog
		 */
		mThanksDialog = new AlertDialog.Builder(this);

		/*
		 * get the translations to be sent
		 */
		Cursor c = mDb.fetchUserTranslations4Sync();
		JSONObject o;
		Log.v("Mundart", "Anzahl zu synchender Wörter: " + c.getCount());
		if (c.moveToFirst()) {
			mWords2Send = new JSONArray();
			do {
				o = new JSONObject();
				o.put("german_word", c.getString(0));
				o.put("translation", c.getString(1));
				o.put("dialect", c.getString(2));
				mWords2Send.put(o);
			} while (c.moveToNext());
			// sendJson(mWords2Send);
			new SendUserWordsTask().execute(mWords2Send);
		} else {
			// nothing to be sent
			mProgressBar.dismiss();
			mThanksDialog
					.setMessage(
							"Es wurden keine Wörter gesendet, da entweder keine neuen Wörter vorhanden oder die bestehenden bereits gesendet worden sind.")
					.setTitle("Keine Wörter gesendet")
					.setCancelable(false)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = mThanksDialog.create();
			alert.show();
		}
	}

	/**
	 * This is an asynchronous task to send the json to the server
	 */
	private class SendUserWordsTask extends AsyncTask<JSONArray, Void, Boolean> {

		@Override
		protected Boolean doInBackground(JSONArray... arg0) {
			boolean result = false;
			/*
			 * create a new JSONObject, in which we will store the translations
			 */
			final JSONObject json = new JSONObject();

			try {
				json.put("data", arg0[0]);
			} catch (JSONException e) {
				mProgressBar.dismiss();
			}

			// prepare for the post
			HttpClient client = new DefaultHttpClient();
			HttpConnectionParams
					.setConnectionTimeout(client.getParams(), 10000); // Timeout
																		// Limit
			HttpResponse response;
			// post data to the server
			try {
				HttpPost post = new HttpPost("http://creendo.com/data/sync.php");
				List<NameValuePair> nVP = new ArrayList<NameValuePair>(2);
				nVP.add(new BasicNameValuePair("json", json.toString()));
				UrlEncodedFormEntity encodedFormEntity = new UrlEncodedFormEntity(
						nVP);
				encodedFormEntity.setContentEncoding(HTTP.UTF_8);
				post.setEntity(encodedFormEntity);
				response = client.execute(post);

				// Checking response
				if (response != null) {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(response.getEntity()
									.getContent()));
					String line;
					String serverResponse = "";
					while ((line = br.readLine()) != null) {
						serverResponse = line;
					}
					if (serverResponse.contains("okay")) {
						result = true;
					}
				} else {
					result = false;
				}

			} catch (Exception e) {
				mProgressBar.dismiss();
			}
			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			mProgressBar.dismiss();
			// if we got a true result, we can update the database and notify
			// the user that everything worked as desired
			if (result) {
				// set translations and user words sent
				mDb.setUserWordsSent();
				// say thank you
				mThanksDialog
						.setMessage(R.string.senden_thanks)
						.setCancelable(false)
						.setTitle("Vielen Dank!")
						.setPositiveButton("Weiter",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										new FetchWordListTask()
												.execute(currentFilterText);
										dialog.cancel();
									}
								});
			} else {
				mThanksDialog
						.setMessage(R.string.senden_fehler)
						.setTitle("Fehler")
						.setCancelable(false)
						.setPositiveButton("Nochmals",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										try {
											sendUserTranslations();
										} catch (JSONException e) {
											// TODO Auto-generated catch block
											mProgressBar.dismiss();
										}
										dialog.cancel();
									}
								})
						.setNegativeButton("Später",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
			}
			// say thank you
			AlertDialog alert = mThanksDialog.create();
			alert.show();
		}

	}

	/**
	 * Fetching words from DB is done via threading.
	 */
	abstract Cursor mDoInBackground(String filter);

	protected class FetchWordListTask extends AsyncTask<String, Void, Cursor> {

		@Override
		protected Cursor doInBackground(String... arg0) {
			return (arg0 == null) ? mDoInBackground(null)
					: mDoInBackground(arg0[0]);
			// return (arg0 == null) ? mDb.fetchWords(null) :
			// mDb.fetchWords(arg0[0]);
		}

		@Override
		protected void onPostExecute(Cursor result) {
			mWordsCursor = result;
			populateWords();
		}

	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// TODO: do nothing in case of requestCode = ACTIVITY_VIEW
		new FetchWordListTask().execute(currentFilterText);
	}

}
