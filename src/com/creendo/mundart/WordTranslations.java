package com.creendo.mundart;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.creendo.mundart.data.MundartDb;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class WordTranslations extends Activity {
	
	private TextView mTitleView;
	private ListView mTranslationsView;
	
	private MundartDb mDb;
    private Cursor mTranslationsCursor;
    private SimpleCursorAdapter translations;
    private Long mRowId;
    private Long mGermanWordId;
    private Integer mRowType;
    
    private static final int EDIT_ID = Menu.FIRST;
    private static final int ACTIVITY_EDIT = 0;
    
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.word_translations);
		
		/*
         * load admob banner
         */
        AdView adView = new AdView(this, AdSize.BANNER, Mundart.MY_AD_UNIT_ID);
        LinearLayout layout = (LinearLayout)findViewById(R.id.ad_translations);
        layout.addView(adView);
        adView.loadAd(Mundart.mAdRequest);
        
		/*
		 * load other GUI elements
		 */
		mTitleView = (TextView) findViewById(R.id.title);
		mTranslationsView = (ListView) findViewById(R.id.translations);
		
		mDb = new MundartDb(this);
        mDb.open();
        
        /*
         * get the row id (user selection)
         * check if it is contained in the saved instance state. if not, try to get it from
         * the extras.
         * NOTE: this is just the row id, which means that the corresponding word could be either
         * a german word or a dialect word. in order to find out, we will check on the KEY_TYPE
         */
        Bundle extras = getIntent().getExtras();
        mRowId = (savedInstanceState == null) ? null : (Long) savedInstanceState.getSerializable(MundartDb.KEY);
        if (mRowId == null) {
            mRowId = extras != null ? extras.getLong(MundartDb.KEY) : null;
        }
        /*
         * get the row type (german word vs dialect word)
         */
        mRowType = (savedInstanceState == null) ? null : (Integer) savedInstanceState.getSerializable(MundartDb.KEY_TYPE);
        if (mRowType == null) {
            mRowType = extras != null ? extras.getInt(MundartDb.KEY_TYPE) : null;
        }
        
        if (mRowType == MundartDb.TYPE_GERMAN_WORD){
        	mGermanWordId = mRowId;
        } else if (mRowType == MundartDb.TYPE_DIALECT_WORD){
        	mGermanWordId = mDb.getGermanWordId(mRowId);
        }
        
        // show the title (german word)
     	mTitleView.setText(mDb.getGermanWord(mGermanWordId));
    	
        populateTranslations();
    
	}
	
	/**
	 * Method to get the translations
	 */
	private void populateTranslations() {
		mTranslationsCursor = mDb.fetchTranslations(mGermanWordId);
		// Create an array to specify the fields we want to display in the list (only TITLE)
    	String[] from = new String[]{MundartDb.KEY_DIALECTS, MundartDb.KEY_TRANSLATIONS};
		// and an array of the fields we want to bind those fields to (in this case just text1)
    	int[] to = new int[]{R.id.icon, R.id.text};
    	translations = new SimpleCursorAdapter(this, R.layout.word_list_entry, mTranslationsCursor, from, to);
    	translations.setViewBinder(new WordsViewBinder());
    	mTranslationsView.setAdapter(translations); 
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, EDIT_ID, 0, R.string.menu_edit);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case EDIT_ID:
        	Intent i = new Intent(this, WordEdit.class);
        	/*
        	 * tell WordEdit.class that this word is a german word and give it the german word id
        	 * 
    		 * further, tell WordEdit.class that this call is originated from the translation list.
    		 * this means that the word edit class will create a new translation
    		 */
        	i.putExtra(MundartDb.KEY, mGermanWordId);
        	i.putExtra(MundartDb.KEY_TYPE, MundartDb.TYPE_GERMAN_WORD);
        	i.putExtra(WordEdit.KEY_USAGE_MODE, WordEdit.MODE_ADD_TRANSLATION);
        	
        	startActivityForResult(i, ACTIVITY_EDIT);        
            //return true;
        default:
        	break;
        }
        
        return super.onMenuItemSelected(featureId, item);
    }
    
    /* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		populateTranslations();
	}
}
