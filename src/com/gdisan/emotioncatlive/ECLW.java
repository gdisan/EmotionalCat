package com.gdisan.emotioncatlive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
//import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class ECLW extends WallpaperService {
	
	private static final String TAG = "ECLW";
	
	// Home app of the phone is at main screen mode, not in application menu.
	private static boolean atMainScreen = true;
	
	private EmotionCatSounds mECSounds = null;
	
	
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate()");
		
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine() {
		Log.d(TAG, "onCreateEngine()");
		
		return new ECEngine();
	}
		
	
	
	
	
    class ECEngine extends Engine
    	implements SharedPreferences.OnSharedPreferenceChangeListener {
    	
    	private final String TAG = "ECLW.ECEngine";
    	    	
    	private EmotionCatGallery mECGallery = null;
    	
    	// Current picture.
    	private Bitmap bitmap = null;
    	
    	// private MotionEvent lastEvent = null;
    	
    	private final Paint mPaint = new Paint();

    	// Need for dynamic drawing.
    	// Now it's not used.
    	/*
    	private final Handler mHandler = new Handler();
    	private final Runnable mDraw = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        */
        
        private boolean mVisible = false;
        
        private SharedPreferences mPrefs = null;
        private String mPrefsCurrentId;

        // private final float	TOUCH_ERROR = 3.0F; 
		// In milliseconds...
		private final int TOUCH_INTERVAL = 650;
		private final long CHECK_INTERVAL = 30;
		private final long TIMER_INTERVAL = 130;
		private long currentTime;
		private long lastTime;
		// Signal about touching.
		private boolean onTouch = false;
	
		// Timer for periodic starting new purrs.
		private final CountDownTimer timer = new CountDownTimer(TIMER_INTERVAL, CHECK_INTERVAL) {
			
			@Override
			public void onFinish() {
				this.start();
			}
			
			@Override
			public void onTick(long millisUntilFinished) {
				currentTime = SystemClock.elapsedRealtime();
				if (onTouch) {
					if (!mVisible || !atMainScreen) onTouch = false;
					else
					if (currentTime - lastTime > TOUCH_INTERVAL) {
						onTouch = false;
						lastTime = currentTime;
					
						mECSounds.play();
					}
				}
			}
		};
        
        
        
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			Log.d(TAG, "onSharedPreferenceChanged(" + key + ")");
			
			if (key.equals(mPrefsCurrentId)) {
				final int currentId = mPrefs.getInt(mPrefsCurrentId, 0);
				bitmap = mECGallery.getBitmap(currentId);
				drawFrame();
			}
		}
		
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
        	Log.d(TAG, "onCreate()");
        	
            super.onCreate(surfaceHolder);
            
            mECSounds = EmotionCatSounds.getInstance(getApplicationContext());
        	mECGallery = EmotionCatGallery.getInstance(getApplicationContext());
        	
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        	mPrefsCurrentId = getResources().getString(R.string.prefs_current_id);
            onSharedPreferenceChanged(mPrefs, mPrefsCurrentId);
            
            setTouchEventsEnabled(true);
            
            timer.start();
            
            drawFrame();
        }

        @Override
        public void onDestroy() {
        	Log.d(TAG, "onDestroy()");
        	
            super.onDestroy();
//			mHandler.removeCallbacks(mDraw);
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
            
            timer.cancel();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
        	Log.d(TAG, "onVisibilityChanged(" + visible + ")");
        	
            mVisible = visible;
            /*
            if (visible) {
                drawFrame();
            } else {
            	mHandler.removeCallbacks(mDraw);
            }
            */
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        	Log.d(TAG, "onSurfaceChanged(format = " + format + ", width = " + width + ", height = " + height + ")");
        	
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
        	Log.d(TAG, "onSurfaceCreated()");
        	
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
        	Log.d(TAG, "onSurfaceDestroyed()");
        	
            super.onSurfaceDestroyed(holder);
			mVisible = false;
//			mHandler.removeCallbacks(mDraw);
        }
        
        public Bundle onCommand (String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
        	if ("android.wallpaper.tap".equals(action)) {
        		onTouch = true;
        	}
        	
        	return null;
        }
        
        /**
		 * Touching screen forces cat to purr.
		 * Set onTouch variable, which is checked in timer to play sounds.
		 */
        /*
		@Override
		public void onTouchEvent(MotionEvent event) {
//			Log.d(TAG, "onTouchEvent(" + event + ")");
			
			boolean accepted = false;
			// Touch the screen
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				lastEvent = MotionEvent.obtain(event);
				accepted = true;
			}
			else
			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				// Palm the screen
				if (lastEvent != null)
				if (Math.abs(lastEvent.getX() - event.getX()) > TOUCH_ERROR ||
						Math.abs(lastEvent.getY() - event.getY()) > TOUCH_ERROR) {
					lastEvent = MotionEvent.obtain(event);
					accepted = true;
				}
			}
			
			if (accepted) onTouch = true;
			
			super.onTouchEvent(event);
		}
		*/
		
		public void drawFrame() {
			Log.d(TAG, "drawFrame()");
			
			final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                
				final Rect frame = holder.getSurfaceFrame();
                final Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
                Log.e(TAG, "frame: " + frame);
                
                if (c != null) c.drawBitmap(bitmap, src, frame, mPaint);

            } finally {
                if (c != null) {
                	holder.unlockCanvasAndPost(c);
                }
            }

            /*
            mHandler.removeCallbacks(mDraw);
            if (mVisible) {
                mHandler.postDelayed(mDraw, 1000 / 10);
            }
            */
		}
   	
    }
    
    public static class MainWindowReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			atMainScreen = !intent.getBooleanExtra("state", false);
		}
    	
    }

}