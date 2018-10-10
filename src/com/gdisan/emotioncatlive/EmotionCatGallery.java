package com.gdisan.emotioncatlive;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * Container class for images (singleton).
 * 
 * @author semen.tolushkin
 */
public class EmotionCatGallery {
	
	private static final String TAG = "EmotionCatGallery";
	
	private static EmotionCatGallery instance = null;

	private final Context mContext;
	private final ImageAdapter mAdapter;
	// Static pictures array.
	private final ArrayList<Integer> imageIds = new ArrayList<Integer>();
	// Thumbnails of pictures in res/drawable stored in memory.
	// !Cause of it number of pre-installed pictures must be small.
	private final ArrayList<Bitmap> imageThumbnails = new ArrayList<Bitmap>();
	
	private int currentId = 0;
	
	private Bitmap mBitmap = null;
	private String mUri = null;
	
	private final int screenWidth;
	private final int screenHeight;
	
	// How many times the images in the gallery are smaller than the screen.
	private static final float DC = 2.4F;
	
	
	
	/** 
	 * @param context Context of application.
	 */
	public static EmotionCatGallery getInstance(Context context) {
		if (instance == null)
			instance = new EmotionCatGallery(context);
		return instance;
	}
	
	private EmotionCatGallery(Context context) {
		Log.d(TAG, "[constructor]");
		
		mContext = context;
		
		DisplayMetrics metrics = new DisplayMetrics();
		((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getMetrics(metrics);
		screenWidth = metrics.widthPixels;
		screenHeight = metrics.heightPixels;

		// Static pictures initializing
		final Resources resources = mContext.getResources();
		final Class<R.drawable> res = R.drawable.class;
		for (Field field : res.getFields()) {
			if ("cat".equals(field.getName().subSequence(0, 3))) {
				try {
					final int code = field.getInt(null);
					imageIds.add(code);
					imageThumbnails.add(Bitmap.createScaledBitmap(
							BitmapFactory.decodeResource(resources, code),
							(int) (screenWidth / DC), (int) (screenHeight / DC), true));
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "[constructor : IllegalArgumentException]");
				} catch (IllegalAccessException e) {
					Log.e(TAG, "[constructor : IllegalAccessException]");
				}
			}
		}
		
		currentId = PreferenceManager.getDefaultSharedPreferences(mContext).getInt(
				mContext.getResources().getString(R.string.prefs_current_id), 0);
		mUri = PreferenceManager.getDefaultSharedPreferences(mContext).getString(
				mContext.getResources().getString(R.string.prefs_uri), "");
		
		if (currentId == -1 && !"".equals(mUri))
			if (!loadPicture())
				currentId = 0;
		
		mAdapter = new ImageAdapter(mContext);
	}

	/**
	 * Method returns number of pictures stored in internal memory.
	 * @return number of static pictures.
	 */
	public int getStaticCount() {
		return imageIds.size();
	}
	/**
	 * Method returns full number of pictures in gallery, including those on external drive.
	 * @return number of pictures.
	 */
	public int getCount() {
		return mAdapter.getCount();
	}
	
	public ImageAdapter getImageAdapter() {
		return mAdapter;
	}

	/**
	 * Check the position for outing the bounds.
	 * @param position
	 * @return 0, if position is out of bounds.
	 */
	private int checkPosition(int position) {
		return ((position >= getCount()) || (position < 0)) ? 0
				: position;
	}

	public class ImageAdapter extends BaseAdapter {
		public ImageAdapter(Context context) {
			TypedArray a = mContext
					.obtainStyledAttributes(R.styleable.MyGallery);
			mGalleryItemBackground = a.getResourceId(
					R.styleable.MyGallery_android_galleryItemBackground, 0);
			a.recycle();
		}

		@Override
		public int getCount() {
			return imageIds.size();
		}

		@Override
		public Object getItem(int _position) {
			int position = checkPosition(_position);
			return position;
		}

		@Override
		public long getItemId(int _position) {
			int position = checkPosition(_position);
			return position;
		}

		@Override
		public View getView(int _position, View convertView, ViewGroup parent) {
			final int position = checkPosition(_position);

			ImageView imageView = null;

			if (convertView == null) {
				
				imageView = new ImageView(mContext);
//				imageView.setImageResource(imageIds.get(position));
				imageView.setImageBitmap(imageThumbnails.get(position));
			
				imageView.setScaleType(ImageView.ScaleType.FIT_XY);
				imageView.setLayoutParams(new Gallery.LayoutParams(
						(int) (screenWidth / DC), (int) (screenHeight / DC)));
				imageView.setBackgroundResource(mGalleryItemBackground);
				
			} else {
				imageView = (ImageView) convertView;
			}
			
			return imageView;
		}

		int mGalleryItemBackground;
	}

	/**
	 * Return bitmap object from gallery.
	 * 
	 * @param _position number of item to get picture of.
	 * @return BitmapDrawable object.
	 */
	public Bitmap getBitmap(int position) {
		Log.d(TAG, "[getBitmap(" + position + ")]");
		
		if (position == -1)
			if (mBitmap != null)
				return mBitmap;
			else
				position = 0;

		Bitmap bitmap = null;

		if (position < imageIds.size()) {
			final Resources resources = mContext.getResources();
			bitmap = BitmapFactory.decodeResource(resources, imageIds
					.get(position));
		} 

		return bitmap;
	}

	/**
	 * Set new picture to background and save changes to preferences.
	 * 
	 * @param newValue id to set picture of.
	 */
	public void setCurrentId(int newValue) {
		Log.d(TAG, "[setCurrentId(" + newValue + ")]");
		
		currentId = newValue;
		final SharedPreferences.Editor edit = PreferenceManager
				.getDefaultSharedPreferences(mContext).edit();
		edit.putInt(mContext.getResources().getString(R.string.prefs_current_id),
				currentId);
		edit.commit();
	}
	
	public int getCurrentId() {
		return currentId;
	}
	
	public void setPicture(Intent data) {
		Log.d(TAG, "setPicture()");
		
		mUri = data.getDataString();
		
		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				return loadPicture();
			}
			
			@Override
			protected void onPostExecute(Boolean result) {
				if (result)
					setCurrentId(-1);
			}
			
		}.execute();
		
	}
	
	private boolean loadPicture() {
		boolean result = false;
		
		Uri uri = Uri.parse(mUri);
		
		Log.d(TAG, "loadPicture(" + uri + ")");
		
		// Load image , resize it and rotate if needed.
		Bitmap _bitmap = null;
		
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(uri,
				new String[] { MediaStore.MediaColumns.DATA }, null, null, null);
		
		if (cursor != null) {
			cursor.moveToFirst();
		
			String fname = cursor.getString(cursor.getColumnIndexOrThrow
					(MediaStore.MediaColumns.DATA));
		
			try {
				FileInputStream in = new FileInputStream(fname);
				_bitmap = BitmapFactory.decodeStream(in);
				if (in != null) {
					in.close();
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, "[addCat() : FileNotFoundException]");
			} catch (IOException e) {
				Log.e(TAG, "[addCat() : IOException]");
			} finally {
				;
			}
		
			boolean rotate = false;
			int _width = _bitmap.getWidth();
			int _height = _bitmap.getHeight();
//			Toast.makeText(mContext, _width + "   " + _height, Toast.LENGTH_LONG).show();
			if (_width > _height) rotate = true;

			// Resizing picture to screen size
			if (!rotate) {
				mBitmap = Bitmap.createScaledBitmap(_bitmap, screenWidth, screenHeight, true);			
			}
			else {
				// (rotate = true) >>> (_width and _height are swapped)
				// Rotate image if it has horizontal orientation
				_bitmap = Bitmap.createScaledBitmap(_bitmap, screenHeight, screenWidth, true);
				final Matrix matrix = new Matrix();
				matrix.postRotate(90);
				mBitmap = Bitmap.createBitmap(_bitmap, 0, 0, screenHeight, screenWidth,
						matrix, true);
			}
			
			result = true;
		}

		return result;
	}
	
}
