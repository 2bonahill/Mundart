/*
 * Some notes on user definition (UD_STATUS_XX):
 * 
 * 1: word or translation is defined by user
 * 2: same as 1 (temporary)
 * 3: word or translation is defined by user and alredy sent to server
 */

package com.creendo.mundart.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class MundartDb {
	
	public static Context mContext;
	
	private String TAG = "Dialects";
	
	private Cursor mCursor;
	
	private static String DB_PATH;
	private static String DB_NAME = "SQLiteDialectstDb.sqlite";
	private static int DB_VERSION = 1;
	private DialectsDbHelper mDbHelper;
	private SQLiteDatabase mDb;
	
	public static String KEY = "_id";
	public static String KEY_WORDS = "word";
	public static String KEY_TRANSLATIONS = "translation";
	public static String KEY_DIALECTS = "dialect";
	public static String KEY_TYPE = "type";
	public static String TABLE_GERMAN_WORDS = "german_words";
	public static int TYPE_DIALECT_WORD = 0;
	public static int TYPE_GERMAN_WORD = 1;
	public static final int ICON_GERMAN_WORD = 999;
	
	public static final int UD_STATUS_SENT = 3;
	public static final int UD_STATUS_UD = 1;
	
	private String query;
	
	private Cursor mCantons;
	
	/**
	 * The database helper class
	 */
	private static class DialectsDbHelper extends SQLiteOpenHelper {

		public DialectsDbHelper(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}
		
		public DialectsDbHelper(Context context, String dbName) {
			super(context, dbName, null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase arg0) {}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
			try {
				MundartDb.copyDb();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.v("Mundart", "Failed to copy the DB");
			}
		}
		
	}
	
	/**
	 * Constructor method. Only stores the context locally
	 * 
	 * @param ctx
	 */
	public MundartDb(Context ctx){
		this.mContext = ctx;
	}
	
	/**
	 * Two methods for opening and closing the db.
	 * 
	 * As we use our own database, we have to copy it from the Assets folder 
	 * to the Android's Database folder.
	 *  
	 * TODO: only copy if not existing
	 * @return DialectsDb
	 */
	public MundartDb open() throws SQLException {
		DB_PATH = readAndroidDbPath();
		if (!checkDataBase()){
			try {
				copyDb();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mDbHelper = new DialectsDbHelper(mContext);
		mDb = mDbHelper.getReadableDatabase();
		return this;
    }
	
	/**
	 * Method called when DB is closed
	 */
	public void close() {
        mDbHelper.close();
    }
    
    /**
     * Fetches the words with a filter
     * @param filter
     * @return cursor
     */
    public Cursor fetchWords(String filter) {
    	query  = "SELECT _id, word, "+ ICON_GERMAN_WORD +" AS dialect, " + TYPE_GERMAN_WORD + " AS type, user_defined ";
		query += "FROM "+ TABLE_GERMAN_WORDS + " ";
		if (filter != null) query += "WHERE word like '%" + filter + "%' ";
		query += "UNION ";
		query += "SELECT _id, translation as word, dialect, " + TYPE_DIALECT_WORD + " AS type, user_defined ";
		query += "FROM translations ";
		if (filter != null) query += "WHERE word like '%" + filter + "%' ";
		query += "ORDER BY word";
    	return mDb.rawQuery(query, null);
    }
    
    /**
     * Fetches the user defined words
     * 
     * we take the translations with user_defined = 1 or 2
     * 
     * @param filter
     * @return cursor
     */
    public Cursor fetchUserWords(String filter) {
    	query  = "SELECT _id, word, "+ ICON_GERMAN_WORD +" AS dialect, " + TYPE_GERMAN_WORD + " AS type, user_defined ";
		query += "FROM "+ TABLE_GERMAN_WORDS + " ";
		query += "WHERE user_defined > 0 ";
		if (filter != null) query += "AND word like '%" + filter + "%' ";
		query += "UNION ";
		query += "SELECT _id, translation as word, dialect, " + TYPE_DIALECT_WORD + " AS type, user_defined ";
		query += "FROM translations ";
		query += "WHERE user_defined > 0 ";
		if (filter != null) query += "AND word like '%" + filter + "%' ";
		query += "ORDER BY word";
    	return mDb.rawQuery(query, null);
	}
    
    /**
     * Fetches the user defined words
     * 
     * we just take the ones with user_defined = 1, as
     * if user_defined == 2, the translation has already be sent.
     * 
     * @param filter
     * @return cursor
     */
    public Cursor fetchUserTranslations4Sync() {
    	query  = "SELECT g.word, t.translation, d.code ";
		query += "FROM translations t ";
		query += "INNER JOIN german_words g ON g._id = t.german_word ";
		query += "LEFT JOIN dialects d ON t.dialect = d._id ";
		query += "WHERE t.user_defined = 1 OR t.user_defined = 2";			
		
		return mDb.rawQuery(query, null);
	}
    
    public void setUserWordsSent(){
    	/*
    	 * set to sent for translations
    	 */
    	query  = "UPDATE translations ";
    	query += "SET user_defined = " + UD_STATUS_SENT + " ";
    	query += "WHERE user_defined = 1 OR user_defined = 2";
    	mDb.execSQL(query);
    	/*
    	 * and for german words
    	 */
    	query  = "UPDATE german_words ";
    	query += "SET user_defined = " + UD_STATUS_SENT + " ";
    	query += "WHERE user_defined = 1 OR user_defined = 2";
    	mDb.execSQL(query);
    }
    
    /**
     * Fetches the words with a filter
     * @param filter
     * @return cursor
     */
    public Cursor fetchTranslations(long id) {
    	
    	query  = "SELECT g.word, t._id AS _id, d.code, t.translation, d._id AS dialect ";
		query += "FROM german_words g ";
		query += "LEFT JOIN translations t ON g._id = t.german_word ";
		query += "LEFT JOIN dialects d ON t.dialect = d._id ";
		query += "WHERE g._id = " + id;			
		
		return mDb.rawQuery(query, null);
    }
    
    /**
     * Get the ID of the german word by the translation id
     */
    public Long getGermanWordId(Long translationID){
    	query  = "SELECT t.german_word AS germanWordId ";
    	query += "FROM translations t ";
    	query += "WHERE _id = " + translationID;
		
    	mCursor =  mDb.rawQuery(query, null);
    	mCursor.moveToFirst();
    	return mCursor.getLong(mCursor.getColumnIndex("germanWordId"));
    }
    
    /**
     * return the details of a translation by the translation id
     */
    public Cursor getTranslationDetails(Long translationID){
    	query  = "SELECT t.german_word AS germanWordId, t.translation AS translation, t.dialect As dialect ";
    	query += "FROM translations t ";
    	query += "WHERE _id = " + translationID;
		
    	return mDb.rawQuery(query, null);
    }
    
    /**
     * Get the german word by its ID
     */
    public String getGermanWord(Long germanWordID){
    	query = "SELECT word FROM german_words WHERE _id = " + germanWordID;
    	mCursor = mDb.rawQuery(query, null);
    	if (mCursor.getCount()>0) {
    		mCursor.moveToFirst();
    		return mCursor.getString(mCursor.getColumnIndex("word"));
    	} else {
    		return "Nicht gefunden";
    	}
    }
    
    /**
     * Helper method to get the path of Android's DB Folder.
     * Open/create new dummy db, get it's path and close it afterwards.
     * 
     * @return String
     */
    private String readAndroidDbPath(){
    	String dummyDBName = "dummyDb.sqlite";
    	DialectsDbHelper dummyDbHelper = new DialectsDbHelper(mContext, dummyDBName);
		SQLiteDatabase dummyDb = dummyDbHelper.getReadableDatabase();
		String path = this.mContext.getDatabasePath(dummyDBName).getAbsolutePath();
		dummyDb.close();
		dummyDbHelper.close();
		/*
		 *  path +- = /data/data/com.creendo.dialects/databases/dummyDb
		 *  -> remove "dummyDb" and we are done
		 */
		return path.substring(0, path.length() - dummyDBName.length());
    }
    
    
    /**
     * A method to get the cantons
     * 
     * @return Cursor
     */
    public Cursor getCantons(){
    	if (mCantons == null){
    		mCantons = mDb.rawQuery("SELECT _id, name FROM dialects ORDER BY _id", null);
    	}	
    	return mCantons;
    }
    
    /**
     * A helper method to clean directories
     */
    private void helper(){
    	String sDir = "/data/data/com.creendo.dialects/databases/";
    	File dir = new File(sDir);
    	File f;
    	String[] children = dir.list();
    	if (children == null) {
    	    // Either dir does not exist or is not a directory
    		Log.v(TAG, "nichts gelšscht");
    	} else {
    	    for (int i=0; i<children.length; i++) {
    	        // Get filename of file or directory
    	    	f = new File(sDir + children[i]);
    	    	Log.v(TAG, "file: " + f.getAbsolutePath());
    	    	Log.v(TAG, f.delete() ? "erfolgreich gelšscht" : "nicht gelšscht");
    	    }
    	}
    }
    
    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){
    	SQLiteDatabase checkDB = null;
    	try{
    		String myPath = DB_PATH + DB_NAME;
    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    	}catch(SQLiteException e){
    		//database does't exist yet.
    	}
    	if(checkDB != null){
    		checkDB.close();
    	}
    	return checkDB != null ? true : false;
    }

	public boolean saveTranslation(Long mGermanWordId, String translation, String dialect) {
		// TODO Auto-generated method stub
		query  = "INSERT INTO translations(german_word, translation, dialect, user_defined) ";
		query += "VALUES (";
		query += mGermanWordId + ", '";
		query += translation;
		query += "', (SELECT _id from dialects WHERE name = '"+ dialect +"'), ";
		query += UD_STATUS_UD + ")";
		
		mDb.execSQL(query);
    	return true;
	}
	
	public boolean updateTranslation(Long translationId, String translation, String dialect) {
		// TODO Auto-generated method stub
		query  = "UPDATE translations ";
		query += "SET translation = '" + translation + "', ";
		query += "dialect = (SELECT _id from dialects WHERE name = '"+ dialect +"'), ";
		query += "user_defined = "+ UD_STATUS_UD + " ";
		query += "WHERE _id = " + translationId;
		
		mDb.execSQL(query);
    	return true;
	}
	
	/*
     * This method inserts a new german word and returns its new id
     */
	public Long addGermanWord(String newGermanWord) {
		// get newest id
		query  = "INSERT INTO german_words (word, user_defined) ";
		query += "VALUES ('"+ newGermanWord+"', " + UD_STATUS_UD + ")";
		mDb.execSQL(query);
		
		// get the id of the new inserted word
		query = "SELECT _id FROM german_words WHERE word = '" + newGermanWord + "'";
		mCursor = mDb.rawQuery(query, null);
		if (mCursor.getCount() > 0){
			mCursor.moveToFirst();
			Log.v("Mundart", "supa");
			return mCursor.getLong(0);
		} else {
			return 0L;
		}
	}

	public void updateGermanWord(Long mGermanWordId, String germanWord) {
		// we first update the german word
		query  = "UPDATE german_words SET word = '"+ germanWord +"', user_defined = " + UD_STATUS_UD + " ";
		query += "WHERE _id = " + mGermanWordId + " ";
		query += "AND user_defined > 0";
		mDb.execSQL(query);
		/*
		 * and reset the corresponding translations to user_defined 1
		 * such that they will be sent in case uf an update
		 */
		query = "UPDATE translations ";
		query += "SET user_defined = "+ UD_STATUS_UD + " ";
		query += "WHERE german_word = " + mGermanWordId + " ";
		query += "AND user_defined > 0";
		mDb.execSQL(query);
	}
	
	/**
     * Private method to copy the Database from Assets folder to Anroid's DB path
     * @throws IOException 
     */
    public static void copyDb() throws IOException{
    	//helper();
    	
    	// delete the db
    	File f = new File(DB_PATH, DB_NAME);
    	f.delete();
    	
    	InputStream in = mContext.getAssets().open(DB_NAME);
    	String outFileName = DB_PATH + DB_NAME;
    	OutputStream out = new FileOutputStream(outFileName);
    	
    	byte[] buffer = new byte[1024];
    	int length;
    	while ((length = in.read(buffer))>0){
    		out.write(buffer, 0, length);
    	}
    	
    	out.flush();
    	out.close();
    	in.close();
    }
    
    /**
     * Helper Method to fetch all tables of db
     * 
     * @return cursor
     */
    private Cursor fetchTablesFromDbMaster(){
    	return mDb.rawQuery("SELECT * FROM sqlite_master WHERE type='table'", null);
    }
	
}
