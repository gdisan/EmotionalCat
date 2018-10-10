package com.gdisan.emotioncatlive;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.DialogInterface;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class EmotionCatSelectSound extends Activity {
	
	private final String TAG = "EmotionCatSelectSound";
	
	private EmotionCatSounds mECSounds = null;
	
	private Context mContext;
	private boolean recording = false;
	private ListView listView;
	
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "[onCreate()]");
		
		mContext = this;
		mECSounds = EmotionCatSounds.getInstance(getApplicationContext());
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectsound);
			
		final Cursor cursor = mECSounds.getCursor();
		final ListAdapter adapter = new SimpleCursorAdapter(mContext,
				R.layout.sounditem, cursor,
				new String[] {MediaStore.Audio.Media.TITLE}, new int[] {R.id.sounditem}) {
		};
		
		listView = (ListView) findViewById(R.id.selectsoundlistview);
		listView.setAdapter(adapter);
		listView.setItemsCanFocus(false);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setItemChecked(mECSounds.getCurrentSound(), true);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				mECSounds.setCurrentSound(listView.getCheckedItemPosition());
				mECSounds.play();
			}
			
		});
				
		final Button recordButton = (Button) findViewById(R.id.selectsoundrecordbutton);
		recordButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (recording) {}
				else {
					String state = android.os.Environment.getExternalStorageState();
                    if (state.equals(android.os.Environment.MEDIA_MOUNTED)) 
                    {
					final ProgressDialog dialog = new ProgressDialog(mContext);
					dialog.setCancelable(false);
					dialog.setMessage(mContext.getResources().getString(R.string.dialog_record_message));
					dialog.setButton(ProgressDialog.BUTTON_NEUTRAL,
							mContext.getResources().getString(R.string.dialog_record_button),
							new DialogInterface.OnClickListener() {
						
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (recording) {
										int count = listView.getCount();
										mECSounds.stopRecord();
										mECSounds.refresh();
										recording = false;
										listView.setItemChecked(count, true);
										dialog.cancel();
									}	
								}
								
					});
					try {
						if (mECSounds.startRecord()) {
							recording = true;
							dialog.show();
						}
					} catch (IOException e) {					
						throw new RuntimeException(e);
					}
				}
                    
				}
			}
		});
		
		final Button deleteButton = (Button) findViewById(R.id.selectsounddeletebutton);
		deleteButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final int position = listView.getCheckedItemPosition();
				if (position >= 0) {
					mECSounds.removeSound(position);
					mECSounds.refresh();
					
					if (position == 0)
						listView.setItemChecked(position, true);
					else					
						listView.setItemChecked(position - 1, true);
				}
			}
		});
	}
	
	@Override
	public void onStop() {
		Log.d(TAG, "[onStop()]");
		
		super.onStop();
		
		// May be remove this?...
		if (recording) {
			final int count = listView.getCount();
			mECSounds.stopRecord();
			mECSounds.refresh();
			recording = false;
			listView.setItemChecked(count, true);
		}
		
		mECSounds.setCurrentSound(listView.getCheckedItemPosition());
	}
	
}
