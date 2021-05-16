package app.kidswarmup;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

public class PasswordDialog {

    public static void show(Context ctx, String pwd) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Enter password");
        // Set up the input
        final EditText input = new EditText(ctx);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        // Set up the buttons
        builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String txt = input.getText().toString();
                dialog.dismiss();
                if (txt.equals(pwd)) {
                    Activity mainActivity = getActivity(ctx);
                    Intent i = new Intent(mainActivity, SettingsActivity.class);
                    mainActivity.startActivity(i);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public static Activity getActivity(Context context) {
        if (context == null)
            return null;
        if (context instanceof ContextWrapper) {
            if (context instanceof Activity)
                return (Activity) context;
            return getActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

}
