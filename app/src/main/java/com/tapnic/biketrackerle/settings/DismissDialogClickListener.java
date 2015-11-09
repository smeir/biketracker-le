package com.tapnic.biketrackerle.settings;

import android.content.DialogInterface;


class DismissDialogClickListener implements DialogInterface.OnClickListener {
    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
