package com.gdisan.emotioncatlive;

import com.gdisan.emotioncatlive.EmotionCatGallery;
import com.gdisan.emotioncatlive.R;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Screen with some settings.
 * @author semen.tolushkin
 */
public class EmotionCatSettings extends PreferenceActivity {
	
	private final String TAG = "EmotionCatSettings";
	private final int ACTIVITY_GALLERY = 0;
	
	private EmotionCatGallery mECGallery;
	
	private Resources resources;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "[onCreate()]");
		
		super.onCreate(savedInstanceState);
		resources = getResources();
		addPreferencesFromResource(R.xml.preferences);
		mECGallery = EmotionCatGallery.getInstance(getApplicationContext());
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		Log.d(TAG, "[onPreferenceTreeClick(...)]");
		
		if (resources.getString(R.string.prefs_current_sound).equals(
				preference.getKey())) {
			
			if (android.os.Environment.MEDIA_MOUNTED
					.equals(android.os.Environment.getExternalStorageState())) {
				final Intent intent = new Intent(this, EmotionCatSelectSound.class);
				startActivity(intent);
				
				return true;
			}
			// No sdcard
			showDialog(resources.getInteger(R.integer.dialog_no_sd_card));
		} else
		
		if (resources.getString(R.string.prefs_current_id).equals(
				preference.getKey())) {
				
				showDialog(resources.getInteger(R.integer.dialog_gallery));	
				
				return true;
		}
		
		if (resources.getString(R.string.prefs_about).equals(
				preference.getKey())) {
				
				showDialog(resources.getInteger(R.integer.dialog_about));	
				
				return true;
		}
		
		return false;
	}
	
	@Override
	public Dialog onCreateDialog(int id) {
		Log.d(TAG, "[onCreateDialog(" + id + ")]");
		
		final Dialog dialog;
		
		if (id == resources.getInteger(R.integer.dialog_no_sd_card)) {
			dialog = EmotionCatDialogs.create(this, resources.getInteger(R.integer.dialog_no_sd_card));
		} else
		if (id == resources.getInteger(R.integer.dialog_gallery)) {
			dialog = new Dialog(this);
			
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialoggallery);
			dialog.getWindow().setLayout(
					this.getWindow().getAttributes().width,
					LayoutParams.WRAP_CONTENT);

			final EmotionCatGallery eGallery = EmotionCatGallery.getInstance(this); 

			final Gallery gallery = (Gallery) dialog.findViewById(R.id.gallerycat);
			
			final Button button = (Button) dialog
					.findViewById(R.id.dialoggallerybutton);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_PICK);
					intent.addCategory(Intent.CATEGORY_DEFAULT);
					intent.setType("vnd.android.cursor.dir/image");
					startActivityForResult(intent, ACTIVITY_GALLERY);
					
				}
			});
			
			gallery.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View v,
						int position, long id) {
					if (position == gallery.getSelectedItemPosition()) {
						dialog.cancel();
						mECGallery.setCurrentId(gallery.getSelectedItemPosition());
					}
				}
			});
			
			gallery.setAdapter(eGallery.getImageAdapter());
			gallery.setSelection(eGallery.getCurrentId());

		} else 
			if (id == resources.getInteger(R.integer.dialog_about)) {
				dialog = EmotionCatDialogs.create(this, resources.getInteger(R.integer.dialog_about));
			}
			else
			
			  dialog = null;
				
		return dialog;
	}
	
	/**
	 * Get picture from gallery.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_GALLERY) {
			if (resultCode == RESULT_OK) {
				dismissDialog(getResources().getInteger(R.integer.dialog_gallery));
				mECGallery.setPicture(data);
			}
		}
	}
	
}
