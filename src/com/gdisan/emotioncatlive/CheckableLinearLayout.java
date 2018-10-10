package com.gdisan.emotioncatlive;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

/**
 * 
 * This class is useful for using inside of ListView that needs to have checkable items.
 * 
 */
public class CheckableLinearLayout extends LinearLayout implements Checkable {
	
    private CheckedTextView mCheckBox;
    	
    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
	}
    
    @Override
    protected void onFinishInflate() {
    	super.onFinishInflate();
		int childCount = getChildCount();
		for (int i = 0; i < childCount; ++i) {
			View v = getChildAt(i);
			if (v instanceof CheckedTextView) {
				mCheckBox = (CheckedTextView) v;
			}
		}    	
    }
    
    @Override 
    public boolean isChecked() {
        return mCheckBox != null ? mCheckBox.isChecked() : false; 
    }
    
    @Override 
    public void setChecked(boolean checked) {
    	if (mCheckBox != null) {
    		mCheckBox.setChecked(checked);
    	}
    }
    
    @Override 
    public void toggle() { 
    	if (mCheckBox != null) {
    		mCheckBox.toggle();
    	}
    }
    
}
