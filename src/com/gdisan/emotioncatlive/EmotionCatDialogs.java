package com.gdisan.emotioncatlive;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;

/**
 * Class provides some dialogs.
 * 
 * @author semen.tolushkin
 */
public class EmotionCatDialogs {
	
	public static Dialog create(Context context, int id) {
		final Dialog dialog;

		if (id == context.getResources().getInteger(R.integer.dialog_no_sd_card)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(context.getResources().getString(R.string.no_sd_card))
					.setCancelable(true).setPositiveButton(
							context.getResources().getString(R.string.ok),
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
		} else
			if (id == context.getResources().getInteger(R.integer.dialog_about)) {
				dialog = new Dialog(context);
				dialog.setTitle(context.getResources().getString(
						R.string.dialog_about_title));
				dialog.setContentView(R.layout.dialogabout);
				dialog.setCancelable(true);

				Button dialogButton = (Button) dialog.findViewById(R.id.buttondialog);
				dialogButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						dialog.cancel();
					}
				  }
				);
			}
		else
			dialog = null;
		
		return dialog;
	}
	
}
