package org.softeg.slartus.forpdaplus.classes;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ContextThemeWrapper;

import org.softeg.slartus.forpdaplus.MyApp;
import org.softeg.slartus.forpdaplus.R;

/*
 * Created by slinkin on 08.08.13.
 */
public class AlertDialogBuilder extends AlertDialog.Builder {
    public AlertDialogBuilder(Context context) {
        //super(new ContextThemeWrapper(context, MyApp.getInstance().isWhiteTheme() ? R.style.Theme_White : R.style.Theme_Black));
        super(context);
    }
}
