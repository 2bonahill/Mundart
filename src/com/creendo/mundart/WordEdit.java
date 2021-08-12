package com.creendo.mundart;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import com.creendo.mundart.data.MundartDb;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class WordEdit extends Activity {

	private Button saveButton;
	private Button cancelButton;
	private EditText mTranslation;
	private EditText mTitleView;
	private Spinner mSpinner;

	private boolean mToggleGermanWord = true;
	private boolean mToggleTranslation = false;

	private MundartDb mDb;
	private Cursor mCantonsCursor;

	private Long mCurrentWordId;
	private Long mGermanWordId;
	private Long mTranslationId;
	private Integer mCurrentRowType;
	private Cursor mCursor;

	public static String KEY_USAGE_MODE = "Usage";
	public static int MODE_ADD_GERMAN_WORD = 1;
	public static int MODE_ADD_TRANSLATION = 2;
	public static int MODE_EDIT_TRANSLATION = 3;
	public static int MODE_EDIT_GERMAN_WORD = 4;
	private int CURRENT_USAGE_MODE = 0;

	private boolean b;

	private String mRegionSelected;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.word_edit);

		/*
		 * load admob banner
		 */
		AdView adView = new AdView(this, AdSize.BANNER, Mundart.MY_AD_UNIT_ID);
		LinearLayout layout = (LinearLayout) findViewById(R.id.ad_edit);
		layout.addView(adView);
		adView.loadAd(Mundart.mAdRequest);

		mDb = new MundartDb(this);
		mDb.open();

		mTitleView = (EditText) findViewById(R.id.title);
		mTitleView.setEnabled(false);

		saveButton = (Button) findViewById(R.id.save_button);
		saveButton.setEnabled(false);
		saveButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				setResult(RESULT_OK);
				if (CURRENT_USAGE_MODE == MODE_EDIT_TRANSLATION) {
					updateTranslation();
				} else if (CURRENT_USAGE_MODE == MODE_ADD_TRANSLATION) {
					storeTranslation();
				} else if (CURRENT_USAGE_MODE == MODE_ADD_GERMAN_WORD) {
					addGermanWord();
					storeTranslation();
				} else if (CURRENT_USAGE_MODE == MODE_EDIT_GERMAN_WORD) {
					updateGermanWord();
				}
				finish();
			}

		});

		mSpinner = (Spinner) findViewById(R.id.spinner1);
		mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				mCantonsCursor.moveToPosition(arg2);
				mRegionSelected = mCantonsCursor.getString(1);

			}

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}

		});
		populateSpinner();

		cancelButton = (Button) findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}

		});

		mTranslation = (EditText) findViewById(R.id.translation);
		mTranslation.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				mToggleTranslation = true;
				toggleSaveButtton();
			}

		});

		Bundle extras = getIntent().getExtras();
		/*
		 * we now have to find out who the caller is: if the caller was
		 * UserWordList the "UserWordList" field of the intent was set to true.
		 * on the other hand, if the caller was WordTranslation, this field is
		 * set to false (as this is not a user word and the purpose is to add a
		 * new translation to an application given german word)
		 */
		CURRENT_USAGE_MODE = extras != null ? extras.getInt(KEY_USAGE_MODE) : 0;
		if (CURRENT_USAGE_MODE == MODE_EDIT_TRANSLATION
				|| CURRENT_USAGE_MODE == MODE_ADD_TRANSLATION
				|| CURRENT_USAGE_MODE == MODE_EDIT_GERMAN_WORD) {
			/*
			 * one of the following has happened: - MODE_EDIT_TRANSLATION: user
			 * clicked on a translation in the "my words" tab -
			 * MODE_EDIT_GERMAN_WORD: user clicked on one of his own german
			 * words in the "my words" tab - MODE_ADD_TRANSLATION: user wants to
			 * add a translation to a "wortschatz" german word.
			 * 
			 * In each case, we have an id and a type indicating whether we are
			 * dealing with a german word or a dialect word.
			 */
			mCurrentWordId = (savedInstanceState == null) ? null
					: (Long) savedInstanceState.getSerializable(MundartDb.KEY);
			if (mCurrentWordId == null) {
				mCurrentWordId = extras != null ? extras.getLong(MundartDb.KEY)
						: null;
			}
			/*
			 * get the row type (german word vs dialect word)
			 */
			mCurrentRowType = (savedInstanceState == null) ? null
					: (Integer) savedInstanceState
							.getSerializable(MundartDb.KEY_TYPE);
			if (mCurrentRowType == null) {
				mCurrentRowType = extras != null ? extras
						.getInt(MundartDb.KEY_TYPE) : null;
			}

			/*
			 * if the row type indicates a dialect word, we have to get the id
			 * of the german word from the db
			 */
			if (mCurrentRowType == MundartDb.TYPE_GERMAN_WORD) {
				mGermanWordId = mCurrentWordId;
			} else if (mCurrentRowType == MundartDb.TYPE_DIALECT_WORD) {
				mGermanWordId = mDb.getGermanWordId(mCurrentWordId);
				mTranslationId = mCurrentWordId;
			}
		}

		if (CURRENT_USAGE_MODE == MODE_EDIT_TRANSLATION) {
			/*
			 * get the translation details and fill up the views
			 */
			mCursor = mDb.getTranslationDetails(mTranslationId);
			if (mCursor.getCount() > 0) {
				mCursor.moveToFirst();
				mTranslation.setText(mCursor.getString(1));
				mSpinner.setSelection(mCursor.getInt(2));
			}
			mTitleView.setText(mDb.getGermanWord(mGermanWordId));
		} else if (CURRENT_USAGE_MODE == MODE_ADD_TRANSLATION) {
			mTitleView.setText(mDb.getGermanWord(mGermanWordId));
		} else if (CURRENT_USAGE_MODE == MODE_ADD_GERMAN_WORD) {
			mToggleGermanWord = false;
			/*
			 * The user wants to add a new german word and a translation
			 */
			mTitleView.setEnabled(true);
			mTitleView.addTextChangedListener(new TextWatcher() {

				public void afterTextChanged(Editable arg0) {
					// TODO Auto-generated method stub
				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub

				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					mToggleGermanWord = true;
					toggleSaveButtton();
				}

			});
		} else if (CURRENT_USAGE_MODE == MODE_EDIT_GERMAN_WORD) {
			/*
			 * the user has clicked a german word which he wants to edit.
			 */
			mTitleView.setEnabled(true);
			// mTitleLabelView.setVisibility(View.GONE);
			findViewById(R.id.translation_label).setVisibility(View.GONE);
			findViewById(R.id.dialect_label).setVisibility(View.GONE);
			mSpinner.setVisibility(View.GONE);
			mTranslation.setVisibility(View.GONE);
			saveButton.setEnabled(true);
			mTitleView.setText(mDb.getGermanWord(mGermanWordId));
		}
		;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		// storeTranslation();
	}

	private void populateSpinner() {
		mCantonsCursor = mDb.getCantons();
		String[] from = new String[] { "_id", "name" };
		int[] to = new int[] { R.id.icon, R.id.text };
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				R.layout.word_spinner_entry, mCantonsCursor, from, to);
		adapter.setViewBinder(new WordsViewBinder());
		mSpinner.setAdapter(adapter);
	}

	private void storeTranslation() {
		// TODO Auto-generated method stub
		b = mDb.saveTranslation(mGermanWordId, mTranslation.getText()
				.toString(), mRegionSelected);
		setResult(b ? RESULT_OK : RESULT_CANCELED);
	}

	private void addGermanWord() {
		// TODO Auto-generated method stub
		mGermanWordId = mDb.addGermanWord(mTitleView.getText().toString());
		setResult(b ? RESULT_OK : RESULT_CANCELED);
	}

	private void updateGermanWord() {
		// TODO Auto-generated method stub
		mDb.updateGermanWord(mGermanWordId, mTitleView.getText().toString());
		setResult(b ? RESULT_OK : RESULT_CANCELED);
	}

	private void updateTranslation() {
		// TODO Auto-generated method stub
		b = mDb.updateTranslation(mTranslationId, mTranslation.getText()
				.toString(), mRegionSelected);
		setResult(b ? RESULT_OK : RESULT_CANCELED);
	}

	/*
	 * checks whether the send button is allowed to be enabled
	 */
	private void toggleSaveButtton() {
		saveButton.setEnabled(mToggleGermanWord && mToggleTranslation);
	}
}
