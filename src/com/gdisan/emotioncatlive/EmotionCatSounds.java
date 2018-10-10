package com.gdisan.emotioncatlive;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Container class for sounds and vibration (singleton).
 * 
 * @author semen.tolushkin
 */
public class EmotionCatSounds {
	
	private static final String TAG = "EmotionCatSounds";
	
	private static EmotionCatSounds instance = null;

	private Context mContext;
	// Cursor to manage available sounds
	private Cursor mCursor;
	
	// Tool for sound recording
	private final MediaRecorder mRecorder = new MediaRecorder();
	// Max duration of record
	// in milliseconds
	private static final int MAX_DURATION = 0;
	private long startRecordTime;
	private String path;
	private String mTitle;
	
	// Variables for SoundPool
	private static final int MUSIC_STREAMS = 2;
	private final SoundPool sSoundPool = new SoundPool(MUSIC_STREAMS, AudioManager.STREAM_MUSIC, 0);
	private final int[] lastStreams = new int[MUSIC_STREAMS];
	private int streamIterator = 0;
	private final ArrayList<String> soundPath = new ArrayList<String>();
	private final ArrayList<Integer> soundId = new ArrayList<Integer>();
	private final ArrayList<Long> soundDuration = new ArrayList<Long>();
	// end SoundPool
	
	private Vibrator mVibrator;
	// in milliseconds
	private static final long VIBRATE_INTERVAL = 650;
	
	// Settings
	private float sound = 1.0F;
	private boolean vibration = true;
	private int currentSound = 0;
	
	private int idOfDefaultSound;
		
	

	/** 
	 * @param context Context of application.
	 */
	public static EmotionCatSounds getInstance(Context context) {
		if (instance == null)
			instance = new EmotionCatSounds(context);
		return instance;
	}
	
	private EmotionCatSounds(Context context) {
		Log.d(TAG, "[initialize()]");
		
		mContext = context.getApplicationContext();

		String state = android.os.Environment.getExternalStorageState();
		if (state.equals(android.os.Environment.MEDIA_MOUNTED)) {
			Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
			String[] projection = { MediaStore.Audio.Media.DATA,
					MediaStore.Audio.Media._ID,
					MediaStore.Audio.Media.DURATION,
					MediaStore.Audio.Media.TITLE };
			String path = "'"
					+ Environment.getExternalStorageDirectory()
					+ "/Android/data/"
					+ mContext.getPackageName()
					+ "/files/sounds/" + "%'";
			String selection = MediaStore.Audio.Media.DATA + " LIKE " + path;
			mCursor = mContext.getContentResolver().query(uri, projection,
					selection, null, null);

			check();
			refresh();
			
			// Create default empty file for convenience
			if (mCursor.getCount() == 0)
				try {
					firstRecord();
					//startRecord();
					//stopRecord();
					refresh();
				} catch (IOException e) {
					Log.e(TAG, "[initialize() : IOException]");
				}
		}
		 
		final AssetFileDescriptor fd = mContext.getResources().openRawResourceFd(R.raw.purr3);
		idOfDefaultSound = sSoundPool.load(fd, 1);
		
		mVibrator = (Vibrator) mContext.getSystemService(
				Context.VIBRATOR_SERVICE);

		PreferenceManager.getDefaultSharedPreferences(mContext)
		.registerOnSharedPreferenceChangeListener(oSPCListener);
		loadPreferences();
	}
	
	/**
	 * Check database.
	 * If sound file doesn't exist, corresponding database raw will be deleted.
	 * 
	 * @return true, if some items were deleted.
	 */
	private boolean check() {
		Log.d(TAG, "[check()]");
		
		boolean deleted = false;
		
		mCursor.moveToFirst();
		while (!mCursor.isAfterLast()) {
			final String path = mCursor.getString(mCursor
					.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
			
			final File file = new File(path);
			if (!file.exists()) {
				
				final String fileName = path.substring(path.lastIndexOf("/") + 1);
//				Toast.makeText(sContext, fileName, Toast.LENGTH_SHORT).show();
				// For default sound we create empty file
				if ("default.ogg".equals(fileName)) {
					try {
						file.createNewFile();
					} catch (IOException e) {
						Log.e(TAG, "[check() : IOException]");
					}
				}
				// Other items must be deleted
				else {
					final int id = mCursor.getInt(mCursor
							.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
					final Uri uri = Uri.withAppendedPath(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + id);
					ContentResolver contentResolver = mContext.getContentResolver();
					contentResolver.delete(uri, null, null);
					deleted = true;
				}
			}
			mCursor.moveToNext();
		}
		
		Log.d(TAG, "[check() returns " + deleted + "]");
		
		return deleted;
	}
	
	/**
	 * Method updates cursor after adding or deleting raws in database.
	 */
	public void refresh() {
		Log.d(TAG, "[refresh()]");
		
		if ((mCursor != null) /*&& (sCursor.getCount() > 0)*/) {
			mCursor.requery();
			mCursor.moveToFirst();
			while (!mCursor.isAfterLast()) {
				String path = mCursor.getString(mCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
	            if (!soundPath.contains(path)) {
	            	soundPath.add(path);
	            	soundId.add(sSoundPool.load(path, 1));
	            		            	
	            	int columnIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
	            	
	            	if (columnIndex != -1) {
	            		soundDuration.add((Long) mCursor.getLong(columnIndex));
	            	}
	            	else
	            		soundDuration.add((Long) VIBRATE_INTERVAL);
	            }
	       	    mCursor.moveToNext();
	        }
		}
	}

	public Cursor getCursor() {
		return mCursor;
	}
	
	public int getCurrentSound() {
		return currentSound;
	}
	
	public void setCurrentSound(int newValue) {
		Log.d(TAG, "[setCurrentSound(" + newValue + ")]");
		
		currentSound = newValue;
		final SharedPreferences.Editor edit = PreferenceManager
				.getDefaultSharedPreferences(mContext).edit();
		edit.putInt(mContext.getResources().getString(R.string.prefs_current_sound),
				currentSound);
		edit.commit();
	}
	
	public void stopSound() {
		Log.d(TAG, "[stopSound()]");
		
		for (int streamId = 0; streamId < MUSIC_STREAMS; streamId++) {
			sSoundPool.stop(lastStreams[streamId]);
		}
	}

	public void stopVibrator() {
		Log.d(TAG, "[stopVibrator()]");
		
		mVibrator.cancel();
	}
	
	/**  
	 * init database with empty file
	 *
	 * @return true if success, false otherwise.
	 * @throws IOException 
	 */

	public void firstRecord() throws IOException
		{
		Log.d(TAG, "[firstRecord()]");

		String state = android.os.Environment.getExternalStorageState();
		if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
				throw new IOException(mContext.getResources().getString(
						R.string.no_sd_card));
			}
 
		 String fileName = "";
		 
		 fileName = "default.ogg";
		 mTitle = "default";
 
		path = Environment.getExternalStorageDirectory()
					+ "/Android/data/"
					+ mContext.getPackageName()
					+ "/files/sounds/"
					+ fileName;

		// Make sure the directory we plan to store the recording in exists
		File file = new File(path);
		File directory = file.getParentFile();
		Log.d(TAG, "[firstRecord():new File(path)]");
		if (!directory.exists() && !directory.mkdirs()) {
				throw new IOException(mContext.getResources().getString(
						R.string.path_error));
		}
		file.createNewFile();
 
		ContentValues values = new ContentValues();
		values.put(MediaStore.Audio.Media.DATA, path);
		values.put(MediaStore.Audio.Media.MIME_TYPE, "sound/gp3");
		values.put(MediaStore.Audio.Media.DURATION, 0);
		values.put(MediaStore.Audio.Media.TITLE, mTitle);

		ContentResolver contentResolver = mContext.getContentResolver();
		contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

	}
	
	
	/**  
	 * Start new recording.
	 *
	 * @return true if success, false otherwise.
	 * @throws IOException
	 */
	public boolean startRecord() throws IOException {
		Log.d(TAG, "[startRecord()]");
		
		String state = android.os.Environment.getExternalStorageState();
		if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
			throw new IOException(mContext.getResources().getString(
					R.string.no_sd_card));
		}
		
		// Name preparing
		String lastFilePath = "-1";
		Long lastFileIndex;
		// Try to get last file in sound directory and cut number from it
		if (mCursor.getCount() > 0) {
			mCursor.moveToLast();
			lastFilePath = mCursor.getString(mCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
			// Find number in file name
			lastFilePath = lastFilePath.substring(
					lastFilePath.lastIndexOf("cat_sound") + "cat_sound".length(),
					lastFilePath.lastIndexOf(".ogg"));
		}
		
		// String to number
		try {
			lastFileIndex = Long.decode(lastFilePath);
		}
		// File exists, but it isn't "cat_sound%.ogg"
		// So we must create "cat_sound1.ogg"
		catch (NumberFormatException e) {
			lastFileIndex = (long) 0;
		}
		
		String fileName = "";
		if (lastFileIndex == -1) {
			fileName = "default.ogg";
			mTitle = "default";
		} else {
			fileName = "cat_sound" + (lastFileIndex + 1) + ".ogg";
			mTitle = fileName;
		}
		path = Environment.getExternalStorageDirectory()
				+ "/Android/data/"
				+ mContext.getPackageName()
				+ "/files/sounds/"
				+ fileName;

		// Make sure the directory we plan to store the recording in exists
		File directory = new File(path).getParentFile();
		if (!directory.exists() && !directory.mkdirs()) {
			throw new IOException(mContext.getResources().getString(
					R.string.path_error));
		}

		boolean success = false;
		while (!success) {
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mRecorder.setOutputFile(path);
			mRecorder.setMaxDuration(MAX_DURATION);
			try {
				mRecorder.prepare();
				success = true;
			} catch (IOException ioe) {
				mRecorder.reset();
				success = false;
			}
		}
		mRecorder.start();
		startRecordTime = SystemClock.elapsedRealtime();
		return true;
	}

	/**
	 * Stop a recording that has been previously started.
	 * Method creates database record.
	 */
	public void stopRecord() {
		Log.d(TAG, "[stopRecord()]");
		
		mRecorder.stop();
		long soundDuration = SystemClock.elapsedRealtime() - startRecordTime;
		
		ContentValues values = new ContentValues();
		values.put(MediaStore.Audio.Media.DATA, path);
		values.put(MediaStore.Audio.Media.MIME_TYPE, "sound/gp3");
		values.put(MediaStore.Audio.Media.DURATION, soundDuration);
		values.put(MediaStore.Audio.Media.TITLE, mTitle);

		ContentResolver contentResolver = mContext.getContentResolver();
		contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
	}
	
	/**
	 * Play sound with id = currentSound.
	 * Turn on vibration.
	 */
	public void play() {
		Log.d(TAG, "[play() AND currentSound = " + currentSound + "]");
		
		if ((currentSound != 0)
			&&
			((mCursor != null) && (mCursor.getCount() > 1))) {
			if (sound > 0.0F) {
				
//				Toast.makeText(sContext, ((Integer)soundId.size()).toString() + "   " + ((Integer)soundDuration.size()).toString() + "   " + ((Integer)soundPath.size()).toString(), Toast.LENGTH_LONG).show();
				
				final Integer currentStreamId = sSoundPool.play(soundId.get(currentSound),
						sound, sound, 1, 0, 1.0F);
				lastStreams[streamIterator] = currentStreamId;
				streamIterator = (streamIterator + 1) % MUSIC_STREAMS;
			}
			
			if (vibration)
				mVibrator.vibrate(soundDuration.get(currentSound));
		}
		else {
			if (sound > 0.0F) {
				final Integer currentStreamId = sSoundPool.play(idOfDefaultSound,
						sound, sound, 1, 0, 1.0F);
				lastStreams[streamIterator] = currentStreamId;
				streamIterator = (streamIterator + 1) % MUSIC_STREAMS;
			}
			
			if (vibration)
				mVibrator.vibrate(VIBRATE_INTERVAL);
		}
	}
	
	private int checkPosition(int position) {
		return ((position >= mCursor.getCount()) || (position < 0)) ? 0
				: position;
	}
	
	public void loadPreferences() {
		Log.d(TAG, "[loadPreferences()]");
		
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		final Resources resources = mContext.getResources();
		
		oSPCListener.onSharedPreferenceChanged(prefs,
				resources.getString(R.string.prefs_sound));
		oSPCListener.onSharedPreferenceChanged(prefs,
				resources.getString(R.string.prefs_vibration));
		oSPCListener.onSharedPreferenceChanged(prefs,
				resources.getString(R.string.prefs_current_sound));
	}
	
	/**
	 * Remove sound and corresponding raw in database.
	 * 
	 * @param _position number of item to delete.
	 */
	public void removeSound(int _position) {
		Log.d(TAG, "[removeSound(" + _position + ")]");
		
		final int position = checkPosition(_position);
		if ((mCursor.getCount() > 1) && (position != 0)) {
			if (position >= 0) {
				mCursor.moveToPosition(position);
				final int id = mCursor.getInt(mCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
				final Uri uri = Uri.withAppendedPath(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + id);
				mContext.getContentResolver().delete(uri, null, null);
				final String path = mCursor.getString(mCursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
				
				sSoundPool.unload(soundId.get(position));
				soundId.remove((int)position);
				soundDuration.remove((int)position);
				soundPath.remove(path);
				final File file = new File(path);
				file.delete();
			}
		}
	}

	private SharedPreferences.OnSharedPreferenceChangeListener oSPCListener 
			= new SharedPreferences.OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			Log.d(TAG, "onSharedPreferenceChanged(" + key + ")");
		
			final Resources resources = mContext.getResources();
			if (resources.getString(R.string.prefs_sound).equals(key)) {
				final int _sound = sharedPreferences.getInt(key, 50);
				sound = (float) _sound / 100;
			} else
				if (resources.getString(R.string.prefs_vibration).equals(key)) {
					vibration = sharedPreferences.getBoolean(key, true);
			} else
			if (resources.getString(R.string.prefs_current_sound).equals(key)) {
				currentSound = sharedPreferences.getInt(key, 0);
				// currentSound can be out of bounds after changing sdcard
				if (mCursor != null) {
					if ((currentSound < 0) || (currentSound >= mCursor.getCount())) {
						currentSound = 0;
					}
				}
				else {
					currentSound = 0;
				}
			}
		}
	
	};
	
}
